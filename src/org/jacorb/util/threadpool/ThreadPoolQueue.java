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
package org.jacorb.util.threadpool;
/**
 * ThreadPoolQueue.java
 *
 *
 * Created: Fri Jun  9 15:18:43 2000
 *
 * @author Nicolas Noffke
 * $Id: ThreadPoolQueue.java,v 1.6 2002-12-20 18:29:06 nicolas Exp $
 */

public interface ThreadPoolQueue
{
    public boolean add( Object job );
    public Object removeFirst();

    public boolean isEmpty();
} // ThreadPoolQueue






