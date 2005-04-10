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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.jacorb.notification.OfferManager;
import org.jacorb.notification.SubscriptionManager;
import org.jacorb.notification.engine.PushSequenceOperation;
import org.jacorb.notification.engine.TaskExecutor;
import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.util.PropertySet;
import org.jacorb.notification.util.PropertySetAdapter;
import org.omg.CORBA.ORB;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventChannelAdmin.TypeError;
import org.omg.CosNotification.MaximumBatchSize;
import org.omg.CosNotification.PacingInterval;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.CosNotifyChannelAdmin.SequenceProxyPushSupplierOperations;
import org.omg.CosNotifyChannelAdmin.SequenceProxyPushSupplierPOATie;
import org.omg.CosNotifyComm.SequencePushConsumer;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.TimeBase.TimeTHelper;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

/**
 * @author Alphonse Bendt
 * @version $Id: SequenceProxyPushSupplierImpl.java,v 1.14 2005-04-10 14:28:58 alphonse.bendt Exp $
 */

public class SequenceProxyPushSupplierImpl extends StructuredProxyPushSupplierImpl implements
        SequenceProxyPushSupplierOperations
{
    public SequenceProxyPushSupplierImpl(IAdmin admin, ORB orb, POA poa, Configuration config,
            TaskProcessor taskProcessor, TaskExecutor taskExecutor, OfferManager offerManager,
            SubscriptionManager subscriptionManager, ConsumerAdmin consumerAdmin)
            throws ConfigurationException
    {
        super(admin, orb, poa, config, taskProcessor, taskExecutor, offerManager, subscriptionManager,
                consumerAdmin);

        configureMaxBatchSize();

        configurePacingInterval();

        qosSettings_.addPropertySetListener(MaximumBatchSize.value, new PropertySetAdapter()
        {
            public void actionPropertySetChanged(PropertySet source) throws UnsupportedQoS
            {
                configureMaxBatchSize();
            }
        });

        qosSettings_.addPropertySetListener(PacingInterval.value, new PropertySetAdapter()
        {
            public void actionPropertySetChanged(PropertySet source) throws UnsupportedQoS
            {
                configurePacingInterval();
            }
        });
    }

    /**
     * The connected SequencePushConsumer.
     */
    private SequencePushConsumer sequencePushConsumer_;

    /**
     * registration for the Scheduled DeliverTask.
     */
    private Object taskId_;

    /**
     * maximum queue size before a delivery is forced.
     */
    private final SynchronizedInt maxBatchSize_ = new SynchronizedInt(1);

    /**
     * how long to wait between two scheduled deliveries.
     */
    private final SynchronizedLong pacingInterval_ = new SynchronizedLong(0);

    
    /**
     * this callback is called by the TimerDaemon. Check if there are pending Events and deliver
     * them to the Consumer. As there's only one TimerDaemon its important to block the daemon only
     * a minimal amount of time. Therefor the Callback does not do the actual delivery. Instead a
     * DeliverTask is scheduled for this Supplier.
     */
    // private Runnable timerCallback_;
    // //////////////////////////////////////
    public ProxyType MyType()
    {
        return ProxyType.PUSH_SEQUENCE;
    }


    public void messageDelivered()
    {
        if (!isSuspended() && isEnabled() && (getPendingMessagesCount() >= maxBatchSize_.get()))
        {
            deliverPendingMessages(false);
        }
    }

    /**
     * overrides the superclass version.
     */
    public void deliverPendingData()
    {
        deliverPendingMessages(true);
    }

    private void deliverPendingMessages(boolean flush)
    {
        final Message[] _messages;

        if (flush)
        {
            _messages = getAllMessages();
        }
        else
        {
            _messages = getAtLeastMessages(maxBatchSize_.get());
        }

        if (_messages != null && _messages.length > 0)
        {
            final StructuredEvent[] _structuredEvents = new StructuredEvent[_messages.length];

            for (int x = 0; x < _messages.length; ++x)
            {
                _structuredEvents[x] = _messages[x].toStructuredEvent();

                _messages[x].dispose();
            }

            try
            {
                sequencePushConsumer_.push_structured_events(_structuredEvents);

                resetErrorCounter();
            } catch (Throwable e)
            {
                PushSequenceOperation _failedOperation = new PushSequenceOperation(
                        sequencePushConsumer_, _structuredEvents);

                handleFailedPushOperation(_failedOperation, e);
            }
        }
    }

    public void connect_sequence_push_consumer(SequencePushConsumer consumer)
            throws AlreadyConnected, TypeError
    {
        logger_.debug("connect_sequence_push_consumer");

        checkIsNotConnected();

        sequencePushConsumer_ = consumer;

        connectClient(consumer);

        startCronJob();
    }

    protected void connectionResumed()
    {
        scheduleDeliverPendingMessagesOperation_.run();

        startCronJob();
    }

    protected void connectionSuspended()
    {
        stopCronJob();
    }

    public void disconnect_sequence_push_supplier()
    {
        destroy();
    }

    protected void disconnectClient()
    {
        stopCronJob();

        sequencePushConsumer_.disconnect_sequence_push_consumer();
        sequencePushConsumer_ = null;
    }

    private void startCronJob()
    {
        if (pacingInterval_.get() > 0 && taskId_ != null)
        {
            taskId_ = getTaskProcessor().executeTaskPeriodically(pacingInterval_.get(),
                    scheduleDeliverPendingMessagesOperation_, true);
        }
    }

    synchronized private void stopCronJob()
    {
        if (taskId_ != null)
        {
            getTaskProcessor().cancelTask(taskId_);
            taskId_ = null;
        }
    }

    private void checkCronJob()
    {
        if (pacingInterval_.get() > 0)
        {
            startCronJob();
        }
        else
        {
            stopCronJob();
        }
    }

    private boolean configurePacingInterval()
    {
        if (qosSettings_.containsKey(PacingInterval.value))
        {
            long _pacingInterval = TimeTHelper.extract(qosSettings_.get(PacingInterval.value));

            if (pacingInterval_.get() != _pacingInterval)
            {
                if (logger_.isInfoEnabled())
                {
                    logger_.info("set PacingInterval=" + _pacingInterval);
                }
                pacingInterval_.set(_pacingInterval);

                checkCronJob();

                return true;
            }
        }
        return false;
    }

    private boolean configureMaxBatchSize()
    {
        if (qosSettings_.containsKey(MaximumBatchSize.value))
        {
            int _maxBatchSize = qosSettings_.get(MaximumBatchSize.value).extract_long();

            if (maxBatchSize_.get() != _maxBatchSize)
            {
                if (logger_.isInfoEnabled())
                {
                    logger_.info("set MaxBatchSize=" + _maxBatchSize);
                }
                
                maxBatchSize_.set(_maxBatchSize);

                return true;
            }
        }

        return false;
    }

    public synchronized Servant getServant()
    {
        if (thisServant_ == null)
        {
            thisServant_ = new SequenceProxyPushSupplierPOATie(this);
        }

        return thisServant_;
    }
}