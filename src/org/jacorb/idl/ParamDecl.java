/*
 *        JacORB - a free Java ORB
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

package org.jacorb.idl;

/**
 * @author Gerald Brose
 * @version $Id: ParamDecl.java,v 1.8.2.1 2002-07-17 13:11:37 gerald Exp $
 */

import java.io.PrintWriter;

class ParamDecl
    extends IdlSymbol
{
    public int paramAttribute;
    public TypeSpec paramTypeSpec;
    public SimpleDeclarator simple_declarator;

    public ParamDecl( int num )
    {
        super( num );
    }

    public void setPackage( String s )
    {
        s = parser.pack_replace( s );
        if( pack_name.length() > 0 )
            pack_name = new String( s + "." + pack_name );
        else
            pack_name = s;
        paramTypeSpec.setPackage( s );
    }


    public void parse()
    {
        while( paramTypeSpec.typeSpec() instanceof ScopedName )
        {
            TypeSpec ts = ( (ScopedName)paramTypeSpec.typeSpec() ).resolvedTypeSpec();
            if( ts != null )
                paramTypeSpec = ts;
        }
        
        Environment.doAssert( paramTypeSpec != null , " paramTypeSpec is null " + name );

        //simple_declarator.print(ps);
    }

    public void print( PrintWriter ps )
    {
        switch( paramAttribute )
        {
            case 1:
                //    if( paramTypeSpec instanceof ConstrTypeSpec )
                //ps.print( paramTypeSpec.typeName() );
                //else
                ps.print( paramTypeSpec.toString() );
                break;
            case 2: /*out*/
            case 3: /*inout*/
                ps.print( paramTypeSpec.holderName() );
                break;
        }
        ps.print( " " + simple_declarator );
        //simple_declarator.print(ps);
    }

    public String printWriteStatement( String ps )
    {
        return printWriteStatement( simple_declarator.toString(), ps );
    }

    public String printWriteStatement( String name, String ps )
    {
        if( paramAttribute != 1 )
            return paramTypeSpec.typeSpec().printWriteStatement( name + ".value", ps );
        else
            return paramTypeSpec.typeSpec().printWriteStatement( name, ps );
    }

    public String printReadExpression( String ps )
    {
        return paramTypeSpec.typeSpec().printReadExpression( ps );
    }

}




