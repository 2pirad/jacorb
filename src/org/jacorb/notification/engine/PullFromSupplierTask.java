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

import org.jacorb.notification.interfaces.MessageSupplier;
import org.omg.CosEventComm.Disconnected;

/**
 * @author Alphonse Bendt
 * @version $Id: PullFromSupplierTask.java,v 1.16 2005-10-02 15:18:39 alphonse.bendt Exp $
 */

public class PullFromSupplierTask extends AbstractTask
{
    private MessageSupplier target_;

    // //////////////////////////////////////

    PullFromSupplierTask(TaskExecutor executor)
    {
        setTaskExecutor(executor);
    }

    // //////////////////////////////////////

    public void setTarget(MessageSupplier target)
    {
        target_ = target;
    }

    public void doWork() throws Disconnected
    {
        target_.runPullMessage();

        dispose();
    }

    public void reset()
    {
        target_ = null;
    }

    public void handleTaskError(AbstractTask task, Exception error)
    {
        if (error instanceof Disconnected)
        {
            target_.destroy();
        }
    }

    public void schedule()
    {
        schedule(false);
    }
}
