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
import org.jacorb.test.notification.MockMessage;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.avalon.framework.configuration.Configuration;

/**
 * @author Alphonse Bendt
 * @version $Id: PushToConsumerTest.java,v 1.11.2.1 2004-04-07 15:00:15 alphonse.bendt Exp $
 */

public class PushToConsumerTest extends TestCase
{
    TaskProcessor taskProcessor_;
    ORB orb;
    private Configuration configuration_;

    public void setUp() throws Exception
    {
        taskProcessor_ = new TaskProcessor();

        orb = ORB.init(new String[] {}, null);

        configuration_ = ((org.jacorb.orb.ORB)orb).getConfiguration();

        taskProcessor_.configure( configuration_ );
    }


    public void tearDown() throws Exception
    {
        taskProcessor_.dispose();
    }


    public void testPush() throws Exception
    {
        PushToConsumerTask task =
            new PushToConsumerTask(taskProcessor_);

        task.configure(configuration_);

        MockMessage event =
            new MockMessage("testEvent");

        event.configure(configuration_);

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


    public PushToConsumerTest (String name)
    {
        super(name);
    }


    public static Test suite()
    {
        TestSuite suite = new TestSuite(PushToConsumerTest.class);

        return suite;
    }


    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
}
