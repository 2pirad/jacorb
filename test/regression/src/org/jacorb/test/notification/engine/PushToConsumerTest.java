package org.jacorb.test.notification.engine;

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

import org.jacorb.notification.engine.PushToConsumerTask;
import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.interfaces.MessageConsumer;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.queue.BoundedPriorityEventQueue;
import org.jacorb.notification.queue.EventQueue;
import org.jacorb.notification.queue.EventQueueOverflowStrategy;
import org.jacorb.test.notification.MockMessage;

import org.omg.CORBA.Any;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TRANSIENT;

import java.util.Iterator;
import java.util.Vector;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import org.jacorb.notification.conf.Configuration;
import org.jacorb.notification.conf.Default;
import org.jacorb.util.Environment;

/**
 *  Unit Test for class PushToConsumer
 *
 *
 * Created: Mon Aug 18 16:11:37 2003
 *
 * @author Alphonse Bendt
 * @version $Id: PushToConsumerTest.java,v 1.7 2004-01-23 19:45:06 alphonse.bendt Exp $
 */

public class PushToConsumerTest extends TestCase
{

    TaskProcessor taskProcessor_;
    ORB orb = ORB.init();

    public void setUp() throws Exception {

        taskProcessor_ = new TaskProcessor();

    }

    public void tearDown() throws Exception {
        taskProcessor_.dispose();
    }

    public void testRepeatedDeliveryErrorsCauseTheConsumerToBeDisconnected() throws Exception {
        MockMessage msg = new MockMessage();

        Any any = orb.create_any();

        msg.setAny(any);

        PushToConsumerTask task =
            new PushToConsumerTask(taskProcessor_,
                                   taskProcessor_.getTaskFactory());

        task.setMessage(msg.getHandle());

        MockEventConsumer eventConsumer = new MockEventConsumer() {
                int counter = 0;
                boolean enabled = true;

                public boolean hasPendingMessages() {
                    return true;
                }

                public void deliverPendingMessages() {
                    throw new TRANSIENT();
                }

                public void deliverMessage(Message event) {
                    counter++;
                    if (enabled) {
                        throw new TRANSIENT();
                    }
                }

                public void enableDelivery() {
                    super.enableDelivery();

                    enabled = true;
                }

                public void disableDelivery() {
                    super.disableDelivery();

                    enabled = false;
                }

                public void check() {
                    super.check();
                    Assert.assertTrue(counter > 0);
                }
            };

        eventConsumer.
            setErrorThreshold(Environment.getIntPropertyWithDefault(Configuration.EVENTCONSUMER_ERROR_THRESHOLD,
                                                                    Default.DEFAULT_EVENTCONSUMER_ERROR_THRESHOLD) );

        eventConsumer.expectedDisposeCalls = 1;

        task.setMessageConsumer(eventConsumer);

        task.schedule();

        //taskProcessor_.schedulePushToConsumerTask(task);

        long sleepTime =
            Environment.getIntPropertyWithDefault( Configuration.BACKOUT_INTERVAL,
                                                   Default.DEFAULT_BACKOUT_INTERVAL )
            * (Environment.getIntPropertyWithDefault(Configuration.EVENTCONSUMER_ERROR_THRESHOLD,
                                                    Default.DEFAULT_EVENTCONSUMER_ERROR_THRESHOLD)
            + 2);

        Thread.sleep(sleepTime);

        eventConsumer.check();
    }

    public void testPushFailRetry() throws Exception {

        MockMessage event1 =
            new MockMessage();

        Any any1 = orb.create_any();

        event1.setAny(any1);

        MockMessage event2 =
            new MockMessage();

        Any any2 = orb.create_any();

        event2.setAny(any2);

        PushToConsumerTask task =
            new PushToConsumerTask(taskProcessor_,
                                   taskProcessor_.getTaskFactory());


        task.setMessage(event1.getHandle());


        PushToConsumerTask task2 =
            new PushToConsumerTask(taskProcessor_,
                                   taskProcessor_.getTaskFactory());


        task2.setMessage(event2.getHandle());


        MockEventConsumer eventConsumer = new MockEventConsumer() {
                boolean once = false;
                public void deliverMessage(Message event) {
                    if (!once) {
                        once = true;
                        throw new TRANSIENT();
                    } else {
                        super.deliverMessage(event);
                    }
                }
            };

        task.setMessageConsumer(eventConsumer);

        task2.setMessageConsumer(eventConsumer);

        task.schedule();

        //taskProcessor_.schedulePushToConsumerTask(task);

        Thread.sleep(100);

        task2.schedule();

        //taskProcessor_.schedulePushToConsumerTask(task2);

        Thread.sleep(4000);

        eventConsumer.addToExcepectedEvents(any1);
        eventConsumer.addToExcepectedEvents(any2);

        eventConsumer.check();
    }

    public void testPushFailDispose() throws Exception {

        PushToConsumerTask task =
            new PushToConsumerTask(taskProcessor_,
                                   taskProcessor_.getTaskFactory());

        MockMessage event =
            new MockMessage();

        Any any = orb.create_any();

        event.setAny(any);

        //        task.setTaskFinishHandler(taskProcessor_.getTaskConfigurator().deliverTaskFinishHandler_);
//         task.setTaskErrorHandler(taskProcessor_.getTaskConfigurator().deliverTaskErrorHandler_);
        task.setMessage(event.getHandle());

        MockEventConsumer eventConsumer = new MockEventConsumer()  {
                public void deliverMessage(Message event) {
                    throw new OBJECT_NOT_EXIST();
                }
            };

        task.setMessageConsumer(eventConsumer);

        task.schedule();

        //taskProcessor_.schedulePushToConsumerTask(task);

        Thread.sleep(1000);

        eventConsumer.check();


        assertTrue(eventConsumer.disposeCalled == 1);
    }

    public void testPush() throws Exception {

        PushToConsumerTask task =
            new PushToConsumerTask(taskProcessor_,
                                   taskProcessor_.getTaskFactory());

        MockMessage event =
            new MockMessage("testEvent");

        Any any = orb.create_any();
        any.insert_string("test");

        event.setAny(any);

        task.setMessage(event.getHandle());

        MockEventConsumer eventConsumer = new MockEventConsumer();

        eventConsumer.addToExcepectedEvents(any);

        task.setMessageConsumer(eventConsumer);

        task.schedule();

        Thread.sleep(1000);

        eventConsumer.check();
    }

    /**
     * Creates a new <code>PushToConsumerTest</code> instance.
     *
     * @param name test name
     */
    public PushToConsumerTest (String name)
    {
        super(name);
    }

    /**
     * @return a <code>TestSuite</code>
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(PushToConsumerTest.class);

        suite = new TestSuite();
        suite.addTest(new PushToConsumerTest("testRepeatedDeliveryErrorsCauseTheConsumerToBeDisconnected"));

        return suite;
    }

    /**
     * Entry point
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

}
