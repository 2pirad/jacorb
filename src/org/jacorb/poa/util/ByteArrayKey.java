package org.jacorb.poa.util;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
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


/**
 * This class wraps byte arrays so that they can be used as keys
 * in hashtables.
 *
 * @author Steve Osselton
 * @version $Id: ByteArrayKey.java,v 1.5 2003-04-01 12:39:23 nick.cross Exp $
 */

public class ByteArrayKey
{
    private int cache = 0;
    private byte[] bytes = null;

    public ByteArrayKey (byte[] array)
    {
        bytes = array;
    }

    public ByteArrayKey (ByteArrayKey bak)
    {
       cache = bak.cache;
       bytes = bak.bytes;
    }

    public byte[] getBytes ()
    {
        return bytes;
    }

    /**
     * @overrides hashCode () in Object
     */
    public int hashCode ()
    {
        if( cache == 0 )
        {
            long h = 1234;

            if ((bytes != null) && (bytes.length > 0))
            {
                for (int i = bytes.length; --i >= 0; )
                {
                    h ^= bytes[i] * (i + 1);
                }
                cache = (int)((h >> 32) ^ h);
            }
        }
        return cache;
    }

    /**
     * @overrides equals () in Object
     */
    public boolean equals (Object obj)
    {
        boolean result = false;

        if (obj instanceof ByteArrayKey)
        {
            ByteArrayKey key = (ByteArrayKey) obj;

            if ((bytes == key.bytes) || ((bytes == null) && (key.bytes == null)))
            {
                result = true;
            }
            else if ((bytes != null) && (key.bytes != null))
            {
                if (bytes.length == key.bytes.length)
                {
                    result = true;
                    for (int i = 0; i < bytes.length; i++)
                    {
                        if (bytes[i] != key.bytes[i])
                        {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    public String toString() {
    	return new String(bytes);
    }
}
