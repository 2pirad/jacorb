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

package org.jacorb.orb.giop;

import java.io.*;

import org.jacorb.orb.iiop.*;
import org.apache.avalon.framework.configuration.*;

/**
 * @author Nicolas Noffke
 * @version $Id: ClientGIOPConnection.java,v 1.11.2.1 2004-03-24 19:05:38 gerald Exp $
 */

public class ClientGIOPConnection
    extends GIOPConnection
{
    private boolean ignore_pending_messages_on_timeout = false;

    public ClientGIOPConnection( org.omg.ETF.Profile profile,
                                 org.omg.ETF.Connection transport,
                                 RequestListener request_listener,
                                 ReplyListener reply_listener,
                                 StatisticsProvider statistics_provider )
    {
        super( profile, transport, request_listener, reply_listener, statistics_provider );
    }


    public void configure(Configuration configuration)
        throws ConfigurationException
    {
        super.configure(configuration);

        ignore_pending_messages_on_timeout =
            configuration.getAttribute("jacorb.connection.client.timeout_ignores_pending_messages","off").equals("on");
    }


    protected void readTimedOut()
    {
        synchronized( pendingUndecidedSync )
        {
            if (ignore_pending_messages_on_timeout)
            {
                this.streamClosed();
            }
            else if (! hasPendingMessages())
            {
                closeAllowReopen();
            }
        }
    }

    protected void streamClosed()
    {
        closeAllowReopen();

        if( connection_listener != null )
        {
            connection_listener.streamClosed();
        }
    }

    public void closeAllowReopen()
    {
        try
        {
            synchronized (connect_sync)
            {
                getWriteLock();
                transport.close();
                // We expect that the same transport can be reconnected
                // after a close, something that the ETF draft isn't
                // particularly clear about.
            }
        }
        finally
        {
            releaseWriteLock();
        }
    }

}// ClientGIOPConnection
