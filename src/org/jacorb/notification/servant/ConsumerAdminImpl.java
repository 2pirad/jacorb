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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.jacorb.notification.MessageFactory;
import org.jacorb.notification.OfferManager;
import org.jacorb.notification.SubscriptionManager;
import org.jacorb.notification.container.CORBAObjectComponentAdapter;
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
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.CachingComponentAdapter;

/**
 * @jmx.mbean extends = "AbstractAdminMBean"
 * @jboss.xmbean
 * 
 * @author Alphonse Bendt
 * @version $Id: ConsumerAdminImpl.java,v 1.6 2005-08-21 13:33:00 alphonse.bendt Exp $
 */

public class ConsumerAdminImpl extends AbstractAdmin implements ConsumerAdminOperations,
        Disposable, ProxyEventListener, ConsumerAdminImplMBean
{
    private final static class FilterstageWithMessageConsumerComparator implements Comparator
    {
        /**
         * compare two FilterStages via their MessageConsumer.
         */
        public int compare(Object l, Object r)
        {
            FilterStage left = (FilterStage) l;
            FilterStage right = (FilterStage) r;
            
            return left.getMessageConsumer().compareTo(right.getMessageConsumer());
        }
    }

    static final FilterstageWithMessageConsumerComparator FILTERSTAGE_COMPARATOR = new FilterstageWithMessageConsumerComparator();
    
    private final ConsumerAdmin thisRef_;

    protected final Servant thisServant_;

    private final FilterStageListManager listManager_;

    private MappingFilter priorityFilter_;

    private MappingFilter lifetimeFilter_;

    ////////////////////////////////////////

    public ConsumerAdminImpl(IEventChannel channelServant, ORB orb, POA poa, Configuration config,
            MessageFactory messageFactory, OfferManager offerManager,
            SubscriptionManager subscriptionManager)
    {
        super(channelServant, orb, poa, config, messageFactory, offerManager, subscriptionManager);

        // register core components (factories)

        listManager_ = new FilterStageListManager()
        {
            public void fetchListData(FilterStageListManager.List listProxy)
            {
                Iterator i = pullServants_.entrySet().iterator();

                while (i.hasNext())
                {
                    listProxy.add((FilterStage) ((Map.Entry) i.next()).getValue());
                }

                i = pushServants_.entrySet().iterator();

                while (i.hasNext())
                {
                    listProxy.add((FilterStage) ((Map.Entry) i.next()).getValue());
                }
            }
            
            protected void sortCheckedList(java.util.List list)
            {
                Collections.sort(list, FILTERSTAGE_COMPARATOR);
            }
        };

        lifetimeFilter_ = MappingFilterHelper.unchecked_narrow(getORB().string_to_object(
                getORB().object_to_string(null)));

        priorityFilter_ = MappingFilterHelper.unchecked_narrow(getORB().string_to_object(
                getORB().object_to_string(null)));

        addProxyEventListener(this);

        thisServant_ = createServant();

        thisRef_ = ConsumerAdminHelper.narrow(getServant()._this_object(getORB()));

        container_.registerComponent(new CachingComponentAdapter(new CORBAObjectComponentAdapter(
                ConsumerAdmin.class, thisRef_)));

        registerDisposable(new Disposable()
        {
            public void dispose()
            {
                container_.unregisterComponent(ConsumerAdmin.class);
            }
        });
    }

    ////////////////////////////////////////

    protected Servant createServant()
    {
        return new ConsumerAdminPOATie(this);
    }

    public final Servant getServant()
    {
        return thisServant_;
    }

    public org.omg.CORBA.Object activate()
    {
        return thisRef_;
    }

    public void subscription_change(EventType[] added, EventType[] removed) throws InvalidEventType
    {
        subscriptionManager_.subscription_change(added, removed);
    }

    public ProxySupplier get_proxy_supplier(int key) throws ProxyNotFound
    {
        return ProxySupplierHelper.narrow(getProxy(key).activate());
    }

    public void lifetime_filter(MappingFilter lifetimeFilter)
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

    public void priority_filter(MappingFilter priorityFilter)
    {
        priorityFilter_ = priorityFilter;
    }

    public ProxySupplier obtain_notification_pull_supplier(ClientType clientType,
            IntHolder intHolder) throws AdminLimitExceeded
    {
        // may throw AdminLimitExceeded
        fireCreateProxyRequestEvent();

        try
        {
            AbstractProxy _servant = obtain_notification_pull_supplier_servant(clientType);

            intHolder.value = _servant.getID().intValue();

            return ProxySupplierHelper.narrow(_servant.activate());
        } catch (Exception e)
        {
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

    private AbstractProxy obtain_notification_pull_supplier_servant(ClientType clientType)
            throws UnsupportedQoS
    {
        AbstractProxySupplier _servant = newProxyPullSupplier(clientType);

        configureMappingFilters(_servant);

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

    public ProxySupplier obtain_notification_push_supplier(ClientType clientType,
            IntHolder intHolder) throws AdminLimitExceeded
    {
        // may throw AdminLimitExceeded
        fireCreateProxyRequestEvent();

        try
        {
            AbstractProxy _servant = obtain_notification_push_supplier_servant(clientType);

            intHolder.value = _servant.getID().intValue();

            return ProxySupplierHelper.narrow(_servant.activate());
        } catch (Exception e)
        {
            logger_.fatalError("obtain_notification_push_supplier: unexpected error", e);

            throw new UNKNOWN();
        }
    }

    private AbstractProxy obtain_notification_push_supplier_servant(ClientType clientType)
            throws UnsupportedQoS
    {
        AbstractProxySupplier _servant = newProxyPushSupplier(clientType);

        configureMappingFilters(_servant);

        configureQoS(_servant);

        configureInterFilterGroupOperator(_servant);

        addProxyToMap(_servant, pushServants_, modifyProxiesLock_);

        return _servant;
    }

    public ProxyPullSupplier obtain_pull_supplier()
    {
        try
        {
            MutablePicoContainer _container = newContainerForEventStyleProxy();

            _container.registerComponentImplementation(AbstractProxy.class, ECProxyPullSupplierImpl.class);

            AbstractProxy _servant = (AbstractProxy) _container
                    .getComponentInstanceOfType(AbstractProxy.class);

            configureQoS(_servant);

            addProxyToMap(_servant, pullServants_, modifyProxiesLock_);

            return org.omg.CosEventChannelAdmin.ProxyPullSupplierHelper.narrow(_servant.activate());
        } catch (Exception e)
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
            MutablePicoContainer _container = newContainerForEventStyleProxy();

            _container.registerComponentImplementation(AbstractProxy.class, ECProxyPushSupplierImpl.class);

            final AbstractProxy _servant = (AbstractProxy) _container
                    .getComponentInstanceOfType(AbstractProxy.class);

            configureQoS(_servant);

            addProxyToMap(_servant, pushServants_, modifyProxiesLock_);

            return org.omg.CosEventChannelAdmin.ProxyPushSupplierHelper.narrow(_servant.activate());
        } catch (Exception e)
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

    public void actionProxyCreationRequest(ProxyEvent event)
    {
        // ignored
    }

    public void actionProxyDisposed(ProxyEvent event)
    {
        listManager_.actionSourceModified();
    }

    public void actionProxyCreated(ProxyEvent event)
    {
        listManager_.actionSourceModified();
    }

    /**
     * factory method for new ProxyPullSuppliers.
     */
    AbstractProxySupplier newProxyPullSupplier(ClientType clientType)
    {
        final MutablePicoContainer _containerForProxy = newContainerForNotifyStyleProxy();
        final Class _proxyClass;

        switch (clientType.value()) {
        case ClientType._ANY_EVENT:
            _proxyClass = ProxyPullSupplierImpl.class;

            break;
        case ClientType._STRUCTURED_EVENT:
            _proxyClass = StructuredProxyPullSupplierImpl.class;

            break;
        case ClientType._SEQUENCE_EVENT:
            _proxyClass = SequenceProxyPullSupplierImpl.class;

            break;
        default:
            throw new BAD_PARAM();
        }

        _containerForProxy
                .registerComponentImplementation(AbstractProxySupplier.class, _proxyClass);

        final AbstractProxySupplier _servant = (AbstractProxySupplier) _containerForProxy
                .getComponentInstanceOfType(AbstractProxySupplier.class);

        return _servant;
    }

    /**
     * factory method for new ProxyPushSuppliers.
     */
    AbstractProxySupplier newProxyPushSupplier(ClientType clientType)
    {
        final Class _proxyClass;

        switch (clientType.value()) {

        case ClientType._ANY_EVENT:
            _proxyClass = ProxyPushSupplierImpl.class;
            break;

        case ClientType._STRUCTURED_EVENT:
            _proxyClass = StructuredProxyPushSupplierImpl.class;
            break;

        case ClientType._SEQUENCE_EVENT:
            _proxyClass = SequenceProxyPushSupplierImpl.class;
            break;

        default:
            throw new BAD_PARAM("The ClientType: " + clientType.value() + " is unknown");
        }

        final MutablePicoContainer _containerForProxy = newContainerForNotifyStyleProxy();

        _containerForProxy
                .registerComponentImplementation(AbstractProxySupplier.class, _proxyClass);

        final AbstractProxySupplier _servant = (AbstractProxySupplier) _containerForProxy
                .getComponentInstanceOfType(AbstractProxySupplier.class);

        return _servant;
    }
    
    public String getMBeanType()
    {
        return "ConsumerAdmin";
    }
}