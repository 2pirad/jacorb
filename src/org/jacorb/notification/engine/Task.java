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

/**
 * Task.java
 *
 *
 * Created: Thu Nov 14 18:33:57 2002
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: Task.java,v 1.4 2003-01-14 11:46:07 alphonse.bendt Exp $
 */

public interface Task extends Runnable {    
    public static int NEW = 0;
    public static int DELIVERING = 1;
    public static int DELIVERED = 2;
    public static int FILTERED = 3;
    public static int FILTERING = 4;
    public static int FINISHED = 10;

    public int getStatus();
    public boolean getDone();
}// Task
