package org.jacorb.trading.util;
import java.util.*;

/**
 * TimeoutThread.java
 *
 *
 * Created: Sat Feb  5 11:45:40 2000
 *
 * @author Nicolas Noffke
 * @version $Id: TimeoutThread.java,v 1.5.4.1 2004-03-30 13:26:15 gerald Exp $
 */

public class TimeoutThread extends Thread {
  private int timeout = 0;
  private TimerListNode last = null;
  private TimerListNode first = null;
  private Hashtable current_nodes = null;

  public TimeoutThread(int timeout){
    this.timeout = timeout;
    last = new TimerListNode();
    first = last;
    current_nodes = new Hashtable();

    start();
  }

  public void run(){
    while(true){
      try{
	//blocks, until node is available
	first = first.getNext();
		
	if (first.wakeup_time <= System.currentTimeMillis())
	  first.doInterrupt();
	else{
	  sleep(Math.abs(first.wakeup_time - 
			 System.currentTimeMillis()));
	  first.doInterrupt();
	}	

      }catch (Exception e){
      }
    }    
  }

  /**
   * Stop the alarm timer.
   */
  public void stopTimer(Thread interruptee){

    TimerListNode _current = (TimerListNode) current_nodes.get(interruptee);
    _current.stopTimer();
  }
    
  /**
   * This method sets a timeout, after wich an interrupt() is scheduled.
   *
   * @param interruptee the thread to interrupt.
   */
  public synchronized void setTimeout (Thread interruptee){
    //create new node
    TimerListNode _new = new TimerListNode(interruptee, timeout +
					   System.currentTimeMillis());
    //hook into list
    last.setNext(_new);
    last = _new;
    current_nodes.put(interruptee, _new);
  }
} // TimeoutThread






