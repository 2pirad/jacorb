package org.jacorb.notification.servant;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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

import org.omg.CORBA.BooleanHolder;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyChannelAdmin.SequenceProxyPullConsumerOperations;
import org.omg.CosNotifyChannelAdmin.SequenceProxyPullConsumerPOATie;
import org.omg.CosNotifyComm.SequencePullSupplier;
import org.omg.PortableServer.Servant;

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.interfaces.Message;

/**
 * @author Alphonse Bendt
 * @version $Id: SequenceProxyPullConsumerImpl.java,v 1.8 2004-05-09 19:01:42 alphonse.bendt Exp $
 */

public class SequenceProxyPullConsumerImpl
    extends StructuredProxyPullConsumerImpl
    implements SequenceProxyPullConsumerOperations
{
    private SequencePullSupplier sequencePullSupplier_;

    ////////////////////////////////////////

    public ProxyType MyType() {
        return ProxyType.PULL_SEQUENCE;
    }


    public void disconnect_sequence_pull_consumer()
    {
        dispose();
    }


    public synchronized void connect_sequence_pull_supplier( SequencePullSupplier sequencePullSupplier )
        throws AlreadyConnected
    {
        assertNotConnected();

        sequencePullSupplier_ = sequencePullSupplier;

        connectClient(sequencePullSupplier);

        startTask();
    }


    /**
     * override superclass impl
     */
    protected void runPullEventInternal()
        throws InterruptedException,
               Disconnected
    {
        BooleanHolder _hasEvent = new BooleanHolder();
        _hasEvent.value = false;
        StructuredEvent[] _events = null;

        try
        {
            pullSync_.acquire();

            _events = sequencePullSupplier_.try_pull_structured_events( 1, _hasEvent );
        }
        finally
        {
            pullSync_.release();
        }

        if ( _hasEvent.value )
        {
            for ( int x = 0; x < _events.length; ++x )
            {
                Message msg =
                    getMessageFactory().newMessage( _events[ x ], this );

                getTaskProcessor().processMessage( msg );
            }
        }
    }


    protected void disconnectClient()
    {
        stopTask();
        sequencePullSupplier_.disconnect_sequence_pull_supplier();
        sequencePullSupplier_ = null;
    }


    public synchronized Servant getServant()
    {
        if ( thisServant_ == null )
        {
            thisServant_ = new SequenceProxyPullConsumerPOATie( this );
        }

        return thisServant_;
    }
}
