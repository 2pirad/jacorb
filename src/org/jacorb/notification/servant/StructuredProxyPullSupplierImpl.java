package org.jacorb.notification.servant;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2003 Gerald Brose
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
 *
 */

import java.util.List;

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.CollectionsWrapper;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;

import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.ORB;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotification.EventHeader;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.FixedEventHeader;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotifyChannelAdmin.ProxySupplierHelper;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyChannelAdmin.StructuredProxyPullSupplierOperations;
import org.omg.CosNotifyChannelAdmin.StructuredProxyPullSupplierPOATie;
import org.omg.CosNotifyComm.NotifyPublishHelper;
import org.omg.CosNotifyComm.NotifyPublishOperations;
import org.omg.CosNotifyComm.StructuredPullConsumer;
import org.omg.PortableServer.Servant;

/**
 * @author Alphonse Bendt
 * @version $Id: StructuredProxyPullSupplierImpl.java,v 1.4 2004-02-13 18:30:16 alphonse.bendt Exp $
 */

public class StructuredProxyPullSupplierImpl
            extends AbstractProxySupplier
            implements StructuredProxyPullSupplierOperations
{
    /**
     * undefined StructuredEvent that is returned on unsuccessful pull operations.
     */
    protected static final StructuredEvent undefinedStructuredEvent_;

    // initialize undefinedStructuredEvent_
    static {
        ORB _orb = ORB.init();

        undefinedStructuredEvent_ = new StructuredEvent();
        EventType _type = new EventType();
        FixedEventHeader _fixed = new FixedEventHeader( _type, "" );
        Property[] _variable = new Property[ 0 ];
        undefinedStructuredEvent_.header = new EventHeader( _fixed, _variable );
        undefinedStructuredEvent_.filterable_data = new Property[ 0 ];
        undefinedStructuredEvent_.remainder_of_body = _orb.create_any();
    }

    /**
     * the associated Consumer.
     */
    private StructuredPullConsumer structuredPullConsumer_;

    private NotifyPublishOperations offerListener_;

    ////////////////////////////////////////

    public StructuredProxyPullSupplierImpl( AbstractAdmin myAdminServant,
                                            ChannelContext channelContext)
    {
        super( myAdminServant,
               channelContext );

        setProxyType( ProxyType.PULL_STRUCTURED );
    }

    ////////////////////////////////////////

    public void connect_structured_pull_consumer( StructuredPullConsumer consumer )
        throws AlreadyConnected
    {
        assertNotConnected();

        structuredPullConsumer_ = consumer;

        connectClient(consumer);

        try
        {
            offerListener_ = NotifyPublishHelper.narrow(consumer);
        }
        catch (Throwable t)
        {
            logger_.info("disable offer_change for StructuredPullConsumer");
        }
    }


    public StructuredEvent pull_structured_event()
        throws Disconnected
    {
        Message _message = null;

        try
        {
            _message = getMessageBlocking();

            return _message.toStructuredEvent();
        }
        catch (InterruptedException e)
        {
            return undefinedStructuredEvent_;
        }
        finally
        {
            if (_message != null)
            {
                _message.dispose();
            }
        }
    }


    public StructuredEvent try_pull_structured_event( BooleanHolder hasEvent )
        throws Disconnected
    {
        Message _notificationEvent =
            getMessageNoBlock();

        if (_notificationEvent != null)
        {
            try
            {
                hasEvent.value = true;

                return _notificationEvent.toStructuredEvent();
            }
            finally
            {
                _notificationEvent.dispose();
            }
        }
        else
        {
            hasEvent.value = false;

            return undefinedStructuredEvent_;
        }
    }


    public void disconnect_structured_pull_supplier()
    {
        dispose();
    }


    protected void disconnectClient()
    {
        structuredPullConsumer_.disconnect_structured_pull_consumer();
        structuredPullConsumer_ = null;
    }


    /**
     * PullSupplier always enqueues.
     */
    public void deliverMessage( Message event )
    {
        enqueue( event );
    }


    public List getSubsequentFilterStages()
    {
        return CollectionsWrapper.singletonList( this );
    }


    public MessageConsumer getMessageConsumer()
    {
        return this;
    }


    public boolean hasMessageConsumer()
    {
        return true;
    }


    public void disableDelivery()
    {
        // as no active deliveries are made this can be ignored
    }


    public void enableDelivery()
    {
        // as no active deliveries are made this can be ignored
    }


    public void deliverPendingMessages()
    {
        // as no active deliveries are made this can be ignored
    }


    public synchronized Servant getServant()
    {
        if ( thisServant_ == null )
        {
            thisServant_ = new StructuredProxyPullSupplierPOATie( this );
        }

        return thisServant_;
    }


    public org.omg.CORBA.Object activate()
    {
        return ProxySupplierHelper.narrow(getServant()._this_object(getORB()));
    }


    NotifyPublishOperations getOfferListener()
    {
        return offerListener_;
    }
}
