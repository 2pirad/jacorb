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

package org.jacorb.test.notification.servant;

import java.util.List;

import junit.framework.Test;

import org.easymock.MockControl;
import org.jacorb.notification.OfferManager;
import org.jacorb.notification.SubscriptionManager;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.servant.AbstractProxySupplier;
import org.jacorb.notification.servant.IAdmin;
import org.jacorb.test.notification.NotificationTestCase;
import org.jacorb.test.notification.NotificationTestCaseSetup;
import org.omg.CORBA.Object;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ProxyType;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractProxySupplierTest.java,v 1.2 2005-04-27 10:51:33 alphonse.bendt Exp $
 */
public class AbstractProxySupplierTest extends NotificationTestCase
{
    private AbstractProxySupplier objectUnderTest_;

    private MockControl controlMessage_;

    private Message mockMessage_;

    private MockControl controlClient_;

    private org.omg.CORBA.Object mockClient_;

    /*
     * @see TestCase#setUp()
     */
    protected void setUpTest() throws Exception
    {
        MockControl controlIAdmin = MockControl.createControl(IAdmin.class);
        IAdmin mockIAdmin = (IAdmin) controlIAdmin.getMock();

        mockIAdmin.getProxyID();
        controlIAdmin.setReturnValue(0);

        mockIAdmin.isIDPublic();
        controlIAdmin.setReturnValue(false);

        mockIAdmin.getContainer();
        controlIAdmin.setReturnValue(null);

        controlIAdmin.replay();

        MockControl controlConsumerAdmin = MockControl.createControl(ConsumerAdmin.class);
        ConsumerAdmin mockConsumerAdmin = (ConsumerAdmin) controlConsumerAdmin.getMock();

        MockControl controlPOA = MockControl.createNiceControl(POA.class);
        POA mockPOA = (POA) controlPOA.getMock();

        objectUnderTest_ = new AbstractProxySupplier(mockIAdmin, getORB(), mockPOA,
                getConfiguration(), getTaskProcessor(), new OfferManager(),
                new SubscriptionManager(), mockConsumerAdmin)
        {
            protected long getCost()
            {
                return 0;
            }

            public ProxyType MyType()
            {
                return ProxyType.PULL_ANY;
            }

            protected void disconnectClient()
            {
                // ignored
            }

            protected Servant getServant()
            {
                return null;
            }

            public void deliverPendingData()
            {
                // ignored
            }

            public MessageConsumer getMessageConsumer()
            {
                return null;
            }

            public List getSubsequentFilterStages()
            {
                return null;
            }

            public Object activate()
            {
                return null;
            }
        };

        controlMessage_ = MockControl.createControl(Message.class);
        mockMessage_ = (Message) controlMessage_.getMock();

        controlClient_ = MockControl.createControl(org.omg.CORBA.Object.class);
        mockClient_ = (org.omg.CORBA.Object) controlClient_.getMock();
    }

    public AbstractProxySupplierTest(String name, NotificationTestCaseSetup setup)
    {
        super(name, setup);
    }

    public void testNotConnectedSupplierDoesNotAccessMessage()
    {
        controlMessage_.replay();

        objectUnderTest_.deliverMessage(mockMessage_);

        controlMessage_.verify();
    }

    public void testConnectedSupplierDoesCloneMessage()
    {
        mockMessage_.clone();
        controlMessage_.setReturnValue(mockMessage_);

        controlMessage_.replay();

        controlClient_.replay();

        objectUnderTest_.connectClient(mockClient_);

        objectUnderTest_.deliverMessage(mockMessage_);

        controlMessage_.verify();
    }

    public void testDisposeDisposesPendingMessages() throws Exception
    {
        mockMessage_.clone();
        controlMessage_.setReturnValue(mockMessage_);

        mockMessage_.dispose();
        controlMessage_.replay();

        objectUnderTest_.connectClient(mockClient_);
        objectUnderTest_.deliverMessage(mockMessage_);
        objectUnderTest_.dispose();

        controlMessage_.verify();
    }

    public static Test suite() throws Exception
    {
        return NotificationTestCase.suite(AbstractProxySupplierTest.class);
    }
}
