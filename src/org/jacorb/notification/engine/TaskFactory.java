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

import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.FilterStage;
import org.jacorb.notification.interfaces.Message;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;

import java.util.List;

/**
 * @author Alphonse Bendt
 * @version $Id: TaskFactory.java,v 1.5 2004-04-28 12:37:28 brose Exp $
 */

public class TaskFactory implements Disposable,Configurable
{
    private TaskProcessor taskProcessor_;

    private AbstractTaskPool filterProxyConsumerTaskPool_ =
        new AbstractTaskPool("FilterProxyConsumerTaskPool")
        {
            public Object newInstance()
            {
                return new FilterProxyConsumerTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   TaskFactory.this);
            }
        };


    private AbstractTaskPool filterSupplierAdminTaskPool_ =
        new AbstractTaskPool("FilterSupplierAdminTaskPool")
        {
            public Object newInstance()
            {
                return new FilterSupplierAdminTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   TaskFactory.this);
            }
        };


    private AbstractTaskPool filterConsumerAdminTaskPool_ =
        new AbstractTaskPool("FilterConsumerAdminTaskPool")
        {
            public Object newInstance()
            {
                return new FilterConsumerAdminTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   TaskFactory.this);
            }
        };


    private AbstractTaskPool filterProxySupplierTaskPool_ =
        new AbstractTaskPool("FilterProxySupplierTaskPool")
        {
            public Object newInstance()
            {
                return new FilterProxySupplierTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   TaskFactory.this);
            }
        };


    private AbstractTaskPool deliverTaskPool_ =
        new AbstractTaskPool("PushToConsumerTaskPool")
        {
            public Object newInstance()
            {
                PushToConsumerTask _task = new PushToConsumerTask(taskProcessor_);

                return _task;
            }
        };

    ////////////////////////////////////////

    public TaskFactory( TaskProcessor taskProcessor )
    {
        taskProcessor_ = taskProcessor;
    }

    ////////////////////////////////////////

    public void configure(Configuration conf)
    {
        filterProxyConsumerTaskPool_.configure(conf);
        filterProxySupplierTaskPool_.configure(conf);
        filterConsumerAdminTaskPool_.configure(conf);
        filterSupplierAdminTaskPool_.configure(conf);

        deliverTaskPool_.configure(conf);
    }


    public void dispose()
    {
        filterProxyConsumerTaskPool_.dispose();
        filterProxySupplierTaskPool_.dispose();
        filterConsumerAdminTaskPool_.dispose();
        filterSupplierAdminTaskPool_.dispose();

        deliverTaskPool_.dispose();
    }

    ////////////////////////////////////////

    ////////////////////////////////////////
    // Factory methods for FilterProxyConsumerTasks

    private FilterProxyConsumerTask newFilterProxyConsumerTask() {
        return (FilterProxyConsumerTask)filterProxyConsumerTaskPool_.lendObject();
    }


    FilterProxyConsumerTask newFilterProxyConsumerTask( Message message )
    {
        FilterProxyConsumerTask task = newFilterProxyConsumerTask();

        task.setMessage( message );

        task.setCurrentFilterStage( new FilterStage[] { message.getInitialFilterStage() } );

        return task;
    }

    ////////////////////////////////////////

    ////////////////////////////////////////
    // Factory methods for FilterSupplierAdminTasks
    ////////////////////////////////////////

    private FilterSupplierAdminTask newFilterSupplierAdminTask() {
        return (FilterSupplierAdminTask)filterSupplierAdminTaskPool_.lendObject();
    }


    FilterSupplierAdminTask newFilterSupplierAdminTask( FilterProxyConsumerTask t )
    {
        FilterSupplierAdminTask task = newFilterSupplierAdminTask();

        if (t.getFilterStageToBeProcessed().length != 1) {
            throw new RuntimeException();
        }

        task.setMessage( t.removeMessage() );

        task.setCurrentFilterStage( t.getFilterStageToBeProcessed() );

        task.setSkip( t.getSkip() );

        return task;
    }

    ////////////////////////////////////////

    ////////////////////////////////////////
    // Factory methods for FilterConsumerAdminTasks
    ////////////////////////////////////////

    private FilterConsumerAdminTask newFilterConsumerAdminTask() {
        return (FilterConsumerAdminTask)filterConsumerAdminTaskPool_.lendObject();
    }

    FilterConsumerAdminTask newFilterConsumerAdminTask( FilterSupplierAdminTask t )
    {
        FilterConsumerAdminTask task = newFilterConsumerAdminTask();

        task.setMessage(t.removeMessage());

        task.setCurrentFilterStage( t.getFilterStageToBeProcessed() );

        return task;
    }

    ////////////////////////////////////////

    ////////////////////////////////////////
    // Factory methods for FilterProxySupplierTasks
    ////////////////////////////////////////

    private FilterProxySupplierTask newFilterProxySupplierTask() {
        return (FilterProxySupplierTask)filterProxySupplierTaskPool_.lendObject();
    }

    FilterProxySupplierTask newFilterProxySupplierTask( FilterConsumerAdminTask task )
    {
        FilterProxySupplierTask _newTask = newFilterProxySupplierTask();

        _newTask.setMessage(task.removeMessage());

        FilterStage[] _filterStageList = task.getFilterStageToBeProcessed();

        _newTask.setCurrentFilterStage( _filterStageList );

        return _newTask;
    }

    ////////////////////////////////////////

    ////////////////////////////////////////
    // Factory methods for AbstractDeliverTasks
    ////////////////////////////////////////

    AbstractDeliverTask[] newPushToConsumerTask(FilterStage[] nodes,
                                                Message event) {

        return newPushToConsumerTask(nodes, event, FilterProxySupplierTask.EMPTY_MAP);

    }


    /**
     * Create a Array of PushToConsumerTask.
     *
     * @param seqFilterStageWithMessageConsumer Array of FilterStages
     * that have an MessageConsumer attached.
     *
     * @param defaultMessage the Message that is to be
     * delivered by the created PushToConsumerTask. This method gains
     * possession of the Message.
     *
     * @param map alternate Messages that should be used for
     * specific MessageConsumers.
     *
     * @return a <code>PushToConsumerTask[]</code> value
     */
    private AbstractDeliverTask[] newPushToConsumerTask(FilterStage [] filterStagesWithMessageConsumer,
                                                        Message defaultMessage,
                                                        FilterProxySupplierTask.AlternateMessageMap map) {

        AbstractDeliverTask[] _seqPushToConsumerTask =
            new AbstractDeliverTask[ filterStagesWithMessageConsumer.length ];

        for ( int x = 0; x < filterStagesWithMessageConsumer.length; ++x )
            {
                _seqPushToConsumerTask[ x ] =
                    ( AbstractDeliverTask ) deliverTaskPool_.lendObject();

                _seqPushToConsumerTask[ x ]
                    .setMessageConsumer( filterStagesWithMessageConsumer[ x ].getMessageConsumer() );

                Message _alternateEvent =
                    map.getAlternateMessage(filterStagesWithMessageConsumer[x]);

                if ( _alternateEvent != null ) {

                    _seqPushToConsumerTask[ x ].setMessage( _alternateEvent );

                } else {
                    if (x == 0) {
                        // the first Message can be simply
                        // used as this method gains possession of the
                        // Message
                        _seqPushToConsumerTask[x].setMessage(defaultMessage);
                    } else {
                        // all following Messages must be copied
                        _seqPushToConsumerTask[x].setMessage((Message)defaultMessage.clone());
                    }
                }
            }
        return _seqPushToConsumerTask;
    }


    /**
     * factory method to create PushToConsumer Tasks. The Tasks are
     * initialized with the data taken from a FilterProxySupplierTask.
     */
    AbstractDeliverTask[] newPushToConsumerTask( FilterProxySupplierTask task )
    {
        AbstractDeliverTask[] _deliverTasks;

        Message _notificationEvent =
            task.removeMessage();

        FilterStage[] _seqFilterStageToBeProcessed =
            task.getFilterStageToBeProcessed();

        _deliverTasks = newPushToConsumerTask(_seqFilterStageToBeProcessed,
                                              _notificationEvent,
                                              task.changedMessages_);

        return _deliverTasks;
    }
}
