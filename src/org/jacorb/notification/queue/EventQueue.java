package org.jacorb.notification.queue;

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

import org.jacorb.notification.interfaces.Message;

/**
 * EventQueue.java
 *
 *
 * Created: Sat Aug  9 11:43:26 2003
 *
 * @author Alphonse Bendt
 * @version $Id: EventQueue.java,v 1.2 2003-11-17 12:04:11 alphonse.bendt Exp $
 */

public interface EventQueue
{

    /**
     * get the next Message from this queue. which particular event is
     * selected depends on the underlying implementation.
     *
     * @param wait a <code>boolean</code> value. If this parameter is
     * set to true the queue will block until an element is
     * available. If the parameter is set to false the queue will
     * return false in case it is empty.
     *
     * @exception InterruptedException
     */
    Message getEvent( boolean wait )
        throws InterruptedException;

    /**
     * get up to <code>n</code> events from this queue.
     */
    Message[] getEvents( int n, boolean wait )
        throws InterruptedException;

    /**
     * get all Messages from this queue.
     */
    Message[] getAllEvents( boolean wait )
        throws InterruptedException;

    /**
     * put a Message into this queue.
     */
    void put( Message event );

    /**
     * check if this queue is empty.
     */
    boolean isEmpty();

    /**
     * access the current size of this queue.
     */
    int getSize();

}
