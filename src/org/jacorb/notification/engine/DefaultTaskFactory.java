package org.jacorb.notification.engine;

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

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.FilterStage;
import org.jacorb.notification.interfaces.Message;

/**
 * @author Alphonse Bendt
 * @version $Id: DefaultTaskFactory.java,v 1.1 2005-02-14 00:03:09 alphonse.bendt Exp $
 */

public class DefaultTaskFactory implements Disposable, Configurable, TaskFactory
{
    private final TaskProcessor taskProcessor_;

    private final AbstractPoolablePool filterProxyConsumerTaskPool_ =
        new AbstractPoolablePool("FilterProxyConsumerTaskPool")
        {
            public Object newInstance()
            {
                return new FilterProxyConsumerTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   DefaultTaskFactory.this);
            }
        };


    private final AbstractPoolablePool filterSupplierAdminTaskPool_ =
        new AbstractPoolablePool("FilterSupplierAdminTaskPool")
        {
            public Object newInstance()
            {
                return new FilterSupplierAdminTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   DefaultTaskFactory.this);
            }
        };


    private final AbstractPoolablePool filterConsumerAdminTaskPool_ =
        new AbstractPoolablePool("FilterConsumerAdminTaskPool")
        {
            public Object newInstance()
            {
                return new FilterConsumerAdminTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   DefaultTaskFactory.this);
            }
        };


    private final AbstractPoolablePool filterProxySupplierTaskPool_ =
        new AbstractPoolablePool("FilterProxySupplierTaskPool")
        {
            public Object newInstance()
            {
                return new FilterProxySupplierTask(taskProcessor_.getFilterTaskExecutor(),
                                                   taskProcessor_,
                                                   DefaultTaskFactory.this);
            }
        };


    private final AbstractPoolablePool deliverTaskPool_ =
        new AbstractPoolablePool("PushToConsumerTaskPool")
        {
            public Object newInstance()
            {
                PushToConsumerTask _task = new PushToConsumerTask(taskProcessor_);

                return _task;
            }
        };

    ////////////////////////////////////////

    public DefaultTaskFactory( TaskProcessor taskProcessor )
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


    public Schedulable newFilterProxyConsumerTask( Message message )
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


    public Schedulable newFilterSupplierAdminTask( FilterProxyConsumerTask t )
    {
        FilterSupplierAdminTask task = newFilterSupplierAdminTask();

        // TODO this really should be an assertion
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

    public Schedulable newFilterConsumerAdminTask( FilterSupplierAdminTask t )
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

    public Schedulable newFilterProxySupplierTask( FilterConsumerAdminTask task )
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

    public AbstractDeliverTask[] newPushToConsumerTask(FilterStage[] nodes,
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
     * delivered by the created PushToConsumerTask. This method assumes
     * ownership of the Message.
     *
     * @param map Map(FilterStage=>Message) of alternate Messages that should be used for
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
                        // the first Message can be passed on
                        // as this method holds ownership of the
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
    public AbstractDeliverTask[] newPushToConsumerTask( FilterProxySupplierTask task )
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
