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

import org.jacorb.test.common.TestUtils;

import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.omg.CosNotification.AnyOrder;
import org.omg.CosNotification.BestEffort;
import org.omg.CosNotification.ConnectionReliability;
import org.omg.CosNotification.DeadlineOrder;
import org.omg.CosNotification.DiscardPolicy;
import org.omg.CosNotification.EventReliability;
import org.omg.CosNotification.FifoOrder;
import org.omg.CosNotification.LifoOrder;
import org.omg.CosNotification.MaxEventsPerConsumer;
import org.omg.CosNotification.OrderPolicy;
import org.omg.CosNotification.Persistent;
import org.omg.CosNotification.Priority;
import org.omg.CosNotification.PriorityOrder;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.EventChannel;
import org.omg.CosNotifyChannelAdmin.EventChannelFactory;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created: Mon Aug 11 21:21:21 2003
 *
 * @author Alphonse Bendt
 * @version $Id: QoSTest.java,v 1.3 2003-11-26 10:15:00 alphonse.bendt Exp $
 */

public class QoSTest extends NotificationTestCase
{

    EventChannelFactory factory_;
    Any fifoOrder;
    Any lifoOrder;
    Any deadlineOrder;
    Any priorityOrder;
    Any anyOrder;
    Any persistent;
    Any bestEffort;

    public void setUp() throws Exception {
        factory_ = getEventChannelFactory();

        fifoOrder = getORB().create_any();
        fifoOrder.insert_short(FifoOrder.value);

        lifoOrder = getORB().create_any();
        lifoOrder.insert_short(LifoOrder.value);

        deadlineOrder = getORB().create_any();;
        deadlineOrder.insert_short(DeadlineOrder.value);

        priorityOrder = getORB().create_any();
        priorityOrder.insert_short(PriorityOrder.value);

        anyOrder = getORB().create_any();
        anyOrder.insert_short(AnyOrder.value);

        bestEffort = getORB().create_any();
        bestEffort.insert_short(BestEffort.value);

        persistent = getORB().create_any();
        persistent.insert_short(Persistent.value);
    }


    public void testCreate_QueueSettings() throws Exception {
        IntHolder channelId = new IntHolder();

        Property[] qosProps;

        qosProps = new Property[] {
            new Property( DiscardPolicy.value, priorityOrder ),
            new Property( OrderPolicy.value, priorityOrder )
        };

        factory_.create_channel( qosProps, new Property[0], channelId);
    }

    public void testCreate_Reliability() throws Exception {
        IntHolder channelId = new IntHolder();

        Property[] qosProps;

        qosProps = new Property[] {
            new Property( ConnectionReliability.value, bestEffort ),
            new Property( EventReliability.value, bestEffort )
        };

        factory_.create_channel( qosProps, new Property[0], channelId);

        qosProps = new Property[] {
            new Property( ConnectionReliability.value, persistent ),
            new Property( EventReliability.value, persistent )
        };

        try {
            factory_.create_channel( qosProps,
                                     new Property[0],
                                     channelId);
            fail();
        } catch (UnsupportedQoS e) {
        }
    }


    /**
     * test if events are reorderd respecting their priority.
     * a supplier pushes some events with ascending priority into a
     * channel that was setup with OrderPolicy=PriorityOrder. A
     * Consumer receives and checks if the Events are delivered in
     * descending Priority order.
     */
    public void testPriorityOrder() throws Exception {

        // create and setup channel
        IntHolder channelId = new IntHolder();

        Property[] qosProps;

        qosProps = new Property[] {
            new Property( OrderPolicy.value, priorityOrder )
        };

        EventChannel channel =
            factory_.create_channel( qosProps,
                                     new Property[0],
                                     channelId);

        // testdata
        StructuredEvent[] events = new StructuredEvent[10];
        for (int x=0; x<events.length; ++x) {
            events[x] = getTestUtils().getStructuredEvent();

            Any priority = getORB().create_any();
            priority.insert_short((short)x);

            events[x].header.variable_header =
                new Property[] {
                    new Property(Priority.value, priority)
                };

        }

        // setup clients
        StructuredPushReceiver receiver =
            new StructuredPushReceiver(this, events.length);

        receiver.connect(getSetup(), channel, false);

        receiver.pushSupplier_.suspend_connection();

        StructuredPushSender sender =
            new StructuredPushSender(this, events, 100);

        sender.connect(getSetup(), channel, false);

        // push events
        sender.run();

        assertFalse(receiver.isEventHandled());

        receiver.pushSupplier_.resume_connection();

        receiver.setTimeOut(events.length * 1000);

        receiver.run();

        assertTrue(receiver.isEventHandled());

        while (!receiver.receivedEvents.isEmpty()) {
            StructuredEvent event =
                (StructuredEvent)receiver.receivedEvents.remove(0);

            Iterator i = receiver.receivedEvents.iterator();
            while (i.hasNext()) {
                short p1 = event.header.variable_header[0].value.extract_short();

                short p2 = ((StructuredEvent)i.next()).header.variable_header[0].value.extract_short();

                assertTrue(p1 + " > " + p2,  p1 > p2);
            }
        }
    }

    /**
     * Creates a new <code>QoSTest</code> instance.
     *
     * @param name test name
     */
    public QoSTest (String name, NotificationTestCaseSetup setup)
    {
        super(name, setup);
    }

    /**
     * @return a <code>TestSuite</code>
     */
    public static Test suite() throws Exception
    {
        TestSuite _suite;

        _suite = new TestSuite("Basic QoS Tests");

        NotificationTestCaseSetup _setup =
            new NotificationTestCaseSetup(_suite);

        String[] methodNames = TestUtils.getTestMethods( QoSTest.class );

        for (int x=0; x<methodNames.length; ++x) {
            _suite.addTest(new QoSTest(methodNames[x], _setup));
        }

        return _setup;
    }

    /**
     * Entry point
     */
    public static void main(String[] args) throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }

}// QoSTest
