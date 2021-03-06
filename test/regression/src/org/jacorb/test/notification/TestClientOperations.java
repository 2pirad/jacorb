package org.jacorb.test.notification;

import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventChannelAdmin.TypeError;
import org.omg.CosNotifyChannelAdmin.AdminLimitExceeded;
import org.omg.CosNotifyChannelAdmin.AdminNotFound;
import org.omg.CosNotifyChannelAdmin.EventChannel;
import org.omg.CosNotifyFilter.FilterNotFound;

/**
 * @author Alphonse Bendt
 * @version $Id: TestClientOperations.java,v 1.5 2006-02-25 14:22:35 alphonse.bendt Exp $
 */

public interface TestClientOperations 
{
    boolean isConnected();
    boolean isEventHandled();
    boolean isError();
    void connect(EventChannel eventChannel,
                 boolean useOrSemantic)
        throws AlreadyConnected,
               TypeError,
               AdminLimitExceeded,
               AdminNotFound;

    void shutdown() throws FilterNotFound;
}
