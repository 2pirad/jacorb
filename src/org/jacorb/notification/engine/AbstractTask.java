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
import org.jacorb.notification.util.TaskExecutor;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractTask.java,v 1.4 2004-01-16 17:21:27 alphonse.bendt Exp $
 */

public abstract class AbstractTask
    extends AbstractPoolable
    implements Runnable
{
    protected Logger logger_ = Debug.getNamedLogger( getClass().getName() );

    protected Message message_;

    protected TaskProcessor taskProcessor_;

    protected TaskFactory taskFactory_;

    protected TaskExecutor executor_;

    ////////////////////

    protected AbstractTask() {
    }

    protected AbstractTask(TaskExecutor executor, TaskProcessor tp, TaskFactory tf) {
        executor_ = executor;
        taskProcessor_ = tp;
        taskFactory_ = tf;
    }

    ////////////////////

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
     * <ol>
     * <li>Call doWork()
     * </ol>
     */
    public void run()
    {
        try
        {
            if ( message_ == null || !isMessageInvalid() )
            {
                doWork();
            }

            if ( isMessageInvalid() )
            {
                dispose();

                return;
            }
        }
        catch ( Throwable t )
        {
            handleTaskError( this, t );
        }
    }

    abstract void handleTaskError(AbstractTask t, Throwable error);

    public void reset()
    {
        message_ = null;
    }


    private boolean isMessageInvalid()
    {
        return (message_ != null && message_.isInvalid());
    }


    protected void checkInterrupt() throws InterruptedException
    {
        if ( Thread.currentThread().isInterrupted() || message_.isInvalid() )
        {
            throw new InterruptedException();
        }
    }


    /**
     * Run this Task on its configured Executor.
     *
     * @param directRunAllowed this param specified if its allowed to
     * run this Task on the calling Thread.
     * @exception InterruptedException if an error occurs
     */
    public void schedule(boolean directRunAllowed) throws InterruptedException
    {
        if (directRunAllowed && executor_.isTaskQueued()) {
            run();
        } else {
            executor_.execute(this);
        }
    }


    /**
     * Run this Task on the provided Executor.
     *
     * @param executor a <code>TaskExecutor</code> value
     * @param directRunAllowed a <code>boolean</code> value
     * @exception InterruptedException if an error occurs
     */
    public void schedule(TaskExecutor executor, boolean directRunAllowed) throws InterruptedException
    {
        if (directRunAllowed  && executor_.isTaskQueued()) {
            run();
        } else {
            executor.execute(this);
        }
    }
}
