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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.log.Hierarchy;
import org.apache.log.Logger;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.FilterStage;
import org.jacorb.notification.interfaces.ProxyEvent;
import org.jacorb.notification.interfaces.ProxyEventListener;
import org.omg.CORBA.ORB;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.NamedPropertyRangeSeqHolder;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.QoSAdminOperations;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ObtainInfoMode;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosNotifyComm.NotifyPublishOperations;
import org.omg.CosNotifyFilter.Filter;
import org.omg.CosNotifyFilter.FilterAdminOperations;
import org.omg.CosNotifyFilter.FilterNotFound;
import org.omg.CosNotifyFilter.MappingFilter;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;

/**
 * ProxyBase.java
 *
 *
 * Created: Sun Nov 03 22:49:01 2002
 *
 * @author Alphonse Bendt
 * @version $Id: ProxyBase.java,v 1.6 2003-06-05 13:04:09 alphonse.bendt Exp $
 */

public abstract class ProxyBase implements FilterAdminOperations, 
					   NotifyPublishOperations, 
					   QoSAdminOperations, 
					   FilterStage,
					   Disposable {
    
    public final static Integer NO_KEY = null;

    protected List proxyDisposedEventListener_;
    protected NotificationEventFactory notificationEventFactory_;
    protected POA poa_;
    protected ORB orb_;

    protected Logger logger_ = 
	Hierarchy.getDefaultHierarchy().getLoggerFor(getClass().getName());

    protected boolean connected_;
    protected ChannelContext channelContext_;
    protected ApplicationContext applicationContext_;
    protected Integer key_;
    protected AdminBase myAdmin_;
    protected FilterManager filterManager_;
    protected boolean disposed_ = false;
    protected PropertyManager adminProperties_;
    protected PropertyManager qosProperties_;
    private ProxyType proxyType_;
    private boolean hasOrSemantic_;
    protected Servant thisServant_;

    protected ProxyBase(AdminBase admin,
			ApplicationContext appContext,
			ChannelContext channelContext,
			PropertyManager adminProperties,
			PropertyManager qosProperties) 
    {
	this(admin,
	     appContext,
	     channelContext,
	     adminProperties,
	     qosProperties,
	     NO_KEY);
    }

    protected ProxyBase(AdminBase admin,
			ApplicationContext appContext,
			ChannelContext channelContext,
			PropertyManager adminProperties,
			PropertyManager qosProperties,
			Integer key) 
    {
	myAdmin_ = admin;
	key_ = key;
	adminProperties_ = adminProperties;
	qosProperties_ = qosProperties;
	applicationContext_ = appContext;
	channelContext_ = channelContext;
	poa_ = appContext.getPoa();
	orb_ = appContext.getOrb();
	connected_ = false;

	notificationEventFactory_ = 
	    applicationContext_.getNotificationEventFactory();

	filterManager_ = new FilterManager();
    }

    abstract public Servant getServant();

    public void addProxyDisposedEventListener(ProxyEventListener listener) 
    {
	if (proxyDisposedEventListener_ == null) {
	    synchronized(this) {
		if (proxyDisposedEventListener_ == null) {
		    proxyDisposedEventListener_ = new Vector();
		}
	    }
	}
	logger_.debug("addProxyDisposedEventListener(" + listener + ")");
	if (listener == null) {
	    throw new RuntimeException();
	}
	proxyDisposedEventListener_.add(listener);
    }
    
    public void removeProxyDisposedEventListener(ProxyEventListener listener) 
    {
	if (proxyDisposedEventListener_ != null) {
	    proxyDisposedEventListener_.remove(listener);
	}
    }
    
    public int add_filter(Filter filter) 
    {
	return filterManager_.add_filter(filter);
    }

    public void remove_filter(int n) throws FilterNotFound 
    {
	filterManager_.remove_filter(n);
    }

    public Filter get_filter(int n) throws FilterNotFound 
    {
	return filterManager_.get_filter(n);
    }
    
    public int[] get_all_filters() 
    {
	return filterManager_.get_all_filters();
    }

    synchronized public void remove_all_filters()
    {
	filterManager_.remove_all_filters();
    }    

    public EventType[] obtain_subscription_types(ObtainInfoMode obtainInfoMode) 
    {
        return null;
    }

    public void validate_event_qos(Property[] qosProps, 
				   NamedPropertyRangeSeqHolder propSeqHolder)
	throws UnsupportedQoS 
    {
    }

    public void validate_qos(Property[] qosProps, 
			     NamedPropertyRangeSeqHolder propSeqHolder)
	throws UnsupportedQoS 
    {
    }

    public void set_qos(Property[] qosProps) throws UnsupportedQoS 
    {
    }

    public Property[] get_qos() 
    {
        return null;
    }
    
    public void offer_change(EventType[] eventTypes, 
			     EventType[] eventTypes2) 
	throws InvalidEventType 
    {
    }

    public void subscription_change(EventType[] eventType, 
				    EventType[] eventType2) 
	throws InvalidEventType 
    {
    }

    public void priority_filter(MappingFilter filter) 
    {
    }

    public MappingFilter priority_filter() 
    {
	return null;
    }

    public MappingFilter lifetime_filter() 
    {
	return null;
    }
    
    public void lifetime_filter(MappingFilter filter) 
    {
    }
    
    public EventType[] obtain_offered_types(ObtainInfoMode obtaininfomode) 
    {
	return null;
    }

    public Integer getKey() 
    {
	return key_;
    }

    /**
     * Override this method from the Servant baseclass.  Fintan Bolton
     * in his book "Pure CORBA" suggests that you override this method to
     * avoid the risk that a servant object (like this one) could be
     * activated by the <b>wrong</b> POA object.
     */
    public POA _default_POA() 
    {
	return poa_;
    }

    void setFilterManager(FilterManager manager) 
    {
	filterManager_ = manager;
    }

    public List getFilters() 
    {
	return filterManager_.getFilters();
    }

    synchronized public void dispose() 
    {
	if (logger_.isDebugEnabled()) {
	    logger_.debug("dispose(" + this + ")");
	}
	
	if (!disposed_) {
	    remove_all_filters();
	    disposed_ = true;
	    Iterator _i;

	    if (proxyDisposedEventListener_ != null) {
		_i = proxyDisposedEventListener_.iterator();
		ProxyEvent _event = new ProxyEvent(this);
		while(_i.hasNext()) {

		    ProxyEventListener _listener = 
			(ProxyEventListener)_i.next();

		    _listener.actionProxyDisposed(_event);
		}
	    }
	    try {
		byte[] _oid = poa_.servant_to_id(getServant());
		poa_.deactivate_object(_oid);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    protected void setProxyType(ProxyType p) 
    {
	proxyType_ = p;
    }

    public ProxyType MyType() 
    {
	return proxyType_;
    }

    void setOrSemantic(boolean b) 
    {
	hasOrSemantic_ = b;
    }

    public boolean hasOrSemantic() 
    {
	return hasOrSemantic_;
    }

    public boolean isDisposed() 
    {
	return disposed_;
    }

    public boolean isConnected() {
	return connected_;
    }
}// ProxyBase
