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

/**
 * 
 * @author Gerald Brose, FU Berlin
 * @version $Id: LocateRequestInputStream.java,v 1.7.4.1 2001-08-08 14:51:55 jacorb Exp $
 *
 * Hack for locate requests: turn a locate request into
 * a _non_existent() request and actually ping the object. This appears
 * to be necessary because we have to get around potentially holding
 * POAs - which can only be done using proper requests in our design.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import org.omg.PortableServer.POA;

public class LocateRequestInputStream
    extends RequestInputStream
{
    public org.omg.GIOP.LocateRequestHeader_1_0 locate_req_hdr;

    public LocateRequestInputStream( org.omg.CORBA.ORB orb, byte [] buf )
    {
	super( orb, buf, true );
	if( buffer[7] != (byte)org.omg.GIOP.MsgType_1_1._LocateRequest )
	    throw new RuntimeException("Error: not a locate request!");
	setLittleEndian( buffer[6]!=0);

	skip(12);
	locate_req_hdr = 
            org.omg.GIOP.LocateRequestHeader_1_0Helper.read(this);	   
        
        org.omg.GIOP.TargetAddress addr = new org.omg.GIOP.TargetAddress();
        addr.object_key( locate_req_hdr.object_key );
        
	req_hdr = 
            new org.omg.GIOP.RequestHeader_1_2( locate_req_hdr.request_id, 
                                                (byte) 0x03, //response_expected 
                                                new byte[3],
                                                addr, 
                                                "_non_existent", 
                                                null ); 
    }
}






