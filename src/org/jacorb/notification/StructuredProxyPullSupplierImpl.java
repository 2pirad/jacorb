package org.jacorb.notification;

import org.apache.log4j.Logger;
import org.omg.CORBA.BooleanHolder;
import org.omg.CORBA.ORB;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyChannelAdmin.StructuredProxyPullSupplierOperations;
import org.omg.CosNotifyComm.StructuredPullConsumer;
import org.omg.PortableServer.POA;
import java.util.LinkedList;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.FixedEventHeader;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.EventHeader;
import java.util.Collections;
import java.util.List;

/*
 *        JacORB - a free Java ORB
 */

/**
 * StructuredProxyPullSupplierImpl.java
 *
 *
 * Created: Tue Nov 05 14:25:49 2002
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: StructuredProxyPullSupplierImpl.java,v 1.1 2002-12-03 23:23:02 alphonse.bendt Exp $
 */

public class StructuredProxyPullSupplierImpl 
    extends ProxyBase 
    implements StructuredProxyPullSupplierOperations, 
	       TransmitEventCapable {

    ConsumerAdminTieImpl myAdminServant_;
    ConsumerAdmin myAdmin_;
    ProxyType myType_ = ProxyType.PULL_STRUCTURED;
    StructuredPullConsumer myConsumer_;
    LinkedList pendingEvents_ = new LinkedList();
    int maxListSize_ = 200;
    static StructuredEvent undefinedStructuredEvent_;

    public StructuredProxyPullSupplierImpl(ApplicationContext appContext, 
					   ChannelContext channelContext,
					   ConsumerAdminTieImpl myAdminServant, 
					   ConsumerAdmin myAdmin) {

	super(appContext, channelContext, Logger.getLogger("Proxy.StructuredProxyPullSupplier"));
	myAdminServant_ = myAdminServant;
	myAdmin_ = myAdmin;

	if (undefinedStructuredEvent_ == null) {
	    synchronized(getClass()) {
		if (undefinedStructuredEvent_ == null) {
		    undefinedStructuredEvent_ = new StructuredEvent();
		    EventType _type = new EventType();
		    FixedEventHeader _fixed = new FixedEventHeader(_type, "");
		    Property[] _variable = new Property[0];
		    undefinedStructuredEvent_.header = new EventHeader(_fixed, _variable);
		    undefinedStructuredEvent_.filterable_data = new Property[0];
		    undefinedStructuredEvent_.remainder_of_body = orb_.create_any();
		}
	    }
	}
    }
    
    public void connect_structured_pull_consumer(StructuredPullConsumer consumer) throws AlreadyConnected {
	if(connected_) {
	    throw new AlreadyConnected();
	}
	connected_ = true;
	myConsumer_ = consumer;
    }

    public ProxyType MyType() {
	return myType_;
    }

    public ConsumerAdmin MyAdmin() {
	return myAdmin_;
    }

    public StructuredEvent pull_structured_event() throws Disconnected {
	StructuredEvent _event = null;
	BooleanHolder _hasEvent = new BooleanHolder();
	while(true) {
	    _event = try_pull_structured_event(_hasEvent);
	    if(_hasEvent.value) {
		return _event;
	    }
	    Thread.yield();
	}
    }

    public StructuredEvent try_pull_structured_event(BooleanHolder hasEvent) throws Disconnected {
	if(!connected_) {
	    throw new Disconnected();
	}
	StructuredEvent _event = null;
	synchronized(pendingEvents_) {
	    int _listSize = pendingEvents_.size();
	    if(_listSize > 0) {
		_event = (StructuredEvent)pendingEvents_.getFirst();
		pendingEvents_.remove(_event);
		hasEvent.value = true;
		return _event;
	    } else {
		hasEvent.value = false;
		return undefinedStructuredEvent_;
	    }
	}
    }
    
    public void disconnect_structured_pull_supplier() {
	connected_ = false;
	myConsumer_ = null;
    }

    public void transmit_event(NotificationEvent event) {
	synchronized(pendingEvents_) {
	    if(pendingEvents_.size() > maxListSize_) {
		pendingEvents_.remove(pendingEvents_.getFirst());
	    }
	    pendingEvents_.add(event.toStructuredEvent());
	}
    }

    public List getSubsequentDestinations() {
	return Collections.singletonList(this);
    }
    
    public TransmitEventCapable getEventSink() {
	return this;
    }

    public void dispose() {
    }
}// StructuredProxyPullSupplierImpl
