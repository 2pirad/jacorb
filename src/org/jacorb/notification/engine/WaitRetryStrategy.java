package org.jacorb.notification.engine;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2004 Gerald Brose.
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

/**
 * @author Alphonse Bendt
 * @version $Id: WaitRetryStrategy.java,v 1.4 2005-02-14 00:03:09 alphonse.bendt Exp $
 */

public class WaitRetryStrategy extends RetryStrategy
{
    public static final long WAIT_TIME_DEFAULT = 1000;

    public static final long WAIT_INCREMENT_DEFAULT = 3000;

    private long currentTimeToWait_;

    private long waitTimeIncrement_;

    ////////////////////////////////////////

    public WaitRetryStrategy(MessageConsumer messageConsumer,
                             PushOperation pushOperation)
    {
        this(messageConsumer,
             pushOperation,
             WAIT_TIME_DEFAULT,
             WAIT_INCREMENT_DEFAULT);
    }


    public WaitRetryStrategy(MessageConsumer messageConsumer,
                             PushOperation pushOperation,
                             long startingWaitTime,
                             long waitTimeIncrement)
    {
        super(messageConsumer, pushOperation);

        currentTimeToWait_ = startingWaitTime;

        waitTimeIncrement_ = waitTimeIncrement;
    }

    ////////////////////////////////////////

    protected long getTimeToWait()
    {
        long _timeToWait = currentTimeToWait_;

        currentTimeToWait_ += waitTimeIncrement_;

        return _timeToWait;
    }


    protected void retryInternal() throws RetryException {
        while (isRetryAllowed()) {
            try {
                pushOperation_.invokePush();

                dispose();
                
                return;
            } catch (Throwable error) {
                remoteExceptionOccured(error);
            }
        }
        throw new RetryException("no more retries possible");
    }
}

