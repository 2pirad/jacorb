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

import org.jacorb.orb.connection.CodeSet;
import org.jacorb.ir.RepositoryID;
import org.jacorb.util.*;

import org.omg.CORBA.TCKind;
import org.omg.PortableServer.*;

/**
 * @author Gerald Brose, FU Berlin 1999
 * @version     $Id: CDROutputStream.java,v 1.20 2001-11-14 12:40:01 jacorb Exp $ 
 * 
 * A stream for CDR marshalling.
 *
 */

public class CDROutputStream
    extends org.omg.CORBA_2_3.portable.OutputStream
{
    private static final int NET_BUF_SIZE = 1024;
    private static final int MEM_BUF_SIZE = 256;

    private int index = 0;
    private int pos = 0;
    private byte[] buffer = null;

    private boolean closed = false;
    private boolean released = false;

    /* character encoding code sets for char and wchar, default ISO8859_1 */
    private int codeSet =  CodeSet.getTCSDefault();
    private int codeSetW=  CodeSet.getTCSWDefault();

    /** can be set on using property */
    private boolean use_BOM = false;
    private BufferManager bufMgr = null;

    private int resize_factor = 1;
    private int encaps_start = -1;
    private Stack encaps_stack = new java.util.Stack();
    private Stack recursiveTCStack = new Stack();

    /**
     * Maps all value objects that have already been written to this stream
     * to their position within the buffer.  The position is stored as 
     * a java.lang.Integer.
     */
    private Map valueMap = new HashMap();

    /**
     * Maps all repository ids that have already been written to this
     * stream to their position within the buffer.  The position is
     * stored as a java.lang.Integer.  
     */
    private Map repIdMap = new HashMap();

    private final static String null_ior_str = 
        "IOR:00000000000000010000000000000000";
    private final static org.omg.IOP.IOR null_ior = 
        new org.omg.IOP.IOR("", new org.omg.IOP.TaggedProfile[0]);

    private org.omg.CORBA.ORB orb = null;

    //default access, so derived classes can access this field
    public int giop_minor = 2;
    
    /** 
     * OutputStreams created using  the empty constructor are used for
     * in  memory marshaling, but do  not use the  ORB's output buffer
     * manager 
     */
    public CDROutputStream()
    {
        bufMgr = BufferManager.getInstance();
        buffer = bufMgr.getBuffer( MEM_BUF_SIZE );
        use_BOM = org.jacorb.util.Environment.isPropertyOn("jacorb.use_bom");
   }

    /** 
     * OutputStreams created using this constructor 
     * are used also for in memory marshaling, but do use the
     * ORB's output buffer manager
     */

    public CDROutputStream( org.omg.CORBA.ORB orb )
    {
        this.orb = orb;
        bufMgr = BufferManager.getInstance();
        buffer = bufMgr.getBuffer( NET_BUF_SIZE );
        use_BOM = org.jacorb.util.Environment.isPropertyOn("jacorb.use_bom");
    }
        

    /** 
     *  Class constructor setting the buffer size for the message
     *  and the character encoding sets
     */

    public CDROutputStream( byte[] buf )
    {
        bufMgr = BufferManager.getInstance();
        buffer = buf;
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

    public void setGIOPMinor( int giop_minor )
    {
        this.giop_minor = giop_minor;
    }

    public int getGIOPMinor()
    {
        return giop_minor;
    }

    public void close() 
    {
        if( closed )
            throw new Error("Stream already closed!");
        
        closed = true;
    }

    public void release()
    {
        if( released )
	{
	    return;
            //throw new Error("Stream already released!");
	}
	
        if( bufMgr != null )
        {
            bufMgr.returnBuffer( buffer );
        }

        released = true;
    }

    /**
     * This version of check does both array length checking and
     * data type alignement. It is a convenience method.
     */

    private final void check(int i,int align)
    {
        check(i);
        int remainder = align - (index % align);
        if (remainder != align)
        {
            index += remainder;
            pos+=remainder;
        }
    }
        
    /** 

     * check whether the current buffer is big enough to receive
     * i more bytes. If it isn't, get a bigger buffer.
     */

    private final void check(int i)
    {
        //if( closed )
        //   throw new java.lang.Error("Trying to write to a closed stream!");

        if( pos + i + 2 > (buffer.length))
        {
            byte [] new_buf;
            if( bufMgr == null )
            {
                int size = buffer.length;
                while( pos + i + 2 > size )
                    size = size<<1;
                new_buf = new byte[size];
                System.arraycopy(buffer,0,new_buf,0,pos);
            }
            else
            {
                new_buf = bufMgr.getBuffer(pos+i+2);
                System.arraycopy(buffer,0,new_buf,0,pos);
                bufMgr.returnBuffer(buffer);
            }
            buffer = new_buf;
        }
    }

    private final static void _write4int(byte[] buf, int _pos, int value)
    {
        buf[_pos]   = (byte)((value >> 24) & 0xFF);
        buf[_pos+1] = (byte)((value >> 16) & 0xFF);
        buf[_pos+2] = (byte)((value >>  8) & 0xFF);
        buf[_pos+3] = (byte) (value        & 0xFF);
    }

    /** 
     *  Start a CDR encapsulation. All subsequent writes
     *  will place data in the encapsulation until
     *  endEncapsulation is called. This will write
     *  the size of the encapsulation. 
     */

    public final void beginEncapsulation()
    {        
        // align to the next four byte boundary 
        // as a preparation for writing the size
        // integer (which we don't know before the
        // encapsulation is closed)
                
        check(8,4);

        // leave 4 bytes for the encaps. size that
        // is to be written later

        pos += 4;
        index += 4;

        /* Because encapsulations can be nested, we need to
           remember the beginnning of the enclosing
           encapsulation (or -1 if we are in the outermost encapsulation)
           Also, remember the current index because we need to 
           restore this when closing the encapsulation */

        encaps_stack.push(new EncapsInfo(index, encaps_start ));

        // the start of this encapsulation

        encaps_start = pos;
        beginEncapsulatedArray();
    }

    /**
     * Can be used locally for data type conversions
     * without preceeding call to beginEncapsulation, i.e.
     * without a leading long that indicates the size.
     */

    public final void beginEncapsulatedArray()
    {
        /* set the index for alignment to 0, i.e. align relative to the
           beginning of the encapsulation */
        resetIndex();
        
        // byte_order flag set to FALSE

        buffer[pos++] = 0;
        index++;
    }

    /**
     * Terminate the encapsulation by writing its length
     * to its beginning. 
     */

    public final void endEncapsulation()
        throws IOException
    {
        if( encaps_start == -1 )
            throw new IOException("too many end-of-encapsulations");

        // determine the size of this encapsulation

        int encaps_size = pos - encaps_start;

        // insert the size integer into the appropriate place

        buffer[encaps_start -4 ]  = (byte)((encaps_size >>> 24) & 0xFF);
        buffer[encaps_start -3 ] = (byte)((encaps_size >>> 16) & 0xFF);
        buffer[encaps_start -2 ] = (byte)((encaps_size >>>  8) & 0xFF);
        buffer[encaps_start -1 ] = (byte)(encaps_size & 0xFF);

        /* restore index and encaps_start information*/

        EncapsInfo ei = (EncapsInfo)encaps_stack.pop();
        encaps_start = ei.start;
        index = ei.index + encaps_size;
    }

    public byte[] getBufferCopy()
    {
        byte[] result = null;

        result = new byte[pos];
        System.arraycopy(buffer,0,result,0,result.length);

        return result;
    }

    public byte[] getInternalBuffer()
    {
        return buffer;
    }

    private void resetIndex()
    {
        index = 0;
    }

    public int size()
    {
        return pos;
    }

    public void reset() 
    {
        pos = 0;
        index = 0;
    }

    public void finalize()
    {
	release();
    }

    public void skip( int step ) 
    {
        pos += step;
        index += step;
    }

    public void reduceSize( int amount )
    {
        pos -= amount;
    }

    /**
     * Add <tt>amount</tt> empty space
     */
    public void increaseSize( int amount )
    {
        pos += amount;
        
        check( amount );
    }

    public void setBuffer( byte[] b )
    {
        bufMgr.returnBuffer( buffer );

        buffer = b;
        
        reset();
    }

    /**************************************************
     * The following operations are from OutputStream *
     **************************************************/

    public org.omg.CORBA.portable.InputStream create_input_stream()
    {
        byte [] buf = (byte [])buffer.clone();
        return new CDRInputStream( orb, buf );
    }

    public final void write_any( org.omg.CORBA.Any value )
    {
        write_TypeCode( value.type() );
        value.write_value( this ) ;
    }

    public  final void write_boolean (boolean value)
    {
        check(1);

        if( value )
            buffer[pos++] = 1;
        else
            buffer[pos++] = 0;
        index++;
    }

    public  final void write_boolean_array(boolean[] value, int offset, int length)
    {
        if( value != null )
        {
            check(length);

            for( int i = offset; i < offset+length; i++ )
            {
                if( value[i] )
                    buffer[pos++] = 1;
                else
                    buffer[pos++] = 0;
            }
            index += length;
        }
    }

  
    /**
     * Writes char according to specified encoding.
     */
    public final void write_char( char c )
    {
        check( 1 );

        if( codeSet == CodeSet.ISO8859_1 )
        {
            if( c > 255 || c < 0 )
            {
                throw new org.omg.CORBA.MARSHAL("char (" + c + 
                                                ") out of range for ISO8859_1");
            }

            index++; 
            buffer[ pos++ ] = (byte) c;
        }
        else
        {
            throw new org.omg.CORBA.MARSHAL( "The char type only allows single-byte codesets, but the selected one is: " + 
                                             CodeSet.csName( codeSet ) );
        }
    }

    public final void write_char_array(char[] value, int offset, int length)
    {
        if( value == null ) 
            throw new org.omg.CORBA.MARSHAL("Null References");
        check( length*3 );
        for( int i = offset; i < offset+length; i++) 
            write_char( value[i] );
    }
        
    public final void write_string( String s )
    {
        if( s == null )
        {
            throw new org.omg.CORBA.MARSHAL("Null References");
        }

        if( codeSet != CodeSet.ISO8859_1 )
        {
            throw new org.omg.CORBA.MARSHAL( "The char type only allows single-byte codesets, but the selected one is: " + 
                                             CodeSet.csName( codeSet ) );
            
        }
        
        // size indicator ulong + length in chars( i.e. bytes for type char)
        // incl. terminating NUL char
        int size = 4 + s.length() + 1;

        check( size, 4 );
            
        _write4int( buffer, pos, size - 4 ); // write length indicator        
            
        pos += 4;
        index += 4;
        
        for( int i = 0; i < s.length(); i++ )
        {
            buffer[ pos++ ] = (byte) s.charAt( i );
        }
        
        index += s.length();
        
        buffer[ pos++ ] = (byte) 0; //terminating NUL char
        index++;
    }

    public final void write_wchar( char c )
    {
        write_wchar( c, use_BOM );
    }

    private final void write_wchar( char c, boolean write_bom )
    {
        check(3);

        switch( codeSetW )
        {
            case CodeSet.UTF8 :
            {
                if( c <= 0x007F ) 
                {
                    if( giop_minor == 2 )
                    {
                        //the chars length in bytes
                        write_octet( (byte) 1 );
                    }

                    buffer[ pos++ ] = (byte) c; 
                } 
                else if( c > 0x07FF ) 
                {
                    if( giop_minor == 2 )
                    {
                        //the chars length in bytes
                        write_octet( (byte) 3 );
                    }

                    buffer[pos++]=(byte)(0xE0 | ((c >> 12) & 0x0F));
                    buffer[pos++]=(byte)(0x80 | ((c >>  6) & 0x3F));
                    buffer[pos++]=(byte)(0x80 | ((c >>  0) & 0x3F));

                    index += 3; 
                } 
                else 
                {
                    if( giop_minor == 2 )
                    {
                        //the chars length in bytes
                        write_octet( (byte) 2 );
                    }

                    buffer[pos++]=(byte)(0xC0 | ((c >>  6) & 0x1F));
                    buffer[pos++]=(byte)(0x80 | ((c >>  0) & 0x3F));

                    index += 2; 
                }
                break;
            }
            case CodeSet.UTF16 :
            {
                if( giop_minor == 2 )
                {
                    //the chars length in bytes
                    write_octet( (byte) 2 );

                    if( write_bom )
                    {
                        //big endian encoding
                        buffer[ pos++ ] = (byte) 0xFE;
                        buffer[ pos++ ] = (byte) 0xFF;
                        
                        index += 2; 
                    }

                    //write unaligned
                    buffer[pos++] = (byte)((c >> 8) & 0xFF);
                    buffer[pos++] = (byte) (c       & 0xFF);
                    index += 2;
                }
                else
                {
                    //UTF-16 char is treated as an ushort (write aligned)
                    write_short( (short) c );
                }

                break;
            }
            default : 
            {
                throw new Error("Bad codeset: " + codeSet);
            }
        }
    }

    public  final void write_wchar_array(char[] value, int offset, int length)
    {
        if( value == null ) 
            throw new org.omg.CORBA.MARSHAL("Null References");

        check( length * 3 );

        for( int i = offset; i < offset+length; i++)
            write_wchar( value[i] );
    }
        
    public final void write_wstring( String s )
    {      
        if( s == null ) 
        {
            throw new org.omg.CORBA.MARSHAL("Null References");
        }
                
        //size ulong + no of bytes per char (max 3 if UTF-8) +
        //terminating NUL
        check( 4 + s.length() * 3 + 3, 4);

        int startPos = pos;         // store position for length indicator
        pos += 4; 
        index += 4;                 // reserve for length indicator

        //the byte order marker
        if( giop_minor == 2 && use_BOM )
        {
            //big endian encoding
            buffer[ pos++ ] = (byte) 0xFE;
            buffer[ pos++ ] = (byte) 0xFF;
            
            index += 2;             
        }

        // write characters in current wide encoding, add null terminator
        for( int i = 0; i < s.length(); i++ ) 
        {
            write_wchar( s.charAt(i), false ); //no BOM
        }

        if( giop_minor < 2 )
        {
            //terminating NUL char
            write_wchar( (char)0, false ); //no BOM
        }
                        
        int size = 0;
        if( giop_minor == 2 )
        {
            //size in bytes (without the size ulong)
            size = pos - startPos - 4;
        }
        else
        {
            if( codeSetW == CodeSet.UTF8 )
            {
                //size in bytes (without the size ulong)
                size = pos - startPos - 4;
            }
            else if( codeSetW == CodeSet.UTF16 )
            {
                //size in chars (+ NUL char)
                size = s.length() + 1;
            }
        }

        // write length indicator
        _write4int( buffer, startPos, size );
    }

    public  final void write_double (double value)
    {
        write_longlong( Double.doubleToLongBits(value) );
    }

    public  final void write_double_array( double[] value, int offset, int length)
    {
        /* align to 8 byte boundary */
                
        check(7 + length*8, 8);
        
        if( value != null )
        {
            for( int i = offset; i < offset+length; i++ )
            {
                long d = Double.doubleToLongBits(value[i]);
                buffer[pos]   = (byte)((d >>> 56) & 0xFF);
                buffer[pos+1] = (byte)((d >>> 48) & 0xFF);
                buffer[pos+2] = (byte)((d >>> 40) & 0xFF);
                buffer[pos+3] = (byte)((d >>> 32) & 0xFF);
                buffer[pos+4] = (byte)((d >>> 24) & 0xFF);
                buffer[pos+5] = (byte)((d >>> 16) & 0xFF);
                buffer[pos+6] = (byte)((d >>>  8) & 0xFF);
                buffer[pos+7] = (byte) (d & 0xFF);
                pos += 8;
            }
            index += 8*length;
        }
    }

    public  final void write_fixed(java.math.BigDecimal value) 
    {    
        String v = value.movePointRight(value.scale()).toString();
        byte [] representation;
        int b, c;

        if( (v.length() %2) == 0)
        {
            representation = new byte[ v.length()/2 +1];
            representation[0] = 0x00;
     
            for( int i = 0; i < v.length(); i++ )
            {
                c = Character.digit(v.charAt(i), 10);
                b = representation[(1 + i)/2] << 4;
                b |= c;
                representation[(1 + i)/2] = (byte)b;
            }
        }
        else
        {
            representation = new byte[ (v.length()+1) /2];
            for( int i = 0; i < v.length(); i++ )
            {
                c = Character.digit(v.charAt(i), 10);
                b = representation[i/2] << 4;
                b |= c;
                representation[i/2] = (byte)b;
            }
        }
        b = representation[representation.length-1] << 4;

        representation[representation.length-1] = (byte)((value.signum() < 0 )? (b | 0xD) : (b | 0xC));

        check(representation.length);
        System.arraycopy(representation,0,buffer,pos,representation.length);
        index += representation.length;
        pos += representation.length;
        
    }

    public  final void write_float (float value)
    {
        write_long(Float.floatToIntBits(value));
    }

    public  final void write_float_array(float[] value, int offset, int length)
    {
        /* align to 4 byte boundary */
                
        check(3 + length*4,4);
        
        if( value != null )
        {
            for( int i = offset; i < offset+length; i++ )
            {
                _write4int(buffer,pos, Float.floatToIntBits( value[i] ));
                pos += 4;
            }
            index += 4*length;
        }
    }

    public  final void write_long (int value)
    {
        check(7,4);

        _write4int(buffer,pos,value);

        pos += 4; index += 4;
    }

    public final void write_long_array(int[] value, int offset, int length)
    {
        /* align to 4 byte boundary */

        check(3 + length*4,4);
        
        
        int remainder = 4 - (index % 4);
        if (remainder != 4)
        {
            index += remainder;
            pos+=remainder;
        }
        
        if( value != null )
        {
            for( int i = offset; i < offset+length; i++ )
            {
                _write4int(buffer,pos,value[i]);
                pos += 4;
            }
            index += 4*length;
        }
    }

    public  final void write_longlong (long value)
    {
        check(15,8);

        buffer[pos]   = (byte)((value >>> 56) & 0xFF);
        buffer[pos+1] = (byte)((value >>> 48) & 0xFF);
        buffer[pos+2] = (byte)((value >>> 40) & 0xFF);
        buffer[pos+3] = (byte)((value >>> 32) & 0xFF);
        buffer[pos+4] = (byte)((value >>> 24) & 0xFF);
        buffer[pos+5] = (byte)((value >>> 16) & 0xFF);
        buffer[pos+6] = (byte)((value >>>  8) & 0xFF);
        buffer[pos+7] = (byte)(value & 0xFF);

        index += 8;
        pos += 8;
    }

    public  final void write_longlong_array(long[] value, int offset, int length)
    {
        check(7 + length*8,8);

        if( value != null )
        {
            for( int i = offset; i < offset+length; i++ )
            {
                buffer[pos]   = (byte)((value[i] >>> 56) & 0xFF);
                buffer[pos+1] = (byte)((value[i] >>> 48) & 0xFF);
                buffer[pos+2] = (byte)((value[i] >>> 40) & 0xFF);
                buffer[pos+3] = (byte)((value[i] >>> 32) & 0xFF);
                buffer[pos+4] = (byte)((value[i] >>> 24) & 0xFF);
                buffer[pos+5] = (byte)((value[i] >>> 16) & 0xFF);
                buffer[pos+6] = (byte)((value[i] >>>  8) & 0xFF);
                buffer[pos+7] = (byte) (value[i] & 0xFF);
                pos += 8;
            }
            index += 8*length;
        }
    }

    public void write_Object (org.omg.CORBA.Object value)
    {

        if( value == null )
        {
            org.omg.IOP.IORHelper.write(this, null_ior );
        }
        else
        {
            if( value instanceof LocalityConstrainedObject )
                throw new org.omg.CORBA.MARSHAL("Attempt to serialize a locality-constrained object.");     
            org.omg.CORBA.portable.ObjectImpl obj = 
                (org.omg.CORBA.portable.ObjectImpl)value;
            org.omg.IOP.IORHelper.write(this,  
                                        ((Delegate)obj._get_delegate()).getIOR()  );
        }
    }

    ////////////////////////////////////////////// NEW!
    public void write_IOR (org.omg.IOP.IOR ior)
    {
        if( ior == null )
        {
            org.omg.IOP.IORHelper.write(this, null_ior );
        }
        else
        {
            org.omg.IOP.IORHelper.write(this, ior);
        }
    }
    ////////////////////////////////////////////// NEW!    

    public  final void write_octet (byte value)
    {
        check(1);
        index++;
        buffer[pos++] = value;
    }

    public  final void write_octet_array(byte[] value, int offset, int length)
    {
        if( value != null )
        {
            check(length);
            System.arraycopy(value,offset,buffer,pos,length);
            index += length;
            pos += length;
        }
    }

    public  final void write_Principal(org.omg.CORBA.Principal value)
    {
        write_octet_array( value.name(), 0, value.name().length);
    }

    public  final void write_short(short value)
    {
        check(3,2);
        
        buffer[pos]   = (byte)((value >>  8) & 0xFF);
        buffer[pos+1] = (byte)(value & 0xFF);
        index += 2; pos+=2;
    }

    public  final void write_short_array(short[] value, int offset, int length)
    {
        /* align to 2-byte boundary */

        check(2*length + 3);
        
        int remainder = 2 - (index % 2);
        if (remainder != 2)
        {
            index += remainder;
            pos+=remainder;
        }
        
        if( value != null )
        {
            for( int i = offset; i < offset+length; i++ )
            {
                buffer[pos]   = (byte)((value[i] >>>  8) & 0xFF);
                buffer[pos+1] = (byte)( value[i] & 0xFF);
                pos += 2;
            }
            index += 2*length;
        }
    }

    public  final void write_TypeCode(org.omg.CORBA.TypeCode value)
    {
        Hashtable tcMap = new Hashtable();
        write_TypeCode( value, tcMap );
        tcMap.clear();
    }

    private final void writeRecursiveTypeCode( org.omg.CORBA.TypeCode value, 
                                               Hashtable tcMap )
    {
        try
        {
//              Debug.output(4,"** Write recursive Type code for id " +
//                           value.id() + " pos " +
//                           (((Integer)tcMap.get( value.id())).intValue() - pos ) );
        
            write_long( -1 ); // recursion marker
            int curr = pos; // that's where we will be in a second...
            int negative_offset = 
                ((Integer) tcMap.get( value.id())).intValue() - curr;
            
            write_long( negative_offset );
        }
        catch( org.omg.CORBA.TypeCodePackage.BadKind bk )
        {
            //            must not happen
        }
    }


    private final void write_TypeCode( org.omg.CORBA.TypeCode value, 
                                       Hashtable tcMap )
    {
        /* remember the buffer position this TC gets written at. This
           is necessary if nested TCs are recursively defined by refererring
           to this TC.
        */

        int start_pos = pos+1;
        int _kind = ((TypeCode)value)._kind();
        int _mc; // member count

        try
        {
            if( ((TypeCode)value).is_recursive() &&
                tcMap.containsKey( value.id()) )
            {
                writeRecursiveTypeCode( value, tcMap );
            }
            else
            {
                // regular TypeCodes
                switch( _kind ) 
                {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 23:
                case 24:
                case 25:
                case 26:
                    write_long( _kind  );
                    break;
                case TCKind._tk_objref: 
                    write_long( _kind  );
                    beginEncapsulation();
                    write_string( value.id() );
                    write_string( value.name() );
                    endEncapsulation();
                    break;
                case TCKind._tk_struct: 
                case TCKind._tk_except:
                    if( tcMap.containsKey( value.id()) )
                    {
                        writeRecursiveTypeCode( value, tcMap );
                    }
                    else
                    {         
                        write_long( _kind  );
                        tcMap.put( value.id(), new Integer( start_pos ) );
                        beginEncapsulation();
                        write_string(value.id());
                        write_string(value.name());
                        _mc = value.member_count();
                        write_long(_mc);
                        for( int i = 0; i < _mc; i++)
                        {
                            write_string( value.member_name(i) );
                            write_TypeCode( value.member_type(i), tcMap );
                        }
                        endEncapsulation();
                    }
                    break;
                case TCKind._tk_enum:
                    if( tcMap.containsKey( value.id()) )
                    {
                        writeRecursiveTypeCode( value, tcMap );
                    }
                    else
                    {
                        write_long( _kind  );
                        beginEncapsulation();
                        write_string( value.id());
                        write_string( value.name());
                        _mc = value.member_count();
                        write_long(_mc);
                        for( int i = 0; i < _mc; i++)
                        {
                            write_string( value.member_name(i) );
                        }
                        endEncapsulation();
                        break;
                    }
                case TCKind._tk_union:
                    if( tcMap.containsKey( value.id()) )
                    {
                        writeRecursiveTypeCode( value, tcMap );
                    }
                    else
                    {
                        tcMap.put( value.id(), new Integer( start_pos ) );

                        write_long( _kind  );
                        beginEncapsulation();
                        write_string( value.id() );
                        write_string( value.name() );

                        write_TypeCode( value.discriminator_type());
                        write_long( value.default_index());
                        _mc = value.member_count();
                        write_long(_mc);
                        for( int i = 0; i < _mc; i++)       
                        {
                            if( i == value.default_index() )
                            {
                                write_octet((byte)0);
                            } 
                            else
                            {
                                value.member_label(i).write_value( this );
                            }
                            write_string( value.member_name(i));
                            write_TypeCode( value.member_type(i), tcMap );
                        }
                        endEncapsulation();
                    }
                    break;
                case TCKind._tk_wstring: 
                case TCKind._tk_string: 
                    write_long( _kind  );
                    write_long(value.length());
                    break;
                case TCKind._tk_fixed: 
                    write_long( _kind  );
                    write_ushort( value.fixed_digits() );
                    write_short( value.fixed_scale() );
                    break;
                case TCKind._tk_array: 
                case TCKind._tk_sequence: 
                    write_long( _kind  );
                    beginEncapsulation();
//                      if( ((TypeCode)value.content_type()).is_recursive())
//                      {
//                          Integer enclosing_tc_pos = (Integer)recursiveTCStack.peek();
//                          write_long( enclosing_tc_pos.intValue() - pos );
//                      }
//                      else
                        write_TypeCode( value.content_type(), tcMap);
                    write_long(value.length());
                    endEncapsulation();
                    break;
                case TCKind._tk_alias: 
                    if( tcMap.containsKey( value.id()) )
                    {
                        writeRecursiveTypeCode( value, tcMap ); 
                    }
                    else
                    {
                        write_long( _kind  );
                        beginEncapsulation();
                        tcMap.put( value.id(), new Integer( start_pos ) );
                        write_string(value.id());
                        write_string(value.name());
                        write_TypeCode( value.content_type(), tcMap);
                        endEncapsulation();
                    }
                    break;
                case TCKind._tk_value: 
                    if( tcMap.containsKey( value.id()) )
                    {
                        writeRecursiveTypeCode( value, tcMap );
                    }
                    else
                    {
                        write_long( _kind  );
                        tcMap.put( value.id(), new Integer( start_pos ) );
                        beginEncapsulation();
                        write_string(value.id());
                        write_string(value.name());
                        write_short( value.type_modifier() );
                        org.omg.CORBA.TypeCode base = value.concrete_base_type();
                        if (base != null)
                            write_TypeCode(base, tcMap);
                        else
                            write_long (TCKind._tk_null);
                        _mc = value.member_count();
                        write_long(_mc);
                        for( int i = 0; i < _mc; i++)
                        {
                            Debug.output(3,"value member name " +  
                                         value.member_name(i)  );

                            write_string( value.member_name(i) );
                            write_TypeCode( value.member_type(i), tcMap );
                            write_short( value.member_visibility(i) );
                        }
                        endEncapsulation();
                    }
                    break;
                case TCKind._tk_value_box: 
                    write_long( _kind  );
                    beginEncapsulation();
                    write_string(value.id());
                    write_string(value.name());
                    write_TypeCode( value.content_type(), tcMap);
                    endEncapsulation();
                    break;
                default: 
                    throw new RuntimeException("Cannot handle TypeCode, kind: " + _kind);
                }
            }
        }
        catch (org.omg.CORBA.TypeCodePackage.BadKind bk)
        { 
            bk.printStackTrace();
            throw new RuntimeException("org.omg.CORBA.TypeCodePackage.BadKind");
        }
        catch (org.omg.CORBA.TypeCodePackage.Bounds bds)
        { 
            throw new RuntimeException("org.omg.CORBA.TypeCodePackage.Bounds");
        }
        catch (java.io.IOException ioe)
        {
        }
    }

    public  final void write_ulong (int value)
    {
        write_long(value);
    }

    public  final void write_ulong_array(int[] value, int offset, int length)
    {
        write_long_array(value, offset, length);
    }

    public  final void write_ulonglong (long value)
    {        
        write_longlong(value);
    }

    public  final void write_ulonglong_array(long[] value, int offset, int length)
    {
        write_longlong_array(value, offset, length);
    }

    public  final void write_ushort(short value)
    {
        write_short(value);
    }

    public  final void write_ushort_array(short[] value, int offset, int length)
    {
        write_short_array(value, offset, length);
    }

    // old versions:

    //      public  final void write_wchar (char value)
    //      {
    //          write_short((short)value);
    //      }

    //      public  final void write_wchar_array(char[] value, int offset, int length)
    //      {
    //          write_char_array( value, offset, length);
    //      }

    //      public  final void write_wstring (String s)
    //      {
    //          if (s==null) 
    //              throw new org.omg.CORBA.MARSHAL("Null References");
    //          else 
    //          {
    //              int size = 2*(s.getBytes().length + 1);
    //              check(7 + size);

    //              int remainder = 4 - (index % 4);
    //              if (remainder != 4)
    //              {
    //                  index += remainder;
    //                  pos+=remainder;
    //              }

    //              _write4int(buffer,pos,size);
    //              pos += 4;
    //              index += 4;

    //              System.arraycopy( s.getBytes(), 0, buffer, pos, (size-2) );
    //              pos += size;
    //              buffer[pos-1]=0;
    //              index += size;
    //              //Connection.dumpBA(buffer);
    //          }
    //      }

    /** 
     *    called from Any 
     */

    public  final void write_value( org.omg.CORBA.TypeCode tc, 
                                    CDRInputStream in)
    {
        Hashtable tcMap = new Hashtable();
        write_value( tc, in, tcMap );
        tcMap.clear();
    }

    private final void write_value( org.omg.CORBA.TypeCode tc, 
                                    CDRInputStream in, 
                                    Hashtable tcMap)
    {
        Debug.assert( tc != null, "Illegal null pointer for TypeCode");
        int kind = ((TypeCode)tc)._kind();
 
        //int kind = tc.kind().value();
        switch (kind)
        {
            case TCKind._tk_null: 
            case TCKind._tk_void:
                break;
            case TCKind._tk_boolean:
                write_boolean( in.read_boolean());
                break;
            case TCKind._tk_char:
                write_char( in.read_char());
                break;
            case TCKind._tk_wchar:
                write_wchar( in.read_wchar());
                break;
            case TCKind._tk_octet:
                write_octet( in.read_octet());
                break;          
            case TCKind._tk_short:
                write_short( in.read_short());
                break;
            case TCKind._tk_ushort:
                write_ushort(in.read_ushort());
                break;
            case TCKind._tk_long:
            {
                write_long( in.read_long());
                break;
            }
            case TCKind._tk_ulong:
                write_ulong( in.read_ulong());
                break;
            case TCKind._tk_float:
                write_float( in.read_float());
                break;
            case TCKind._tk_double:
                write_double(in.read_double());
                break;
            case TCKind._tk_longlong:
                write_longlong(in.read_longlong());
                break;
            case TCKind._tk_ulonglong:
                write_ulonglong( in.read_ulonglong());
                break;
            case TCKind._tk_any:
                write_any( in.read_any());
                break;
            case TCKind._tk_TypeCode:
                write_TypeCode(in.read_TypeCode());
                break;
            case TCKind._tk_Principal:
                write_Principal( in.read_Principal());
                break;
            case TCKind._tk_objref: 
                write_Object( in.read_Object());
                break;
            case TCKind._tk_string: 
                write_string( in.read_string());
                break;
            case TCKind._tk_wstring: 
                write_wstring( in.read_wstring());
                break;
            case TCKind._tk_array: 
                try
                {
                    int length = tc.length();
                    int a_kind = ((TypeCode)tc.content_type())._kind();
                    if( a_kind == TCKind._tk_octet )
                    {
                        check( length );
                        in.read_octet_array( buffer, pos, length);
                        index+= length;
                        pos += length;
                    }
                    else
                    {
                        for( int i = 0; i < length; i++ )
                            write_value( tc.content_type(), in, tcMap);
                    }
                } 
                catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
                {} 
                break;
            case TCKind._tk_sequence: 
                try
                {
                    int len = in.read_long();
                    write_long(len);
                    int s_kind = ((TypeCode)tc.content_type())._kind();

                    org.omg.CORBA.TypeCode content_tc = tc.content_type();
                    for( int i = 0; i < len; i++ )
                        write_value(  content_tc, in, tcMap);
                    
                } 
                catch ( org.omg.CORBA.TypeCodePackage.BadKind b )
                {
                    b.printStackTrace();
                } 
                break;
            case TCKind._tk_except:
                write_string( in.read_string());
                // don't break, fall through to ...
            case TCKind._tk_struct: 
            {
                try
                {
                    tcMap.put( tc.id(), tc );
                    for( int i = 0; i < tc.member_count(); i++)
                        write_value( tc.member_type(i), in, tcMap);
                } 
                catch ( org.omg.CORBA.TypeCodePackage.BadKind b ){} 
                catch ( org.omg.CORBA.TypeCodePackage.Bounds b ){}
                break;
            }
            case TCKind._tk_enum:
            {
                write_long( in.read_long() );
                break;
            }
            case TCKind._tk_union:
            {
                try
                {       
                    tcMap.put( tc.id(), tc );
                    TypeCode disc = (TypeCode)tc.discriminator_type();
                    int def_idx = tc.default_index();
                    int member_idx = -1;

                    switch( disc._kind() )
                    {
                        case TCKind._tk_short:
                        {
                            short s = in.read_short();
                            write_short(s);
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
                        case TCKind._tk_ushort:
                        {
                            short s = in.read_ushort();
                            write_ushort(s);
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
                        case TCKind._tk_long:
                        {
                            int s = in.read_long();
                            write_long(s);
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
                        case TCKind._tk_ulong:
                        {
                            int s = in.read_ulong();
                            write_ulong(s);
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
                            long s = in.read_longlong();
                            write_longlong(s);
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
                            long s = in.read_ulonglong();
                            write_ulonglong(s);
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
                        case TCKind._tk_boolean:
                        {
                            boolean s = in.read_boolean();
                            write_boolean(s);
                            for(int i = 0 ; i < tc.member_count() ; i++)
                            {
                                if(i != def_idx)
                                {
                                    if(s == tc.member_label(i).extract_boolean())
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
                            int s = in.read_long();
                            write_long(s);
                            for( int i = 0 ; i < tc.member_count(); i++ )
                            {
                                if( i != def_idx)
                                {
                                    int label = 
                                        tc.member_label(i).create_input_stream().read_long();
                                /*  we  have to  use  the any's  input
                                   stream   because   enums  are   not
                                   inserted as longs */

                                    if( s == label)
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
                            char s = in.read_char();
                            write_char(s);
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
                        default:
                            throw new RuntimeException("Unfinished implementation for unions in anys, sorry.");
                    }

                    // write the member or default value, if any
                    // (if the union has no explicit default but the
                    // the case labels do not cover the range of 
                    // possible discriminator values, there may be
                    // several "implicit defaults" without associated
                    // union values.

                    if( member_idx != -1 )
                    {
                        write_value( tc.member_type( member_idx ), in, tcMap);
                    }
                    else if( def_idx != -1 )
                    {
                        write_value( tc.member_type( def_idx ), in, tcMap);
                    }
                } 
                catch ( org.omg.CORBA.TypeCodePackage.BadKind b ){} 
                catch ( org.omg.CORBA.TypeCodePackage.Bounds b ){}
                //            recursiveTCStack.pop();
                break;
            }
            case TCKind._tk_alias:
            {
                try
                {
                    tcMap.put( tc.id(), tc );
                    write_value( tc.content_type(), in, tcMap );


                } 
                catch ( org.omg.CORBA.TypeCodePackage.BadKind b ){} 
                break;
            }
            case 0xffffffff:
            {
                try
                {
                    org.omg.CORBA.TypeCode _tc = 
                        (org.omg.CORBA.TypeCode)tcMap.get(tc.id());
                    if( _tc == null )
                    {
                        throw new RuntimeException("Recursive TypeCode not found " 
                                                   + tc.id());
                    }
                    write_value( _tc , in, tcMap );
                } 
                catch ( org.omg.CORBA.TypeCodePackage.BadKind b ){
                    b.printStackTrace();
                } 
                break;
            }
            default:
                throw new RuntimeException("Cannot handle TypeCode with kind " + 
                                           kind);
        }
    }

    /**
     * Writes the serialized state of `value' to this stream.
     */

    public void write_value (java.io.Serializable value) 
    {
        if (!write_special_value (value))
            write_value_internal (value, 
                                  RepositoryID.repId (value.getClass()));
    }

    public void write_value (java.io.Serializable value,
                             org.omg.CORBA.portable.BoxedValueHelper factory)
    {
        if (!write_special_value (value))
        {
            valueMap.put (value, new Integer(pos));
            if (value instanceof org.omg.CORBA.portable.IDLEntity)
                write_long (0x7fffff00);
            else
            {
                // repository id is required for RMI: types
                write_long (0x7fffff02);
                write_repository_id (RepositoryID.repId (value.getClass()));
            }
            factory.write_value (this, value);
        }
    }

    public void write_value (java.io.Serializable value,
                             java.lang.Class clz) 
    {
        if (!write_special_value (value))
        {
            Class c = value.getClass();
	    String repId = RepositoryID.repId (c);
            if (c == clz && !repId.startsWith("RMI:"))
		// the repository id is required for "RMI:" valuetypes
                write_value_internal (value, null);
            else if (clz.isInstance (value))
                write_value_internal (value, repId);
            else
                throw new org.omg.CORBA.BAD_PARAM();
        }
    }

    public void write_value (java.io.Serializable value,
                             String repository_id)
    {
        if (!write_special_value (value))
            write_value_internal (value, repository_id);
    }

    /**
     * If `value' is null, or has already been written to this stream,
     * then this method writes that information to the stream and returns 
     * true, otherwise does nothing and returns false.
     */
    private boolean write_special_value (java.io.Serializable value) 
    {
        if (value == null)
        {
            // null tag
            write_long (0x00000000);
            return true;
        }
        else 
        {
            Integer index = (Integer)valueMap.get (value);
            if (index != null) 
            {
                // value has already been written -- make an indirection
                write_long (0xffffffff);
                write_long (index.intValue() - pos);
                return true;
            }
            else
                return false;
        }
    }

    /**
     * Writes `repository_id' to this stream, perhaps via indirection.
     */
    private void write_repository_id (String repository_id)
    {
        Integer index = (Integer)repIdMap.get (repository_id);
        if (index == null)
        {
            // a new repository id -- write it
            repIdMap.put (repository_id, new Integer(pos));
            write_string (repository_id);
        }
        else
        {
            // a previously written repository id -- make an indirection
            write_long (0xffffffff);
            write_long (index.intValue() - pos);
        }
    }

    /**
     * This method does the actual work of writing `value' to this 
     * stream.  If `repository_id' is non-null, then it is used as
     * the type information for `value' (possibly via indirection).
     * If `repository_id' is null, `value' is written without
     * type information.
     * Note: This method does not check for the special cases covered
     * by write_special_value().
     */
    private void write_value_internal (java.io.Serializable value,
                                       String repository_id) 
    {
        valueMap.put (value, new Integer(pos));
        if (repository_id != null) 
        {
            write_long (0x7fffff02);
            write_repository_id (repository_id);
        }
        else
            write_long (0x7fffff00);

        if (value.getClass() == String.class) 
            // special handling for strings required according to spec
	    write_wstring((String)value);
        else if (value instanceof org.omg.CORBA.portable.StreamableValue)
            ((org.omg.CORBA.portable.StreamableValue)value)._write (this);
        else
            ValueHandler.writeValue (this, value);

    }
}



