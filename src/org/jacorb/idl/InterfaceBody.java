package org.jacorb.idl;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2001  Gerald Brose.
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
 * @author Gerald Brose
 * @version $Id: InterfaceBody.java,v 1.3 2001-03-19 11:07:42 brose Exp $
 */

import java.util.*;
import java.io.*;

class InterfaceBody 
    extends IdlSymbol
{
    public Vector v;
    public Interface my_interface;
    SymbolList inheritance_spec = null;
    private Operation[] methods = null;

    public class ParseThread 
	extends Thread 
    {
	InterfaceBody b = null;
	public ParseThread( InterfaceBody _b )
	{
	    b= _b;
	    start();
	}

	public void run()
	{
	    parser.set_pending( b.full_name() );
	    Object o = null;
	    for( Enumeration e = inheritance_spec.v.elements(); e.hasMoreElements(); )
	    {
		String s = ((ScopedName)(e.nextElement())).resolvedName();
		o = parser.get_pending( s );
		if( o != null )
		{
		    try
		    { 
			synchronized(o)
			{ 
			    o.wait();
			}
		    } 
		    catch ( InterruptedException ie )
		    { 
			System.out.println("ParseThread " + this + " interrupted!");
		    }
		}
	    }
	    b.internal_parse();
	    parser.remove_pending( b.full_name() );
	}
    }

    public InterfaceBody(int num)
    {
	super(num);
	v = new Vector();
    }

    public void commit()
    {
    }


    public void setEnclosingSymbol( IdlSymbol s )
    {
	if( enclosing_symbol != null && enclosing_symbol != s )
	    throw new RuntimeException("Compiler Error: trying to reassign container for " + name );
	enclosing_symbol = s;
	for(Enumeration e = v.elements(); e.hasMoreElements(); )
	    ((IdlSymbol)e.nextElement()).setEnclosingSymbol( my_interface );
    }

    public void set_ancestors( SymbolList _inheritance_spec )
    {
	inheritance_spec = _inheritance_spec;
    }


    public void set_name( String n )
    {
	name = n;
	for(Enumeration e = v.elements(); e.hasMoreElements();)
	    ((IdlSymbol)e.nextElement()).setPackage( name );
    }


    public void setPackage( String s)
    {
        s = parser.pack_replace(s);
	if( pack_name.length() > 0 )
	    pack_name = new String( s + "." + pack_name );
	else
	    pack_name = s;
		
	for(Enumeration e = v.elements(); e.hasMoreElements(); )
	    ((IdlSymbol)e.nextElement()).setPackage( s);
    }

    public void parse() 
	 
    {
	if( inheritance_spec != null )
	{
	    Object o = null;
	    boolean pending = false;
	    for( Enumeration e = inheritance_spec.v.elements(); e.hasMoreElements(); )
	    {
		o = parser.get_pending(	((ScopedName)(e.nextElement())).resolvedName() );
		pending = pending || ( o != null );
	    }
	    ParseThread p = null;
	    if( pending )
	    {
		parser.set_pending( full_name() );
		p = new ParseThread( this );
	    } 
	    else 
	    {
		internal_parse();
		parser.remove_pending( full_name() );
	    }
	} 
	else 
	{
	    internal_parse();
	    parser.remove_pending( full_name() );
	}
    }

    public void internal_parse() 	 
    {
	if( inheritance_spec != null )
        {
            try
            {
                NameTable.inheritFrom( full_name(), inheritance_spec );
            } 
            catch( NameAlreadyDefined nad )
            {
                parser.fatal_error("Name " + nad.getMessage() + 
                                   " already defined in  base interface(s)", token );                
            }
        }
	Definition d = null;
	for( Enumeration e = v.elements(); e.hasMoreElements(); )
	{
	    d = (Definition)e.nextElement();
	    Declaration dec = d.get_declaration();
	    if( is_pseudo )
		dec.set_pseudo();
	    dec.parse();
	}
    }

    /**
     * print definitions that appeared in an interface scope
     * do not call print() in OpDecls and on Typedefs
     */

    public void print(PrintWriter ps)
    {
	if( ps != null )
	    throw new Error("Compiler Error, interface body cannot be printed thus!");

	for(Enumeration e = v.elements(); e.hasMoreElements();)
	{
	    Declaration d = ((Definition)e.nextElement()).get_declaration();
	    if(! ( d instanceof OpDecl ) )
		d.print( ps );
	}
    }

    /** print signatures to the operations file */

    public void printOperationSignatures(PrintWriter ps )
    {
	for(Enumeration e = v.elements(); e.hasMoreElements();)
	{
	    Definition d = (Definition)e.nextElement();
	    if( d.get_declaration() instanceof OpDecl )
	    {
		((OpDecl)d.get_declaration()).printSignature(ps);
	    }
	    else if (d.get_declaration() instanceof AttrDecl)
	    {
		for( Enumeration m = ((AttrDecl)d.get_declaration()).getOperations();
		     m.hasMoreElements();)
		{
		    ((Operation)m.nextElement()).printSignature(ps);
		}
	    }
	}
    }

    /** print signatures to the operations file */

    public void printConstants(PrintWriter ps )
    {
	for(Enumeration e = v.elements(); e.hasMoreElements();)
	{
	    Definition d = (Definition)e.nextElement();
	    if( d.get_declaration() instanceof ConstDecl )
	    {
		((ConstDecl)d.get_declaration()).printContained(ps);
	    }
	}
    }

    /** print only constant definitions to the interface file */

    public void printInterfaceMethods(PrintWriter ps )
    {
	for(Enumeration e = v.elements(); e.hasMoreElements();)
	{
	    Definition d = (Definition)e.nextElement();
	    if( ! (d.get_declaration() instanceof ConstDecl) && is_pseudo())
	    {
		((IdlSymbol)d).print(ps);
	    }
	}
    }

    Operation[] getMethods()
    {
	if( methods == null )
	{
	    Hashtable table = new Hashtable();
	    for(Enumeration e = v.elements(); e.hasMoreElements();)
	    {
		Definition d = (Definition)e.nextElement();
		if( d.get_declaration() instanceof OpDecl )
		{
		    table.put( ((OpDecl)d.get_declaration()).signature(), d.get_declaration());
		}
		else if( d.get_declaration() instanceof AttrDecl)
		{
		    for( Enumeration elems = ((AttrDecl)d.get_declaration()).getOperations();
			 elems.hasMoreElements();)
		    {
			Operation op = (Operation)elems.nextElement();
			table.put( op.signature(), op);
		    }
		}
	    }
	    if( my_interface.inheritanceSpec.v.size() > 0 )
	    {
		for( Enumeration e = my_interface.inheritanceSpec.v.elements(); e.hasMoreElements();)
		{
		    ScopedName sn = ((ScopedName)e.nextElement());
		    Interface base = null;
		    try
		    {
			base = (Interface)((ConstrTypeSpec)sn.resolvedTypeSpec()).c_type_spec;
		    }
		    catch( Exception ex)
		    {
			ex.printStackTrace();
			parser.fatal_error("Cannot find base interface " + sn, token );
		    }
		    Operation [] base_ops = base.getBody().getMethods();
		    for( int j = 0; j < base_ops.length; j++ )  
		    {
			if( !table.contains(base_ops[j].signature()) )
			    table.put( base_ops[j].signature(), base_ops[j]);
		    }
					
		}
	    }
	    Enumeration o = table.elements();
	    methods = new Operation[ table.size() ];
	    for( int i =0; i < methods.length; i++ )
		methods[i] = (Operation)o.nextElement();
	}
	return methods;
    }

    /** print methods to the stub file */

    public void printStubMethods(PrintWriter ps, String classname, boolean locality_constraint)
    {
	Operation [] ops = getMethods();
	if( ops.length > 0 )
	{
	    for( int i= 0; i < ops.length; i++)
		ops[i].printMethod(ps, classname, locality_constraint);
	}
    }

 

    /** print methods to the stub file */

    public void printDelegatedMethods(PrintWriter ps)
    {
	Operation [] ops = getMethods();
	if( ops.length > 0 )
	{
	    for( int i= 0; i < ops.length; i++)
	    {
		ops[i].printDelegatedMethod(ps);
	    }
	}
    }


    /** print hash table that associates an operation string with an int */

    public void printOperationsHash(PrintWriter ps)
    {
        Operation [] ops = getMethods();
        if ( ops.length <= 0 )
            return;

        ps.println ( "\tstatic private final java.util.Hashtable m_opsHash = new java.util.Hashtable();" );
        ps.println ( "\tstatic" );
        ps.println ( "\t{" );

        for ( int i = 0; i < ops.length; i++ )
        {
	    /* Some operation names have been escaped with "_" to 
	       avoid name clashes with Java names. The operation name
	       on the wire is the original IDL name, however, so we
	       need to ask for the right name here. We need to take
	       care not to scramble up "_set_/_get" accessor methods!
           (hence the check on instanceof OpDecl).
           */

            String name;
            if( ops[i] instanceof OpDecl && ops[i].opName().startsWith("_") )
                name = ops[i].opName().substring(1);
            else
                name = ops[i].opName();
            
            ps.println ( "\t\tm_opsHash.put ( \"" + name + "\", new java.lang.Integer(" + i + "));" );
        }
        
        ps.println ( "\t}" );
    }
    
    /** print methods for impl-based skeletons */

    public void printSkelInvocations(PrintWriter ps)
    {
        Operation [] ops = getMethods();
        if( ops.length <= 0 )
        {
            ps.println( "\t\tthrow new org.omg.CORBA.BAD_OPERATION(method + \" not found\");" );
            return;
        }        
        ps.println( "\t\t// quick lookup of operation" );
        ps.println( "\t\tjava.lang.Integer opsIndex = (java.lang.Integer)m_opsHash.get ( method );" );
        ps.println( "\t\tif ( null == opsIndex )" );
        ps.println( "\t\t\tthrow new org.omg.CORBA.BAD_OPERATION(method + \" not found\");" );
    
        ps.println ( "\t\tswitch ( opsIndex.intValue() )" );
        ps.println ( "\t\t{" );

        int nextIndex = 0;
        for ( int i = 0; i < ops.length; i++ )
        {
            String name;
            if( ops[i] instanceof OpDecl && ops[i].opName().startsWith("_") )
                name = ops[i].opName().substring(1);
            else
                name = ops[i].opName();
            
            ps.println ( "\t\t\tcase " + nextIndex++ + ": // " + name );
            ps.println ( "\t\t\t{" );
            ops[i].printInvocation(ps);
            ps.println ( "\t\t\t\tbreak;" );
            ps.println ( "\t\t\t}" );
        }

        ps.println ( "\t\t}" );
        ps.println("\t\treturn _out;");
    }

    void getIRInfo(Hashtable irInfoTable )
    {
	for(Enumeration e = v.elements(); e.hasMoreElements();)
	{
	    Definition d = (Definition)e.nextElement();
	    if( d.get_declaration() instanceof OpDecl )
	    {
		((OpDecl)d.get_declaration()).getIRInfo(irInfoTable);
	    }
	    else if (d.get_declaration() instanceof AttrDecl)
	    {
		((AttrDecl)d.get_declaration()).getIRInfo(irInfoTable);
	    }
	} 
    }


    /** print methods for impl-based skeletons */

//      public void printSkelInvocations(PrintWriter ps)
//      {
//  	Operation [] ops = getMethods();
//  	String name = null;
//  	if( ops.length > 0 )
//  	{
//  	    /* Some operation names have been escaped with "_" to 
//  	       avoid name clashes with Java names. The operation name
//  	       on the wire is the original IDL name, however, so we
//  	       need to ask for the right name here. We need to take
//  	       care not to scramble up "_set_/_get" accessor methods! */

//  	    if( ops[0] instanceof OpDecl && ops[0].opName().startsWith("_") )
//  		name = ops[0].opName().substring(1);
//  	    else
//  		name = ops[0].opName();

//  	    ps.println("\t\tif( method.equals(\"" + name + "\"))");
//  	    ps.print("\t\t{\n\t\t\t");
//  	    ops[0].printInvocation(ps);
//  	    ps.println("\t\t}");

//  	    for( int i= 1; i < ops.length; i++)
//  	    {
//  		if( ops[i] instanceof OpDecl && ops[i].opName().startsWith("_") )
//  		    name = ops[i].opName().substring(1);
//  		else
//  		    name = ops[i].opName();

//  		ps.println("\t\telse if( method.equals(\"" + name + "\"))");
//  		ps.print("\t\t{\n\t\t\t");
//  		ops[i].printInvocation(ps);
//  		ps.println("\t\t}");
//  	    }
//  	}
//      }

}





















