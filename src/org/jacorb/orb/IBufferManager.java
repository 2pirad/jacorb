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
 * A BufferManager is used to share a pool of buffers and to implement
 * a buffer  allocation policy.  This  reduces the  number of  memory
 * allocations and deallocations and the overall memory footprint.
 * Buffers are generally created on demand.
 *
 * @author Gerald Brose
 * @version $Id: IBufferManager.java,v 1.2 2009-10-01 14:45:28 alexander.bykov Exp $
 */
public interface IBufferManager
{
    /**
     * @return a buffer that has a pre-configured size
     * @see #getBuffer(int, boolean)
     */
    byte[] getPreferredMemoryBuffer();

    /**
     * equals returnBuffer(buffer, false)
     * @see #returnBuffer(byte[], boolean)
     */
    void returnBuffer(byte[] buf);

    /**
     * @param buffer a <code>byte[]</code> value
     * @param cdrStr a <code>boolean</code> value value to denote if CDROuputStream is
     *               caller (may use cache in this situation)
     */
    void returnBuffer(byte[] buffer, boolean cdrStr);

    /**
     * <code>getBuffer</code> returns a new buffer.
     *
     * @param size an <code>int</code> value
     * @param cdrStr a <code>boolean</code> value to denote if CDROuputStream is caller
     *               (may use cache in this situation)
     * @return a <code>byte[]</code> value
     */
    byte[] getBuffer(int size, boolean cdrStr);

    /**
     * equals getBuffer(size, false);
     * @see #getBuffer(int, boolean)
     */
    byte[] getBuffer(int size);

    /**
     * release all resources that are held by this buffer
     */
    void release();
}
