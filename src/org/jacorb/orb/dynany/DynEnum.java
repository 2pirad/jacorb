package org.jacorb.orb.dynany;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-99  Gerald Brose.
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

import org.omg.DynamicAny.DynAnyPackage.*;
import org.jacorb.orb.*;

/**
 * CORBA DynEnum
 *
 * @author (c) Gerald Brose, FU Berlin 1999
 * $Id: DynEnum.java,v 1.2 2001-03-17 18:44:58 brose Exp $
 * $Log: DynEnum.java,v $
 * Revision 1.2  2001-03-17 18:44:58  brose
 * *** empty log message ***
 *
 * Revision 1.5  2000/03/30 13:55:49  noffke
 * added portable intercetor support
 *
 * Revision 1.4  1999/11/25 16:07:23  brose
 * cosmetics
 *
 * Revision 1.3  1999/11/03 17:30:33  brose
 * replaced Environment.output by Debug.output and moved
 * Environment.java to package util
 *
 * Revision 1.2  1999-10-09 21:42:22+02  brose
 * passed orb and factory to all DynAnys in order to get hold of
 * correct orb instance
 *
 * Revision 1.1.1.1  1999-08-05 12:22:22+02  brose
 * First initial preliminary ... attempt
 *
 * Revision 1.1  1999-07-27 14:37:04+02  brose
 * Initial revision
 *
 *
 */


public final class DynEnum
    extends DynAny
    implements org.omg.DynamicAny.DynEnumOperations
{
    private int enum_value;
    private int max;
    private String [] member_names;

    DynEnum(org.jacorb.orb.ORB orb,org.omg.DynamicAny.DynAnyFactory dynFactory,jacorb.orb.Any any)
	throws InvalidValue, TypeMismatch
    {
	super(orb,dynFactory,any);
    }

    DynEnum(org.jacorb.orb.ORB orb,org.omg.DynamicAny.DynAnyFactory dynFactory,org.omg.CORBA.TypeCode tc)
	throws InvalidValue, TypeMismatch
    {
	if( tc.kind().value() != org.omg.CORBA.TCKind._tk_enum )
	    throw new TypeMismatch();
	type = tc;

	this.orb = orb;
	this.dynFactory = dynFactory;
	pos = -1;
	enum_value = 0;

	try
	{	  
	    member_names = new String[ type().member_count()];
	    max = member_names.length;
	    for( int i = 0; i < member_names.length; i++ )
		member_names[i] = type().member_name(i);
	}
	catch( org.omg.CORBA.TypeCodePackage.BadKind bk )
	{	    
	    // should not happen
	    bk.printStackTrace();
	}
	catch( org.omg.CORBA.TypeCodePackage.Bounds b )
	{	    
	    // should not happen
	    b.printStackTrace();
	}    
    }


    public void from_any(org.omg.CORBA.Any value) 
	throws InvalidValue, TypeMismatch
    {
	if( ! value.type().equal( type()) )
	    throw new TypeMismatch();
	try
	{	    
	    enum_value = value.create_input_stream().read_long();
	    member_names = new String[ type().member_count()];
	    max = member_names.length;
	    for( int i = 0; i < member_names.length; i++ )
	    {
		member_names[i] = type().member_name(i);
	    }
	}
	catch( org.omg.CORBA.TypeCodePackage.Bounds b )
	{	    
	    // should not happen
	    b.printStackTrace();
	}
	catch( org.omg.CORBA.TypeCodePackage.BadKind bk )
	{
	    // should not happen anymore
	    bk.printStackTrace();
	}
    }
   

    public org.omg.CORBA.Any to_any() 
    {
	CDROutputStream os = new CDROutputStream();
	os.write_long( enum_value );

	jacorb.orb.Any out_any = (org.jacorb.orb.Any)org.omg.CORBA.ORB.init().create_any();
	out_any.type(type());	
	out_any.read_value( new CDRInputStream(orb, os.getBufferCopy()), type());
	return out_any;
    }

    public java.lang.String get_as_string()
    {
	return member_names[enum_value];
    }
	
    public void set_as_string(java.lang.String arg)
	throws InvalidValue
    {
	int i = 0;
	while( i < member_names.length && !(arg.equals(member_names[i])) )
	    i++;

	if( i < member_names.length )
	    set_as_ulong(i);
	else
	    throw new InvalidValue();
    }

    public int get_as_ulong()
    {
	return enum_value;
    }
    
    public void set_as_ulong(int arg)
	throws InvalidValue
    {
	if( arg < 0 || arg > max )
	    throw new InvalidValue();

	enum_value = arg;
    }


}


