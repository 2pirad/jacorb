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


import org.omg.CosNotifyChannelAdmin.SequenceProxyPushConsumerOperations;
import org.omg.CosNotifyChannelAdmin.SupplierAdmin;
import org.apache.log4j.Logger;
import org.omg.CosNotifyComm.SequencePushSupplier;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosEventComm.Disconnected;
import org.jacorb.notification.framework.EventDispatcher;
import java.util.List;
import java.util.Collections;

/**
 * SequenceProxyPushConsumerImpl.java
 *
 *
 * Created: Sat Jan 11 17:06:27 2003
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: SequenceProxyPushConsumerImpl.java,v 1.1 2003-01-14 11:46:07 alphonse.bendt Exp $
 */

public class SequenceProxyPushConsumerImpl 
    extends StructuredProxyPushConsumerImpl 
    implements SequenceProxyPushConsumerOperations {

    SequencePushSupplier mySequencePushSupplier_;

    public SequenceProxyPushConsumerImpl(ApplicationContext appContext,
					   ChannelContext channelContext,
					   SupplierAdminTieImpl supplierAdminServant, 
					   SupplierAdmin supplierAdmin,
					   Integer key) {
	super(
	      appContext, 
	      channelContext,
	      supplierAdminServant,
	      supplierAdmin,
	      key);
    }
    
    protected void disconnectClient() {
	if (connected_) {
	    if (mySequencePushSupplier_ != null) {
		connected_ = false;
		mySequencePushSupplier_.disconnect_sequence_push_supplier();
		mySequencePushSupplier_ = null;
	    }

	}
    }

    public void connect_sequence_push_supplier(SequencePushSupplier supplier) throws AlreadyConnected {
	if (connected_) {
	    throw new AlreadyConnected();
	}
	connected_ = true;
	mySequencePushSupplier_ = supplier;
    }

    public void push_structured_events(StructuredEvent[] events) throws Disconnected {
	for (int x=0; x<events.length; ++x) {
	    push_structured_event(events[x]);
	}
    }

    public void disconnect_sequence_push_consumer() {
	dispose();
    }

}// SequenceProxyPushConsumerImpl
