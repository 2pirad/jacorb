package org.jacorb.notification.interfaces;

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

import org.jacorb.notification.util.ObjectPoolBase;

/**
 * Interface to indicate that a Object can be pooled. Objects can be
 * pooled to spare ressources.
 *
 * Created: Sat Jan 04 17:01:16 2003
 *
 * @author Alphonse Bendt
 * @version $Id: Poolable.java,v 1.2 2003-07-17 18:11:26 alphonse.bendt Exp $
 */

public abstract class Poolable {

    private ObjectPoolBase objectPool_;

    /**
     * The call to this Method indicates that this Object is not
     * needed by the user anymore. After a call to
     * <code>release</code> the Object can be returned to its
     * ObjectPool. It's forbidden to use the Object
     * after release has been called as this may cause unexpected behaviour.
     */
    public void release() {
	if (objectPool_ != null) {
	    objectPool_.returnObject(this);
	}
    }

    /**
     * Set the ObjectPool that administers this instance.
     */
    public void setObjectPool(ObjectPoolBase pool) {
	objectPool_ = pool;
    }

    /**
     * Reset the Object to an initial state. Subclasses should
     * override this method appropiately to reset the instance to an
     * initial state.
     */
    public abstract void reset();

}
