package org.jacorb.notification.servant;

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
import java.util.Map;

import org.jacorb.notification.engine.TaskExecutor;
import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.engine.TaskProcessorDependency;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.FilterStage;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.interfaces.ProxyEvent;
import org.jacorb.notification.interfaces.ProxyEventListener;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UNKNOWN;
import org.omg.CosEventChannelAdmin.ProxyPullSupplier;
import org.omg.CosEventChannelAdmin.ProxyPushSupplier;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.AdminLimitExceeded;
import org.omg.CosNotifyChannelAdmin.ClientType;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ConsumerAdminHelper;
import org.omg.CosNotifyChannelAdmin.ConsumerAdminOperations;
import org.omg.CosNotifyChannelAdmin.ConsumerAdminPOATie;
import org.omg.CosNotifyChannelAdmin.ProxyNotFound;
import org.omg.CosNotifyChannelAdmin.ProxySupplier;
import org.omg.CosNotifyChannelAdmin.ProxySupplierHelper;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosNotifyFilter.MappingFilter;
import org.omg.CosNotifyFilter.MappingFilterHelper;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.POA;

/**
 * @author Alphonse Bendt
 * @version $Id: ConsumerAdminImpl.java,v 1.2 2004-05-09 19:01:42 alphonse.bendt Exp $
 */

public class ConsumerAdminImpl
    extends AbstractAdmin
    implements ConsumerAdminOperations,
               Disposable,
               ProxyEventListener,
               TaskProcessorDependency
{
    private ConsumerAdmin thisRef_;

    protected Servant thisServant_;

    private FilterStageListManager listManager_;

    private MappingFilter priorityFilter_;

    private MappingFilter lifetimeFilter_;

    private TaskProcessor taskProcessor_;

    ////////////////////////////////////////

    public ConsumerAdminImpl()
    {
        super();

        listManager_ = new FilterStageListManager()
            {
                public void fetchListData(FilterStageListManager.List listProxy)
                {
                    Iterator i = pullServants_.entrySet().iterator();

                    while (i.hasNext())
                        {
                            listProxy.add((FilterStage) ((Map.Entry)i.next()).getValue());
                        }


                    i = pushServants_.entrySet().iterator();

                    while (i.hasNext())
                        {
                            listProxy.add((FilterStage) ((Map.Entry)i.next()).getValue());
                        }
                }
            };

        addProxyEventListener(this);
    }

    public void setTaskProcessor(TaskProcessor taskProcessor) {
        taskProcessor_ = taskProcessor;
    }

    public TaskProcessor getTaskProcessor() {
        return taskProcessor_;
    }

    ////////////////////////////////////////

    public synchronized Servant getServant()
    {
        if ( thisServant_ == null )
        {
            thisServant_ = new ConsumerAdminPOATie( this );
        }

        return thisServant_;
    }

    public void preActivate() {
        lifetimeFilter_ =
            MappingFilterHelper.unchecked_narrow(getORB().string_to_object(getORB().object_to_string(null)));

        priorityFilter_ =
            MappingFilterHelper.unchecked_narrow(getORB().string_to_object(getORB().object_to_string(null)));
    }


    public org.omg.CORBA.Object activate()
    {
        if ( thisRef_ == null )
        {
            thisRef_ = ConsumerAdminHelper.narrow(getServant()._this_object( getORB() ));
        }

        return thisRef_;
    }


    public void subscription_change( EventType[] added,
                                     EventType[] removed )
        throws InvalidEventType
    {
        subscriptionManager_.subscription_change(added, removed);
    }


    public ProxySupplier get_proxy_supplier( int key ) throws ProxyNotFound
    {
        return ProxySupplierHelper.narrow(getProxy(key).activate());
    }


    public void lifetime_filter( MappingFilter lifetimeFilter )
    {
        lifetimeFilter_ = lifetimeFilter;
    }


    public MappingFilter lifetime_filter()
    {
        return lifetimeFilter_;
    }


    public MappingFilter priority_filter()
    {
        return priorityFilter_;
    }


    public void priority_filter( MappingFilter priorityFilter )
    {
        priorityFilter_ = priorityFilter;
    }


    public ProxySupplier obtain_notification_pull_supplier( ClientType clientType,
                                                            IntHolder intHolder )
        throws AdminLimitExceeded
    {
        // may throw AdminLimitExceeded
        fireCreateProxyRequestEvent();

        try {
            AbstractProxy _servant =
                obtain_notification_pull_supplier_servant( clientType );

            intHolder.value = _servant.getID().intValue();

            _servant.preActivate();

            return ProxySupplierHelper.narrow( _servant.activate() );
        }
        catch (Exception e) {
            logger_.fatalError("obtain_notification_pull_supplier: unexpected error", e);

            throw new UNKNOWN();
        }
    }


    protected void configureMappingFilters(AbstractProxySupplier servant)
    {
        if (lifetimeFilter_ != null)
        {
            servant.lifetime_filter(lifetimeFilter_);
        }


        if (priorityFilter_ != null)
        {
            servant.priority_filter(priorityFilter_);
        }
    }


    private AbstractProxy obtain_notification_pull_supplier_servant( ClientType clientType )
        throws UnsupportedQoS
    {
        AbstractProxySupplier _servant =
            AbstractProxySupplier.newProxyPullSupplier(this,
                                                       clientType );

        configureManagers(_servant);

        configureNotifyStyleID(_servant);

        configureMappingFilters(_servant);

        _servant.setTaskExecutor(TaskExecutor.getDefaultExecutor());

        configureQoS(_servant);

        configureInterFilterGroupOperator(_servant);

        addProxyToMap(_servant, pullServants_, modifyProxiesLock_);

        return _servant;
    }


    public int[] pull_suppliers()
    {
        return get_all_notify_proxies(pullServants_, modifyProxiesLock_);
    }


    public int[] push_suppliers()
    {
        return get_all_notify_proxies(pushServants_, modifyProxiesLock_);
    }


    public ProxySupplier obtain_notification_push_supplier( ClientType clientType,
                                                            IntHolder intHolder )
        throws AdminLimitExceeded
    {
        // may throw AdminLimitExceeded
        fireCreateProxyRequestEvent();

        try
        {
            AbstractProxy _servant =
                obtain_notification_push_supplier_servant( clientType );

            intHolder.value = _servant.getID().intValue();

            _servant.preActivate();

            return ProxySupplierHelper.narrow( _servant.activate() );
        }
        catch (Exception e)
        {
            logger_.fatalError("obtain_notification_push_supplier: unexpected error", e);

            throw new UNKNOWN();
        }
    }


    private AbstractProxy obtain_notification_push_supplier_servant( ClientType clientType)
        throws UnsupportedQoS
    {

        AbstractProxySupplier _servant = AbstractProxySupplier.newProxyPushSupplier(this, clientType);

        configureNotifyStyleID(_servant);

        configureManagers(_servant);

        configureMappingFilters(_servant);

        configureQoS(_servant);

        getTaskProcessor().configureTaskExecutor(_servant);

        configureInterFilterGroupOperator(_servant);

        addProxyToMap(_servant, pushServants_, modifyProxiesLock_);

        return _servant;
    }


    public ProxyPullSupplier obtain_pull_supplier()
    {
        try
        {
            ProxyPullSupplierImpl _servant =
                new ECProxyPullSupplierImpl();

            configureEventStyleID(_servant);

            configureQoS(_servant);

            addProxyToMap(_servant, pullServants_, modifyProxiesLock_);

            _servant.setTaskExecutor(TaskExecutor.getDefaultExecutor());

            _servant.preActivate();

            return org.omg.CosEventChannelAdmin.ProxyPullSupplierHelper.narrow( _servant.activate() );
        }
        catch (Exception e)
        {
            logger_.fatalError("obtain_pull_supplier: exception", e);

            throw new UNKNOWN();
        }
    }


    /**
     * get ProxyPushSupplier (EventStyle)
     */
    public ProxyPushSupplier obtain_push_supplier()
    {
        try
        {
            final ProxyPushSupplierImpl _servant =
                new ECProxyPushSupplierImpl();

            configureEventStyleID(_servant);

            configureQoS(_servant);

            getTaskProcessor().configureTaskExecutor(_servant);

            addProxyToMap(_servant, pushServants_, modifyProxiesLock_);

            _servant.preActivate();

            return org.omg.CosEventChannelAdmin.ProxyPushSupplierHelper.narrow( _servant.activate() );
        }
        catch (Exception e)
        {
            logger_.fatalError("obtain_push_supplier: exception", e);

            throw new UNKNOWN();
        }
    }


    public List getSubsequentFilterStages()
    {
        return listManager_.getList();
    }


    /**
     * ConsumerAdmin never has a MessageConsumer
     */
    public MessageConsumer getMessageConsumer()
    {
        return null;
    }


    /**
     * ConsumerAdmin never has a MessageConsumer
     */
    public boolean hasMessageConsumer()
    {
        return false;
    }


    public void actionProxyCreationRequest( ProxyEvent event)
    {
    }


    public void actionProxyDisposed( ProxyEvent event )
    {
        listManager_.actionSourceModified();
    }


    public void actionProxyCreated( ProxyEvent event )
    {
        listManager_.actionSourceModified();
    }
}
