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

import junit.framework.Test;

import org.easymock.MockControl;
import org.jacorb.notification.OfferManager;
import org.jacorb.notification.SubscriptionManager;
import org.jacorb.notification.engine.TaskExecutor;
import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.servant.IAdmin;
import org.jacorb.notification.servant.StructuredProxyPushSupplierImpl;
import org.jacorb.test.notification.NotificationTestCase;
import org.jacorb.test.notification.NotificationTestCaseSetup;
import org.omg.CosEventChannelAdmin.AlreadyConnected;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotifyComm.StructuredPushConsumer;

/**
 * @author Alphonse Bendt
 * @version $Id: StructuredProxyPushSupplierImplTest.java,v 1.1 2005-02-14 00:17:38 alphonse.bendt Exp $
 */
public class StructuredProxyPushSupplierImplTest extends NotificationTestCase
{
    private StructuredProxyPushSupplierImpl objectUnderTest_;

    private MockControl controlTaskExecutor_;

    private TaskExecutor mockTaskExecutor_;

    private MockControl controlTaskProcessor_;

    private TaskProcessor mockTaskProcessor_;

    /*
     * @see TestCase#setUp()
     */
    protected void setUpTest() throws Exception
    {
        MockControl controlAdmin = MockControl.createControl(IAdmin.class);
        IAdmin mockAdmin = (IAdmin) controlAdmin.getMock();

        mockAdmin.isIDPublic();
        controlAdmin.setReturnValue(true);

        mockAdmin.getProxyID();
        controlAdmin.setReturnValue(10);

        mockAdmin.getContainer();
        controlAdmin.setReturnValue(null);

        controlAdmin.replay();

        controlTaskProcessor_ = MockControl.createControl(TaskProcessor.class);
        mockTaskProcessor_ = (TaskProcessor) controlTaskProcessor_.getMock();
        controlTaskExecutor_ = MockControl.createControl(TaskExecutor.class);
        mockTaskExecutor_ = (TaskExecutor) controlTaskExecutor_.getMock();
        objectUnderTest_ = new StructuredProxyPushSupplierImpl(mockAdmin, getORB(), getPOA(),
                getConfiguration(), mockTaskProcessor_, mockTaskExecutor_, new OfferManager(),
                new SubscriptionManager());

        assertEquals(new Integer(10), objectUnderTest_.getID());
    }

    /**
     * Constructor for StructuredProxyPushSupplierImplTest.
     * 
     * @param name
     */
    public StructuredProxyPushSupplierImplTest(String name, NotificationTestCaseSetup setup)
    {
        super(name, setup);
    }

    public void testDeliverMessage_NotConnectedDoesNotAccessMessage()
    {
        MockControl controlMessage = MockControl.createControl(Message.class);
        Message mockMessage = (Message) controlMessage.getMock();

        controlMessage.replay();

        controlTaskExecutor_.replay();

        controlTaskProcessor_.replay();

        objectUnderTest_.deliverMessage(mockMessage);

        controlMessage.verify();

        controlTaskExecutor_.verify();

        controlTaskProcessor_.verify();
    }

    public void testDeliverMessage_EnqueueClonesMessages() throws Exception
    {
        MockControl controlMessage = MockControl.createControl(Message.class);
        Message mockMessage = (Message) controlMessage.getMock();

        mockMessage.clone();
        controlMessage.setReturnValue(mockMessage);

        controlMessage.replay();

        controlTaskExecutor_.replay();

        controlTaskProcessor_.replay();

        MockControl controlStructuredPushConsumer = MockControl
                .createControl(StructuredPushConsumer.class);
        StructuredPushConsumer mockStructuredPushConsumer = (StructuredPushConsumer) controlStructuredPushConsumer
                .getMock();

        objectUnderTest_.connect_structured_push_consumer(mockStructuredPushConsumer);

        objectUnderTest_.disableDelivery();

        objectUnderTest_.deliverMessage(mockMessage);

        controlMessage.verify();

        controlTaskExecutor_.verify();

        controlTaskProcessor_.verify();
    }

    public void testDeliverMessageDoesNotCloneMessage() throws Exception
    {
        StructuredEvent event = new StructuredEvent();

        MockControl controlMessage = MockControl.createControl(Message.class);
        Message mockMessage = (Message) controlMessage.getMock();

        mockMessage.toStructuredEvent();
        controlMessage.setReturnValue(event);

        controlMessage.replay();

        controlTaskExecutor_.replay();

        controlTaskProcessor_.replay();

        MockControl controlStructuredPushConsumer = MockControl
                .createControl(StructuredPushConsumer.class);
        StructuredPushConsumer mockStructuredPushConsumer = (StructuredPushConsumer) controlStructuredPushConsumer
                .getMock();

        mockStructuredPushConsumer.push_structured_event(event);

        controlStructuredPushConsumer.replay();

        objectUnderTest_.connect_structured_push_consumer(mockStructuredPushConsumer);

        objectUnderTest_.deliverMessage(mockMessage);

        controlMessage.verify();

        controlTaskExecutor_.verify();

        controlTaskProcessor_.verify();
    }

    public static Test suite() throws Exception
    {
        return NotificationTestCase.suite(StructuredProxyPushSupplierImplTest.class);
    }
}