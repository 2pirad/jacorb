package org.jacorb.notification;

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.CosNotifyChannelAdmin.EventChannel;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventChannelAdmin.TypeError;
import org.omg.CosNotifyChannelAdmin.AdminLimitExceeded;
import org.omg.CosNotifyFilter.FilterNotFound;

/**
 * TestClientOperations.java
 *
 *
 * Created: Mon Dec 09 18:43:07 2002
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: TestClientOperations.java,v 1.1 2003-01-14 11:46:07 alphonse.bendt Exp $
 */

public interface TestClientOperations {
    
    public boolean isConnected();
    public boolean isEventHandled();
    public boolean isError();
    public void connect(ORB orb, POA poa, EventChannel eventChannel) throws AlreadyConnected, TypeError, AdminLimitExceeded;
    public void shutdown() throws FilterNotFound;

}// TestClientOperations
