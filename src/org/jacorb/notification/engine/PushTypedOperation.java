package org.jacorb.notification.engine;

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

import org.omg.CosEventComm.Disconnected;
import org.omg.CosEventComm.PushConsumer;

import org.jacorb.notification.interfaces.Message;
import org.omg.CORBA.Request;

/**
 * @author Alphonse Bendt
 * @version $Id: PushTypedOperation.java,v 1.2 2004-05-09 19:01:42 alphonse.bendt Exp $
 */
public class PushTypedOperation implements PushOperation {

    private Request request_;

    public PushTypedOperation(Request request) {
        request_ = request;
    }

    public void invokePush() throws Disconnected {
        request_.invoke();
    }

    public void dispose() {
        // No Op
    }
}
