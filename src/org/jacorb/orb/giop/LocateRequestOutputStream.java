package org.jacorb.orb.connection;

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
import org.omg.GIOP.*;
import org.jacorb.orb.*;

/**
 * @author Gerald Brose, FU Berlin 1999
 * @version $Id: LocateRequestOutputStream.java,v 1.6.4.1 2001-08-08 14:51:55 jacorb Exp $
 *
 */

public class LocateRequestOutputStream
    extends org.jacorb.orb.CDROutputStream
{
    private org.omg.GIOP.LocateRequestHeader_1_0 req_hdr;

    public LocateRequestOutputStream( byte[] object_key, int request_id )
    {
        req_hdr = new org.omg.GIOP.LocateRequestHeader_1_0( request_id, object_key);
        writeHeader();
    }

    private void writeHeader()
    {
        writeGIOPMsgHeader( (byte)org.omg.GIOP.MsgType_1_1._LocateRequest );
        org.omg.GIOP.LocateRequestHeader_1_0Helper.write( this, req_hdr );
        insertMsgSize();
    }

    public int requestId()
    {
	return req_hdr.request_id;
    }


}






