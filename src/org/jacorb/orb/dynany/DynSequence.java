package org.jacorb.orb.dynany;

/*
 *        JacORB  - a free Java ORB
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

import org.omg.DynamicAny.NameValuePair;
import org.omg.DynamicAny.DynAnyPackage.*;
import org.omg.DynamicAny.NameDynAnyPair;
import org.jacorb.orb.*;
import java.util.Vector;

/**
 * CORBA DynSequence
 *
 * @author (c) Gerald Brose, FU Berlin 1999
 * @version $Id: DynSequence.java,v 1.4 2001-03-19 11:08:49 brose Exp $
 *
 */

public final class DynSequence
    extends DynAny
    implements org.omg.DynamicAny.DynSequenceOperations
{
    private Vector members;
    private int length;
    private org.omg.CORBA.TypeCode elementType;

    DynSequence(org.jacorb.orb.ORB orb,
                org.omg.DynamicAny.DynAnyFactory dynFactory,
                org.jacorb.orb.Any any)
        throws org.omg.DynamicAny.DynAnyPackage.TypeMismatch, InvalidValue
    {
        super(orb, dynFactory, any);
    }

    DynSequence(org.jacorb.orb.ORB orb,
                org.omg.DynamicAny.DynAnyFactory dynFactory,
                org.omg.CORBA.TypeCode tc)
        throws InvalidValue, TypeMismatch
    {
        if( tc.kind() != org.omg.CORBA.TCKind.tk_sequence )
            throw new TypeMismatch();   
        try
        {
            type = tc;

            this.orb = orb;
            this.dynFactory = dynFactory;

            elementType = tc.content_type();
            limit = tc.length();
            length = 0;
            members = new Vector();
        }
        catch( org.omg.CORBA.TypeCodePackage.BadKind bk )
        {
            bk.printStackTrace();
        }
    }

    public void from_any(org.omg.CORBA.Any value) 
        throws InvalidValue, TypeMismatch
    {
        if( ! type().equal( value.type() ))
        {
            System.err.println("expected tc kind " + type().kind().value()
                       + ", got " + value.type().kind().value() );      
            try
            { 
                System.err.println("expected element tc kind " + 
                                   type().content_type().kind().value()+ ", got " + 
                                   value.type().content_type().kind().value() );         
                System.err.println("expected length " + type().length()
                                   + ", got " + value.type().length() );         
            }
            catch( org.omg.CORBA.TypeCodePackage.BadKind bk )
            {
                // should not happen anymore
                bk.printStackTrace();
            }
            throw new org.omg.DynamicAny.DynAnyPackage.TypeMismatch();
        }

        try
        {
            limit = type().length();
            org.omg.CORBA.portable.InputStream is = 
                value.create_input_stream();
            length = is.read_long();
            if( length > 0 )
                pos = 0;

            if( limit != 0 && length > limit )
                throw new InvalidValue();

            members = new Vector(length);
            elementType = type().content_type();

            for( int i = 0 ; i < length; i++ )
            {
                Any a = (org.jacorb.orb.Any)orb.create_any();
                a.read_value(is, elementType);  
                members.addElement(a);         
            }   
        }
        catch( org.omg.CORBA.TypeCodePackage.BadKind bk )
        {
            // should not happen anymore
            bk.printStackTrace();
        }
    }

    public org.omg.CORBA.Any to_any() 
    {
        org.jacorb.orb.Any out_any = (org.jacorb.orb.Any)orb.create_any();
        out_any.type( type());

        CDROutputStream os = new CDROutputStream();
        os.write_long( length );

        for( int i = 0; i < length; i++)
        {
            os.write_value( elementType, 
                            (CDRInputStream)((Any)members.elementAt(i)).create_input_stream());
        }

        CDRInputStream is = new CDRInputStream(orb, os.getBufferCopy());
        out_any.read_value( is, type());
        return out_any;
    }

    public int get_length()
    {
        return length;
    }

    public void set_length(int len) 
        throws InvalidValue
    {
        if( limit > 0 && len > limit )
            throw new InvalidValue();

        if( len == 0 )
        {
            members = new Vector();
            pos = -1;
        }

        if( len > length )
        {
            try
            {
                for( int i = length; i < len; i++ )
                {
                    // create correctly initialized anys
                    members.addElement( dynFactory.create_dyn_any_from_type_code( elementType ).to_any() );
                }
            }
            catch( org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode itc )
            {
                // should neever happen
                itc.printStackTrace();
            }

            if( pos != -1 )
                pos = length + 1;
        }
        else if( len < length )
        {
            members.setSize(len);

            if( pos > len )
                pos = -1;
        }
        length = len;
    }

    public org.omg.CORBA.Any[] get_elements()
    {
        Any[] result = new Any[ members.size()];
        for( int i = members.size(); i-- > 0; )
            result[i] = (Any)members.elementAt(i);
        return result;
    }

    public void set_elements(org.omg.CORBA.Any[] value) 
        throws TypeMismatch, InvalidValue
    {
        if( limit > 0 && value.length > limit )
            throw new InvalidValue();

        for( int i = value.length; i-- > 0 ;)
            if( value[i].type().kind() != elementType.kind() )
                throw new TypeMismatch();

        /** ok now */
        length = value.length;
        members = new Vector();
        for( int i = 0; i < length; i++)
        {
            members.addElement(value[i]);
        }

        if( length > 0 )
            pos = 0;
        else
            pos = -1;
       
    }

    public org.omg.DynamicAny.DynAny[] get_elements_as_dyn_any()
    {
        org.omg.DynamicAny.DynAny[] result = new org.omg.DynamicAny.DynAny[ members.size()];
        try
        {
            for( int i = members.size(); i-- > 0; )
                result[i] = dynFactory.create_dyn_any( (Any)members.elementAt(i));
            return result;
        }
        catch( org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode itc )
        {
            // should never happen
            itc.printStackTrace();
        }
        return null;
    }

    public void set_elements_as_dyn_any(org.omg.DynamicAny.DynAny[] value) 
        throws TypeMismatch, InvalidValue
    {
        org.omg.CORBA.Any [] any_seq = new org.omg.CORBA.Any[value.length];
        for( int i = value.length; i-- > 0; )
            any_seq[i] = value[i].to_any();

        set_elements( any_seq );
    }

    public void destroy()
    {
        super.destroy();
        members.removeAllElements();
        members = null;
        elementType = null;
    }


    public boolean next()
    {
        if( pos < length-1 )
        {
            pos++;
            return true;
        }
        pos = -1;
        return false;
    }

   public boolean seek(int index)    
    {
        if( index < 0 )
        {
            pos = -1;
            return false;
        }
        if( index < length )
        {
            pos = limit;
            return true;
        }
        pos = -1;
        return false;
    }

    /* iteration interface */

    public org.omg.DynamicAny.DynAny current_component()
    {   
        if( pos == -1 )
        {
            return null;
        }
        try
        {
            return dynFactory.create_dyn_any( (Any)members.elementAt(pos) );
        }
        catch( org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode itc )
        {
            // should never happen
            itc.printStackTrace();
        }
        return null;
    }
   
    public int component_count()
    {
        return get_length();
    }


}








