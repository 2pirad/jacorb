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

import org.jacorb.test.common.ClientServerSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.jacorb.notification.EventChannelFactoryImpl;

/**
 * NotificationTestCaseSetup.java
 *
 * @author Alphonse Bendt
 * @version $Id: NotificationTestCaseSetup.java,v 1.1 2003-06-05 13:12:00 alphonse.bendt Exp $
 */

public class NotificationTestCaseSetup extends ClientServerSetup {

    private TestUtils testUtils_;

    public TestUtils getTestUtils() {
	return(testUtils_);
    }

    public NotificationTestCaseSetup(Test suite, String servantname) throws Exception {
	super(suite, servantname);
    } 

    public void setUp() throws Exception {
	super.setUp();
	testUtils_ = new TestUtils(getClientOrb());
    }

    public void tearDown() throws Exception {
	super.tearDown();
    }

    public NotificationTestCaseSetup(Test suite) throws Exception {
	this(suite, EventChannelFactoryImpl.class.getName());
    }
    
}
