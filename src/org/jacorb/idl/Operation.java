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

package org.jacorb.idl;

/**
 * @author Gerald Brose
 * @version $Id: Operation.java,v 1.11.2.1 2003-08-27 13:34:01 brose Exp $
 */

import java.io.PrintWriter;
import java.io.Serializable;

public interface Operation
    extends Serializable
{

    /** plain name of the operation */

    String name();

    /*  mangled name in case of attributes (_get_, _set_)*/

    String opName();

    String signature();

    void printSignature( PrintWriter ps );

    /**
     * @param printModifiers whether "public abstract" should be added
     */
    void printSignature( PrintWriter ps, boolean printModifiers );

    /** method code for stubs */

    void printMethod( PrintWriter ps, String classname, boolean is_local );

    void print_sendc_Method( PrintWriter ps, String classname );

    /** method code for skeletons */

    void printDelegatedMethod( PrintWriter ps );

    void printInvocation( PrintWriter ps );


    void accept( IDLTreeVisitor visitor );

}




















