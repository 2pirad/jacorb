/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
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

import java.io.*;

import org.jacorb.util.*;

/**
 * @author Nicolas Noffke
 * @version $Id: ServerGIOPConnection.java,v 1.1 2003-01-07 18:06:59 nicolas Exp $
 */

public class ServerGIOPConnection 
    extends GIOPConnection 
{
    private static final byte[] CLOSE_CONNECTION_MESSAGE =
    new byte[] { 
        'G', 'I', 'O', 'P', //magic start
        1, //GIOP major
        0, //GIOP minor
        0, //endianess, big-endian
        5, //message type, CloseConnection message
        0, 0, 0, 0 // message size, 0 because CloseConnection has no body
    };


    private GIOPConnectionManager manager = null;

    public ServerGIOPConnection( Transport transport,
                                 RequestListener request_listener,
                                 ReplyListener reply_listener,
                                 GIOPConnectionManager manager )
    {
        super( transport, request_listener, reply_listener );
        
        this.manager = manager;
    }
    

    public boolean tryClose()
    {
        if( tryDiscard() )
        {
            close();
            
            return true;
        }
        else
        {
            return false;
        }
    }

    public void close()
    {
        try
        {
            getWriteLock();

            transport.write( CLOSE_CONNECTION_MESSAGE,
                             0,
                             CLOSE_CONNECTION_MESSAGE.length );
            transport.flush();
            //transport.close();
        }
        catch( IOException e )
        {
            Debug.output( 1, e );
        }

        releaseWriteLock();        
        manager.unregisterServerGIOPConnection( this );
    }
}// ServerGIOPConnection



