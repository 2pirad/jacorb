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
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import org.jacorb.notification.ApplicationContext;
import org.jacorb.notification.ChannelContext;
import org.jacorb.notification.ConsumerAdminTieImpl;
import org.jacorb.notification.PropertyManager;
import org.jacorb.notification.interfaces.ApplicationEvent;
import org.jacorb.notification.interfaces.ProxyCreationRequestEvent;
import org.jacorb.notification.interfaces.ProxyCreationRequestEventListener;

import java.util.List;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *  Unit Test for class AdminLimit
 *
 *
 * Created: Wed Feb 12 19:21:45 2003
 *
 * @author Alphonse Bendt
 * @version $Id: AdminLimitTest.java,v 1.2 2003-08-25 21:00:46 alphonse.bendt Exp $
 */

public class AdminLimitTest extends TestCase {

    ConsumerAdminTieImpl consumerAdmin_;
    ChannelContext channelContext_;
    List events_;
    int counter_;
    ApplicationContext appContext_;

    public void setUp() throws Exception {
        ORB _orb = ORB.init(new String[0], null);
        POA _poa = POAHelper.narrow(_orb.resolve_initial_references("RootPOA"));

        appContext_ = new ApplicationContext(_orb, _poa);

        channelContext_ = new ChannelContext();

        PropertyManager _adminProps = new PropertyManager(appContext_);
        PropertyManager _qosProps = new PropertyManager(appContext_);

        consumerAdmin_ =
            new ConsumerAdminTieImpl(appContext_, channelContext_, _adminProps, _qosProps);

        events_ = new Vector();
    }

    public void tearDown() {
        appContext_.dispose();
    }

    public void testObtainNotificationPullSupplierFiresEvent() throws Exception {
        IntHolder _proxyId = new IntHolder();

        ProxyCreationRequestEventListener _listener =
            new ProxyCreationRequestEventListener() {
                public void actionProxyCreationRequest(ProxyCreationRequestEvent e)
                    throws AdminLimitExceeded {

                    events_.add(e);
                }
            };

        consumerAdmin_.addProxyCreationEventListener(_listener);

        ProxySupplier _proxySupplier =
            consumerAdmin_.obtain_notification_pull_supplier(ClientType.STRUCTURED_EVENT, _proxyId);

        assertTrue(events_.size() == 1);
        assertEquals(consumerAdmin_, ((ApplicationEvent)events_.get(0)).getSource());
    }

    public void testDenyCreateNotificationPullSupplier() throws Exception {
        IntHolder _proxyId = new IntHolder();

        ProxyCreationRequestEventListener _listener =
            new ProxyCreationRequestEventListener() {
                public void actionProxyCreationRequest(ProxyCreationRequestEvent e)
                    throws AdminLimitExceeded {

                    throw new AdminLimitExceeded();

                }
            };

        consumerAdmin_.addProxyCreationEventListener(_listener);

        try {
            ProxySupplier _proxySupplier =
                consumerAdmin_.obtain_notification_pull_supplier(ClientType.STRUCTURED_EVENT, _proxyId);

            fail();
        } catch (AdminLimitExceeded e) {}
    }

    public void testEvents() throws Exception {
        IntHolder _proxyId = new IntHolder();

        ProxyCreationRequestEventListener _listener =
            new ProxyCreationRequestEventListener() {

                public void actionProxyCreationRequest(ProxyCreationRequestEvent e)
                    throws AdminLimitExceeded {

                    counter_++;
                }
            };

        consumerAdmin_.addProxyCreationEventListener(_listener);

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

    /**
     * Creates a new <code>AdminLimitTest</code> instance.
     *
     * @param name test name
     */
    public AdminLimitTest (String name){
        super(name);
    }

    /**
     * @return a <code>TestSuite</code>
     */
    public static Test suite(){
        TestSuite suite = new TestSuite(AdminLimitTest.class);

        return suite;
    }

    public static void main(String[] args)  {
        junit.textui.TestRunner.run(suite());
    }

}
