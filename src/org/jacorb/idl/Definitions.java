/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-98  Gerald Brose.
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

package org.jacorb.idl;

import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

/**
 * @author Gerald Brose
 * @version $Id: Definitions.java,v 1.1.1.1 2001-03-17 18:08:19 brose Exp $
 */

class Definitions 
    extends SymbolList
{
    public Definitions(int num)
    {
	super(num);
	v = new Vector();
	Enumeration e = v.elements();
    }

    public void setPackage( String s)
    {
        s = parser.pack_replace(s);
	Enumeration e = v.elements();
	for(; e.hasMoreElements(); )
	{
	    IdlSymbol i = (IdlSymbol)e.nextElement();
	    i.setPackage(s);
	}
    }

    public void setEnclosingSymbol( IdlSymbol s )
    {
	if( enclosing_symbol != null && enclosing_symbol != s )
	{
	    System.err.println("was " + enclosing_symbol.getClass().getName() + " now: " + s.getClass().getName());
	    throw new RuntimeException("Compiler Error: trying to reassign container for " + name );
	}
	enclosing_symbol = s;
	for(Enumeration e = v.elements(); e.hasMoreElements(); )
	    ((IdlSymbol)e.nextElement()).setEnclosingSymbol( s );
    }

    public void set_included(boolean i)
    {
	included = i;
	Enumeration e = v.elements();
	for(; e.hasMoreElements(); )
	    ((IdlSymbol)e.nextElement()).set_included(i);
    }

    public void print(PrintWriter ps)
    {
	Enumeration e = v.elements();
	for(; e.hasMoreElements(); )
	    ((IdlSymbol)e.nextElement()).print(ps);
    }	
}















