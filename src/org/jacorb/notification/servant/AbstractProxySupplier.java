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

import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.queue.EventQueue;
import org.jacorb.notification.util.TaskExecutor;
import org.jacorb.util.Environment;

import org.omg.CORBA.UNKNOWN;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.UnsupportedQoS;

import java.util.Map;

import org.jacorb.notification.queue.EventQueueFactory;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.PropertyManager;
import org.jacorb.notification.PropertyValidator;
import org.jacorb.notification.conf.Configuration;
import org.jacorb.notification.conf.Default;

/**
 * Abstract base class for ProxySuppliers.
 * This class provides following logic for the different
 * ProxySuppliers:
 * <ul>
 * <li> generic queue management,
 * <li> error threshold settings.
 * </ul>
 *
 * @author Alphonse Bendt
 * @version $Id: AbstractProxySupplier.java,v 1.1 2004-01-23 19:41:53 alphonse.bendt Exp $
 */

public abstract class AbstractProxySupplier
    extends AbstractProxy
    implements MessageConsumer
{
    private TaskExecutor taskExecutor_;

    private Disposable disposeTaskExecutor_;

    private EventQueue pendingEvents_;

    private int errorThreshold_;

    /**
     * lock variable used to control access to the reference to the
     * queue object not the queue object itself.
     */
    private Object pendingEventsLock_ =
        new Object();


    ////////////////////

    protected AbstractProxySupplier(AbstractAdmin admin,
                                    ChannelContext channelContext,
                                    PropertyManager adminProperties,
                                    PropertyManager qosProperties)
        throws UnsupportedQoS
    {
        super(admin,
              channelContext,
              adminProperties,
              qosProperties);

        init(qosProperties);
    }

    protected AbstractProxySupplier(AbstractAdmin admin,
                                    ChannelContext channelContext,
                                    PropertyManager adminProperties,
                                    PropertyManager qosProperties,
                                    Integer key)
        throws UnsupportedQoS
    {
        super(admin,
              channelContext,
              adminProperties,
              qosProperties,
              key,
              true);

        init(qosProperties);
    }

    ////////////////////////////////////////

    private void init(PropertyManager qosProperties)
        throws UnsupportedQoS
    {
        synchronized (pendingEventsLock_)
            {
                pendingEvents_ = EventQueueFactory.newEventQueue(qosProperties);
            }

        errorThreshold_ =
            Environment.getIntPropertyWithDefault(Configuration.EVENTCONSUMER_ERROR_THRESHOLD,
                                                  Default.DEFAULT_EVENTCONSUMER_ERROR_THRESHOLD);

        if (logger_.isInfoEnabled()) {
            logger_.info(toString() + ": set Error Threshold to : " + errorThreshold_);
        }
    }


    public TaskExecutor getExecutor() {
        return taskExecutor_;
    }


    public void setTaskExecutor(TaskExecutor executor) {
        if (taskExecutor_ == null) {
            taskExecutor_ = executor;
        } else {
            throw new IllegalArgumentException("set only once");
        }
    }


    public void setTaskExecutor(TaskExecutor executor, Disposable disposeTaskExecutor) {
        setTaskExecutor(executor);
        disposeTaskExecutor_ = disposeTaskExecutor;
    }

    public void set_qos(Property[] qosProps) throws UnsupportedQoS
    {
        logger_.info("set_qos called");

        try
        {
            PropertyValidator.checkQoSPropertySeq(qosProps);

            Map _uniqueQoSProps =
                PropertyValidator.getUniqueProperties(qosProps);

            PropertyManager _qosManager =
                new PropertyManager(null, //applicationContext_,
                                    _uniqueQoSProps);

            if (logger_.isInfoEnabled())
                {
                    logger_.info("qos props: " + _qosManager);
                }

            EventQueue _newQueue = EventQueueFactory.newEventQueue( _qosManager );

            synchronized (pendingEventsLock_)
            {
                if (!pendingEvents_.isEmpty())
                {
                    Message[] _allEvents =
                        pendingEvents_.getAllEvents(true);

                    for (int x = 0; x < _allEvents.length; ++x)
                    {
                        _newQueue.put(_allEvents[x]);
                    }
                }

                pendingEvents_ = _newQueue;
            }
        }
        catch (InterruptedException e)
        {
            logger_.error("interupted", e);
            throw new UNKNOWN(e.getMessage());
        }
    }

    public boolean hasPendingMessages()
    {
        synchronized (pendingEventsLock_)
        {
            return !pendingEvents_.isEmpty();
        }
    }

    /**
     * put a Message in the queue of pending Messages.
     *
     * @param message the <code>Message</code> to queue.
     */
    protected void enqueue(Message message) {
        synchronized(pendingEventsLock_) {
            pendingEvents_.put(message);
        }

        if (logger_.isDebugEnabled() ) {
            logger_.debug("added " + message + " to pending Messages.");
        }
    }


    protected Message getMessageBlocking() throws InterruptedException {
        synchronized(pendingEventsLock_) {
            return pendingEvents_.getEvent(true);
        }
    }


    protected Message getMessageNoBlock() {
        synchronized(pendingEventsLock_) {
            try {
                return pendingEvents_.getEvent(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                return null;
            }
        }
    }

    protected Message[] getAllMessages() {
        synchronized(pendingEventsLock_) {
            try {
                return pendingEvents_.getAllEvents(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                return null;
            }
        }
    }

    protected Message[] getUpToMessages(int max) {
        try {
            synchronized(pendingEventsLock_) {
                return pendingEvents_.getEvents(max, false);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    protected Message[] getAtLeastMessages(int min) {
        try {
            synchronized(pendingEventsLock_) {
                if (pendingEvents_.getSize() >= min) {
                    return pendingEvents_.getAllEvents(true);
                }
            }
        } catch (InterruptedException e) {
        }
        return null;
    }

    public int getErrorThreshold() {
        return errorThreshold_;
    }

    public void dispose() {
        super.dispose();

        if (disposeTaskExecutor_ != null) {
            disposeTaskExecutor_.dispose();
        }
    }

}
