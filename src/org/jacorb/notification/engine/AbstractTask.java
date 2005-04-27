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

import org.jacorb.notification.util.AbstractPoolable;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractTask.java,v 1.15 2005-04-27 10:48:40 alphonse.bendt Exp $
 */

public abstract class AbstractTask extends AbstractPoolable implements Runnable, Schedulable
{    
    private TaskExecutor taskExecutor_;

    ////////////////////

    protected TaskExecutor getTaskExecutor()
    {
        return taskExecutor_;
    }

    protected void setTaskExecutor(TaskExecutor taskExecutor)
    {
        taskExecutor_ = taskExecutor;
    }

    /**
     * Override this Method in Subclasses to do the "real work".
     */
    public abstract void doWork() throws Exception;

    protected boolean isRunnable()
    {
        return true;
    }
    
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
            if (isRunnable())
            {
                doWork();
            }
        } catch (Throwable t)
        {
            handleTaskError(this, t);
        } finally
        {
            dispose();
        }
    }

    abstract void handleTaskError(AbstractTask t, Throwable error);

    
    protected void checkInterrupt() throws InterruptedException
    {
        if (Thread.currentThread().isInterrupted())
        {
            throw new InterruptedException();
        }
    }

    /**
     * Run this Task on its configured Executor.
     * 
     * @param directRunAllowed
     *            this param specified if its allowed to run this Task on the calling Thread.
     * @exception InterruptedException
     *                if an error occurs
     */
    protected void schedule(boolean directRunAllowed) throws InterruptedException
    {
        schedule(taskExecutor_, directRunAllowed);
    }

    /**
     * Run this Task on the provided Executor.
     * 
     * @param executor
     *            a <code>TaskExecutor</code> value
     * @param directRunAllowed
     *            a <code>boolean</code> value
     * @exception InterruptedException
     *                if an error occurs
     */
    protected void schedule(TaskExecutor executor, boolean directRunAllowed)
            throws InterruptedException
    {
        if (directRunAllowed)
        {
            run();
        }
        else
        {
            executor.execute(this);
        }
    }
}