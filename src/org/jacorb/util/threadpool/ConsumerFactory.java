package org.jacorb.util.threadpool;
/**
 * ConsumerFactory.java
 *
 *
 * Created: Thu Dec 21 11:07:04 2000
 *
 * @author Nicolas Noffke
 * $Id: ConsumerFactory.java,v 1.1 2001-03-17 18:09:04 brose Exp $
 */

public interface ConsumerFactory  
{
    public Consumer create();
} // ConsumerFactory
