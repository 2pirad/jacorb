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
 *
 * @author Alphonse Bendt
 * @version $Id: Task.java,v 1.5 2003-04-12 21:04:53 alphonse.bendt Exp $
 */

public interface Task extends Runnable {    

    public static int NEW = 0;
    public static int ERROR = 1;
    public static int RESCHEDULE = 2;
    public static int DONE = 3;

    /**
     * return the status of this Task.
     */ 
    int getStatus();

}// Task
