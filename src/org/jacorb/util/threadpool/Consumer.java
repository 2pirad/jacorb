package org.jacorb.util.threadpool;
/**
 * Consumer.java
 *
 *
 * Created: Thu Dec 21 10:52:28 2000
 *
 * @author Nicolas Noffke
 * $Id: Consumer.java,v 1.4 2002-03-19 09:25:54 nicolas Exp $
 */

public interface Consumer  
{
    public void doWork( Object job );
} // Consumer






