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
 * @author Gerald Brose, FU Berlin
 * @version $Id: LocateRequest.java,v 1.6.4.3 2001-08-15 09:04:57 jacorb Exp $
 */

import java.io.*;
import java.lang.*;
import org.jacorb.orb.*;

import org.omg.GIOP.*;

public class LocateRequest 
    extends org.jacorb.orb.dsi.ServerRequest 
{
    public LocateRequest( org.jacorb.orb.ORB orb,  
                          byte[] _buf, 
                          ServerConnection _connection )
    {
	super( orb, _connection );
	in = new LocateRequestInputStream( orb, _buf );
	oid = org.jacorb.poa.util.POAUtil.extractOID( in.req_hdr.target.object_key());
    }

    public java.lang.String operation()
    {
	return "_non_existent";
    }

    public int requestId()
    {
	return ((LocateRequestInputStream)in).locate_req_hdr.request_id;
    }

    public byte[] objectKey()
    {
	return ((LocateRequestInputStream)in).locate_req_hdr.target.object_key();
    }

    public void reply()
    {       
	try 
	{ 
	    if( out == null )
	    {
		out = 
                    new ReplyOutputStream( new org.omg.IOP.ServiceContext[0],
                                           requestId(), 
                                           ReplyStatusType_1_2.from_int(status),
                                           in.getGIOPMinor() );
	    }

	    /* DSI-based servers set results and user exceptions using anys, so 
	       we have to treat this differently */

	    if( !stream_based )
	    {
		if( status == ReplyStatusType_1_2._USER_EXCEPTION )
		{
		    out.write_string( ex.type().id() );
		    ex.write_value( out );
		}
		else if( status == ReplyStatusType_1_2._NO_EXCEPTION )
		{
		    result.write_value( out );
		}
	    }

	    /* these two exceptions are set in the same way for both stream-based and
	       DSI-based servers */
	    
	    if( status == ReplyStatusType_1_2._LOCATION_FORWARD )
	    {
		out.write_Object( location_forward.forward_reference );
	    }
	    else if( status == ReplyStatusType_1_2._SYSTEM_EXCEPTION )
	    {
		org.jacorb.orb.SystemExceptionHelper.write( out, sys_ex );
	    }
	    
	    /* everything is written to out by now, be it results or exceptions */
	    
	    out.close();
	    int reply_status;
	    if( status != ReplyStatusType_1_2._NO_EXCEPTION
		// || _non_existent()
		)
	    {
		reply_status = LocateStatusType_1_2._UNKNOWN_OBJECT;
	    }
	    else
	    {
		reply_status = LocateStatusType_1_2._OBJECT_HERE;
	    }

	    connection.sendLocateReply( requestId(), 
                                        reply_status , 
                                        null, 
                                        in.getGIOPMinor() );	    
	}
	catch ( Exception ioe )
	{
				// debug:
	    ioe.printStackTrace();
	    System.err.println("ServerRequest: Error replying to request!");
	}
    }
}







