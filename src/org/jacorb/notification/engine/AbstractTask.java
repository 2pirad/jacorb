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

import org.jacorb.notification.interfaces.AbstractPoolable;
import org.jacorb.notification.interfaces.Message;
import org.jacorb.util.Debug;

import org.apache.avalon.framework.logger.Logger;

/**
 * TaskBase.java
 *
 * @author Alphonse Bendt
 * @version $Id: AbstractTask.java,v 1.3 2003-11-03 10:32:43 alphonse.bendt Exp $
 */

public abstract class AbstractTask extends AbstractPoolable implements Task
{
    protected Logger logger_ = Debug.getNamedLogger( getClass().getName() );

    private TaskFinishHandler coordinator_;
    private TaskErrorHandler errorHandler_;
    protected Message message_;
    protected int status_;

    AbstractTask()
    {
        status_ = NEW;
    }

    /**
     * Set Status of this Task.
     */
    protected void setStatus( int status )
    {
        status_ = status;
    }

    /**
     * Get the current Status of this Task.
     */
    public int getStatus()
    {
        return status_;
    }

    /**
     *
     */
    public void setTaskFinishHandler( TaskFinishHandler coord )
    {
        coordinator_ = coord;
    }

    public void setTaskErrorHandler( TaskErrorHandler handler )
    {
        errorHandler_ = handler;
    }

    /**
     * set the Message for this Task to use.
     */
    public void setMessage( Message event )
    {
        if ( message_ != null )
        {
            throw new RuntimeException( "remove old first" );
        }

        message_ = event;
    }

    public Message removeMessage()
    {
        Message _event = message_;
        message_ = null;
        return _event;
    }


    public Message copyMessage()
    {
        return ( Message ) message_.clone();
    }

    /**
     * Override this Method in Subclasses to do the "real work".
     */
    public abstract void doWork() throws Exception;

    /**
     * template method.
     * <ol><li>Call doWork()
     * <li>Call TaskFinishHandler in case of success
     * <li>Call TaskErrorHandler in case a exception occurs while
     * executing doWork
     * </ol>
     */
    public void run()
    {
        try
        {
            if ( message_ == null || !isEventDisposed() )
            {
                doWork();
            }

            if ( isEventDisposed() )
            {
                logger_.debug( "event has been marked disposable" );
                setStatus( DISPOSABLE );
            }

            coordinator_.handleTaskFinished( this );
        }
        catch ( Throwable t )
        {
            errorHandler_.handleTaskError( this, t );
        }

    }

    public void reset()
    {
        coordinator_ = null;
        message_ = null;
        status_ = NEW;
    }

    private boolean isEventDisposed()
    {
        return message_ != null && message_.isInvalid();
    }

    protected void checkInterrupt() throws InterruptedException
    {
        if ( Thread.currentThread().isInterrupted() || message_.isInvalid() )
        {
            logger_.debug( "Worker Thread has been interrupted" );
            throw new InterruptedException();
        }
    }
}
