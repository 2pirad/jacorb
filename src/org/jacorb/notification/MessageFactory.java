package org.jacorb.notification;

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

import org.jacorb.notification.interfaces.AbstractPoolable;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.FilterStage;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.servant.AbstractProxyConsumerI;
import org.jacorb.notification.util.AbstractObjectPool;

import org.omg.CORBA.Any;
import org.omg.CORBA.TypeCode;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotification.StructuredEventHelper;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.Configurable;

/**
 * @author Alphonse Bendt
 * @version $Id: MessageFactory.java,v 1.10 2004-05-06 12:39:59 nicolas Exp $
 */

public class MessageFactory implements Disposable, Configurable
{

    ////////////////////////////////////////

    private AbstractObjectPool anyMessagePool_ =
        new AbstractObjectPool("AnyMessagePool")
        {
            public Object newInstance()
            {
                return new AnyMessage();
            }

            public void activateObject( Object o )
            {
                AbstractPoolable obj = (AbstractPoolable) o;
                obj.reset();
                obj.setObjectPool( this );
            }
        };

    private AbstractObjectPool structuredEventMessagePool_ =
        new AbstractObjectPool("StructuredEventMessagePool")
        {
            public Object newInstance()
            {
                return new StructuredEventMessage();
            }

            public void activateObject( Object o )
            {
                AbstractPoolable obj = (AbstractPoolable) o;
                obj.reset();
                obj.setObjectPool( this );
            }
        };


    public void configure(Configuration conf)
    {
        anyMessagePool_.configure (conf);

        structuredEventMessagePool_.configure(conf);
    }

    public void dispose()
    {
        structuredEventMessagePool_.dispose();

        anyMessagePool_.dispose();
    }

    ////////////////////////////////////////

    // Used by the Proxies

    public Message newMessage( Any any,
                               AbstractProxyConsumerI consumer )
    {
        if (StructuredEventHelper.type().equals(any.type())) {
            // received a StructuredEvent wrapped inside an Any
            // see Spec. 2-11
            return newMessage(StructuredEventHelper.extract(any), consumer);
        } else {
            Message _mesg = newMessage( any );

            _mesg.setInitialFilterStage( consumer.getFirstStage() );

            return _mesg;
        }
    }


    public Message newMessage( StructuredEvent structuredEvent,
                               AbstractProxyConsumerI consumer )
    {
        String _typeName = structuredEvent.header.fixed_header.event_type.type_name;

        if (AnyMessage.TYPE_NAME.equals(_typeName)) {
            // received an Any wrapped inside a StructuredEvent
            // see Spec. 2-11
            return newMessage(structuredEvent.remainder_of_body, consumer);
        } else {
            StructuredEventMessage _mesg =
                ( StructuredEventMessage ) structuredEventMessagePool_.lendObject();

            _mesg.setFilterStage( consumer.getFirstStage() );

            _mesg.setStructuredEventValue( structuredEvent ,
                                           consumer.isStartTimeSupported(),
                                           consumer.isTimeOutSupported());

            return _mesg.getHandle();
        }
    }

    ////////////////////////////////////////

    // used by the Filters

    public Message newMessage( Any any )
    {
        if (StructuredEventHelper.type().equals(any.type())) {
            return newMessage(StructuredEventHelper.extract(any));
        } else {
            AnyMessage _mesg =
                ( AnyMessage ) anyMessagePool_.lendObject();

            _mesg.setAny( any );

            return _mesg.getHandle();
        }
    }


    public Message newMessage( StructuredEvent structuredEvent )
    {
        String _typeName = structuredEvent.header.fixed_header.event_type.type_name;

        if (AnyMessage.TYPE_NAME.equals(_typeName)) {
            return newMessage(structuredEvent.remainder_of_body);
        } else {
            StructuredEventMessage _mesg =
                ( StructuredEventMessage ) structuredEventMessagePool_.lendObject();

            _mesg.setStructuredEventValue( structuredEvent , false, false);

            return _mesg.getHandle();
        }
    }
}
