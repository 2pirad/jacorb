package org.jacorb.orb.connection;

/**
 * ConnectionListener.java
 *
 *
 * Created: Thu Oct  4 15:56:02 2001
 *
 * @author Nicolas Noffke
 * @version $Id: ConnectionListener.java,v 1.1 2001-10-04 13:58:00 jacorb Exp $
 */

public interface ConnectionListener 
{   
    public void connectionClosed();
    public void connectionTimedOut();
    public void streamClosed();
}// ConnectionListener
