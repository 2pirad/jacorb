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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.FilterManager;
import org.jacorb.notification.PropertyManager;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.FilterStage;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.interfaces.ProxyEvent;
import org.jacorb.notification.interfaces.ProxyEventListener;
import org.jacorb.notification.util.TaskExecutor;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.IntHolder;
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
import org.omg.CosNotifyChannelAdmin.InterFilterGroupOperator;
import org.omg.CosNotifyChannelAdmin.ProxyNotFound;
import org.omg.CosNotifyChannelAdmin.ProxySupplier;
import org.omg.CosNotifyChannelAdmin.ProxySupplierHelper;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosNotifyFilter.MappingFilter;
import org.omg.PortableServer.Servant;

/**
 * @author Alphonse Bendt
 * @version $Id: ConsumerAdminTieImpl.java,v 1.1 2004-01-23 19:41:53 alphonse.bendt Exp $
 */

public class ConsumerAdminTieImpl
    extends AbstractAdmin
    implements ConsumerAdminOperations,
               Disposable,
               ProxyEventListener
{
    private Object modifyProxiesLock_ = new Object();

    List eventStyleServants_ = new ArrayList();

    ConsumerAdmin thisRef_;

    ConsumerAdminPOATie thisServant_;

    //    List subsequentDestinations_;

    FilterStageListManager listManager_;

    boolean proxyListDirty_ = true;

    ////////////////////////////////////////

    public ConsumerAdminTieImpl(ChannelContext channelContext,
                                PropertyManager adminProperties,
                                PropertyManager qosProperties )
    {
        super(channelContext,
              adminProperties,
              qosProperties );

        common();
    }

    private void common() {
        listManager_ = new FilterStageListManager() {
                public void fetchListData(FilterStageListManager.List listProxy) {

                    Iterator i = pullServants_.entrySet().iterator();

                    while (i.hasNext()) {
                        listProxy.add((FilterStage) ((Map.Entry)i.next()).getValue());
                    }

                    i = pushServants_.entrySet().iterator();

                    while (i.hasNext()) {
                        listProxy.add((FilterStage) ((Map.Entry)i.next()).getValue());
                    }

                    i = eventStyleServants_.iterator();

                    while (i.hasNext()) {
                        listProxy.add((FilterStage) i.next());
                    }
                }
            };
    }


    public ConsumerAdminTieImpl(ChannelContext channelContext,
                                PropertyManager adminProperties,
                                PropertyManager qosProperties,
                                int myId,
                                InterFilterGroupOperator filterGroupOperator )
    {
        super(channelContext,
              adminProperties,
              qosProperties,
              myId,
              filterGroupOperator );

        common();
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


    public synchronized ConsumerAdmin getConsumerAdmin()
    {
        if ( thisRef_ == null )
            {
                thisRef_ = ConsumerAdminHelper.narrow(getServant()._this_object( getORB() ));
            }

        return thisRef_;
    }


    public org.omg.CORBA.Object getCorbaRef()
    {
        return getConsumerAdmin();
    }


    public void subscription_change( EventType[] eventType1,
                                     EventType[] eventType2 )
        throws InvalidEventType
        {}


    public ProxySupplier get_proxy_supplier( int n ) throws ProxyNotFound
    {
        synchronized(modifyProxiesLock_) {

            ProxySupplier _ret = ( ProxySupplier ) allProxies_.get( new Integer( n ) );

            if ( _ret == null )
                {
                    throw new ProxyNotFound();
                }

            return _ret;
        }
    }


    public void lifetime_filter( MappingFilter mappingFilter )
    {
    }


    public MappingFilter lifetime_filter()
    {
        return null;
    }


    public MappingFilter priority_filter()
    {
        return null;
    }


    public void priority_filter( MappingFilter mappingFilter )
    {
    }


    public ProxySupplier obtain_notification_pull_supplier( ClientType clientType,
                                                            IntHolder intHolder )
        throws AdminLimitExceeded
    {
        try {
            AbstractProxy _servant =
                obtain_notification_pull_supplier_servant( clientType, intHolder );

            Integer _key = _servant.getKey();

            ProxySupplier _proxySupplier = ProxySupplierHelper.narrow( _servant.getCorbaRef() );

            synchronized (modifyProxiesLock_) {
                allProxies_.put( _key, _proxySupplier );

                listManager_.actionSourceModified();

                proxyListDirty_ = true;
            }

            return _proxySupplier;
        } catch (UnsupportedQoS e) {
            logger_.fatalError("Could not create pull supplier", e);
            throw new RuntimeException();
        }
    }


    public AbstractProxy obtain_notification_pull_supplier_servant( ClientType clientType,
                                                                    IntHolder intHolder )
        throws AdminLimitExceeded,
               UnsupportedQoS
    {
        // may throw AdminLimitExceeded
        fireCreateProxyRequestEvent();

        intHolder.value = getPullProxyId();
        Integer _key = new Integer( intHolder.value );

        AbstractProxySupplier _servant;

        PropertyManager _qosProperties = ( PropertyManager ) qosProperties_.clone();
        PropertyManager _adminProperties = ( PropertyManager ) adminProperties_.clone();

        switch ( clientType.value() )
        {

        case ClientType._ANY_EVENT:
            _servant = new ProxyPullSupplierImpl( this, // applicationContext_,
                                                  channelContext_,
                                                  _adminProperties,
                                                  _qosProperties,
                                                  _key );
            break;

        case ClientType._STRUCTURED_EVENT:
            _servant =
                new StructuredProxyPullSupplierImpl( this, //applicationContext_,
                                                     channelContext_,
                                                     _adminProperties,
                                                     _qosProperties,
                                                     _key );
            break;

        case ClientType._SEQUENCE_EVENT:
            _servant =
                new SequenceProxyPullSupplierImpl( this, //applicationContext_,
                                                   channelContext_,
                                                   _adminProperties,
                                                   _qosProperties,
                                                   _key );

            break;

        default:
            throw new BAD_PARAM();
        }

        _servant.setTaskExecutor(TaskExecutor.getDefaultExecutor());


        if ( filterGroupOperator_.value() == InterFilterGroupOperator._OR_OP )
        {
            _servant.setOrSemantic( true );
        }

        _servant.addProxyDisposedEventListener( this );

        if ( channelContext_.getRemoveProxySupplierListener() != null )
        {
            _servant.addProxyDisposedEventListener( channelContext_.getRemoveProxySupplierListener() );
        }

        synchronized (modifyProxiesLock_) {
            pullServants_.put( _key, _servant );
            proxyListDirty_ = true;
        }

        return _servant;
    }


    /**
     *
     */
    public void remove
        ( AbstractProxy pb )
    {
        super.remove( pb );

        Integer _key = pb.getKey();

        if ( _key != null )
        {
            synchronized(modifyProxiesLock_) {
                allProxies_.remove( _key );
                proxyListDirty_ = true;
            }

            if ( pb instanceof StructuredProxyPullSupplierImpl ||
                    pb instanceof ProxyPullSupplierImpl ||
                    pb instanceof SequenceProxyPullSupplierImpl )
            {

                synchronized(modifyProxiesLock_) {
                    pullServants_.remove( _key );

                    listManager_.actionSourceModified();

                    proxyListDirty_ = true;
                }

            }
            else if ( pb instanceof StructuredProxyPushSupplierImpl ||
                      pb instanceof ProxyPushSupplierImpl ||
                      pb instanceof SequenceProxyPushSupplierImpl )
            {

                synchronized(modifyProxiesLock_) {
                    pushServants_.remove( _key );

                    listManager_.actionSourceModified();

                    proxyListDirty_ = true;
                }
            }
        }
        else
        {
            synchronized(modifyProxiesLock_) {
                eventStyleServants_.remove( pb );

                listManager_.actionSourceModified();

                proxyListDirty_ = true;
            }
        }
    }

    /**
     * access the ids of all push_suppliers
     */
    public int[] push_suppliers()
    {
        synchronized(modifyProxiesLock_) {

            int[] _ret = new int[ pushServants_.size() ];
            Iterator _i = pushServants_.keySet().iterator();
            int x = -1;

            while ( _i.hasNext() )
                {
                    _ret[ ++x ] = ( ( Integer ) _i.next() ).intValue();
                }

            return _ret;
        }
    }


    public int[] pull_suppliers()
    {
        synchronized(modifyProxiesLock_) {

            int[] _ret = new int[ pullServants_.size() ];
            Iterator _i = pullServants_.keySet().iterator();
            int x = -1;

            while ( _i.hasNext() )
                {
                    _ret[ ++x ] = ( ( Integer ) _i.next() ).intValue();
                }

            return _ret;
        }
    }


    public ProxySupplier obtain_notification_push_supplier( ClientType clientType,
                                                            IntHolder intHolder )
        throws AdminLimitExceeded
    {
        try {
            AbstractProxy _servant = obtain_notification_push_supplier_servant( clientType,
                                                                                intHolder );

            Integer _key = _servant.getKey();

            if (logger_.isInfoEnabled()) {
                logger_.info("created ProxyPushSupplier with ID: " + _key);
            }

            ProxySupplier _proxySupplier =
                ProxySupplierHelper.narrow( _servant.getCorbaRef() );

            synchronized(modifyProxiesLock_) {
                allProxies_.put( _key, _proxySupplier );
                proxyListDirty_ = true;
            }

            return _proxySupplier;
        } catch (UnsupportedQoS e) {
            logger_.fatalError("could not create push_supplier", e);
            throw new RuntimeException();
        }
    }


    public AbstractProxy obtain_notification_push_supplier_servant( ClientType clientType,
                                                                    IntHolder intHolder )
        throws AdminLimitExceeded,
               UnsupportedQoS
    {
        // may throw exception if admin limit is exceeded
        fireCreateProxyRequestEvent();

        intHolder.value = getPushProxyId();

        Integer _key = new Integer( intHolder.value );
        AbstractProxySupplier _servant;

        PropertyManager _qosProperties = ( PropertyManager ) qosProperties_.clone();
        PropertyManager _adminProperties = ( PropertyManager ) adminProperties_.clone();

        switch ( clientType.value() )
        {

        case ClientType._ANY_EVENT:
            _servant = new ProxyPushSupplierImpl( this, //applicationContext_,
                                                  channelContext_,
                                                  _adminProperties,
                                                  _qosProperties,
                                                  _key );
            break;

        case ClientType._STRUCTURED_EVENT:
            _servant =
                new StructuredProxyPushSupplierImpl( this, //applicationContext_,
                                                     channelContext_,
                                                     _adminProperties,
                                                     _qosProperties,
                                                     _key );
            break;

        case ClientType._SEQUENCE_EVENT:
            _servant =
                new SequenceProxyPushSupplierImpl( this, //applicationContext_,
                                                   channelContext_,
                                                   _adminProperties,
                                                   _qosProperties,
                                                   _key );
            break;

        default:
            throw new BAD_PARAM();
        }

        channelContext_.getTaskProcessor().configureTaskExecutor(_servant);


        if ( filterGroupOperator_.value() == InterFilterGroupOperator._OR_OP )
        {
            _servant.setOrSemantic( true );
        }

        _servant.addProxyDisposedEventListener( this );
        _servant.addProxyDisposedEventListener( channelContext_.getRemoveProxySupplierListener() );

        synchronized(modifyProxiesLock_) {
            pushServants_.put( _key, _servant );

            listManager_.actionSourceModified();

            proxyListDirty_ = true;
        }

        return _servant;
    }


    public ProxyPullSupplier obtain_pull_supplier()
    {
        try {
            ProxyPullSupplierImpl _servant =
                new ECProxyPullSupplierImpl( this,
                                             channelContext_,
                                             ( PropertyManager ) adminProperties_.clone(),
                                             ( PropertyManager ) qosProperties_.clone(),
                                             new Integer(getPullProxyId() ) );

            _servant.setTaskExecutor(TaskExecutor.getDefaultExecutor());

            _servant.addProxyDisposedEventListener( this );

            _servant.setFilterManager( FilterManager.EMPTY_FILTER_MANAGER );


            ProxyPullSupplier _supplier =
                org.omg.CosEventChannelAdmin.ProxyPullSupplierHelper.narrow( _servant.getCorbaRef() );

            synchronized(modifyProxiesLock_) {
                eventStyleServants_.add( _servant );

                listManager_.actionSourceModified();

                proxyListDirty_ = true;
            }

            return _supplier;
        } catch (UnsupportedQoS e) {
            logger_.fatalError("Could not create PullSupplier", e);

            throw new RuntimeException();
        }
    }

    /**
     * get ProxyPushSupplier (EventStyle)
     */
    public ProxyPushSupplier obtain_push_supplier()
    {
        try {
            ProxyPushSupplierImpl _servant =
                new ECProxyPushSupplierImpl( this,
                                             channelContext_,
                                             ( PropertyManager ) adminProperties_.clone(),
                                             ( PropertyManager ) qosProperties_.clone(),
                                             new Integer( getPushProxyId() ) );

            channelContext_.getTaskProcessor().configureTaskExecutor(_servant);

            _servant.addProxyDisposedEventListener( this );

            _servant.setFilterManager( FilterManager.EMPTY_FILTER_MANAGER );

            ProxyPushSupplier _supplier =
                org.omg.CosEventChannelAdmin.ProxyPushSupplierHelper.narrow( _servant.getCorbaRef() );

            synchronized(modifyProxiesLock_) {
                eventStyleServants_.add( _servant );

                listManager_.actionSourceModified();

                proxyListDirty_ = true;
            }

            return _supplier;
        } catch (UnsupportedQoS e) {
            logger_.fatalError("Could not create ProxyPushSupplier", e);

            throw new RuntimeException();
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


    public void dispose()
    {
        super.dispose();

        Iterator _i = eventStyleServants_.iterator();

        while ( _i.hasNext() )
        {
            ( ( Disposable ) _i.next() ).dispose();
        }

        eventStyleServants_.clear();
    }


    public boolean hasOrSemantic()
    {
        return filterGroupOperator_.value() == InterFilterGroupOperator._OR_OP;
    }


    public void actionProxyDisposed( ProxyEvent event )
    {
        synchronized ( this )
        {
            proxyListDirty_ = true;
        }
    }


    public void actionProxyCreated(ProxyEvent e)
    {
        // NO Op
    }
}
