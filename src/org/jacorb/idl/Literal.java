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

import java.util.*;
import java.io.*;

/**
 * @author Gerald Brose
 * @version $Id: Literal.java,v 1.5 2001-05-31 08:04:13 jacorb Exp $
 */

class Literal 
    extends IdlSymbol
{
    public String string;
    public java_cup.runtime.token token;

    private ConstDecl declared_in;

    public Literal(int num)
    {
        super(num);
    }

    public void setDeclaration( ConstDecl declared_in )
    {
        this.declared_in = declared_in;
    }

    public void parse()
    {
        TypeSpec ts = declared_in.const_type.symbol.typeSpec();
        Environment.output( 2, "Literal: ts " + ts.getClass().getName() +
                            " token " + token.getClass().getName() );
        if( ts instanceof FloatType && 
            token instanceof java_cup.runtime.int_token
            )
        {
            throw new ParseException("Excpecting float/double constant, found integral type!");
        }
    }


    public void print(PrintWriter ps)
    {
        ps.print( string );
    }
}

