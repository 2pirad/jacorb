package org.jacorb.util;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2002  Gerald Brose.
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

import java.util.*;
import java.lang.ref.*;

/**
 * WeakHashtable.java
 *
 * Created: Thu Nov  2 13:16:34 2000
 *
 * @author Nicolas Noffke
 * $Id: WeakHashtable.java,v 1.6 2002-10-06 11:02:16 andre.spiegel Exp $
 */

public class WeakHashtable 
    extends Hashtable
{
    
    public WeakHashtable()
    {
        super();
    }

    public Object put( Object key, Object value )
    {
        return super.put( key, new WeakReference( value ));
    }

    public Object get( Object key )
    {
        WeakReference ref = 
            (WeakReference) super.get( key );
        
        if( ref != null )
        {
            Object o = ref.get();

            if( o == null )
            {
                remove( key );
            }

            return o;
        }
        else
        {
            return null;
        }
    }

    /**
     * @overrides elements() in Hashtable
     */

    public Enumeration elements()
    {
        Vector v = new Vector();

        for( Enumeration e = super.elements(); e.hasMoreElements(); )
        {
            v.addElement(((java.lang.ref.WeakReference)e.nextElement()).get());
        }

        return v.elements();
    }

} // WeakHashtable






