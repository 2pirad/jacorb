package org.jacorb.notification;

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

import org.jacorb.notification.interfaces.EventConsumer;
import org.jacorb.notification.interfaces.Message;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventChannelAdmin.TypeError;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotifyChannelAdmin.ConnectionAlreadyActive;
import org.omg.CosNotifyChannelAdmin.ConnectionAlreadyInactive;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.NotConnected;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyChannelAdmin.StructuredProxyPushSupplierOperations;
import org.omg.CosNotifyChannelAdmin.StructuredProxyPushSupplierPOATie;
import org.omg.CosNotifyComm.StructuredPushConsumer;
import org.omg.PortableServer.Servant;
import org.jacorb.notification.queue.EventQueue;
import org.omg.CosNotification.UnsupportedQoS;

/**
 * StructuredProxyPushSupplierImpl.java
 *
 *
 * Created: Sun Nov 03 22:41:38 2002
 *
 * @author Alphonse Bendt
 * @version $Id: StructuredProxyPushSupplierImpl.java,v 1.12 2003-08-25 21:00:46 alphonse.bendt Exp $
 */

public class StructuredProxyPushSupplierImpl
            extends AbstractProxy
            implements StructuredProxyPushSupplierOperations,
            EventConsumer
{

    private StructuredPushConsumer pushConsumer_;
    protected EventQueue pendingEvents_;
    protected boolean active_;
    protected boolean enabled_;

    public StructuredProxyPushSupplierImpl( ConsumerAdminTieImpl myAdminServant,
                                            ApplicationContext appContext,
                                            ChannelContext channelContext,
                                            PropertyManager adminProperties,
                                            PropertyManager qosProperties,
                                            Integer key ) throws UnsupportedQoS
    {
        super( myAdminServant,
               appContext,
               channelContext,
               adminProperties,
               qosProperties,
               key );

        setProxyType( ProxyType.PUSH_STRUCTURED );
        enabled_ = true;

        pendingEvents_ = appContext.newEventQueue(qosProperties);
    }

    public void deliverEvent( Message event )
    {
        logger_.debug( "deliverEvent connected="+connected_+" active="+active_+" enabled="+enabled_ );

        if ( connected_ )
        {
            try
            {
                if ( active_ && enabled_ )
                {
                    pushConsumer_.push_structured_event( event.toStructuredEvent() );
                    event.dispose();
                }
                else
                {
                    // not enabled
                    pendingEvents_.put( event );

                }
            }
            catch ( Disconnected d )
            {
                connected_ = false;
                logger_.warn( "push failed - PushConsumer was disconnected" );
            }
        }
        else
        {
            logger_.debug( "Not connected" );
        }
    }

    public void connect_structured_push_consumer( StructuredPushConsumer consumer )
        throws AlreadyConnected,
               TypeError
    {
        if ( connected_ )
        {
            throw new AlreadyConnected();
        }

        pushConsumer_ = consumer;
        connected_ = true;
        active_ = true;
    }

    public void disconnect_structured_push_supplier()
    {
        dispose();
    }

    synchronized public void suspend_connection()
    throws NotConnected, ConnectionAlreadyInactive
    {
        if ( !connected_ )
        {
            throw new NotConnected();
        }

        if ( !active_ )
        {
            throw new ConnectionAlreadyInactive();
        }

        active_ = false;
    }

    public void deliverPendingEvents() throws NotConnected
    {
        try {
            if ( !pendingEvents_.isEmpty() )
                {
                    Message[] _events = pendingEvents_.getAllEvents(true);
                    for (int x=0; x<_events.length; ++x) {
                        try {
                            pushConsumer_.push_structured_event( _events[x].toStructuredEvent() );
                        } catch (Disconnected e) {
                            connected_ = false;
                            throw new NotConnected();
                        } finally {
                            _events[x].dispose();
                        }
                    }
                }
        } catch (InterruptedException e) {}

    }

    public void resume_connection() throws NotConnected, ConnectionAlreadyActive
    {
        if ( !connected_ )
        {
            throw new NotConnected();
        }

        if ( active_ )
        {
            throw new ConnectionAlreadyActive();
        }

        deliverPendingEvents();
        active_ = true;
    }

    protected void disconnectClient()
    {
        if ( connected_ )
        {
            if ( pushConsumer_ != null )
            {
                try {
                    pushConsumer_.disconnect_structured_push_consumer();
                } catch (Exception e) {
                    logger_.warn("Error disconnecting consumer: ", e);
                }
                pushConsumer_ = null;
                connected_ = false;
            }
        }
    }

    public ConsumerAdmin MyAdmin()
    {
        return ( ConsumerAdmin ) myAdmin_.getThisRef();
    }

    public List getSubsequentFilterStages()
    {
        return CollectionsWrapper.singletonList( this );
    }

    public EventConsumer getEventConsumer()
    {
        return this;
    }

    public boolean hasEventConsumer()
    {
        return true;
    }

    synchronized public void dispose()
    {
        super.dispose();
        disconnectClient();
    }

    synchronized public void enableDelivery()
    {
        enabled_ = true;
    }

    synchronized public void disableDelivery()
    {
        enabled_ = false;
    }

    public Servant getServant()
    {
        if ( thisServant_ == null )
        {
            synchronized ( this )
            {
                if ( thisServant_ == null )
                {
                    thisServant_ = new StructuredProxyPushSupplierPOATie( this );
                }
            }
        }

        return thisServant_;
    }

    public boolean hasPendingEvents() {
        return !pendingEvents_.isEmpty();
    }

} // StructuredProxyPushSupplierImpl
