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

import org.omg.PortableServer.Servant;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosEventComm.PullSupplier;
import org.omg.CosEventChannelAdmin.ProxyPullConsumerPOATie;
import org.omg.CosEventChannelAdmin.ProxyPullConsumerHelper;
import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.PropertyManager;

/**
 * @author Alphonse Bendt
 * @version $Id: ECProxyPullConsumerImpl.java,v 1.1 2004-01-23 19:41:53 alphonse.bendt Exp $
 */
public class ECProxyPullConsumerImpl
    extends ProxyPullConsumerImpl
    implements org.omg.CosEventChannelAdmin.ProxyPullConsumerOperations {

    ECProxyPullConsumerImpl( AbstractAdmin admin,
                             ChannelContext channelContext,
                             PropertyManager adminProperties,
                             PropertyManager qosProperties,
                             Integer key)
    {

        super( admin,
               channelContext,
               adminProperties,
               qosProperties,
               key);

        isKeyPublic_ = false;
    }

    ////////////////////////////////////////

    public synchronized Servant getServant()
    {
        if ( thisServant_ == null )
            {
                thisServant_ = new ProxyPullConsumerPOATie( this );
            }

        return thisServant_;
    }


    public void connect_pull_supplier( PullSupplier pullSupplier )
        throws AlreadyConnected
    {
        connect_any_pull_supplier( pullSupplier );
    }


    public org.omg.CORBA.Object getCorbaRef() {
        return ProxyPullConsumerHelper.narrow(getServant()._this_object(getORB()));
    }

}
