package org.jacorb.util.threadpool;
/**
 * Consumer.java
 *
 *
 * Created: Thu Dec 21 10:52:28 2000
 *
 * @author Nicolas Noffke
 * $Id: Consumer.java,v 1.3 2001-03-19 11:10:29 brose Exp $
 */

public interface Consumer  
{
    public void doWork( Object job );
} // Consumer






