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

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.FilterManager;
import org.jacorb.notification.PropertyManager;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.interfaces.ProxyEvent;
import org.jacorb.notification.interfaces.ProxyEventListener;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.IntHolder;
import org.omg.CosEventChannelAdmin.ProxyPullConsumer;
import org.omg.CosEventChannelAdmin.ProxyPushConsumer;
import org.omg.CosNotification.EventType;
import org.omg.CosNotifyChannelAdmin.AdminLimitExceeded;
import org.omg.CosNotifyChannelAdmin.ClientType;
import org.omg.CosNotifyChannelAdmin.InterFilterGroupOperator;
import org.omg.CosNotifyChannelAdmin.ProxyConsumer;
import org.omg.CosNotifyChannelAdmin.ProxyConsumerHelper;
import org.omg.CosNotifyChannelAdmin.ProxyNotFound;
import org.omg.CosNotifyChannelAdmin.SupplierAdmin;
import org.omg.CosNotifyChannelAdmin.SupplierAdminHelper;
import org.omg.CosNotifyChannelAdmin.SupplierAdminOperations;
import org.omg.CosNotifyChannelAdmin.SupplierAdminPOATie;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.PortableServer.Servant;

/**
 * @author Alphonse Bendt
 * @version $Id: SupplierAdminTieImpl.java,v 1.1 2004-01-23 19:41:53 alphonse.bendt Exp $
 */

public class SupplierAdminTieImpl
    extends AbstractAdmin
    implements SupplierAdminOperations,
               Disposable
{
    private SupplierAdminPOATie thisCorbaServant_;

    private SupplierAdmin thisCorbaRef_;

    private List eventStyleServants_ = new ArrayList();

    private List listProxyEventListener_ = new ArrayList();

    private Object modifyProxiesLock_ = new Object();

    ////////////////////////////////////////

    public SupplierAdminTieImpl(ChannelContext channelContext,
                                PropertyManager adminProperties,
                                PropertyManager qosProperties )
    {

        super(channelContext,
              adminProperties,
              qosProperties );
    }

    public SupplierAdminTieImpl(ChannelContext channelContext,
                                 PropertyManager adminProperties,
                                 PropertyManager qosProperties,
                                 int myId,
                                 InterFilterGroupOperator myOperator )
    {
        super(channelContext,
              adminProperties,
              qosProperties,
              myId,
              myOperator );
    }

    ////////////////////////////////////////

    public synchronized Servant getServant()
    {
        if ( thisCorbaServant_ == null )
            {
                thisCorbaServant_ = new SupplierAdminPOATie( this );
            }

        return thisCorbaServant_;
    }


    public synchronized SupplierAdmin getSupplierAdmin()
    {
        if ( thisCorbaRef_ == null )
            {
                thisCorbaRef_ = SupplierAdminHelper.narrow(getServant()._this_object( getORB() ));
            }

        return thisCorbaRef_;
    }

    public org.omg.CORBA.Object getCorbaRef()
    {
        return getSupplierAdmin();
    }

    public void offer_change( EventType[] eventType1,
                              EventType[] eventType2 ) throws InvalidEventType
    {
    }

    // Implementation of org.omg.CosNotifyChannelAdmin.SupplierAdminOperations

    /**
     * access the ids of all PullConsumers
     */
    public int[] pull_consumers()
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


    /**
     * access the ids of all PushConsumers
     */
    public int[] push_consumers()
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


    public ProxyConsumer obtain_notification_pull_consumer( ClientType clientType,
                                                            IntHolder intHolder )
        throws AdminLimitExceeded
    {
        AbstractProxy _servant = obtain_notification_pull_consumer_servant( clientType, intHolder );

        Integer _key = _servant.getKey();

        ProxyConsumer _proxyConsumer = ProxyConsumerHelper.narrow( _servant.getCorbaRef() );

        fireProxyCreated( _servant );

        synchronized (modifyProxiesLock_) {
            allProxies_.put( _key, _proxyConsumer );
        }

        return _proxyConsumer;
    }


    public AbstractProxy obtain_notification_pull_consumer_servant( ClientType clientType,
                                                                IntHolder intHolder )
        throws AdminLimitExceeded
    {

        fireCreateProxyRequestEvent();

        intHolder.value = getPullProxyId();
        Integer _key = new Integer( intHolder.value );
        AbstractProxy _servant;

        PropertyManager _adminProperties = ( PropertyManager ) adminProperties_.clone();
        PropertyManager _qosProperties = ( PropertyManager ) qosProperties_.clone();

        switch ( clientType.value() )
            {

            case ClientType._ANY_EVENT:

                _servant = new ProxyPullConsumerImpl( this,
                                                      channelContext_,
                                                      adminProperties_,
                                                      qosProperties_,
                                                      _key );

                break;

            case ClientType._STRUCTURED_EVENT:

                _servant =
                    new StructuredProxyPullConsumerImpl( this, // applicationContext_,
                                                         channelContext_,
                                                         _adminProperties,
                                                         _qosProperties,
                                                         _key );

                break;

            case ClientType._SEQUENCE_EVENT:

                _servant =
                    new SequenceProxyPullConsumerImpl( this, // applicationContext_,
                                                       channelContext_,
                                                       _adminProperties,
                                                       _qosProperties,
                                                       _key );

                break;

            default:
                throw new BAD_PARAM();
            }

        synchronized(modifyProxiesLock_) {
            pullServants_.put( _key, _servant );
        }

        if ( filterGroupOperator_.value() == InterFilterGroupOperator._OR_OP )
            {
                _servant.setOrSemantic( true );
            }

        //        _servant.addProxyDisposedEventListener( this );
        _servant.addProxyDisposedEventListener( channelContext_.getRemoveProxyConsumerListener() );

        return _servant;
    }

    /**
     * Describe <code>get_proxy_consumer</code> method here.
     *
     * @param n an <code>int</code> value
     * @return a <code>ProxyConsumer</code> value
     * @exception ProxyNotFound if an error occurs
     */
    public ProxyConsumer get_proxy_consumer( int n ) throws ProxyNotFound
    {
        synchronized (modifyProxiesLock_) {
            ProxyConsumer _ret = ( ProxyConsumer ) allProxies_.get( new Integer( n ) );

            if ( _ret == null )
                {
                    throw new ProxyNotFound();
                }

            return _ret;
        }
    }


    public ProxyConsumer obtain_notification_push_consumer( ClientType clienttype,
                                                            IntHolder intholder )
        throws AdminLimitExceeded
    {

        AbstractProxy _servant = obtain_notification_push_consumer_servant( clienttype, intholder );
        Integer _key = _servant.getKey();

        ProxyConsumer _proxyConsumer = ProxyConsumerHelper.narrow( _servant.getCorbaRef() );

        fireProxyCreated( _servant );

        synchronized(modifyProxiesLock_) {
            allProxies_.put( _key, _proxyConsumer );
        }

        return _proxyConsumer;
    }


    public AbstractProxy obtain_notification_push_consumer_servant( ClientType clientType,
                                                                    IntHolder intHolder )
        throws AdminLimitExceeded
    {

        // may throws AdminLimitExceeded
        fireCreateProxyRequestEvent();

        intHolder.value = getPushProxyId();
        Integer _key = new Integer( intHolder.value );
        AbstractProxy _servant;

        PropertyManager _adminProperties = ( PropertyManager ) adminProperties_.clone();
        PropertyManager _qosProperties = ( PropertyManager ) qosProperties_.clone();

        switch ( clientType.value() )
        {

        case ClientType._ANY_EVENT:
            _servant = new ProxyPushConsumerImpl( this, // applicationContext_,
                                                  channelContext_,
                                                  _adminProperties,
                                                  _qosProperties,
                                                  _key );
            break;

        case ClientType._STRUCTURED_EVENT:
            _servant =
                new StructuredProxyPushConsumerImpl( this,
                                                     channelContext_,
                                                     _adminProperties,
                                                     _qosProperties,
                                                     _key );
            break;

        case ClientType._SEQUENCE_EVENT:
            _servant =
                new SequenceProxyPushConsumerImpl( this, // applicationContext_,
                                                   channelContext_,
                                                   _adminProperties,
                                                   _qosProperties,
                                                   _key );
            break;

        default:
            throw new BAD_PARAM();
        }

        synchronized(modifyProxiesLock_) {
            pushServants_.put( _key, _servant );
        }

        if ( filterGroupOperator_.value() == InterFilterGroupOperator._OR_OP )
        {
            _servant.setOrSemantic( true );
        }

        //        _servant.addProxyDisposedEventListener( this );
        _servant.addProxyDisposedEventListener( channelContext_.getRemoveProxyConsumerListener() );

        return _servant;
    }

    // Implementation of org.omg.CosEventChannelAdmin.SupplierAdminOperations

    /**
     * get a ProxyPullConsumer (EventService Style)
     */
    public ProxyPullConsumer obtain_pull_consumer()
    {
        ECProxyPullConsumerImpl _servant =
            new ECProxyPullConsumerImpl( this, // applicationContext_,
                                         channelContext_,
                                         adminProperties_,
                                         qosProperties_,
                                         new Integer(getPullProxyId()) );

        _servant.setFilterManager( FilterManager.EMPTY_FILTER_MANAGER );

        synchronized(modifyProxiesLock_) {
            eventStyleServants_.add( _servant );
        }

        ProxyPullConsumer _ret =
            org.omg.CosEventChannelAdmin.ProxyPullConsumerHelper.narrow( _servant.getCorbaRef() );

        fireProxyCreated( _servant );

        return _ret;
    }


    /**
     * get a ProxyPushConsumer (EventService Style)
     */
    public ProxyPushConsumer obtain_push_consumer()
    {
        ProxyPushConsumerImpl _servant =
            new ECProxyPushConsumerImpl( this,
                                         channelContext_,
                                         adminProperties_,
                                         qosProperties_,
                                         new Integer(getPushProxyId()) );

        _servant.setFilterManager( FilterManager.EMPTY_FILTER_MANAGER );

        synchronized(modifyProxiesLock_) {
            eventStyleServants_.add( _servant );
        }

        ProxyPushConsumer _ret =
            org.omg.CosEventChannelAdmin.ProxyPushConsumerHelper.narrow( _servant.getCorbaRef() );

        fireProxyCreated( _servant );

        return _ret;
    }

    ////////////////////////////////////////

    public List getSubsequentFilterStages()
    {
        return getChannelServant().getAllConsumerAdmins();
    }


    /**
     * SupplierAdmin does not ever have a MessageConsumer.
     */
    public MessageConsumer getMessageConsumer()
    {
        return null;
    }


    /**
     * SupplierAdmin does not ever have a MessageConsumer.
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

        listProxyEventListener_.clear();
    }


    void fireProxyRemoved( AbstractProxy b )
    {
        Iterator i = listProxyEventListener_.iterator();
        ProxyEvent e = new ProxyEvent( b );

        while ( i.hasNext() )
        {
            ( ( ProxyEventListener ) i.next() ).actionProxyDisposed( e );
        }
    }


    void fireProxyCreated( AbstractProxy b )
    {
        Iterator i = listProxyEventListener_.iterator();
        ProxyEvent e = new ProxyEvent( b );

        while ( i.hasNext() )
        {
            ( ( ProxyEventListener ) i.next() ).actionProxyCreated( e );
        }
    }


    public void remove( AbstractProxy pb )
    {
        super.remove( pb );

        Integer _key = pb.getKey();

        if ( _key != null )
        {
            synchronized(modifyProxiesLock_) {
                allProxies_.remove( _key );
            }

            if ( pb instanceof StructuredProxyPullConsumerImpl
                    || pb instanceof ProxyPullConsumerImpl
                    || pb instanceof SequenceProxyPullConsumerImpl )
            {
                synchronized(modifyProxiesLock_) {
                    pullServants_.remove( _key );
                }

            }
            else if ( pb instanceof StructuredProxyPushConsumerImpl
                      || pb instanceof ProxyPushConsumerImpl
                      || pb instanceof SequenceProxyPushConsumerImpl )
            {

                synchronized(modifyProxiesLock_) {
                    pushServants_.remove( _key );
                }
            }
        }
        else
        {
            synchronized(modifyProxiesLock_) {
                eventStyleServants_.remove( pb );
            }
        }

        fireProxyRemoved( pb );
    }


    public boolean hasOrSemantic()
    {
        return false;
    }


    public void addProxyEventListener( ProxyEventListener l )
    {
        listProxyEventListener_.add( l );
    }


    public void removeProxyEventListener( ProxyEventListener l )
    {
        listProxyEventListener_.remove( l );
    }
}