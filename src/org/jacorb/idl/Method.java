/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2004 Gerald Brose.
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
 * @version $Id: Method.java,v 1.20 2006-10-13 20:01:01 andre.spiegel Exp $
 *
 * This class is used to represent accessor operations
 */

import java.io.PrintWriter;

public class Method
    implements Operation
{
    public  TypeSpec resultType;
    public  TypeSpec parameterType;

    private String name;
    private RaisesExpr raisesExpr;

    private boolean pseudo;


    public Method(TypeSpec res, TypeSpec params, String name, 
                  RaisesExpr raisesExpr, boolean pseudo)
    {
        resultType = res;
        parameterType = params;
        this.name = name;
        this.raisesExpr = raisesExpr;
        this.pseudo = pseudo;
    }

    public boolean isGetter()
    {
        return resultType != null;
    }

    public String name()
    {
        return name;
    }

    public String opName()
    {
        if( isGetter() )
            return "_get_" + name;
        else
            return "_set_" + name;
    }

    public String signature()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( name + "(" );
        if( parameterType != null )
        {
            sb.append( parameterType.toString() );
        }
        sb.append( ")" );
        return sb.toString();
    }

    public void printSignature( PrintWriter ps )
    {
        printSignature( ps, pseudo );
    }

    /**
     * @param printModifiers whether "public abstract" should be added
     */
    public void printSignature( PrintWriter ps, boolean printModifiers )
    {
        ps.print( "\t" );
        if( printModifiers )
            ps.print( "public abstract " );

        if( isGetter() )
        {
            ps.print( resultType.toString() );
            ps.print( " " + name + "()" );
            raisesExpr.print(ps);
            ps.println(";");
        }
        else
        {
            ps.print("void " + name + "(");
            ps.print(parameterType.toString());
            ps.print(" arg)");
            raisesExpr.print(ps);
            ps.println(";");
        }
    }


    public void printMethod( PrintWriter ps, String classname, boolean is_local, boolean is_abstract )
    {
        ps.print( "\tpublic " );

        if( isGetter() )
        {
            // accessor method
            ps.print( resultType.toString() );
            ps.print( " " + name + "()" );
            raisesExpr.print(ps);
            ps.println();
            ps.println( "\t{" );
            ps.println( "\t\twhile(true)" );
            ps.println( "\t\t{" );

            // remote part, not for locality constrained objects
            //
            if( !is_local )
            {
                ps.println( "\t\tif(! this._is_local())" );
                ps.println( "\t\t{" );

                ps.println( "\t\t\torg.omg.CORBA.portable.InputStream _is = null;" );

                ps.println( "\t\t\ttry" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\torg.omg.CORBA.portable.OutputStream _os = _request(\"_get_" + name + "\",true);" );
                ps.println( "\t\t\t\t_is = _invoke(_os);" );
                TypeSpec ts = resultType.typeSpec();
                ps.println( "\t\t\t\treturn " + ts.printReadExpression( "_is" ) + ";" );
                ps.println( "\t\t\t}" );
                ps.println( "\t\t\tcatch( org.omg.CORBA.portable.RemarshalException _rx ){}" );
                ps.println( "\t\t\tcatch( org.omg.CORBA.portable.ApplicationException _ax )" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\tString _id = _ax.getId();" );

                if( !raisesExpr.empty() )
                {
                    String[] exceptIds = raisesExpr.getExceptionIds();
                    String[] classNames = raisesExpr.getExceptionClassNames();
                    ps.print( "\t\t\t\t" );
                    for( int i = 0; i < exceptIds.length; i++ )
                    {
                        if (i > 0)
                        {
                            ps.print( "\t\t\t\telse " );
                        }
                        ps.println( "if( _id.equals(\"" + exceptIds[ i ] + "\"))" );
                        ps.println( "\t\t\t\t{" );
                        ps.println( "\t\t\t\t\tthrow " + classNames[ i ] + "Helper.read(_ax.getInputStream());" );
                        ps.println( "\t\t\t\t}" );
                    }
                }                
                
                ps.println( "\t\t\t\tthrow new RuntimeException(\"Unexpected exception \" + _id );" );
                ps.println( "\t\t\t}" );
                ps.println( "\t\t\tfinally" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\tthis._releaseReply(_is);" );
                ps.println( "\t\t\t}" );
                ps.println( "\t\t}\n" );

                // local part
                ps.println( "\t\telse" );
                ps.println( "\t\t{" );
            }

            ps.println( "\t\torg.omg.CORBA.portable.ServantObject _so = _servant_preinvoke( \"_get_" + name + "\", _opsClass);" );

            ps.println( "\t\tif( _so == null )" );
            ps.println( "\t\t\tthrow new org.omg.CORBA.UNKNOWN(\"local invocations not supported!\");" );
            if( is_abstract )
            {
                ps.println( "\t\t\t" + classname + " _localServant = (" +
                            classname + ")_so.servant;" );
            }
            else
            {
                ps.println( "\t\t\t" + classname + "Operations _localServant = (" +
                            classname + "Operations)_so.servant;" );
            }

            ps.println( "\t\t\t" + resultType + " _result;" );

            ps.println( "\t\ttry" );
            ps.println( "\t\t{" );
            ps.println( "\t\t\t_result = _localServant." + name + "();" );
            ps.println( "\t\t}" );
            ps.println( "\t\tfinally" );
            ps.println( "\t\t{" );
            ps.println( "\t\t\t_servant_postinvoke(_so);" );
            ps.println( "\t\t}" );
            ps.println( "\t\treturn _result;" );
            ps.println( "\t\t}" );
            if( !is_local ) ps.println( "\t\t}\n" );
            ps.println( "\t}\n" );
        }
        else
        {
            /** modifier */

            ps.print( "void " + name + "(" + parameterType.toString() );
            ps.print( " a)" );
            raisesExpr.print(ps);
            ps.println();
            ps.println( "\t{" );
            ps.println( "\t\twhile(true)" );
            ps.println( "\t\t{" );
            // remote part not for locality constrained objects
            //
            if( !is_local )
            {
                ps.println( "\t\tif(! this._is_local())" );
                ps.println( "\t\t{" );
                ps.println( "\t\t\torg.omg.CORBA.portable.InputStream _is = null;" );

                ps.println( "\t\t\ttry" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\torg.omg.CORBA.portable.OutputStream _os = _request(\"_set_" + name + "\",true);" );
                ps.println( "\t\t\t\t" + parameterType.typeSpec().printWriteStatement( "a", "_os" ) );
                ps.println( "\t\t\t\t_is = _invoke(_os);" );
                ps.println( "\t\t\t\treturn;" );
                ps.println( "\t\t\t}" );
                ps.println( "\t\t\tcatch( org.omg.CORBA.portable.RemarshalException _rx ){}" );
                ps.println( "\t\t\tcatch( org.omg.CORBA.portable.ApplicationException _ax )" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\tString _id = _ax.getId();" );
                
                if( !raisesExpr.empty() )
                {
                    String[] exceptIds = raisesExpr.getExceptionIds();
                    String[] classNames = raisesExpr.getExceptionClassNames();
                    ps.print( "\t\t\t\t" );
                    for( int i = 0; i < exceptIds.length; i++ )
                    {
                        if (i > 0)
                        {
                            ps.print( "\t\t\t\telse " );
                        }
                        ps.println( "if( _id.equals(\"" + exceptIds[ i ] + "\"))" );
                        ps.println( "\t\t\t\t{" );
                        ps.println( "\t\t\t\t\tthrow " + classNames[ i ] + "Helper.read(_ax.getInputStream());" );
                        ps.println( "\t\t\t\t}" );
                    }
                }                
                
                ps.println( "\t\t\t\tthrow new RuntimeException(\"Unexpected exception \" + _id );" );
                ps.println( "\t\t\t}" );
                ps.println( "\t\t\tfinally" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\tthis._releaseReply(_is);" );
                ps.println( "\t\t\t}" );
                ps.println( "\t\t}\n" );

                // local part
                ps.println( "\t\telse" );
                ps.println( "\t\t{" );

            }
            ps.println( "\t\t\torg.omg.CORBA.portable.ServantObject _so = _servant_preinvoke( \"_set_" + name + "\", _opsClass);" );

            ps.println( "\t\t\tif( _so == null )" );
            ps.println( "\t\t\t\tthrow new org.omg.CORBA.UNKNOWN(\"local invocations not supported!\");" );
            ps.println( "\t\t\t" + classname + "Operations _localServant = (" + classname + "Operations)_so.servant;" );

            ps.println( "\t\t\t\ttry" );
            ps.println( "\t\t\t\t{" );
            ps.println( "\t\t\t\t\t_localServant." + name + "(a);" );
            ps.println( "\t\t\t\t}" );
            ps.println( "\t\t\t\tfinally" );
            ps.println( "\t\t\t\t{" );
            ps.println( "\t\t\t\t\t_servant_postinvoke(_so);" );
            ps.println( "\t\t\t\t}" );
            ps.println( "\t\t\t\treturn;" );
            ps.println( "\t\t\t}" );
            if( !is_local ) ps.println( "\t\t}\n" );
            ps.println( "\t}\n" );
        }
    }


    public void print_sendc_Method( PrintWriter ps,
                                    String classname )
    {
        ps.print( "\tpublic void sendc_" );

        if( isGetter() )
        {
            // accessor method
            ps.print  ( "get_" + name );
            ps.println( "(AMI_" + classname + "Handler ami_handler)" );
            ps.println( "\t{" );
            ps.println( "\t\twhile(true)" );
            ps.println( "\t\t{" );

            ps.println( "\t\t\ttry" );
            ps.println( "\t\t\t{" );
            ps.println( "\t\t\t\torg.omg.CORBA.portable.OutputStream _os = _request(\"_get_" + name + "\",true);" );
            ps.println( "\t\t\t\t((org.jacorb.orb.Delegate)_get_delegate()).invoke(this, _os, ami_handler);" );
            ps.println( "\t\t\t\treturn;" );
            ps.println( "\t\t\t}" );
            ps.println( "\t\t\tcatch( org.omg.CORBA.portable.RemarshalException _rx ){}" );
            ps.println( "\t\t\tcatch( org.omg.CORBA.portable.ApplicationException _ax )" );
            ps.println( "\t\t\t{" );
            ps.println( "\t\t\t\tString _id = _ax.getId();" );
            ps.println( "\t\t\t\tthrow new RuntimeException(\"Unexpected exception \" + _id );" );
            ps.println( "\t\t\t}" );
            ps.println( "\t\t}" );
            ps.println( "\t}\n" );
        }
        else
        {
            // modifier
            ps.print   ( "set_" + name );
            ps.print   ( "(AMI_" + classname + "Handler ami_handler, " );
            ps.println ( parameterType.toString() + " attr_" + name + ")");
            ps.println( "\t{" );
            ps.println( "\t\twhile(true)" );
            ps.println( "\t\t{" );
            ps.println( "\t\t\ttry" );
            ps.println( "\t\t\t{" );
            ps.println( "\t\t\t\torg.omg.CORBA.portable.OutputStream _os = _request(\"_set_" + name + "\",true);" );
            ps.println( "\t\t\t\t" + parameterType.typeSpec().printWriteStatement( "attr_" + name, "_os" ) );
            ps.println( "\t\t\t\t((org.jacorb.orb.Delegate)_get_delegate()).invoke(this, _os, ami_handler);" );
            ps.println( "\t\t\t\treturn;" );
            ps.println( "\t\t\t}" );
            ps.println( "\t\t\tcatch( org.omg.CORBA.portable.RemarshalException _rx ){}" );
            ps.println( "\t\t\tcatch( org.omg.CORBA.portable.ApplicationException _ax )" );
            ps.println( "\t\t\t{" );
            ps.println( "\t\t\t\tString _id = _ax.getId();" );
            ps.println( "\t\t\t\tthrow new RuntimeException(\"Unexpected exception \" + _id );" );
            ps.println( "\t\t\t}" );
            ps.println( "\t\t}" );
            ps.println( "\t}\n" );
        }
    }

    public void printDelegatedMethod( PrintWriter ps )
    {
        ps.print( "\tpublic " );
        if( isGetter() )
        {
            ps.print( resultType.toString() );
            ps.print( " " + name + "()" );
            raisesExpr.print(ps);
            ps.println();
            ps.println( "\t{" );
            ps.println( "\t\treturn _delegate." + name + "();" );
            ps.println( "\t}\n" );
        }
        else
        {
            /** modifier */

            ps.print( "void " + name + "(" + parameterType.toString() );
            ps.print( " a)" );
            raisesExpr.print(ps);
            ps.println();
            ps.println( "\t{" );
            ps.println( "\t\t_delegate." + name + "(a);" );
            ps.println( "\t}\n" );
        }
    }

    public void printInvocation( PrintWriter ps )
    {
        if( !raisesExpr.empty() )
        {
            ps.println( "\t\t\ttry" );
            ps.println( "\t\t\t{" );
        }
        
        ps.println( "\t\t\t_out = handler.createReply();" );
        ps.print( "\t\t\t" );

        if( isGetter() )
        {
            ps.println( resultType.typeSpec().printWriteStatement( name + "()", "_out" ) );
        }
        else
        {
            ps.println( name + "(" + parameterType.printReadExpression( "_input" ) + ");" );
        }
        
        if( !raisesExpr.empty() )
        {
            ps.println( "\t\t\t}" );
            String[] excepts = raisesExpr.getExceptionNames();
            String[] classNames = raisesExpr.getExceptionClassNames();
            for( int i = 0; i < excepts.length; i++ )
            {
                ps.println( "\t\t\tcatch(" + excepts[ i ] + " _ex" + i + ")" );
                ps.println( "\t\t\t{" );
                ps.println( "\t\t\t\t_out = handler.createExceptionReply();" );
                ps.println( "\t\t\t\t" + classNames[ i ] + "Helper.write(_out, _ex" + i + ");" );
                ps.println( "\t\t\t}" );
            }
        }        
    }

    public void accept( IDLTreeVisitor visitor )
    {
        visitor.visitMethod( this );
    }



}
