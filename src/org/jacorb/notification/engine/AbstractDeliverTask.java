package org.jacorb.notification.engine;

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
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.util.TaskExecutor;

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CosEventComm.Disconnected;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractDeliverTask.java,v 1.7 2004-03-03 12:04:28 alphonse.bendt Exp $
 */

public abstract class AbstractDeliverTask extends AbstractTask
{
    private MessageConsumer messageConsumer_;

    ////////////////////

    protected AbstractDeliverTask(TaskProcessor tp, TaskFactory tf) {
        super(null, tp, tf);
    }

    ////////////////////

    public static void scheduleTasks(AbstractDeliverTask[] tasks) throws InterruptedException
    {
        for ( int x = 0; x < tasks.length; ++x )
        {
            tasks[x].schedule(false);
        }
    }

    ////////////////////////////////////////

    public void reset()
    {
        super.reset();

        messageConsumer_ = null;
    }


    protected MessageConsumer getMessageConsumer()
    {
        return messageConsumer_;
    }


    public void setMessageConsumer( MessageConsumer messageConsumer )
    {
        messageConsumer_ = messageConsumer;
    }


    public void handleTaskError(AbstractTask task, Throwable error)
    {
        if (logger_.isDebugEnabled())
        {
            logger_.debug("Entering Exceptionhandler for Task:"
                          + task.getClass().getName(),
                          error);
        }

        AbstractDeliverTask _pushToConsumerTask = (AbstractDeliverTask)task;

        if ( error instanceof OBJECT_NOT_EXIST )
        {
            // push operation caused a OBJECT_NOT_EXIST Exception
            // default strategy is to
            // destroy the ProxySupplier

            if ( logger_.isErrorEnabled() )
            {
                logger_.error( "push raised OBJECT_NOT_EXIST: will dispose MessageConsumer", error );
            }

            _pushToConsumerTask.getMessageConsumer().dispose();
        }
        else if (error instanceof Disconnected)
        {
            // push operation caused Disconnected
            // this indicates an illegal state
            // the ProxySupplier thinks the Consumer is connected but
            // the Consumer thinks its not connected. the default
            // strategy it to destroy the ProxySupplier

            if (logger_.isErrorEnabled()) {
                logger_.error("push raised Disconnected: will dispose MessageConsumer", error);
            }

            _pushToConsumerTask.getMessageConsumer().dispose();
        }
        else
        {
            // push raised an unexpected error:
            // 1) increment the error counter for this MessageConsumer
            //    (the error counter is reset on the first successful
            //    push operation).
            // 2) disable the MessageConsumer for a while (during this
            //    period the MessageConsumer only queues Messages).
            // 3) enable the MessageConsumer again and deliver the
            //    pending Messages.

            if (logger_.isErrorEnabled()) {
                logger_.error("push raised unexpected error", error);
            }

            MessageConsumer _consumer = _pushToConsumerTask.getMessageConsumer();

            if (logger_.isInfoEnabled())
            {
                logger_.info(_consumer + "/errorCount: " + _consumer.getErrorCounter());
                logger_.info(_consumer + "/errorThreshold: " + _consumer.getErrorThreshold());
            }

            if (_consumer.getErrorCounter() > _consumer.getErrorThreshold() )
            {
                if (logger_.isWarnEnabled())
                {
                    logger_.warn("MessageConsumer is repeatingly failing. Error Counter is: "
                                 + _consumer.getErrorCounter()
                                 + ". The MessageConsumer will be disconnected");
                }

                _consumer.dispose();

            }
            else
            {
                _consumer.incErrorCounter();

                if (logger_.isInfoEnabled())
                {
                    logger_.info("Increased the ErrorCount for "
                                 + _consumer
                                 + " to "
                                 + _consumer.getErrorCounter());
                }

                _consumer.disableDelivery();

                try
                {
                    // as delivery has been disabled
                    // the message will be queued by the MessageConsumer
                    _consumer.deliverMessage(_pushToConsumerTask.removeMessage());

                    logger_.info("will backoff MessageConsumer for a while");

                    taskProcessor_.backoutMessageConsumer(_consumer);
                }
                catch (Exception e)
                {
                    // if regardless of disabling the MessageConsumer
                    // above the MessageConsumer still
                    // throws an exception we'll assume its totally
                    // messed up and get rid of it.
                    logger_.error("a disabled MessageConsumer should not throw "
                                  + " an exception during deliverEvent", e);
                    try
                    {
                        _consumer.dispose();
                    }
                    catch (Exception ex)
                    {
                        logger_.debug("Error disposing misbehaving Consumer", ex);
                    }
                }
            }
        }

        Message m = _pushToConsumerTask.removeMessage();

        if (m != null)
        {
            m.dispose();
        }

        _pushToConsumerTask.dispose();
    }


    /**
     * override default schedule to use the TaskExecutor provided
     * by the current MessageConsumer.
     */
    protected void schedule(boolean directRunAllowed) throws InterruptedException {
        schedule(getTaskExecutor(), directRunAllowed);
    }


    public void schedule() throws InterruptedException {
        schedule(!getTaskExecutor().isTaskQueued());
    }


    /**
     * override to use the TaskExecutor provided by the current MessageConsumer
     */
    protected TaskExecutor getTaskExecutor() {
        return getMessageConsumer().getExecutor();
    }
}
