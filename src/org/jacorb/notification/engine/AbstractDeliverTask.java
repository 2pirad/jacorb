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

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractDeliverTask.java,v 1.3 2004-01-17 01:14:31 alphonse.bendt Exp $
 */

public abstract class AbstractDeliverTask extends AbstractTask
{
    private MessageConsumer target_;

    ////////////////////

    protected AbstractDeliverTask(TaskExecutor te, TaskProcessor tp, TaskFactory tc)
    {
        super(te, tp, tc);
    }

    ////////////////////

    public static void scheduleTasks(AbstractDeliverTask[] tasks,
                                     boolean runAllowed) throws InterruptedException
    {
        for ( int x = 0; x < tasks.length; ++x )
        {
            tasks[x].schedule(runAllowed);
        }
    }

    ////////////////////////////////////////

    public void reset()
    {
        super.reset();

        target_ = null;
    }


    public MessageConsumer getMessageConsumer()
    {
        return target_;
    }


    public void setMessageConsumer( MessageConsumer mc )
    {
        target_ = mc;

        if (logger_.isDebugEnabled())
        {
            logger_.debug("DeliverTask.setMessageConsumer Target="
                          + target_ );

        }
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

            if ( logger_.isWarnEnabled() )
            {
                logger_.warn( "push to Consumer failed: Dispose MessageConsumer" );
            }

            _pushToConsumerTask.getMessageConsumer().dispose();
        }
        else
        {
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

                    //                    _consumer.backoff();

                    taskProcessor_.backoffMessageConsumer(_consumer);
                }
                catch (Exception e)
                {
                    // if regardless of disabling the MessageConsumer
                    // above the MessageConsumer still
                    // throws an exception we'll assume its totally
                    // messed up and dispose it.
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
}
