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

import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import org.jacorb.notification.EventChannelFactoryImpl;

import java.util.Properties;

import junit.extensions.TestSetup;
import junit.framework.Test;
import org.apache.avalon.framework.logger.Logger;

/**
 * @author Alphonse Bendt
 * @version $Id: NotificationTestCaseSetup.java,v 1.10.2.1 2004-04-07 15:00:15 alphonse.bendt Exp $
 */

public class NotificationTestCaseSetup extends TestSetup {

    private ORB orb_;
    private POA poa_;
    private Thread orbThread_;
    private NotificationTestUtils testUtils_;
    private EventChannelFactoryImpl eventChannelFactory_;

    ////////////////////////////////////////

    public NotificationTestCaseSetup(Test suite) throws Exception {
        super(suite);
    }

    ////////////////////////////////////////

    public NotificationTestUtils getTestUtils() {
        return(testUtils_);
    }


    public void setUp() throws Exception {
        super.setUp();

        orb_ = ORB.init(new String[0], null);
        poa_ = POAHelper.narrow(orb_.resolve_initial_references("RootPOA"));
        testUtils_ = new NotificationTestUtils(orb_);

        poa_.the_POAManager().activate();

        orbThread_ = new Thread(
                   new Runnable() {
                       public void run() {
                           orb_.run();
                       }
                   });

        orbThread_.setDaemon(true);
        orbThread_.start();
    }


    public void tearDown() throws Exception {
        super.tearDown();

        if (eventChannelFactory_ != null) {
            eventChannelFactory_.dispose();
        }

        orb_.shutdown(true);
    }


    public EventChannelFactoryImpl getFactoryServant() throws Exception {
        if (eventChannelFactory_ == null) {
            eventChannelFactory_ = EventChannelFactoryImpl.newFactory();
        }

        return eventChannelFactory_;
    }


    public ORB getORB() {
        return orb_;
    }


    public POA getPOA() {
        return poa_;
    }
}
