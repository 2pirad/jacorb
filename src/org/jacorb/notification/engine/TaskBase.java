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

import org.jacorb.notification.NotificationEvent;
import org.jacorb.notification.interfaces.Poolable;
import org.apache.log.Logger;
import org.apache.log.Hierarchy;

/**
 * TaskBase.java
 *
 *
 * Created: Fri Jan 03 11:22:52 2003
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: TaskBase.java,v 1.2 2003-04-12 21:04:53 alphonse.bendt Exp $
 */

public abstract class TaskBase extends Poolable implements Task {

    protected Logger logger_ =
        Hierarchy.getDefaultHierarchy().getLoggerFor( getClass().getName() );

    private TaskFinishHandler coordinator_;
    private TaskErrorHandler errorHandler_;
    protected NotificationEvent event_;
    protected int status_;

    TaskBase() {
	status_ = NEW;
    }

    /**
     * Set Status of this Task.
     */
    protected void setStatus(int status) {
	status_ = status;
    }

    /**
     * Get the current Status of this Task.
     */
    public int getStatus() {
	return status_;
    }
    
    /**
     *
     */
    public void setTaskFinishHandler(TaskFinishHandler coord) {
	coordinator_ = coord;
    }

    public void setTaskErrorHandler(TaskErrorHandler handler) {
	errorHandler_ = handler;
    }

    /**
     * set the NotificationEvent for this Task to use. the reference
     * counter of the NotificationEvent is increased by one.
     */
    public void setNotificationEvent(NotificationEvent event) {
	if (event_ != null) {
	    throw new RuntimeException("remove old first");
	}

	event_ = event;
	event_.addReference();
    }

    /**
     * remove the reference to the NotificationEvent from this task.
     */
    public NotificationEvent removeNotificationEvent() {
	NotificationEvent _event = event_;
	event_ = null;
	return _event;
    }

    /**
     * access the NotificationEvent this Task is using.
     */
    public NotificationEvent getNotificationEvent() {
	return event_;
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
    public void run() {
	try {
	    doWork();
	    coordinator_.handleTaskFinished(this);
	} catch (Throwable t) {
	    errorHandler_.handleTaskError(this, t);
	}
    }
    
    public void reset() {
	coordinator_ = null;
	event_ = null;
	status_ = NEW;
    }

}// TaskBase
