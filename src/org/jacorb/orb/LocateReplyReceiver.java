package org.jacorb.orb;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2002  Gerald Brose.
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

import org.jacorb.orb.*;
import org.jacorb.orb.connection.MessageInputStream;
import org.jacorb.orb.connection.LocateReplyInputStream;
import org.jacorb.util.*;

import org.omg.GIOP.*;
import org.omg.CORBA.portable.RemarshalException;

/**
 * A special ReplyPlaceholder that receives LocateReplies.
 *
 * @author Andre Spiegel <spiegel@gnu.org>
 * @version $Id: LocateReplyReceiver.java,v 1.2 2002-11-04 18:05:22 andre.spiegel Exp $
 */
public class LocateReplyReceiver 
    extends org.jacorb.orb.connection.ReplyPlaceholder
{
    public LocateReplyReceiver()
    {        
        super();
    }

    /**
     * This method blocks until a reply becomes available.
     */    
    public synchronized LocateReplyInputStream getReply() 
	throws RemarshalException
    {
        return (LocateReplyInputStream)super.getInputStream();
    }
}














