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

import org.omg.CORBA.ARG_IN;
import org.omg.CORBA.NVList;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Request;
import org.omg.CORBA.TCKind;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventChannelAdmin.TypeError;
import org.omg.CosNotification.EventTypeHelper;
import org.omg.CosNotification.Property;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosTypedEventComm.TypedPushConsumer;
import org.omg.CosTypedNotifyChannelAdmin.TypedProxyPushSupplierHelper;
import org.omg.CosTypedNotifyChannelAdmin.TypedProxyPushSupplierOperations;
import org.omg.CosTypedNotifyChannelAdmin.TypedProxyPushSupplierPOATie;
import org.omg.PortableServer.Servant;

import org.jacorb.notification.AbstractMessage;
import org.jacorb.notification.TypedEventMessage;
import org.jacorb.notification.engine.PushTypedOperation;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;

import java.util.List;
import org.omg.CORBA.TypeCode;
import org.jacorb.notification.*;

/**
 * @author Alphonse Bendt
 * @version $Id: TypedProxyPushSupplierImpl.java,v 1.3 2004-05-11 12:14:55 alphonse.bendt Exp $
 */

public class TypedProxyPushSupplierImpl
    extends AbstractProxySupplier
    implements TypedProxyPushSupplierOperations
{
    private TypedPushConsumer pushConsumer_;

    private org.omg.CORBA.Object typedConsumer_;

    private static final TypeCode TYPE_CODE_VOID =
        ORB.init().get_primitive_tc(TCKind.tk_void);

    private String consumerInterface_;

    public TypedProxyPushSupplierImpl(String consumerInterface) {
        consumerInterface_ = consumerInterface;
    }


    public void disconnect_push_supplier() {
        dispose();
    }


    public void connect_typed_push_consumer(TypedPushConsumer typedPushConsumer)
        throws AlreadyConnected, TypeError {

        logger_.info( "connect typed_push_supplier" );

        assertNotConnected();

        connectClient(typedPushConsumer);

        pushConsumer_ = typedPushConsumer;

        typedConsumer_ = pushConsumer_.get_typed_consumer();

        if (!typedConsumer_._is_a(consumerInterface_)) {
            throw new TypeError();
        }
    }


    public ProxyType MyType() {
        return ProxyType.PUSH_TYPED;
    }

    public boolean hasMessageConsumer() {
        return true;
    }

    public MessageConsumer getMessageConsumer() {
        return this;
    }

    public List getSubsequentFilterStages() {
        return null;
    }


    public org.omg.CORBA.Object activate() {
        return TypedProxyPushSupplierHelper.narrow( getServant()._this_object(getORB()) );
    }


    public void deliverPendingData() {
    }


    public void isIDLAssignable(String ifName) throws IllegalArgumentException {
        if (typedConsumer_._is_a(ifName) ) {
            return;
        }

        if (ifName.indexOf("Pull") > 0) {
            int idx = ifName.indexOf("Pull");

            StringBuffer _nonPullIF = new StringBuffer();
            _nonPullIF.append(ifName.substring(0, idx));
            _nonPullIF.append(ifName.substring(idx + 4));

            if (typedConsumer_._is_a(_nonPullIF.toString())) {
                return;
            }
        }
        throw new IllegalArgumentException();
    }


    public void deliverMessage(Message message) {
        try {
            Property[] _props = message.toTypedEvent();

            String _fullQualifiedOperation;

            if (TypedEventMessage.OPERATION_NAME.equals(_props[0].name)) {
                _fullQualifiedOperation = _props[0].value.extract_string();
            } else if (TypedEventMessage.EVENT_TYPE.equals(_props[0].name)) {
                _fullQualifiedOperation = EventTypeHelper.extract(_props[0].value).type_name;

                String _idlType = EventTypeHelper.extract(_props[0].value).domain_name;

                isIDLAssignable(_idlType);
            } else {
                throw new IllegalArgumentException();
            }

            int _idx = _fullQualifiedOperation.lastIndexOf("::");
            String _operation = _fullQualifiedOperation.substring(_idx + 2);

            Request _request = typedConsumer_._request(_operation);

            NVList _arguments = _request.arguments();

            for (int x=1; x<_props.length; ++x) {
                _arguments.add_value(_props[x].name, _props[x].value, ARG_IN.value);
            }

            _request.set_return_type( TYPE_CODE_VOID );

            try {
                _request.invoke();
            } catch (Throwable t) {
                PushTypedOperation _failedOperation = new PushTypedOperation(_request);

                handleFailedPushOperation(_failedOperation, t);
            }
        } catch (NoTranslationException e) {
            // ignore
            // nothing will be delivered to the consumer

            logger_.info("No Translation possible", e);
        }
    }


    protected void disconnectClient() {
        if (pushConsumer_ != null) {
            pushConsumer_.disconnect_push_consumer();
            pushConsumer_ = null;
        }
    }


    public Servant getServant() {
        if (thisServant_ == null)
            {
                thisServant_ = new TypedProxyPushSupplierPOATie( this );
            }
        return thisServant_;
    }
}
