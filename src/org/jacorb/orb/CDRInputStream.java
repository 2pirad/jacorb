package org.jacorb.orb;

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

import java.io.*;
import java.util.*;

import org.omg.CORBA.*;

import org.jacorb.util.*;
import org.jacorb.orb.connection.CodeSet;

import org.jacorb.util.ValueHandler;

/**
 * Read CDR encoded data 
 *
 * @author Gerald Brose, FU Berlin
 * $Id: CDRInputStream.java,v 1.25 2001-11-16 16:13:10 jacorb Exp $
 */

public class CDRInputStream
    extends org.omg.CORBA_2_3.portable.InputStream
{
    /** index for reading from the stream in plain java.io. style */
    int read_index;

    /** the stack for saving/restoring encapsulation information */
    private Stack encaps_stack = new Stack();
    private Hashtable recursiveTCMap = new Hashtable();

    /** indexes to support mark/reset */
    private int marked_pos;
    private int marked_index;

    private boolean closed = false;

    /** can be set on using property */
    private boolean use_BOM = false;

    /* character encoding code sets for char and wchar, default ISO8859_1 */
    private int codeSet =  CodeSet.getTCSDefault();
    private int codeSetW=  CodeSet.getTCSWDefault();
    public int giop_minor = 2; // needed to determine size in chars

    /**
     * Maps indices within the buffer (java.lang.Integer) to the values that 
     * appear at these indices.
     */
    private Hashtable valueMap = new Hashtable();

    /**
     * Maps indices within the buffer (java.lang.Integer) to repository ids
     * that appear at these indices.
     */
    private Hashtable repIdMap = new Hashtable();

    public boolean littleEndian = false;

    /** indices into the actual buffer */
    protected byte[] buffer = null;
    protected int pos = 0;
    protected int index = 0;

    /** 
     * for this stream to be able to return a live object reference, a
     * full ORB (not the Singleton!) must be known. If this stream is
     * used only to demarshal base type data, the Singleton is enough
     */
    private org.omg.CORBA.ORB orb = null;

 
    public CDRInputStream( org.omg.CORBA.ORB orb, byte[] buf )
    {
	this.orb = orb;
	buffer = buf;

        use_BOM = org.jacorb.util.Environment.isPropertyOn("jacorb.use_bom");
    }

    public CDRInputStream( org.omg.CORBA.ORB orb, 
                           byte[] buf, 
                           boolean littleEndian )
    {       
        this( orb, buf );
	this.littleEndian = littleEndian;
    }

    public void setGIOPMinor( int giop_minor )
    {
        this.giop_minor = giop_minor;
    }

    public int getGIOPMinor()
    {
        return giop_minor;
    }

    public void close()
	throws java.io.IOException
    {
	if( closed )
	{
	    return;
	    //throw new java.io.IOException("Stream already closed!");
	}

	encaps_stack.removeAllElements();
	BufferManager.getInstance().returnBuffer(buffer);
        recursiveTCMap.clear();
	closed = true;
    }
	
    public org.omg.CORBA.ORB orb ()
    {
        if (orb == null) orb = org.omg.CORBA.ORB.init();
        return orb;
    }

    public void setCodeSet( int codeSet, int codeSetWide )
    {
        this.codeSet = codeSet;
        this.codeSetW = codeSetWide;
    }

    private static final int _read4int(boolean _littleEndian, 
                                       byte[] _buffer, 
                                       int _pos)
    {
	if (_littleEndian)
	    return (((_buffer[_pos+3] & 0xff) << 24) +
		    ((_buffer[_pos+2] & 0xff) << 16) +
		    ((_buffer[_pos+1] & 0xff) <<  8) +
		    ((_buffer[_pos]   & 0xff) <<  0));
	else
	    return (((_buffer[_pos]   & 0xff) << 24) +
		    ((_buffer[_pos+1] & 0xff) << 16) +
		    ((_buffer[_pos+2] & 0xff) <<  8) +
		    ((_buffer[_pos+3] & 0xff) <<  0));
    }    

    private static final short _read2int( boolean _littleEndian, 
                                          byte[] _buffer, 
                                          int _pos )
    {
	if (_littleEndian)
	    return  (short)(((_buffer[_pos+1] & 0xff) << 8) +
			    ((_buffer[_pos]   & 0xff) << 0));
	else
	    return (short)(((_buffer[_pos ]    & 0xff) << 8) +
			   ((_buffer[_pos + 1] & 0xff) << 0));
    }


    protected final void skip(int distance)
    {
	pos += distance;
	index += distance;
    }

    /** 
     * close a CDR encapsulation and
     * restore index and byte order information 
     */

    public final void closeEncapsulation()
    {
	EncapsInfo ei = (EncapsInfo)encaps_stack.pop();
	littleEndian = ei.littleEndian;
	int size = ei.size;
	int start = ei.start;

	if( pos < start + size )
	    pos = start + size;

	index = ei.index + size;

        //	Debug.output(8,"Closing Encapsulation at pos: " + pos  + " littleEndian now: " + littleEndian + ",  index now " + index );
    }

    /** 
     * open a CDR encapsulation and
     * restore index and byte order information 
     */

    public final void openEncapsulation()
    {
	boolean old_endian = littleEndian;
	int _pos = pos;
	int size = read_long();

	/* save current index plus size of the encapsulation on the stack.
	   When the encapsulation is closed, this value will be restored as 
	   index */

	encaps_stack.push(new EncapsInfo(old_endian, index, pos, size ));

        //        Debug.output(8,"Opening Encapsulation at pos: " + _pos + " size: " + size);
        openEncapsulatedArray();	
    }

    public final void openEncapsulatedArray()
    {
        /* reset index  to zero, i.e. align relative  to the beginning
           of the encaps. */
	resetIndex();
	littleEndian = read_boolean();	
    }


    public byte[] getBuffer()
    {
	return buffer;
    }


    /* from java.io.InputStream */

    public int read() 
	throws java.io.IOException
    {     
	if( closed )
	    throw new java.io.IOException("Stream already closed!");

	if( available() < 1 )
	    return -1;

	return buffer[read_index++];
    }

    public int available()
    {
	return pos - read_index;
    }

    public int read( byte[] b )
	throws java.io.IOException
    {
	return read(b, 0, b.length);
    }
    

    public int read(byte[] b, int off, int len)
	throws java.io.IOException
    {
	if( b == null )
	    throw new NullPointerException();

	if( off < 0 || 
            len < 0 || 
            off + len > b.length )
	    throw new IndexOutOfBoundsException();

	if( len == 0 )
	    return 0;

	if( available() < 1 )
	    return -1;

	if( closed )
	    throw new java.io.IOException("Stream already closed!");

	int min = ( len < available() ? len : available());
	System.arraycopy(buffer, 0, b, off, min );
	return min;
    }


    public final org.omg.CORBA.Any read_any()
    {
	org.omg.CORBA.TypeCode _tc = read_TypeCode();
	org.omg.CORBA.Any any = orb.create_any();
	any.read_value( this, _tc );
	return any;
    }

    public final boolean read_boolean()
    {
	index++;
	byte bb = buffer[pos++];

	if( bb == 1 )
	    return true;
	else if ( bb == 0 )
	    return false;
	else
        {
            Debug.output( 1, "", buffer );
	    throw new Error("Unexpected boolean value: " + bb 
			    + " pos: " + pos + " index: " + index);
        }
    }

    /** arrays */

    public final  void read_boolean_array(boolean[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_boolean(); // inlining later...
    }

	
    public final char read_char()
    {
        if( codeSet == CodeSet.ISO8859_1 )
        {
	    index++; 

	    return (char)(0xff & buffer[pos++]);	       	
        }
        else
        {
            throw new org.omg.CORBA.MARSHAL( "The char type only allows single-byte codesets, but the selected one is: " + 
                                             CodeSet.csName( codeSet ) );
        }
    }

    public final void read_char_array(char[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_char(); // inlining later...
    }

    public final double read_double() 
    {
	return Double.longBitsToDouble(read_longlong());
    }

    public final void read_double_array(double[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_double(); // inlining later...
    }

    public final java.math.BigDecimal read_fixed() 
    {     
	StringBuffer sb = new StringBuffer();
	
	int b = buffer[pos++];
	int c = b & 0x0F; // second half byte
	index++;

	while(true)
	{ 
	    c = (b & 0xF0) >>> 4;
	    sb.append(c );
	    c = b & 0x0F;
	    if( c == 0xC || c == 0xD )
		break;
	    sb.append(c );
	    b = buffer[pos++];
	    index++;
	}

	java.math.BigDecimal result =
            new java.math.BigDecimal( new java.math.BigInteger( sb.toString()));

	if( c == 0xD )
	    return result.negate();
	else
	    return result;

    }

    public final float read_float() 
    {
	return Float.intBitsToFloat(read_long());
    }

    public final void read_float_array(float[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_float(); // inlining later...
    }

    public final int read_long() 
    {
	int result;

	int remainder = 4 - (index % 4);
	if (remainder != 4)
	{
	    index += remainder;
	    pos+=remainder;
	}

	result = _read4int(littleEndian,buffer,pos);

	index += 4;
	pos += 4;
	return result;
    }

    public final void read_long_array(int[] value, int offset, int length)
    {
	int remainder = 4 - (index % 4);
	if (remainder != 4)
	{
	    index += remainder;
	    pos+=remainder;
	}

	for(int j=offset; j < offset+length; j++)
	{
	    value[j] = _read4int(littleEndian,buffer,pos); // inlining read_long()
	    pos += 4;
	}

	index += 4 * length;
    }


    public final long read_longlong() 
    {    
 	int remainder = 8 - (index % 8);
 	if (remainder != 8)
 	{
 	    index += remainder;
 	    pos+=remainder;
 	}

	if (littleEndian)
	    return ((long) read_long() & 0xFFFFFFFFL) + ((long) read_long() << 32);
	else
	    return ((long) read_long() << 32) + ((long) read_long() & 0xFFFFFFFFL);
    }

    public final void read_longlong_array(long[] value, int offset, int length)
    {
 	int remainder = 8 - (index % 8);
 	if (remainder != 8)
 	{
 	    index += remainder;
 	    pos+=remainder;
 	}
        
	if (littleEndian)
        {
            for(int j=offset; j < offset+length; j++)
            {
                value[j] = ( (long) read_long() & 0xFFFFFFFFL) + 
                            ((long) read_long() << 32);
            }
        }
        else
        {
            for(int j=offset; j < offset+length; j++)
            {
                value[j] = ((long) read_long() << 32) + 
                            ((long) read_long() & 0xFFFFFFFFL);
            }
        }

        // not necessary as long as the read_long() stuff above is not inlined
        //  	pos += 8 * length;
        //  	index += 8 * length;
    }

    public final org.omg.CORBA.Object read_Object()
    {
       	org.omg.IOP.IOR ior = org.omg.IOP.IORHelper.read(this);
	ParsedIOR pior = new ParsedIOR( ior );

	if( pior.isNull() ) 
        {
	    return null;
        }
	else
	{
	    if( ! (orb instanceof org.jacorb.orb.ORB))
            {
		throw new RuntimeException( "Can not use the singleton ORB to receive object references" + 
                                            ", please initialize a full ORB instead.");
            }
	    else
            {
		return ((org.jacorb.orb.ORB)orb)._getObject( pior );
            }
	}
    }

    public final byte read_octet()
    {
	index++;
	return buffer[pos++];
    }

    public final void read_octet_array(byte[] value, int offset, int length)
    {
	System.arraycopy(buffer,pos,value,offset,length);
	index += length;
	pos += length;

	//	for(int j=offset; j < offset+length; j++)
	//  value[j] = read_octet(); // inlining later...
    }

    public final org.omg.CORBA.Principal read_Principal()
    {
	throw new org.omg.CORBA.NO_IMPLEMENT();
    }

    /**
     *   Read methods for big-endian as well as little endian data input
     *   contributed by Mark Allerton <MAllerton@img.seagatesoftware.com>
     */

    public final short read_short() 
    {
	int remainder = 2 - (index % 2);
	if (remainder != 2)
	{
	    index += remainder;
	    pos+=remainder;
	}

	short result = _read2int(littleEndian,buffer,pos);
	pos += 2;
	index += 2;
	return result;
    }

    public final void read_short_array(short[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_short(); // inlining later...
    }
	
    public final String read_string()
    {
        if( codeSet != CodeSet.ISO8859_1 )
        {
            throw new org.omg.CORBA.MARSHAL( "The char type only allows single-byte codesets, but the selected one is: " + 
                                             CodeSet.csName( codeSet ) );
            
        }

	int remainder = 4 - (index % 4);
	if( remainder != 4 )
	{
	    index += remainder;
	    pos += remainder;
	}

	// read size (#bytes == #chars)
	int size = _read4int( littleEndian, buffer, pos);
	index += 4; 
	pos += 4;

	char[] buf = new char[ size ];
        for( int i = 0; i < size; i++ )
        {
            buf[ i ] = (char) buffer[ pos++ ];
        }
        
        index += size;

        if( (size > 0) &&
            (buf[ size - 1 ] == 0) )
        {
            //omit terminating NULL char
            return new String( buf, 0, size - 1 );
        }
        else
        {
            return new String( buf );
        }
    }


    public final org.omg.CORBA.TypeCode read_TypeCode()
    {
        Hashtable tcMap = new Hashtable();
        org.omg.CORBA.TypeCode result = read_TypeCode( tcMap );
        tcMap.clear();
        return result;
    }

    private final org.omg.CORBA.TypeCode read_TypeCode( Hashtable tcMap )
    {
	int start_pos = pos;
	int kind = read_long();
        //  Debug.output( 4, "Read Type code of kind " + 
//                        kind + " at pos: " + start_pos );

	String id, name;
	String[] member_names;
	org.omg.CORBA.TypeCode[] member_types;
	int member_count, length;
	org.omg.CORBA.TypeCode content_type;
	org.omg.CORBA.TypeCode result_tc;
	boolean byteorder = false;

	switch( kind ) 
	{
	case TCKind._tk_null:
	case TCKind._tk_void:
	case TCKind._tk_short:
	case TCKind._tk_long:
	case TCKind._tk_ushort:
	case TCKind._tk_ulong:
	case TCKind._tk_float:
	case TCKind._tk_double:
	case TCKind._tk_boolean:
	case TCKind._tk_char:
	case TCKind._tk_octet:
	case TCKind._tk_any:
	case TCKind._tk_TypeCode:
	case TCKind._tk_longlong:
	case TCKind._tk_ulonglong:
        case TCKind._tk_wchar:
	case TCKind._tk_Principal:
	    return orb.get_primitive_tc( org.omg.CORBA.TCKind.from_int(kind) );
	case TCKind._tk_objref: 
	    openEncapsulation();
	    id = read_string();
	    name = read_string();
	    closeEncapsulation();
	    return orb.create_interface_tc(id, name);
	case TCKind._tk_struct: 
	    openEncapsulation();
	    id = read_string();

            //  Debug.output(4, "** remember " + id + " at pos " + start_pos );

            tcMap.put( new Integer( start_pos ), id );

	    name = read_string();
	    member_count = read_long();
	    StructMember[] struct_members = new StructMember[member_count];
	    for( int i = 0; i < member_count; i++)
	    {
		struct_members[i] = new StructMember( read_string(),
                                                      read_TypeCode(tcMap), 
                                                      null);
	    }
	    closeEncapsulation();
	    result_tc = orb.create_struct_tc(id, name, struct_members );

            recursiveTCMap.put( id , result_tc );

	    return result_tc;
	case TCKind._tk_except:
	    openEncapsulation();
	    id = read_string();

            // Debug.output(4, "** remember " + id + " at pos " + start_pos );
            tcMap.put( new Integer( start_pos ), id );

	    name = read_string();
	    member_count = read_long();
	    StructMember[] members = new StructMember[member_count];
	    for( int i = 0; i < member_count; i++)
	    {
		members[i] = new StructMember( read_string(),read_TypeCode(), null);
	    }
	    closeEncapsulation();
	    result_tc = orb.create_struct_tc(id, name, members );
            recursiveTCMap.put( id , result_tc );
	    return result_tc;
	case TCKind._tk_enum:
	    openEncapsulation();
	    id = read_string();
	    name = read_string();
	    member_count = read_long();
	    member_names = new String[member_count];
	    for( int i = 0; i < member_count; i++)
	    {
		member_names[i] = read_string();
	    }
	    closeEncapsulation();
	    return orb.create_enum_tc(id, name, member_names);
	case TCKind._tk_union:
	    {
                //		Debug.output(4, "TC Union at pos" + 
                //           pos, buffer, pos, buffer.length );

		openEncapsulation();
		id = read_string();

                // remember this TC's id and start_pos
                tcMap.put( new Integer(start_pos), id ); 

		name = read_string();
//  		Debug.output(4, "TC Union has name " + 
//                               name + " at pos" + pos );
		org.omg.CORBA.TypeCode discriminator_type = 
                    read_TypeCode(tcMap);

		int default_index = read_long();

//  		Debug.output(4, "TC Union has default idx: " +  
//                               default_index +  "  (at pos " + pos );

		member_count = read_long();

                //  Debug.output(4, "TC Union has " + member_count + 
                //               " members at pos " + pos );

		UnionMember[] union_members = new UnionMember[member_count];
		for( int i = 0; i < member_count; i++)
		{
		    // Debug.output(4, "Member " + i + "in  union " + 
                    //             id + " , " + name + ", start reading TC at pos " + pos );
		    org.omg.CORBA.Any label = orb.create_any();
		    
		    if( i == default_index )
		    {
			//Debug.output(4, "Default discr.");
                        label.insert_octet( read_octet());
		    } 
		    else 
		    {
			label.read_value( this,discriminator_type  );
		    }
 
		    String mn = read_string();

		    union_members[i] = 
                        new UnionMember( mn, label, read_TypeCode(tcMap), null);
		}		
		closeEncapsulation();
		result_tc = 
                    orb.create_union_tc( id, name, discriminator_type, union_members );
                recursiveTCMap.put( id , result_tc );
		return result_tc;
	    }
	case TCKind._tk_string: 
	    return orb.create_string_tc(read_long());
	case TCKind._tk_wstring: 
	    return orb.create_wstring_tc(read_long());
	case TCKind._tk_fixed: 
	    return orb.create_fixed_tc(read_ushort(), read_short() );
	case TCKind._tk_array: 
	    openEncapsulation();

	    content_type = read_TypeCode(tcMap);
	    length = read_long();

	    closeEncapsulation();
	    return orb.create_array_tc(length, content_type);
	case TCKind._tk_sequence: 
	    openEncapsulation();

	    content_type = read_TypeCode(tcMap);
	    length = read_long();

	    closeEncapsulation();
	    org.omg.CORBA.TypeCode seq_tc = 
                orb.create_sequence_tc(0, content_type);
	    return seq_tc;
	case TCKind._tk_alias: 
	    openEncapsulation();
	    id = read_string();
	    name = read_string();

            // Debug.output(4, "** remember alias at pos " + start_pos );
            tcMap.put( new Integer( start_pos ), id );

	    content_type = read_TypeCode( tcMap );
	    closeEncapsulation();
            result_tc = orb.create_alias_tc( id, name, content_type );
	    return result_tc;
	case TCKind._tk_value: 
	    openEncapsulation();
	    id = read_string();

            tcMap.put( new Integer( start_pos ), id );

	    name = read_string();
            short type_modifier = read_short();
	    org.omg.CORBA.TypeCode concrete_base_type = read_TypeCode( tcMap );
	    member_count = read_long();
	    ValueMember[] vMembers = new ValueMember[member_count];
	    for( int i = 0; i < member_count; i++)
	    {
		vMembers[i] = new ValueMember(read_string(),
                                              null, // id
                                              null, // defined_in
                                              null, // version
                                              read_TypeCode( tcMap ),
                                              null, // type_def
                                              read_short());
	    }
	    closeEncapsulation();
	    return  orb.create_value_tc(id, name, type_modifier,
                                        concrete_base_type, vMembers);
	case TCKind._tk_value_box: 
	    openEncapsulation();
	    id = read_string();
	    name = read_string();
	    content_type = read_TypeCode( tcMap );
	    closeEncapsulation();
            return orb.create_value_box_tc( id, name, content_type );
	case 0xffffffff:
	    /* recursive TC */
	    int negative_offset = read_long();
            String recursiveId = 
                (String)tcMap.get( new Integer( pos -4-1 + negative_offset ) );

            Debug.myAssert( recursiveId != null,
                          "No recursive TypeCode! (pos: " + 
                          (pos-4-1+negative_offset) + ")");

            
	    org.omg.CORBA.TypeCode rec_tc = 
                orb.create_recursive_tc( recursiveId );

            // Debug.output(4, "** found type code in map " + recursiveId );

	    return rec_tc;
	default:
	    // error, dump buffer contents for diagnosis
	    throw new org.omg.CORBA.MARSHAL("Cannot handle TypeCode with kind " + kind);
	}
    }

    public final int read_ulong()
    {
	return read_long();
    }

    public final void read_ulong_array(int[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_ulong(); // inlining later...
    }

    public final long read_ulonglong()
    {
	return read_longlong();
    }

    public final void read_ulonglong_array(long[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_ulonglong(); // inlining later...
    }

    public final short read_ushort()
    {
	return read_short();
    }

    public final void read_ushort_array(short[] value, int offset, int length)
    {
	for( int j = offset; j < offset+length; j++ )
	    value[j] = read_ushort(); // inlining later...
    }

    public final char read_wchar()
    {
        if( giop_minor == 2 )
        {
            //ignore size indicator
            read_wchar_size();
            
            boolean wchar_little_endian = readBOM();
            
            return read_wchar( wchar_little_endian );
        }
        else
        {
            return read_wchar( littleEndian );
        }
    }
        
    /**
     * The number of bytes this char takes. This is actually not
     * necessary since the encodings used are either fixed-length
     * (UTF-16) or have their length encoded internally (UTF-8).  
     */
    private final int read_wchar_size()
    {
        index++;
        
        return buffer[ pos++ ];
    }


    private final char read_wchar( boolean wchar_little_endian )
    {
	switch( codeSetW )
	{
            case CodeSet.UTF8 :
            {
                if( giop_minor < 2 )
                {
                    throw new Error( "GIOP 1." + giop_minor + 
                                     " only allows 2 Byte encodings for wchar, but the selected TCSW is UTF-8" );
                }

                short b = (short) (0xff & buffer[pos++]); 
                index++;

                if( (b & 0x80) == 0 ) 
                {
                    return (char) b;
                }
                else if( (b & 0xe0) == 0xc0 ) 
                { 
                    index++; 
                    return (char)(((b & 0x1F) << 6) | 
                                  ((short)buffer[pos++] & 0x3F)); 
                }
                else 
                {
                    index += 2; 
                    short b2 = (short)(0xff & buffer[pos++]);	
                    return (char)(( ( b & 0x0F) << 12) | 
                                  ( (b2 & 0x3F) << 6) | 
                                  ( (short)buffer[pos++] & 0x3F));
                }
            }
            case CodeSet.UTF16 :
            {
                char c;

                if( wchar_little_endian )
                {
                    c = (char) ( (buffer[ pos++ ] & 0xFF) | 
                                 (buffer[ pos++ ] << 8) );
                }
                else
                {
                    c = (char) ( (buffer[ pos++ ] << 8) | 
                                 (buffer[ pos++ ] & 0xFF) );
                }
                
                index += 2;
                return c;
            }
        }
    
	throw new Error( "Bad CodeSet: " + codeSetW );
    }

    /**
     * Read the byte order marker indicating the endianess.
     *
     * @return true for little endianess, false otherwise (including
     * no BOM present. In this case, big endianess is assumed per
     * spec).  
     */
    private final boolean readBOM()
    {
        if( !use_BOM )
            return littleEndian;

        if( (buffer[ pos     ] == (byte) 0xFE) &&
            (buffer[ pos + 1 ] == (byte) 0xFF) )
        {
            //encountering a byte order marker indicating big
            //endianess

            pos += 2;
            index += 2;
            
            return false;
        }
        else if( (buffer[ pos     ] == (byte) 0xFF) &&
                 (buffer[ pos + 1 ] == (byte) 0xFE) )
        {
            //encountering a byte order marker indicating
            //little endianess
            
            pos += 2;
            index += 2;
            
            return true;
        }
        else
        {
            //no BOM so big endian per spec.
            return false;
        }
    }

    public final void read_wchar_array(char[] value, int offset, int length)
    {
	for(int j=offset; j < offset+length; j++)
	    value[j] = read_wchar(); // inlining later...
    }

    public final String read_wstring()
    {
	int remainder = 4 - (index % 4);
	if( remainder != 4 )
	{
	    index += remainder;
	    pos += remainder;
	}

        if( giop_minor == 2 )
        {	
            // read size in bytes
            int size = _read4int( littleEndian, buffer, pos);
            index += 4; 
            pos += 4;
            
            char[] buf = new char[ size ];
            
            int i = 0;
            int endPos = pos + size;

            boolean wchar_litte_endian = readBOM();

            while( pos < endPos )
            {
                //ignore size
                read_wchar_size();

                buf[ i++ ] = read_wchar( wchar_litte_endian );
            }
            
            return new String( buf, 0, i );
        }
        else //GIOP 1.1 / 1.0
        {
            // read size
            int size = _read4int( littleEndian, buffer, pos);
            index += 4; 
            pos += 4;
            char[] buf = new char[ size ];

            int endPos = pos + size;

            if( codeSetW == CodeSet.UTF16 )
            {                
                //size is in chars, but char has 2 bytes
                endPos += size;
            }

            int i = 0;

            while( pos < endPos )
            {
                //use the stream-wide endianess
                buf[ i++ ] = read_wchar( littleEndian ); 
            }
            
            if( (i != 0) &&
                (buf[ i - 1 ] == 0) )
            {
                //don't return terminating NUL
                return new String( buf, 0, i - 1 );
            }
            else
            {
                //doesn't have a terminating NUL. This is actually not
                //allowed.
                return new String( buf, 0, i );
            }
        }                
    }

    public boolean markSupported()
    {
	return true;
    }

    public void mark( int readLimit )
    {
	marked_pos = pos;
	marked_index = index;
    }

    public void reset()
	throws IOException
    {
	if( pos < 0 )
	    throw new IOException("Mark has not been set!");
	pos = marked_pos;
	index = marked_index;
    }

    // JacORB-specific

    private final void resetIndex()
    {
	index = 0;
    }

    public final void setLittleEndian(boolean b)
    {
	littleEndian = b;
    }

    /** 
     * to be called from Any 
     */

    final void read_value( org.omg.CORBA.TypeCode tc, CDROutputStream out)
    {
	int kind = ((org.jacorb.orb.TypeCode)tc)._kind();

	switch (kind)
	{
	case TCKind._tk_null: 
	case TCKind._tk_void:
	    break;
	case TCKind._tk_boolean:
	    out.write_boolean( read_boolean());
	    break;
	case TCKind._tk_char:
	    out.write_char( read_char());
	    break;
	case TCKind._tk_wchar:
	    out.write_wchar( read_wchar());
	    break;
	case TCKind._tk_octet:
	    out.write_octet( read_octet());
	    break;	    
	case TCKind._tk_ushort:
	    out.write_ushort( read_ushort());
	    break;
	case TCKind._tk_short:
	    out.write_short( read_short());
	    break;
	case TCKind._tk_long:
	    out.write_long( read_long());
	    break;
	case TCKind._tk_ulong:
	    out.write_ulong( read_ulong());
	    break;
	case TCKind._tk_float:
	    out.write_float( read_float());
	    break;
	case TCKind._tk_double:
	    out.write_double( read_double());
	    break;
	case TCKind._tk_longlong:
	    out.write_longlong( read_longlong());
	    break;
	case TCKind._tk_ulonglong:
	    out.write_ulonglong( read_ulonglong());
	    break;
	case TCKind._tk_any:
	    out.write_any( read_any());
	    break;
	case TCKind._tk_TypeCode:
	    out.write_TypeCode( read_TypeCode());
	    break;
	case TCKind._tk_Principal:
	    out.write_Principal( read_Principal());
	    break;
	case TCKind._tk_objref: 
	    out.write_Object( read_Object());
	    break;
	case TCKind._tk_string: 
	    out.write_string( read_string());
	    break;
	case TCKind._tk_wstring: 
	    out.write_wstring( read_wstring());
	    break;
	case TCKind._tk_array: 
	    try
	    {
		int length = tc.length();
		for( int i = 0; i < length; i++ )
		    read_value( tc.content_type(), out );
	    } 
            catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
            {} 
	    break;
	case TCKind._tk_sequence: 
	    try
	    {
		int len = read_long();
		out.write_long(len);
		for( int i = 0; i < len; i++ )
		    read_value( tc.content_type(), out );
	    } 
	    catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
            {} 
	    break;
	case TCKind._tk_except:
	    out.write_string( read_string());
	    // don't break, fall through to ...
	case TCKind._tk_struct: 
	    try
	    {
		for( int i = 0; i < tc.member_count(); i++)
		    read_value( tc.member_type(i), out );
	    } 
	    catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
            {
                b.printStackTrace();
            } 
	    catch ( org.omg.CORBA.TypeCodePackage.Bounds b )
            {
                b.printStackTrace();
            }

	    break;
	case TCKind._tk_enum:
	    out.write_long( read_long() );
	    break;
	case TCKind._tk_alias:
	    try
	    {
                read_value( tc.content_type(), out  );
	    }
	    catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
            {
                b.printStackTrace();
            } 
	    break;
	case TCKind._tk_union:
	    try
	    {
		org.omg.CORBA.TypeCode disc = tc.discriminator_type();
		int def_idx = tc.default_index();
		int member_idx = -1;
		switch( disc.kind().value() )
		{
		case TCKind._tk_short:
		    {
			short s = read_short();
			out.write_short(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_short())
				{
				    member_idx = i;
				    break;
				}
			    }		
			}
			break;
		    }

		case TCKind._tk_long:
		    {
			int s = read_long();
			out.write_long(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_long())
				{
				    member_idx = i;
				    break;
				}
			    }
			}
			break;
		    }
		case TCKind._tk_ushort:
		    {
			short s = read_ushort();
			out.write_ushort(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_ushort())
				{
				    member_idx = i;
				    break;
				}
			    }		
			}
			break;
		    }

		case TCKind._tk_ulong:
		    {
			int s = read_ulong();
			out.write_ulong(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_ulong())
				{
				    member_idx = i;
				    break;
				}
			    }
			}
			break;
		    }
		case TCKind._tk_longlong:
		    {
			long s = read_longlong();
			out.write_longlong(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_longlong())
				{
				    member_idx = i;
				    break;
				}
			    }
			}
			break;
		    }
		case TCKind._tk_ulonglong:
		    {
			long s = read_ulonglong();
			out.write_ulonglong(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_ulonglong())
				{
				    member_idx = i;
				    break;
				}
			    }
			}
			break;
		    }
		case TCKind._tk_char:
		    {
			char s = read_char();
			out.write_char(s);
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if(i != def_idx)
			    {
				if(s == tc.member_label(i).extract_char())
				{
				    member_idx = i;
				    break;
				}
			    }		
			}
			break;
		    }
		case TCKind._tk_boolean:
		    {
			boolean b = read_boolean();
			out.write_boolean( b );
			for(int i = 0 ; i < tc.member_count() ; i++)
			{
			    if( i != def_idx)
			    {
				if( b == tc.member_label(i).extract_boolean() )
				{
				    member_idx = i;
				    break;
				}
			    }		
			}
			break;
		    }
		case TCKind._tk_enum:
		    {
			int s = read_long();
			out.write_long(s);
			for( int i = 0 ; i < tc.member_count() ; i++)
			{
			    if( i != def_idx)
			    {
				int label = 
                                    tc.member_label(i).create_input_stream().read_long();
				if(s == label)
				{
				    member_idx = i;
				    break;
				}
			    }
			}
			break;
		    }
		default:
		    throw new RuntimeException("Unfinished implementation for unions in anys, sorry.");
		} // switch

		if( member_idx != -1 )
                {
		    read_value( tc.member_type( member_idx ), out );
                }
		else if( def_idx != -1 )
                {
		    read_value( tc.member_type( def_idx ), out );
		}
	    } 
	    catch ( org.omg.CORBA.TypeCodePackage.BadKind b ){} 
	    catch ( org.omg.CORBA.TypeCodePackage.Bounds b ){}

	    break;	
	case 0xffffffff:
            try
            {
                org.omg.CORBA.TypeCode _tc = 
                    (org.omg.CORBA.TypeCode)recursiveTCMap.get(tc.id());


                if( _tc == null )
                {
                    throw new RuntimeException("No recursive TC found for " + 
                                               tc.id());
                }

                // Debug.output(4, "++ found recursive tc " + tc.id()  );

                read_value( _tc , out );
            } 
            catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
            {
                b.printStackTrace();
            } 
	    break;
	default:
	    throw new RuntimeException("Cannot handle TypeCode with kind " + kind);
	}
    }

    public java.io.Serializable read_value() 
    {
        int tag = read_long();
        if (tag == 0x7fffff00)
            throw new org.omg.CORBA.MARSHAL ("missing value type information");
        else if (tag == 0x7fffff02)
            return read_typed_value();
        else
            return read_special_value (tag);
    }

    public java.io.Serializable read_value (String rep_id) 
    {
        int tag = read_long();
        if (tag == 0x7fffff00)
            return read_untyped_value (rep_id, pos - 4);
        else if (tag == 0x7fffff02)
            return read_typed_value();
        else
            return read_special_value (tag);
    }

    public java.io.Serializable read_value (java.lang.Class clz) 
    {
        int tag = read_long();
        if (tag == 0x7fffff00)
            return read_untyped_value (org.jacorb.ir.RepositoryID.repId (clz),
                                       pos - 4);
        else if (tag == 0x7fffff02)
            return read_typed_value();
        else
            return read_special_value (tag);
    }

    public java.io.Serializable read_value (
      org.omg.CORBA.portable.BoxedValueHelper factory) 
    {
        int tag = read_long();
        if (tag == 0x7fffff00) 
        {
            int index = pos - 4;
            java.io.Serializable result = factory.read_value (this);
            valueMap.put (new Integer(index), result);
            return result;
        } 
        else if (tag == 0x7fffff02)
            // Read value according to type information.
            // Possible optimization: ignore type info and use factory for
            // reading the value anyway, since the type information is 
            // most likely redundant.
            return read_typed_value(); 
        else 
            return read_special_value (tag);
    }

    /**
     * Immediateley reads a value from this stream; i.e. without any
     * repository id preceding it.  The expected type of the value is given
     * by `repository_id', and the index at which the value started is
     * `index'.
     */
    private java.io.Serializable read_untyped_value (String repository_id,
                                                     int index)
    {
        java.io.Serializable result;
	if (repository_id.equals("IDL:omg.org/CORBA/WStringValue:1.0"))
            // special handling of strings, according to spec
	    result = read_wstring();
        else if (repository_id.startsWith ("IDL:")) 
        {
            org.omg.CORBA.portable.ValueFactory factory =
                ((org.omg.CORBA_2_3.ORB)orb).lookup_value_factory 
                                                            (repository_id);
            if (factory == null)
                throw new org.omg.CORBA.MARSHAL 
                    ("could not find value factory for " + repository_id);
            else
                result = factory.read_value (this);
        }
        else // RMI
        {
            // ValueHandler wants class, repository_id, and sending context.
            // I wonder why it wants all of these.
            // If we settle down on this implementation, compute these 
            // values more efficiently elsewhere.
            String className = 
                org.jacorb.ir.RepositoryID.className (repository_id);
            Class c = null;
            try {
	        c = Thread.currentThread().getContextClassLoader().loadClass
		                                                   (className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException ("class not found: " + className);
            }
            result = ValueHandler.readValue(this, index, 
                                            c,
                                            repository_id, 
                                            // use our own code base for now
                                            ValueHandler.getRunTimeCodeBase());
        }
        
        valueMap.put (new Integer (index), result);
        return result;
    }

    /**
     * Reads a value with type information, i.e. one that is preceded 
     * by a RepositoryID.  It is assumed that the tag of the value
     * has already been read.
     */
    private java.io.Serializable read_typed_value() 
    {
        int index = pos - 4;
        return read_untyped_value (read_repository_id(), index);
    }

    /**
     * Reads a RepositoryID from the buffer, either directly or via
     * indirection.
     */
    private String read_repository_id() 
    {
        int tag = read_long();
        if (tag == 0xffffffff)  
        {
            // indirection
            int index = read_long();
            index = index + pos - 4;
            String repId = (String)repIdMap.get (new Integer(index));
            if (repId == null)
                throw 
                 new org.omg.CORBA.MARSHAL ("stale RepositoryID indirection");
            else
                return repId;
        }
        else
        {
            // a new id
            pos -= 4;
            int index = pos;
            String repId = read_string();
            repIdMap.put (new Integer(index), repId);
            return repId;
        }
    }

    private java.io.Serializable read_special_value (int tag) {
        if (tag == 0x00000000) 
            // null tag
            return null;
        else if (tag == 0xffffffff) 
        {
            // indirection
            int index = read_long();
            index = index + pos - 4;
            java.lang.Object value = valueMap.get (new Integer(index));
            if (value == null)
                throw new org.omg.CORBA.MARSHAL ("stale value indirection");
            else
                return (java.io.Serializable)value;
        } 
        else
            throw new org.omg.CORBA.MARSHAL ("unknown value tag: " 
					     + Integer.toHexString(tag));
    } 

    //      public byte[]  get_buffer(){
    //  	return buffer;
    //      }

    public int get_pos(){
	return pos;
    }

//      public void finalize()
//      {
//  	try
//  	{
//  	    close();
//  	}
//  	catch( IOException iox )
//  	{
//  	    //ignore
//  	}
//      }
}



