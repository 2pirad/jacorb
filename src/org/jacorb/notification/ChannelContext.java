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

import org.jacorb.notification.engine.TaskProcessor;
import org.omg.CosNotifyChannelAdmin.EventChannelFactory;
import org.omg.CosNotifyFilter.FilterFactory;
import org.omg.CosNotifyChannelAdmin.EventChannel;
import org.jacorb.notification.interfaces.ProxyEventListener;
import org.jacorb.notification.interfaces.ProxyCreationRequestEventListener;
import org.apache.log.Logger;
import org.apache.log.Hierarchy;

/**
 * ChannelContext.java
 *
 *
 * Created: Sat Nov 30 16:02:18 2002
 *
 * @author Alphonse Bendt
 * @version $Id: ChannelContext.java,v 1.6 2003-06-05 13:04:09 alphonse.bendt Exp $
 */

public class ChannelContext {

    private Logger logger_ = Hierarchy.getDefaultHierarchy().getLoggerFor(getClass().getName());

    private EventChannel eventChannel;
    private EventChannelImpl eventChannelServant;
    private EventChannelFactory eventChannelFactory;
    private EventChannelFactoryImpl eventChannelFactoryServant;
    private FilterFactory defaultFilterFactory;
    private TaskProcessor taskProcessor_;

    private ProxyCreationRequestEventListener proxyCreationEventListener_;
    private ProxyEventListener proxySupplierDisposedListener_;
    private ProxyEventListener proxyConsumerDisposedListener_;

    /**
     * @return the TaskProcessor for this Channel
     */
    public TaskProcessor getTaskProcessor()  {
	return taskProcessor_;
    }

    /**
     * sets the TaskProcessor for this Channel
     */
    public void setTaskProcessor(TaskProcessor taskProcessor) {
	taskProcessor_ = taskProcessor;
    }

    /**
     * Gets the value of eventChannelFactory
     *
     * @return the value of eventChannelFactory
     */
    public EventChannelFactory getEventChannelFactory()  {
	return eventChannelFactory;
    }

    /**
     * Sets the value of eventChannelFactory
     *
     * @param argEventChannelFactory Value to assign to this.eventChannelFactory
     */
    public void setEventChannelFactory(EventChannelFactory argEventChannelFactory) {
	eventChannelFactory = argEventChannelFactory;
    }

    /**
     * Gets the value of eventChannelFactoryServant
     *
     * @return the value of eventChannelFactoryServant
     */
    public EventChannelFactoryImpl getEventChannelFactoryServant()  {
	return eventChannelFactoryServant;
    }

    /**
     * Sets the value of eventChannelFactoryServant
     *
     * @param argEventChannelFactoryServant Value to assign to this.eventChannelFactoryServant
     */
    public void setEventChannelFactoryServant(EventChannelFactoryImpl argEventChannelFactoryServant) {
	eventChannelFactoryServant = argEventChannelFactoryServant;
    }

    /**
     * Gets the value of defaultFilterFactory
     *
     * @return the value of defaultFilterFactory
     */
    public FilterFactory getDefaultFilterFactory()  {
	return defaultFilterFactory;
    }

    /**
     * Sets the value of defaultFilterFactory
     *
     * @param argDefaultFilterFactory Value to assign to this.defaultFilterFactory
     */
    public void setDefaultFilterFactory(FilterFactory argDefaultFilterFactory) {
	defaultFilterFactory = argDefaultFilterFactory;
    }

    /**
     * Gets the value of eventChannelServant
     *
     * @return the value of eventChannelServant
     */
    public EventChannelImpl getEventChannelServant()  {
	return eventChannelServant;
    }

    /**
     * Sets the value of eventChannelServant
     *
     * @param argEventChannelServant Value to assign to this.eventChannelServant
     */
    public void setEventChannelServant(EventChannelImpl argEventChannelServant) {
	logger_.debug("setEventChannelServant(" + argEventChannelServant + ")");
	if (argEventChannelServant == null) {
	    throw new RuntimeException();
	}
	eventChannelServant = argEventChannelServant;
    }

    /**
     * Get the EventChannel value.
     * @return the EventChannel value.
     */
    public EventChannel getEventChannel() {
	return eventChannel;
    }

    /**
     * Set the EventChannel value.
     * @param newEventChannel The new EventChannel value.
     */
    public void setEventChannel(EventChannel newEventChannel) {
	this.eventChannel = newEventChannel;
    }
    
    public Object clone() {
	ChannelContext _copy = new ChannelContext();

	_copy.setEventChannelFactory(eventChannelFactory);
	_copy.setEventChannelFactoryServant(eventChannelFactoryServant);
	_copy.setDefaultFilterFactory(defaultFilterFactory);

	return _copy;
    }

    public void setProxyConsumerDisposedEventListener(ProxyEventListener listener) {
	proxyConsumerDisposedListener_ = listener;
    }

    public void setProxySupplierDisposedEventListener(ProxyEventListener listener) {
	proxySupplierDisposedListener_ = listener;
    }

    public ProxyEventListener getRemoveProxyConsumerListener() {
	return proxyConsumerDisposedListener_;
    }
    
    public ProxyEventListener getRemoveProxySupplierListener() {
	return proxySupplierDisposedListener_;
    }

    public void dispatchEvent(NotificationEvent event) {
	getTaskProcessor().processEvent( event );
    }

}// ChannelContext
