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

import org.omg.CORBA.BAD_PARAM;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ConnectionAlreadyActive;
import org.omg.CosNotifyChannelAdmin.ConnectionAlreadyInactive;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.NotConnected;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplierHelper;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplierOperations;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplierPOATie;
import org.omg.PortableServer.Servant;
import org.omg.CosNotifyChannelAdmin.ProxyType;

/**
 * @author Alphonse Bendt
 * @version $Id: ProxyPushSupplierImpl.java,v 1.2 2004-01-29 14:22:57 alphonse.bendt Exp $
 */

public class ProxyPushSupplierImpl
    extends AbstractProxySupplier
    implements ProxyPushSupplierOperations
{
    private org.omg.CosEventComm.PushConsumer myPushConsumer_;

    private boolean enabled_;

    private boolean active_;

    ////////////////////////////////////////

    ProxyPushSupplierImpl(AbstractAdmin myAdminServant,
                          ChannelContext channelContext)
        throws UnsupportedQoS
    {
        super(myAdminServant,
              channelContext);

        setProxyType(ProxyType.PUSH_ANY);

        connected_ = false;
        enabled_ = true;
    }

    ////////////////////////////////////////

    public String toString()
    {
        return "<ProxyPushSupplier connected: " + connected_ + ">";
    }


    public void disconnect_push_supplier()
    {
        dispose();
    }


    protected void disconnectClient()
    {
        if (myPushConsumer_ != null)
        {
            myPushConsumer_.disconnect_push_consumer();
            myPushConsumer_ = null;
            connected_ = false;
        }
    }


    public void deliverMessage(Message event)
    {
        if (connected_)
        {
            try
            {
                if (active_ && enabled_)
                {
                    myPushConsumer_.push(event.toAny());

                    event.dispose();
                }
                else
                {
                    enqueue(event);
                }
            }
            catch (Disconnected e)
            {
                connected_ = false;
                logger_.debug("push failed: Not connected");
            }
        }
        else
        {
            logger_.debug("Not connected");
        }
    }


    public void connect_any_push_consumer(org.omg.CosEventComm.PushConsumer pushConsumer)
        throws AlreadyConnected
    {

        if (connected_)
        {
            throw new AlreadyConnected();
        }

        if (pushConsumer == null)
        {
            throw new BAD_PARAM();
        }

        myPushConsumer_ = pushConsumer;
        connected_ = true;
        active_ = true;
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


    synchronized public void suspend_connection()
        throws NotConnected,
               ConnectionAlreadyInactive
    {

        if (!connected_)
        {
            throw new NotConnected();
        }

        if (!active_)
        {
            throw new ConnectionAlreadyInactive();
        }
        active_ = false;
    }


    public void deliverPendingMessages()
        throws NotConnected
    {
        Message[] _events = getAllMessages();

        for (int x = 0; x < _events.length; ++x)
        {
            try
            {
                myPushConsumer_.push(_events[x].toAny());
            }
            catch (Disconnected e)
            {
                connected_ = false;
                throw new NotConnected();
            }
            finally
            {
                _events[x].dispose();
                _events[x] = null;
            }
        }
    }


    public void resume_connection()
        throws NotConnected,
               ConnectionAlreadyActive
    {
        synchronized (this)
        {
            if (!connected_)
            {
                throw new NotConnected();
            }

            if (active_)
            {
                throw new ConnectionAlreadyActive();
            }

            active_ = true;
        }

        deliverPendingMessages();
    }


    synchronized public void enableDelivery()
    {
        enabled_ = true;
    }


    synchronized public void disableDelivery()
    {
        enabled_ = false;
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
