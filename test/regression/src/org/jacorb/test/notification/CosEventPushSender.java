package org.jacorb.test.notification;

import org.omg.CORBA.Any;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventChannelAdmin.EventChannel;
import org.omg.CosEventChannelAdmin.EventChannelHelper;
import org.omg.CosEventChannelAdmin.ProxyPushConsumer;
import org.omg.CosEventChannelAdmin.SupplierAdmin;
import org.omg.CosEventComm.PushSupplierPOA;

import org.jacorb.util.Debug;

import junit.framework.TestCase;
import org.apache.avalon.framework.logger.Logger;

/**
 * @author Alphonse Bendt
 * @version $Id: CosEventPushSender.java,v 1.4 2004-02-09 16:26:42 alphonse.bendt Exp $
 */

public class CosEventPushSender extends PushSupplierPOA implements TestClientOperations, Runnable {

    Logger logger_ = Debug.getNamedLogger(getClass().getName());

    boolean connected_;
    boolean sent_;
    boolean error_;
    ProxyPushConsumer myConsumer_;
    Any event_;
    NotificationTestCase testCase_;

    CosEventPushSender(NotificationTestCase testCase, Any event) {
        event_ = event;
        testCase_ = testCase;
    }

    public void disconnect_push_supplier() {
        connected_ = false;
    }

    public boolean isConnected() {
        return connected_;
    }

    public boolean isEventHandled() {
        return sent_;
    }

    public boolean isError() {
        return error_;
    }

    public void connect(org.omg.CosNotifyChannelAdmin.EventChannel channel,
                        boolean useOrSemantic) throws AlreadyConnected {

        testCase_.assertNotNull(channel);
        EventChannel _channel = EventChannelHelper.narrow(channel);
        testCase_.assertNotNull(_channel);

        SupplierAdmin _admin = _channel.for_suppliers();
        testCase_.assertNotNull(_admin);

        myConsumer_ = _admin.obtain_push_consumer();
        testCase_.assertNotNull(myConsumer_);

        myConsumer_.connect_push_supplier(_this(testCase_.getSetup().getORB()));

        connected_ = true;
    }
    public void shutdown() {
        myConsumer_.disconnect_push_consumer();
    }

    // Implementation of java.lang.Runnable

    /**
     * Describe <code>run</code> method here.
     *
     */
    public void run() {
        try {
            myConsumer_.push(event_);

            event_ = null;
            sent_ = true;
        } catch (Throwable t) {
            error_ = true;
            logger_.fatalError("while push", t);
        }
    }

}
