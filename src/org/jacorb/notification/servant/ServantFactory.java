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

import org.omg.CosNotifyChannelAdmin.ClientType;
import org.omg.CORBA.IntHolder;
import org.omg.CosNotifyChannelAdmin.AdminLimitExceeded;
import org.jacorb.notification.PropertyManager;
import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.EventChannelImpl;
import org.omg.CORBA.BAD_PARAM;

/**
 * @author Alphonse Bendt
 * @version $Id: ServantFactory.java,v 1.1 2004-01-23 19:41:53 alphonse.bendt Exp $
 */

public class ServantFactory {

    private ServantFactory() {

    }

    ////////////////////////////////////////

    public AbstractProxy obtain_notification_pull_consumer_servant( AbstractAdmin admin,
                                                                    int proxyId,
                                                                    ChannelContext channelContext,
                                                                    PropertyManager adminProperties,
                                                                    PropertyManager qosProperties,
                                                                    ClientType clientType,
                                                                    IntHolder intHolder )
        throws AdminLimitExceeded
    {

        //        fireCreateProxyRequestEvent();

        intHolder.value = proxyId; //getPullProxyId();
        Integer _key = new Integer( intHolder.value );
        AbstractProxy _servant;

        switch ( clientType.value() )
            {

            case ClientType._ANY_EVENT:

                _servant = new ProxyPullConsumerImpl( admin,
                                                      channelContext,
                                                      adminProperties,
                                                      qosProperties,
                                                      _key );

                break;

            case ClientType._STRUCTURED_EVENT:

                _servant =
                    new StructuredProxyPullConsumerImpl( admin,
                                                         channelContext,
                                                         adminProperties,
                                                         qosProperties,
                                                         _key );

                break;

            case ClientType._SEQUENCE_EVENT:

                _servant =
                    new SequenceProxyPullConsumerImpl( admin,
                                                       channelContext,
                                                       adminProperties,
                                                       qosProperties,
                                                       _key );

                break;

            default:
                throw new BAD_PARAM();
            }

//         pullServants_.put( _key, _servant );

//         if ( filterGroupOperator_.value() == InterFilterGroupOperator._OR_OP )
//             {
//                 _servant.setOrSemantic( true );
//             }

//         _servant.addProxyDisposedEventListener( channelContext_.getRemoveProxyConsumerListener() );

        return _servant;
    }

}
