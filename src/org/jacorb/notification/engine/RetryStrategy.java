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

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CosEventComm.Disconnected;

import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.MessageConsumer;

import org.apache.avalon.framework.logger.Logger;

/**
 * @author Alphonse Bendt
 * @version $Id: RetryStrategy.java,v 1.3 2004-05-06 12:39:59 nicolas Exp $
 */
public abstract class RetryStrategy implements Disposable
{
    protected PushOperation pushOperation_;

    protected MessageConsumer messageConsumer_;

    ////////////////////////////////////////

    public RetryStrategy(MessageConsumer mc, PushOperation operation)
    {
        messageConsumer_ = mc;
        pushOperation_ = operation;
    }

    ////////////////////////////////////////

    public void dispose() {
        pushOperation_.dispose();
    }


    public boolean shouldRetry()
    {
        return !messageConsumer_.isDisposed()
            && (messageConsumer_.getErrorCounter() < messageConsumer_.getErrorThreshold());
    }


    public void remoteExceptionOccured(Throwable error)
        throws RetryException
    {
        if (isFatalException(error)) {
            messageConsumer_.dispose();
            dispose();

            throw new RetryException("fatal exception caused while retrying push");
        }

        messageConsumer_.incErrorCounter();

        if (!shouldRetry())
        {
            messageConsumer_.dispose();
            dispose();

            throw new RetryException("no more retries. giving up.");
        }

        waitUntilNextTry();
    }


    public static boolean isFatalException(Throwable error) {
        if (error instanceof OBJECT_NOT_EXIST) {
            return true;
        } else if (error instanceof Disconnected) {
            return true;
        }
        return false;
    }


    protected abstract long getTimeToWait();


    public abstract void retry() throws RetryException;


    private void waitUntilNextTry()
    {
        long timeToWait = getTimeToWait();

        try
        {
            Thread.sleep(timeToWait );
        }
        catch (InterruptedException ignored)
        {}
    }
}
