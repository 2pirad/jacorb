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

package org.jacorb.orb.connection;

import org.jacorb.orb.*;
import org.omg.GIOP.*;

/**
 * 
 * @author Gerald Brose, FU Berlin
 * @version $Id: RequestInputStream.java,v 1.8.4.4 2001-08-22 07:22:17 jacorb Exp $
 * 
 */

public class RequestInputStream
    extends GIOPInputStream
{
    private static byte[] reserved = new byte[3];

    public RequestHeader_1_2 req_hdr = null;

    /**
     * used by subclass, flag is a dummy
     */
    /*
    protected RequestInputStream( org.omg.CORBA.ORB orb, 
                                  byte [] buf, 
                                  boolean flag )
    {
	super( orb, buf );
    }
    */
    public RequestInputStream( org.omg.CORBA.ORB orb, byte[] buf )
    {
	super( orb,  buf );

        //check message type
	if( Messages.getMsgType( buffer ) != MsgType_1_1._Request )
        {
	    throw new Error( "Error: not a request!" );
        }

        switch( giop_minor )
        { 
            case 0 : 
            {
                //GIOP 1.0
                RequestHeader_1_0 hdr = 
                    RequestHeader_1_0Helper.read( this );

                TargetAddress addr = new TargetAddress();
                addr.object_key( hdr.object_key );

                req_hdr = 
                    new RequestHeader_1_2( hdr.request_id,
                                           (byte) ((hdr.response_expected)? 0x03 : 0x00),//flags
                                           reserved,
                                           addr, //target
                                           hdr.operation, 
                                           hdr.service_context );
                break;
            }
            case 1 : 
            {
                //GIOP 1.1
                RequestHeader_1_1 hdr = 
                    RequestHeader_1_1Helper.read( this );

                TargetAddress addr = new TargetAddress();
                addr.object_key( hdr.object_key );

                req_hdr = 
                    new RequestHeader_1_2( hdr.request_id,
                                           Messages.responseFlags( hdr.response_expected ),
                                           reserved,
                                           addr, //target
                                           hdr.operation, 
                                           hdr.service_context );
                break;
            }
            case 2 : 
            {
                //GIOP 1.2
                req_hdr = RequestHeader_1_2Helper.read( this );
                
                skipHeaderPadding();

                break;
            }
            default : {
                throw new Error( "Unknown GIOP minor version: " + giop_minor );
            }
        }
        
        System.out.println(">>>>>>>>>received request for op " + 
                           req_hdr.operation + 
                           " with GIOP 1." + 
                           giop_minor);
        
    }

    public int getGIOPMinor()
    {
        return giop_minor;
    }
    
    //needed for Appligator
    /*
    public int getMsgSize()
    {
        return msg_size;
    }
    */

    public void finalize()
    {
	try
	{
	    close();
	}
	catch( java.io.IOException iox )
	{
	    //ignore
	}
    }
}






