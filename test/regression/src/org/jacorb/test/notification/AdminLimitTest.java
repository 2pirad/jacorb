package org.jacorb.test.notification;

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

import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;
import org.omg.CosEventChannelAdmin.ProxyPullSupplier;
import org.omg.CosNotifyChannelAdmin.AdminLimitExceeded;
import org.omg.CosNotifyChannelAdmin.ClientType;
import org.omg.CosNotifyChannelAdmin.ProxySupplier;

import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.interfaces.ApplicationEvent;
import org.jacorb.notification.interfaces.ProxyEvent;
import org.jacorb.notification.interfaces.ProxyEventListener;
import org.jacorb.notification.servant.ConsumerAdminImpl;
import org.jacorb.notification.util.QoSPropertySet;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

/**
 * @author Alphonse Bendt
 * @version $Id: AdminLimitTest.java,v 1.12 2004-05-09 19:37:25 alphonse.bendt Exp $
 */

public class AdminLimitTest extends NotificationTestCase
{
    ConsumerAdminImpl consumerAdmin_;
    ChannelContext channelContext_;
    int counter_;

    public void setUp() throws Exception
    {
        QoSPropertySet qosSettings_ =
            new QoSPropertySet(getConfiguration(), QoSPropertySet.ADMIN_QOS);

        channelContext_ = getChannelContext();

        channelContext_.setEventChannel(getDefaultChannel());

        consumerAdmin_ =
            new ConsumerAdminImpl();

        channelContext_.resolveDependencies(consumerAdmin_);

        consumerAdmin_.set_qos(qosSettings_.get_qos());
    }


    public void testObtainNotificationPullSupplierFiresEvent() throws Exception
    {
        IntHolder _proxyId = new IntHolder();

        final List _events = new ArrayList();

        ProxyEventListener _listener =
            new ProxyEventListener()
            {
                public void actionProxyCreationRequest(ProxyEvent event)
                    throws AdminLimitExceeded
                {
                    _events.add(event);
                }

                public void actionProxyCreated(ProxyEvent event) {
                }

                public void actionProxyDisposed(ProxyEvent event) {}
            };

        consumerAdmin_.addProxyEventListener(_listener);

        ProxySupplier _proxySupplier =
            consumerAdmin_.obtain_notification_pull_supplier(ClientType.STRUCTURED_EVENT, _proxyId);

        assertTrue(_events.size() == 1);

        assertEquals(consumerAdmin_, ((ApplicationEvent)_events.get(0)).getSource());
    }


    public void testDenyCreateNotificationPullSupplier() throws Exception
    {
        IntHolder _proxyId = new IntHolder();

        ProxyEventListener _listener =
            new ProxyEventListener()
            {
                public void actionProxyCreationRequest(ProxyEvent e)
                    throws AdminLimitExceeded
                {
                    throw new AdminLimitExceeded();
                }

                public void actionProxyDisposed(ProxyEvent event) {}

                public void actionProxyCreated(ProxyEvent event) {}
            };

        consumerAdmin_.addProxyEventListener(_listener);

        try
        {
            ProxySupplier _proxySupplier =
                consumerAdmin_.obtain_notification_pull_supplier(ClientType.STRUCTURED_EVENT, _proxyId);

            fail();
        }
        catch (AdminLimitExceeded e)
        {}
    }

    public void testEvents() throws Exception
    {
        IntHolder _proxyId = new IntHolder();

        ProxyEventListener _listener =
            new ProxyEventListener()
            {
                public void actionProxyCreated(ProxyEvent event) {}

                public void actionProxyDisposed(ProxyEvent event) {}

                public void actionProxyCreationRequest(ProxyEvent event)
                    throws AdminLimitExceeded
                {
                    counter_++;
                }
            };

        consumerAdmin_.addProxyEventListener(_listener);

        ProxySupplier[] _seqProxySupplier = new ProxySupplier[3];

        _seqProxySupplier[0] =
            consumerAdmin_.obtain_notification_pull_supplier(ClientType.STRUCTURED_EVENT, _proxyId);
        assertEquals(_seqProxySupplier[0], consumerAdmin_.get_proxy_supplier(_proxyId.value));

        _seqProxySupplier[1] =
            consumerAdmin_.obtain_notification_pull_supplier(ClientType.ANY_EVENT, _proxyId);
        assertEquals(_seqProxySupplier[1], consumerAdmin_.get_proxy_supplier(_proxyId.value));

        _seqProxySupplier[2] =
            consumerAdmin_.obtain_notification_pull_supplier(ClientType.SEQUENCE_EVENT, _proxyId);
        assertEquals(_seqProxySupplier[2], consumerAdmin_.get_proxy_supplier(_proxyId.value));

        ProxyPullSupplier _p =
            consumerAdmin_.obtain_pull_supplier();

        assertTrue(counter_ == 3);
    }


    public AdminLimitTest (String name, NotificationTestCaseSetup setup)
    {
        super(name, setup);
    }


    public static Test suite() throws Exception
    {
        return NotificationTestCase.suite(AdminLimitTest.class);
    }
}
