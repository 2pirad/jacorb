package org.jacorb.util.threadpool;
/**
 * ThreadPoolQueue.java
 *
 *
 * Created: Fri Jun  9 15:18:43 2000
 *
 * @author Nicolas Noffke
 * $Id: ThreadPoolQueue.java,v 1.1.1.1 2001-03-17 18:09:05 brose Exp $
 */

public interface ThreadPoolQueue
{
    public boolean add( Object job );
    public Object removeFirst();

    public boolean isEmpty();
} // ThreadPoolQueue
