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

import org.jacorb.notification.interfaces.Poolable;
import org.jacorb.notification.util.ObjectPoolBase;

/**
 * TaskPoolBase.java
 *
 *
 * @author Alphonse Bendt
 * @version $Id: TaskPoolBase.java,v 1.1 2003-08-02 10:28:32 alphonse.bendt Exp $
 */

abstract class TaskPoolBase extends ObjectPoolBase {
    
    public void passivateObject( Object o )
    {
	( ( Poolable ) o ).reset();
    }
    
    public void activateObject( Object o )
    {
	( ( Poolable ) o ).setObjectPool( this );
    }

}
