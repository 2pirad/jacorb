package org.jacorb.orb.connection;

/**
 * ConnectionListener.java
 *
 *
 * Created: Thu Oct  4 15:56:02 2002
 *
 * @author Nicolas Noffke
 * @version $Id: ConnectionListener.java,v 1.2 2002-03-19 09:25:24 nicolas Exp $
 */

public interface ConnectionListener 
{   
    public void connectionClosed();
    public void connectionTimedOut();
    public void streamClosed();
}// ConnectionListener
