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
 * @version $Id: ClientGIOPConnection.java,v 1.2 2003-04-24 10:07:56 andre.spiegel Exp $
 */

public class ClientGIOPConnection 
    extends GIOPConnection 
{
    public ClientGIOPConnection( Transport transport,
                                 RequestListener request_listener,
                                 ReplyListener reply_listener )
    {
        super( transport, request_listener, reply_listener );
    }
    

    
    public void readTimedOut()
    {
        synchronized( pendingUndecidedSync )
        {
            if( ! hasPendingMessages() )
            {
                closeAllowReopen();
            }
        }
    }

    /**
     * We're server side and can't reopen, therefore close completely
     * if stream closed.  
     */
    public void streamClosed()
    {
        /**
         * We're server side and can't reopen, therefore close completely
         * if stream closed.  
         */
        closeAllowReopen();

        super.streamClosed();
    }

    public void closeAllowReopen()
    {
        getWriteLock();

        try
        {
            transport.close();
            transport = new Client_TCP_IP_Transport ((Client_TCP_IP_Transport)transport);            
        }
        catch( IOException e )
        {
            //Debug.output( 1, e );
        }
        finally
        {
            releaseWriteLock();
        }        
    }

}// ClientGIOPConnection



