package org.jacorb.notification.servant;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
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
 */

import java.util.List;

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.CollectionsWrapper;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.engine.PushAnyOperation;

import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.PushConsumer;
import org.omg.CosNotifyChannelAdmin.ConnectionAlreadyActive;
import org.omg.CosNotifyChannelAdmin.ConnectionAlreadyInactive;
import org.omg.CosNotifyChannelAdmin.NotConnected;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplierHelper;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplierOperations;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplierPOATie;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.PortableServer.Servant;

/**
 * @author Alphonse Bendt
 * @version $Id: ProxyPushSupplierImpl.java,v 1.7.2.1 2004-05-09 17:38:44 alphonse.bendt Exp $
 */

public class ProxyPushSupplierImpl
    extends AbstractProxySupplier
    implements ProxyPushSupplierOperations
{
    private PushConsumer pushConsumer_;

    ////////////////////////////////////////

    public ProxyType MyType() {
        return ProxyType.PUSH_ANY;
    }


    public void disconnect_push_supplier()
    {
        dispose();
    }


    protected void disconnectClient()
    {
        pushConsumer_.disconnect_push_consumer();

        pushConsumer_ = null;
    }


    public void deliverMessage(final Message message)
    {
        if (isConnected())
        {
            if (!isSuspended() && isEnabled())
                {
                    try {
                        logger_.debug("pushConsumer.push(Any)");

                        pushConsumer_.push(message.toAny());
                    } catch (Throwable e) {
                        PushAnyOperation _failedOperation =
                            new PushAnyOperation(pushConsumer_, (Message)message.clone());

                        handleFailedPushOperation(_failedOperation, e);
                    }
                }
            else
                {
                    enqueue(message);
                }
        }
        else
        {
            logger_.debug("Not connected");
        }
    }


    public void connect_any_push_consumer(PushConsumer pushConsumer)
        throws AlreadyConnected
    {
        assertNotConnected();

        pushConsumer_ = pushConsumer;

        connectClient(pushConsumer);
    }


    public List getSubsequentFilterStages()
    {
        return CollectionsWrapper.singletonList(this);
    }


    public MessageConsumer getMessageConsumer()
    {
        return this;
    }


    public boolean hasMessageConsumer()
    {
        return true;
    }


    public void deliverPendingData()
    {
        Message[] _events = getAllMessages();

        try {
            for (int x = 0; x < _events.length; ++x)
                {
                    deliverMessage(_events[x]);
                }
        }
        finally {
            for (int x=0; x<_events.length; ++x) {
                _events[x].dispose();
            }
        }
    }



    protected void connectionResumed()
    {
        scheduleDeliverPendingMessagesOperation_.run();
    }


    public synchronized Servant getServant()
    {
        if (thisServant_ == null)
        {
            thisServant_ = new ProxyPushSupplierPOATie(this);
        }
        return thisServant_;
    }


    public org.omg.CORBA.Object activate()
    {
        return ProxyPushSupplierHelper.narrow( getServant()._this_object(getORB()) );
    }
}
