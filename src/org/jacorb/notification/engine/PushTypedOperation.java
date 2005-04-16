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

import org.jacorb.notification.interfaces.MessageConsumer;
import org.omg.CORBA.Request;
import org.omg.CosEventComm.Disconnected;

/**
 * @author Alphonse Bendt
 * @version $Id: PushTypedOperation.java,v 1.5 2005-04-16 23:11:20 alphonse.bendt Exp $
 */
public class PushTypedOperation implements PushOperation {

    private final Request request_;
    private final MessageConsumer messageConsumer_;

    public PushTypedOperation(MessageConsumer messageConsumer, Request request) {
        request_ = request;
        messageConsumer_ = messageConsumer;
    }

    public void invokePush() throws Disconnected {
        request_.invoke();
        messageConsumer_.resetErrorCounter();
    }

    public void dispose() {
        // No Op
    }
}
