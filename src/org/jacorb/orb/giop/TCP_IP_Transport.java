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

import java.net.*;
import java.io.*;

import org.jacorb.util.*;

/**
 * TCP_IP_Transport.java
 *
 *
 * Created: Sun Aug 12 20:18:47 2002
 *
 * @author Nicolas Noffke
 * @version $Id: TCP_IP_Transport.java,v 1.17 2003-04-22 09:56:04 andre.spiegel Exp $
 */

public abstract class TCP_IP_Transport
    implements Transport
{
    protected InputStream in_stream = null;
    protected OutputStream out_stream = null;

    private ByteArrayOutputStream b_out = null;
    private boolean dump_incoming = false;

    String connection_info;
    Socket socket;

    //the statistics provider, may stay null
    private StatisticsProvider statistics_provider = null;

    //used to unregister this transport
    protected TransportManager transport_manager = null;

    private TransportListener transport_listener = null;

    private int finalTimeout = 20000;

    public TCP_IP_Transport( StatisticsProvider statistics_provider,
                             TransportManager transport_manager )
    {
        this.statistics_provider = statistics_provider;
        this.transport_manager = transport_manager;

        String dump_outgoing =
            Environment.getProperty( "jacorb.debug.dump_outgoing_messages",
                                     "off" );

        if( "on".equals( dump_outgoing ))
        {
            b_out = new ByteArrayOutputStream();
        }

        finalTimeout = 
            Environment.getIntPropertyWithDefault( "jacorb.connection.timeout_after_closeconnection", 
                                                   20000 );
    }

    /**
     * Open up a new connection (if not already done). This is always
     * called prior to sending a message.
     */
    protected abstract void connect();

    /**
     * Wait until the connection is established. This is called from
     * getMessage() so the connection may be opened up not until the
     * first message is sent (instead of opening it up when the
     * transport is created).
     *
     * @return true if connection ready, false if connection closed.
     */
    protected abstract boolean waitUntilConnected();

    /**
     * This method tries to read in <tt>length</tt> bytes from
     * <tt>in_stream</tt> and places them into <tt>buffer</tt>
     * beginning at <tt>start_pos</tt>. It doesn't care about the
     * contents of the bytes.
     *
     * @return the actual number of bytes that were read.
     */

    public final int readToBuffer( byte[] buffer,
                                    int start_pos,
                                    int length )
        throws IOException
    {
        int read = 0;

        while( read < length )
        {
            int n = 0;

            try
            {
                n = in_stream.read( buffer,
                                    start_pos + read,
                                    length - read );
            }
            catch( InterruptedIOException e )
            {
                if (socket.getSoTimeout () != 0)
                {
                    Debug.output
                        (
                         2,
                         "Socket timed out with timeout period of " +
                         socket.getSoTimeout ()
                         );

                    transport_listener.readTimedOut();
                    return -1;
                }
                else
                {
                    throw e;
                }
            }
            catch( SocketException se )
            {
                Debug.output( 2, "Transport to " + connection_info +
                              ": stream closed" );

                transport_listener.streamClosed();
                return -1;
            }

            if( n < 0 )
            {
                Debug.output( 2, "Transport to " + connection_info +
                              ": stream closed" );

                transport_listener.streamClosed();
                return -1;
            }

            read += n;
        }

        return read;
    }

    // implementation of org.jacorb.orb.connection.Transport interface


    public void write( byte[] message,
                       int start,
                       int size )
        throws IOException
    {
        connect();
        
        out_stream.write( message, start, size );

        if( b_out != null )
        {
            b_out.write( message, start, size );
        }

        if( statistics_provider != null )
        {
            statistics_provider.messageChunkSent( size );
        }
    }


    public void flush()
        throws IOException
    {
        if( b_out != null )
        {
            byte[] b = b_out.toByteArray();

            Debug.output( 1, "sendMessages()", b );

            b_out.reset();
        }

        out_stream.flush();

        if( statistics_provider != null )
        {
            statistics_provider.flushed();
        }
    }

    /**
     * Get the statistics provider for transport usage statistics.
     */
    public StatisticsProvider getStatisticsProvider()
    {
        return statistics_provider;
    }

    public void setTransportListener( TransportListener transport_listener )
    {
       this.transport_listener = transport_listener;
    }

    /**
     * This is used to tell the transport that a CloseConnection has
     * been sent, and that it should set a timeout in case the client
     * doesn't close its side of the connection right away.
     *
     * This should only be called on the thread that listens on the
     * socket because timeouts are not applied until read() is called
     * the next time.  
     */
    public void turnOnFinalTimeout()
    {
        if( socket != null )
        {
            try
            {
                socket.setSoTimeout( finalTimeout );
            }
            catch( SocketException se )
            {
                Debug.output( 2, se );
            }
        }
    }

}
// TCP_IP_Transport

