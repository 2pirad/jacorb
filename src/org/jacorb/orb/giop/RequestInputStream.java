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

/**
 * 
 * @author Gerald Brose, FU Berlin
 * @version $Id: RequestInputStream.java,v 1.8 2001-03-28 10:13:10 noffke Exp $
 * 
 */

public class RequestInputStream
    extends org.jacorb.orb.CDRInputStream
{
    public org.omg.GIOP.RequestHeader_1_0 req_hdr;
    public org.omg.GIOP.MessageHeader_1_0 msg_hdr=null;

    /**
     * used by subclass, flag is a dummy
     */

    protected RequestInputStream( org.omg.CORBA.ORB orb, 
                                  byte [] buf, 
                                  boolean flag )
    {
	super( orb, buf );
    }

    public RequestInputStream( org.omg.CORBA.ORB orb, byte [] buf )
    {
	super( orb,  buf);

	if( buffer[7] != (byte)org.omg.GIOP.MsgType_1_0._Request )
	    throw new RuntimeException("Error: not a request!");

	setLittleEndian( buffer[6] != 0);

	if (buffer[5]==1)
	{
	    skip(12);	    
	}
	else	    
	    msg_hdr= org.omg.GIOP.MessageHeader_1_0Helper.read(this);

	req_hdr = org.omg.GIOP.RequestHeader_1_0Helper.read(this);	   
    }
}






