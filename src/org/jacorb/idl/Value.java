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

package org.jacorb.idl;

/**
 * 
 * @author Gerald Brose
 * @version $Id: Value.java,v 1.2 2001-03-17 18:43:50 brose Exp $
 */

import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

class Value 
    extends Declaration 
{
    public Value(int num)
    {
	super(num);
    }

    public void setPackage( String s)
    {
    }

    public void parse()
    {	
        System.out.println("(Warning: Values types  are not implemented and therefore ignored!)");
    }

    public void print(PrintWriter ps)
    {
    }
}















