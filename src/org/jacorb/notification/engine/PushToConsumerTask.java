package org.jacorb.notification.engine;

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

/**
 * @author Alphonse Bendt
 * @version $Id: PushToConsumerTask.java,v 1.10.2.2 2004-04-02 05:30:36 phil.mesnier Exp $
 */

public class PushToConsumerTask extends AbstractDeliverTask
{
    private static int COUNT = 0;

    private int id_ = ++COUNT;

    ////////////////////

    public PushToConsumerTask(TaskProcessor tp) {
        super(tp);
    }

    ////////////////////

    public void doWork() throws Exception
    {
        if ( logger_.isDebugEnabled() )
        {
            logger_.debug( this
                           + ".push "
                           + message_
                           + " to "
                           + getMessageConsumer() );
        }

        getMessageConsumer().deliverMessage( message_ );

        message_.dispose();

        getMessageConsumer().resetErrorCounter();

        dispose();
    }


    public String toString() {
        return "[PushToConsumerTask#" + id_ + "]";
    }
}
