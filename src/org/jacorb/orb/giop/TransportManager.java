package org.jacorb.orb.connection;

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

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.Constructor;

import org.jacorb.orb.*;
import org.jacorb.orb.factory.*;
import org.jacorb.util.*;

/**
 * This class manages Transports. On the one hand it creates them, and
 * on the other it enforces an upper limit on the open transports.
 *
 * @author Nicolas Noffke
 * @version $Id: TransportManager.java,v 1.7 2003-04-14 15:28:01 andre.spiegel Exp $
 * */

public class TransportManager
{    
    public static final String FACTORY_PROP = "jacorb.net.socket_factory";

    private SocketFactory socket_factory = null;
    private SocketFactory ssl_socket_factory = null;

    private Class statistics_provider_class = null;

    public TransportManager( ORB orb )
    {
        socket_factory = SocketFactoryManager.getSocketFactory (orb);

        if( Environment.isPropertyOn( "jacorb.security.support_ssl" ))
        {
            String s = Environment.getProperty( "jacorb.ssl.socket_factory" );
            if( s == null || s.length() == 0 )
            {
                throw new RuntimeException( "SSL support is on, but the property \"jacorb.ssl.socket_factory\" is not set!" );
            }
            
            try
            {
                Class ssl = Class.forName( s );
                
                Constructor constr = ssl.getConstructor( new Class[]{
                    ORB.class });
                
                ssl_socket_factory = (SocketFactory)
                    constr.newInstance( new Object[]{ orb });
            }
            catch (Exception e)
            {
                Debug.output( Debug.IMPORTANT | Debug.ORB_CONNECT,
                              e );
                
                throw new RuntimeException( "SSL support is on, but the ssl socket factory can't be instanciated (see trace)!" );
            }
        }

        if( Environment.hasProperty( 
            "jacorb.connection.statistics_provider_class" ))
        {
            String s = Environment.getProperty( 
                "jacorb.connection.statistics_provider_class" );

            if( s != null && s.length() > 0 )
            {
                try
                {
                    statistics_provider_class =
                        Class.forName( s );
                }
                catch( Exception e )
                {
                    Debug.output( 1, "ERROR: Unable to create class from property >jacorb.connection.statistics_provider_class<: " + e );
                               
                }
            }
        }
    }

    public Transport createClientTransport( InternetIOPProfile target_profile,
                                            boolean use_ssl )
    {
        Transport transport =
            new Client_TCP_IP_Transport( target_profile,
                                         use_ssl,
                                         use_ssl ? ssl_socket_factory
                                                 : socket_factory,
                                         null,
                                         this );

        return transport;
    }

    public Transport createServerTransport( Socket socket,
                                            boolean is_ssl )
        throws IOException
    {
        //create a new statistics provider for each new Transport
        StatisticsProvider provider = null;
        if( statistics_provider_class != null )
        {
            try
            {
                provider = (StatisticsProvider) 
                    statistics_provider_class.newInstance();
            }
            catch( Exception e )
            {
                Debug.output( 1, "ERROR: Unable to create instance from Class >" +
                              statistics_provider_class + '<');
                
            }
        }

        Transport transport = 
            new Server_TCP_IP_Transport( socket, 
                                         is_ssl,
                                         provider,
                                         this );

        return transport;
    }
}



