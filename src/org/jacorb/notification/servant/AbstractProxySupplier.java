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

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.conf.Configuration;
import org.jacorb.notification.conf.Default;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.queue.EventQueue;
import org.jacorb.notification.queue.EventQueueFactory;
import org.jacorb.notification.util.TaskExecutor;
import org.jacorb.util.Environment;

import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CosNotification.DiscardPolicy;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.OrderPolicy;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ConsumerAdminHelper;
import org.omg.CosNotifyChannelAdmin.ObtainInfoMode;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosNotifyComm.NotifyPublishOperations;
import org.omg.CosNotifyComm.NotifySubscribeOperations;

import java.util.List;

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
 * @version $Id: AbstractProxySupplier.java,v 1.3 2004-02-09 16:25:27 alphonse.bendt Exp $
 */

public abstract class AbstractProxySupplier
    extends AbstractProxy
    implements MessageConsumer,
               NotifySubscribeOperations

{
    private final static EventType[] EMPTY_EVENT_TYPE_ARRAY = new EventType[0];

    private TaskExecutor taskExecutor_;

    private Disposable disposeTaskExecutor_;

    private EventQueue pendingEvents_;

    private int errorThreshold_;

    /**
     * lock variable used to control access to the reference to the
     * Message Queue.
     */
    private Object pendingEventsLock_ = new Object();

    private NotifyPublishOperations offerListener_;

    ////////////////////////////////////////

    protected AbstractProxySupplier(AbstractAdmin admin,
                                    ChannelContext channelContext)
    throws UnsupportedQoS
    {
        super(admin,
              channelContext);
    }


    protected AbstractProxySupplier(AbstractAdmin admin,
                                    ChannelContext channelContext,
                                    Integer key)
    throws UnsupportedQoS
    {
        super(admin,
              channelContext);
    }

    ////////////////////////////////////////

    public void preActivate() throws UnsupportedQoS
    {
        synchronized (pendingEventsLock_)
        {
            pendingEvents_ = EventQueueFactory.newEventQueue(qosSettings_);
        }

        errorThreshold_ =
            Environment.getIntPropertyWithDefault(Configuration.EVENTCONSUMER_ERROR_THRESHOLD,
                                                  Default.DEFAULT_EVENTCONSUMER_ERROR_THRESHOLD);

        if (logger_.isInfoEnabled())
        {
            logger_.info("set Error Threshold to : " + errorThreshold_);
        }

        qosSettings_.addPropertySetListener(new String[] {OrderPolicy.value, DiscardPolicy.value},
                                            eventQueueConfigurationChangedCB);
    }


    private void configureEventQueue() throws UnsupportedQoS
    {
        EventQueue _newQueue = EventQueueFactory.newEventQueue( qosSettings_ );

        try
        {
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
            throw new RuntimeException(e.getMessage());
        }
    }


    private PropertySetListener eventQueueConfigurationChangedCB =
        new PropertySetListener()
        {

            public void validateProperty(Property[] p, List errors)
            {}

            public void actionPropertySetChanged(PropertySet source)
            throws UnsupportedQoS
            {
                configureEventQueue();
            }
        };


    public TaskExecutor getExecutor()
    {
        return taskExecutor_;
    }


    public void setTaskExecutor(TaskExecutor executor)
    {
        if (taskExecutor_ == null)
        {
            taskExecutor_ = executor;
        }
        else
        {
            throw new IllegalArgumentException("set only once");
        }
    }


    public void setTaskExecutor(TaskExecutor executor, Disposable disposeTaskExecutor)
    {
        setTaskExecutor(executor);

        disposeTaskExecutor_ = disposeTaskExecutor;
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
    protected void enqueue(Message message)
    {
        synchronized (pendingEventsLock_)
        {
            pendingEvents_.put(message);
        }

        if (logger_.isDebugEnabled() )
        {
            logger_.debug("added " + message + " to pending Messages.");
        }
    }


    protected Message getMessageBlocking() throws InterruptedException
    {
        synchronized (pendingEventsLock_)
        {
            return pendingEvents_.getEvent(true);
        }
    }


    protected Message getMessageNoBlock()
    {
        synchronized (pendingEventsLock_)
        {
            try
            {
                return pendingEvents_.getEvent(false);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();

                return null;
            }
        }
    }


    protected Message[] getAllMessages()
    {
        synchronized (pendingEventsLock_)
        {
            try
            {
                return pendingEvents_.getAllEvents(false);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();

                return null;
            }
        }
    }


    protected Message[] getUpToMessages(int max)
    {
        try
        {
            synchronized (pendingEventsLock_)
            {
                return pendingEvents_.getEvents(max, false);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();

            return null;
        }
    }


    protected Message[] getAtLeastMessages(int min)
    {
        try
        {
            synchronized (pendingEventsLock_)
            {
                if (pendingEvents_.getSize() >= min)
                {
                    return pendingEvents_.getAllEvents(true);
                }
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        return null;
    }


    public int getErrorThreshold()
    {
        return errorThreshold_;
    }


    final public void dispose()
    {
        super.dispose();

        if (disposeTaskExecutor_ != null)
        {
            disposeTaskExecutor_.dispose();
        }
    }


    final public ConsumerAdmin MyAdmin()
    {
        return ConsumerAdminHelper.narrow(myAdmin_.activate());
    }


    final public void subscription_change(EventType[] added,
                                          EventType[] removed)
        throws InvalidEventType
    {
        subscriptionManager_.subscription_change(added, removed);
    }


    final public EventType[] obtain_offered_types(ObtainInfoMode obtainInfoMode)
    {
        logger_.debug("obtain_offered_types " + obtainInfoMode.value() );

        EventType[] _offeredTypes = EMPTY_EVENT_TYPE_ARRAY;

        switch(obtainInfoMode.value()) {
        case ObtainInfoMode._ALL_NOW_UPDATES_ON:
            registerListener();
            _offeredTypes = offerManager_.obtain_offered_types();
            break;
        case ObtainInfoMode._ALL_NOW_UPDATES_OFF:
            _offeredTypes = offerManager_.obtain_offered_types();
            removeListener();
            break;
        case ObtainInfoMode._NONE_NOW_UPDATES_ON:
            registerListener();
            break;
        case ObtainInfoMode._NONE_NOW_UPDATES_OFF:
            removeListener();
            break;
        default:
            throw new IllegalArgumentException("Illegal ObtainInfoMode");
        }

        return _offeredTypes;
    }


    private void registerListener() {
        if (offerListener_ == null) {
            final NotifyPublishOperations _listener = getOfferListener();

            if (_listener != null) {

                offerListener_ = new NotifyPublishOperations() {
                        public void offer_change(EventType[] added, EventType[] removed) {
                            try {
                                _listener.offer_change(added, removed);
                            } catch (NO_IMPLEMENT e) {
                                logger_.info("Listener does not support offer_change. remove it.", e);
                                removeListener();
                            } catch (InvalidEventType e) {
                                logger_.error("invalid event type", e);
                            }
                        }
                    };

                offerManager_.addListener(offerListener_);
            }
        }
    }


    private void removeListener() {
        if (offerListener_ != null) {
            offerManager_.removeListener(offerListener_);
            offerListener_ = null;
        }
    }


    abstract NotifyPublishOperations getOfferListener();
}
