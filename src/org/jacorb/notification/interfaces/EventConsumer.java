package org.jacorb.notification.interfaces;

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

import org.jacorb.notification.interfaces.Message;
import org.omg.CosNotifyChannelAdmin.NotConnected;

/**
 * Abstraction of an ProxySupplier.
 *
 * @author Alphonse Bendt
 * @version $Id: EventConsumer.java,v 1.3 2003-08-28 15:57:52 alphonse.bendt Exp $
 */

public interface EventConsumer extends Disposable {

    /**
     * Deliver Pending Events. If this ProxySupplier has some Events queued
     * for a Consumer, a call to this method causes it to
     * deliver them.
     */
    void deliverPendingEvents() throws NotConnected;

    boolean hasPendingEvents();


    /**
     * Activate Deliveries to Consumer via ProxySupplier.
     *
     */
    void enableDelivery();

    /**
     * Disable Deliveries to this Consumer. Subsequent calls to
     * deliverEvent queue the Events in an internal queue.
     */
    void disableDelivery();

    /**
     * Deliver a NotificationEvent. The Consumer is responsible
     * to maintain the Connection to the real EventConsumer. If Delivery is
     * enabled the Proxy converts the NotificationEvent to the
     * appropiate Format (Any, StructuredEvent, Sequence of
     * StructuredEvent) and delivers it to the real EventConsumer. If
     * Delivery is disabled the Object must queue the Events and not
     * try to deliver them.
     */
    void deliverEvent(Message event);

    void resetErrorCounter();

    int getErrorCounter();

    int incErrorCounter();

}// EventDispatcher
