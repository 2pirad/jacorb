package org.jacorb.test.notification.mocks;

import org.omg.CosNotification.EventType;
import org.omg.CosNotifyComm.InvalidEventType;
import org.omg.CosNotifyComm.PushSupplierPOA;



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

/**
 * @author Alphonse Bendt
 * @version $Id: MockPushSupplier.java,v 1.3 2004-08-01 17:55:28 alphonse.bendt Exp $
 */

public class MockPushSupplier extends PushSupplierPOA {

    // Implementation of org.omg.CosNotifyComm.NotifySubscribeOperations
    public void subscription_change(EventType[] eventTypeArray,
                                    EventType[] eventTypeArray1)
        throws InvalidEventType {

    }

    // Implementation of org.omg.CosEventComm.PushSupplierOperations
    public void disconnect_push_supplier() {

    }

}
