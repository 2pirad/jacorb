package org.jacorb.notification.engine;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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
import org.jacorb.notification.engine.TaskExecutor;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractTask.java,v 1.10 2004-05-09 19:01:42 alphonse.bendt Exp $
 */

public abstract class AbstractTask
    extends AbstractPoolable
    implements Runnable
{
    protected Message message_;

    private TaskProcessor taskProcessor_;

    private TaskExecutor executor_;

    ////////////////////

    protected AbstractTask(TaskProcessor tp) {
        taskProcessor_ = tp;
    }

    ////////////////////

    protected TaskExecutor getTaskExecutor() {
        return executor_;
    }


    protected void setTaskExecutor(TaskExecutor taskExecutor) {
        executor_ = taskExecutor;
    }


    protected TaskProcessor getTaskProcessor() {
        return taskProcessor_;
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
            else if ( isMessageInvalid() )
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
    protected void schedule(boolean directRunAllowed)
        throws InterruptedException
    {
        if (directRunAllowed) {
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
    protected void schedule(TaskExecutor executor,
                            boolean directRunAllowed)
        throws InterruptedException
    {
        if (directRunAllowed) {
            run();
        } else {
            executor.execute(this);
        }
    }

    abstract public void schedule() throws InterruptedException;
}
