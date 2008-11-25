package org.jacorb.ir;

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

import java.util.Hashtable;
import org.apache.avalon.framework.logger.Logger;
import org.jacorb.orb.TypeCode;
import org.omg.CORBA.TCKind;

/**
 * @author Gerald Brose, FU Berlin
 * @version $Id: TypeCodeUtil.java,v 1.17 2008-11-25 17:20:50 nick.cross Exp $
 */

public class TypeCodeUtil
{
    private static Hashtable cache = new Hashtable();

    static
    {
            cache.put( "java.lang.String", new TypeCode( TCKind._tk_string ));
            cache.put( "org.omg.CORBA.String", new TypeCode( TCKind._tk_string ));
            cache.put( "java.lang.Void", new TypeCode( TCKind._tk_void ));
            cache.put( "java.lang.Long",new TypeCode( TCKind._tk_longlong ));
            cache.put( "java.lang.Integer",new TypeCode( TCKind._tk_long ));
            cache.put( "java.lang.Short",new TypeCode( TCKind._tk_short ));
            cache.put( "java.lang.Float",new TypeCode( TCKind._tk_float ));
            cache.put( "java.lang.Double",new TypeCode( TCKind._tk_double ));
            cache.put( "java.lang.Boolean",new TypeCode( TCKind._tk_boolean ));
            cache.put( "java.lang.Byte" ,new TypeCode( TCKind._tk_octet));

            cache.put( "org.omg.CORBA.Any", new TypeCode( TCKind._tk_any ));
            cache.put( "java.lang.Character",new TypeCode( TCKind._tk_char));
            cache.put( "org.omg.CORBA.TypeCode",new TypeCode( TCKind._tk_TypeCode));
            cache.put( "org.omg.CORBA.Principal",new TypeCode( TCKind._tk_Principal));
            cache.put( "org.omg.CORBA.Object",
                       new TypeCode( TCKind._tk_objref,
                                     "IDL:omg.org/CORBA/Object:1.0",
                                     "IDL:omg.org/CORBA/Object:1.0" ));

    }


    /**
     * get a TypeCode for Class c. An object o of this class is needed
     * in order to get at nested types, as e.g. in array of arrays of arrays
     */

    public static org.omg.CORBA.TypeCode getTypeCode( Class c,
                                                      java.lang.Object o,
                                                      Logger logger )
        throws ClassNotFoundException
    {
        return getTypeCode( c, null, o, null, logger );
    }

    /**
     * get a TypeCode for Class c. An object o of this class is needed
     * in order to get at nested types, as e.g. in array of arrays of arrays
     */

    public static org.omg.CORBA.TypeCode getTypeCode( Class c,
                                                      ClassLoader classLoader,
                                                      java.lang.Object o,
                                                      String idlName,
                                                      Logger logger )
        throws ClassNotFoundException
    {
        String typeName = c.getName();

        if (logger.isDebugEnabled())
        {
            logger.debug("TypeCodes.getTypeCode for class : " +
                         typeName + " idlName: " + idlName);
        }

        ClassLoader loader;
        if( classLoader != null )
            loader = classLoader;
        else
            loader = c.getClassLoader(); // important for ir

        org.omg.CORBA.TypeCode _tc =
            (org.omg.CORBA.TypeCode)cache.get( typeName  );
        if( _tc != null )
        {
            //System.out.println("[ cached TypeCode ]");
            return _tc;
        }

        if( idlName != null )
        {
            _tc = (org.omg.CORBA.TypeCode)cache.get( idlName );
            if( _tc != null )
            {
                //System.out.println("[ cached TypeCode ]");
                return _tc;
            }
        }

        // debug:
        // System.out.println("- TypeCodes.getTypeCode for class : " + c.getName() );
        if( c.isPrimitive() )
        {
            if( typeName.equals("void"))
                return new TypeCode( TCKind._tk_void );
            if( typeName.equals("int"))
                return new TypeCode( TCKind._tk_long );
            if( typeName.equals("long"))
                return new TypeCode( TCKind._tk_longlong );
            if( typeName.equals("short"))
                return new TypeCode( TCKind._tk_short );
            if( typeName.equals("float"))
                return new TypeCode( TCKind._tk_float );
            if( typeName.equals("double"))
                return new TypeCode( TCKind._tk_double );
            if( typeName.equals("boolean"))
                return new TypeCode( TCKind._tk_boolean );
            if( typeName.equals("byte") )
                return new TypeCode( TCKind._tk_octet );
            if( typeName.equals("char") )
                return new TypeCode( TCKind._tk_char );
            if( typeName.equals("wchar") )
                return new TypeCode( TCKind._tk_wchar );
            else
            {
                System.err.println("- TypeCode.getTypeCode, primitive class not found " +
                                   typeName );
                return null;
            }
        }

        /* else */

        Class tcClass = null;
        Class idlEntity = null;
        try
        {
            tcClass = Class.forName("org.omg.CORBA.TypeCode", true, loader );
            idlEntity = Class.forName("org.omg.CORBA.portable.IDLEntity", true, loader);
        }
        catch ( ClassNotFoundException ce )
        {
            logger.fatalError("Can't load org.jacorb base classes!", ce);
            throw ce;
        }
        int field_size = 0;

        if ( tcClass.isAssignableFrom(c))
        {
            return new TypeCode( TCKind._tk_TypeCode );
        }
        else
        {
            if( idlName != null && idlName.length() > 0 )
            {
                try
                {
                    if( idlName.equals( "java.lang.String"))
                        return new TypeCode( TCKind._tk_string);
                    else if( idlName.equals( "org.omg.CORBA.Boolean"))
                        return new TypeCode(TCKind._tk_boolean);
                    else if( idlName.equals( "org.omg.CORBA.Byte"))
                        return new TypeCode(TCKind._tk_octet);
                    else if( idlName.equals( "org.omg.CORBA.Short"))
                        return new TypeCode(TCKind._tk_short);
                    else if( idlName.equals( "org.omg.CORBA.Long"))
                        return new TypeCode(TCKind._tk_longlong);
                    else if( idlName.equals( "org.omg.CORBA.Int"))
                        return new TypeCode(TCKind._tk_long);
                    else if( idlName.equals( "org.omg.CORBA.String"))
                        return new TypeCode(TCKind._tk_string);
                    else if( idlName.equals( "org.omg.CORBA.Char"))
                        return new TypeCode(TCKind._tk_char);
                    else if( idlName.equals( "org.omg.CORBA.Float"))
                        return new TypeCode(TCKind._tk_float);
                    else if( idlName.equals( "org.omg.CORBA.Double"))
                        return new TypeCode(TCKind._tk_double);
                    else if( idlName.equals( "org.omg.CORBA.Any"))
                        return new TypeCode(TCKind._tk_any);
                    else if( idlName.equals( "org.omg.CORBA.Object"))
                        return new TypeCode(TCKind._tk_objref);
                    else if( idlName.equals( "org.omg.CORBA.TypeCode"))
                        return new TypeCode(TCKind._tk_TypeCode);

                    Class type = Class.forName( idlName + "Helper", true, loader);

                    return (org.omg.CORBA.TypeCode)type.getDeclaredMethod(
                                                    "type",
                                                    (Class[]) null).invoke( null, (Object[]) null );
                }
                catch( ClassNotFoundException cnfe )
                {
                    logger.debug("Caught Exception", cnfe );
                    throw new RuntimeException("Could not create TypeCode for: " +
                                               c.getName() + ", no helper class for " + idlName );
                }
                catch( Exception e )
                {
                    logger.error("Caught Exception", e );
                }
            }

            if( idlEntity.isAssignableFrom(c))
            {
                try
                {
                    Class resultHelperClass = Class.forName( c.getName()+ "Helper", true, loader);

                    return (org.omg.CORBA.TypeCode)
                        resultHelperClass.getDeclaredMethod(
                                                    "type",
                                                    (Class[]) null).invoke( null, (Object[]) null );
                }
                catch( Exception cnfe )
                {
                    logger.error("Caught Exception", cnfe);
                    throw new RuntimeException("Could not create TypeCode for: " +
                                               c.getName() );
                }
            }
            else
            {
                throw new RuntimeException("Could not create TypeCode for: " +
                                           c.getName() + ", not an IDLEntity" );
            }
        }
    }

    private static String idToIDL( String s )
    {
        if( s.startsWith("IDL:"))
            s = s.substring( 4, s.lastIndexOf(":") );
        else
            s = s.replace('.','/') + ":1.0";

        StringBuffer sb = new StringBuffer( s );
        int i = 0;
        while( i < sb.length() )
        {
            if( sb.charAt(i) == '/' )
            {
                sb.setCharAt(i,':');
                sb.insert(i,':');
            }
            i++;
        }
        return sb.toString();
    }
}
