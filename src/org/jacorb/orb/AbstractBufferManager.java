/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2006 The JacORB project.
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
 */

package org.jacorb.orb;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractBufferManager.java,v 1.1 2009-09-29 10:27:52 alexander.bykov Exp $
 */
public abstract class AbstractBufferManager implements IBufferManager
{
    public byte[] getBuffer(int size)
    {
        return getBuffer(size, false);
    }

    public void returnBuffer(byte[] buf)
    {
        returnBuffer(buf, false);
    }

    public byte[] getPreferredMemoryBuffer()
    {
        return getBuffer(256);
    }

    public void release()
    {
    }

    public void returnBuffer(byte[] buffer, boolean b)
    {
    }
}
