package org.jacorb.idl;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2000  Gerald Brose.
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
import java.io.*;

/**
 * @author Gerald Brose
 * @version $Id: SymbolList.java,v 1.1.1.1 2001-03-17 18:08:22 brose Exp $
 */

class SymbolList 
    extends IdlSymbol
{
    public Vector v;

    public SymbolList(int num)
    {
        super(num);
        v = new Vector();
    }

    public void setPackage( String s)
    {
        s = parser.pack_replace(s);
        if( pack_name.length() > 0  )
            pack_name = new String( s + "." + pack_name );
        else
            pack_name = s;
        Enumeration e = v.elements();
        for(; e.hasMoreElements(); )
            ((IdlSymbol)e.nextElement()).setPackage(s);
    }

    public void parse() 
                 
    {
        Enumeration e = v.elements();
        for(; e.hasMoreElements(); )
            ((IdlSymbol)e.nextElement()).parse();
    }

    public void print(PrintWriter ps)
    {
        Enumeration e = v.elements();
        if(e.hasMoreElements())
            ((IdlSymbol)e.nextElement()).print(ps);
        
        for(; e.hasMoreElements();)
        {
            ps.print(",");
            ((IdlSymbol)e.nextElement()).print(ps);
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
                Enumeration e = v.elements();

        if(e.hasMoreElements())
            sb.append( (IdlSymbol)e.nextElement() );
        
        for(; e.hasMoreElements();)
        {
            sb.append(","+ (IdlSymbol)e.nextElement() );
        }
        return sb.toString();
    }
}


