package org.jacorb.notification.interfaces;

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

import org.omg.CosEventComm.Disconnected;

/**
 * Abstraction of a ProxyPullConsumer.
 *
 * The implementation maintains a connection to a PullSupplier.
 *
 * @author Alphonse Bendt
 * @version $Id: MessageSupplier.java,v 1.1 2004-02-20 12:41:54 alphonse.bendt Exp $
 */

public interface MessageSupplier extends Disposable {

    /**
     * the implementation pulls one or more events from its Supplier
     * and hands over the pulled events to the TaskProcessor.
     */
    void runPullMessage() throws Disconnected;
}
