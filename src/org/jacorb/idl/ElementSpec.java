package org.jacorb.idl;

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

/**
 * @version $Id: ElementSpec.java,v 1.1.1.1 2001-03-17 18:08:19 brose Exp $
 */ 

class ElementSpec  
    extends IdlSymbol 
{
    public TypeSpec t = new TypeSpec(new_num());
    public Declarator d = null;
    private UnionType containingUnion;

    public ElementSpec( int num )
    {
	super(num);
    }

    public void setPackage( String s)
    {
        s = parser.pack_replace(s);
	if( pack_name.length() > 0 )
	    pack_name = new String( s + "." + pack_name );
	else
	    pack_name = s;
	t.setPackage(s);
        d.setPackage(s);
    }

    public void setUnion( UnionType ut )
    {
        containingUnion = ut;
    }

    public void setEnclosingSymbol( IdlSymbol s )
    {
	t.setEnclosingSymbol( s );
	d.setEnclosingSymbol( s );
    }

    public void parse() 		 
    {
	if( t.typeSpec() instanceof TemplateTypeSpec ||
	    t.typeSpec() instanceof ConstrTypeSpec )
	{
	    t.parse();
	}
	else if( t.typeSpec() instanceof ScopedName )
	{
	    TypeSpec ts = ((ScopedName)t.typeSpec()).resolvedTypeSpec();
	    if( ts != null ) 
		t = ts;
	} 
        try
        {
            NameTable.define( containingUnion.full_name() + "." + d.name(), "declarator");
        }
        catch( NameAlreadyDefined nad )
        {
            parser.error("Declarator " + d.name() + 
                         " already defined in union " + containingUnion.full_name(), token);
        }
        //        d.parse();
    }

    public void print(java.io.PrintWriter ps)
    {	
	if( t.typeSpec() instanceof TemplateTypeSpec ||
	    t.typeSpec() instanceof ConstrTypeSpec )
	{
	    t.print(ps);
	}
    }



}














