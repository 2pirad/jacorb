/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2002 Gerald Brose
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
package org.jacorb.util.threadpool;
/**
 * ThreadPool.java
 *
 *
 * Created: Fri Jun  9 15:09:01 2000
 *
 * @author Nicolas Noffke
 * $Id: ThreadPool.java,v 1.6 2002-07-01 07:54:17 nicolas Exp $
 */
import org.jacorb.util.Debug;

public class ThreadPool
{
    private int max_threads = 0;
    private int max_idle_threads = 0;

    private int total_threads = 0;
    private int idle_threads = 0;

    private ThreadPoolQueue job_queue = null;
    private ConsumerFactory factory = null;

    public ThreadPool( ConsumerFactory factory ) 
    {
        this( new LinkedListQueue(),
              factory, 
              10,
              10 );
    }

    public ThreadPool( ConsumerFactory factory,
                       int max_threads,
                       int max_idle_threads) 
    {
        this.job_queue = new LinkedListQueue();
        this.factory = factory;
        this.max_threads = max_threads;
        this.max_idle_threads = max_idle_threads;        
    }

    public ThreadPool( ThreadPoolQueue job_queue,
                       ConsumerFactory factory,
                       int max_threads,
                       int max_idle_threads) 
    {
        this.job_queue = job_queue;
        this.factory = factory;
        this.max_threads = max_threads;
        this.max_idle_threads = max_idle_threads;        
    }

    protected synchronized Object getJob()
    {
        /*
         * This tells the newly idle thread to exit,
         * because there are already too much idle 
         * threads.
         */
        if (idle_threads >= max_idle_threads)
        {
            Debug.output( Debug.DEBUG1 | Debug.TOOLS,
                          "(Pool)[" + idle_threads + "/" + total_threads + 
                          "] Telling thread to exit (too many idle)" );
            
            total_threads--;
            return null;
        }
    
        idle_threads++;
    
        Debug.output( Debug.DEBUG1 | Debug.TOOLS,
                      "(Pool)[" + idle_threads + "/" + total_threads + 
                      "] added idle thread" );

        while( job_queue.isEmpty() )
        {
            try
            {
                Debug.output( Debug.DEBUG1 | Debug.TOOLS,
                              "(Pool)[" + idle_threads + "/" + total_threads + 
                              "] job queue empty" );
                wait();
            }
            catch( InterruptedException e )
            {
                Debug.output( Debug.IMPORTANT | Debug.TOOLS, e );
            }
        }

        idle_threads--;

        Debug.output( Debug.DEBUG1 | Debug.TOOLS,
                      "(Pool)[" + idle_threads + "/" + total_threads + 
                      "] removed idle thread (job scheduled)" );

        return job_queue.removeFirst();
    }

    public synchronized void putJob( Object job )
    {
        job_queue.add(job);
        notifyAll();
    
        if ((idle_threads == 0) && 
            (total_threads < max_threads))
        {
            createNewThread();
        }
    }

    private void createNewThread()
    {
        Debug.output( Debug.DEBUG1 | Debug.TOOLS,
                      "(Pool)[" + idle_threads + "/" + total_threads + 
                      "] creating new thread" );

        Thread t = new Thread( new ConsumerTie( this, factory.create() ));
        t.setDaemon( true );
        t.start();

        total_threads++;
    }
} // ThreadPool






