package org.jacorb.security.ssl.sun_jsse;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 2000-2003 Nicolas Noffke, Gerald Brose.
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
import java.security.cert.*;
import org.omg.SecurityReplaceable.*;
import org.omg.Security.*;
import org.omg.SecurityLevel2.ReceivedCredentials;

import org.omg.PortableInterceptor.*;
import org.omg.CORBA.ORBPackage.*;
import org.omg.CORBA.Any;

import org.jacorb.orb.portableInterceptor.ServerRequestInfoImpl;
import org.jacorb.security.level2.*;
import org.jacorb.orb.dsi.ServerRequest;
import org.jacorb.orb.iiop.*;
import org.jacorb.orb.giop.*;

import org.apache.avalon.framework.logger.Logger;

import javax.net.ssl.SSLSocket;

/**
 *
 * 
 * @author Nicolas Noffke
 * $Id: ServerInvocationInterceptor.java,v 1.8.4.1 2004-03-25 15:55:08 gerald Exp $
 */

public class ServerInvocationInterceptor
    extends org.omg.CORBA.LocalObject 
    implements ServerRequestInterceptor
{
    public static final String DEFAULT_NAME = "ServerInvocationInterceptor";

    private String name = null;

    private org.jacorb.security.level2.CurrentImpl current = null;
    private SecAttributeManager attrib_mgr = null;
    private AttributeType type = null; 
    private Logger logger;


    public ServerInvocationInterceptor(org.omg.SecurityLevel2.Current current)
    {
        this( current, DEFAULT_NAME );
    }

    public ServerInvocationInterceptor( org.omg.SecurityLevel2.Current current,
                                        String name )
    {
        this.current = (CurrentImpl) current;
        this.name = name;

        attrib_mgr = SecAttributeManager.getInstance();

        type = new AttributeType
            ( new ExtensibleFamily( (short) 0,
                                    (short) 1 ),
              AccessId.value );  
        logger = ((org.jacorb.security.level2.CurrentImpl)current).getLogger();
    }

    public String name()
    {
        return name;
    }

    public void destroy()
    {
    } 

    public void receive_request( ServerRequestInfo ri )
        throws ForwardRequest
    {
    }


    public void receive_request_service_contexts( ServerRequestInfo ri )
        throws ForwardRequest
    {
        ServerRequest request = ((ServerRequestInfoImpl) ri).request;       
        GIOPConnection connection = request.getConnection();
        
        // lookup for context
        if (connection == null)
        {
            if (logger.isErrorEnabled())
                logger.error("target has no connection!");
            return;
        }
        
        if( !connection.isSSL() )
        {
            return;
        }
            
        ServerIIOPConnection transport =
            (ServerIIOPConnection) connection.getTransport();
        
        SSLSocket sslSocket = (SSLSocket) transport.getSocket();

        KeyAndCert kac = null;
        
        try
        {
            kac = 
                new KeyAndCert( null, sslSocket.getSession().getPeerCertificates() );
        }
        catch( javax.net.ssl.SSLPeerUnverifiedException pue )
        {
            if (logger.isWarnEnabled())
                logger.warn("Exception " + pue.getMessage() + " in ServerInvocationInterceptor");
            return;
        }

        if( kac.chain == null )
        {
            if (logger.isInfoEnabled())
                logger.info("Client sent no certificate chain!" );
            
            return;
        }
                
        SecAttribute [] atts = new SecAttribute[] {
            attrib_mgr.createAttribute( kac, type ) } ;
        
        current.set_received_credentials( new ReceivedCredentialsImpl( atts ) );
    }

    public void send_reply( ServerRequestInfo ri )
    {
        removeAttribute();
        current.remove_received_credentials();
    }

    public void send_exception( ServerRequestInfo ri )
        throws ForwardRequest
    {
        removeAttribute();
        current.remove_received_credentials();
    }

    public void send_other( ServerRequestInfo ri )
        throws ForwardRequest
    {
        removeAttribute();
        current.remove_received_credentials();
    }

    private void removeAttribute()
    {
        ReceivedCredentials creds = current.received_credentials();

        if (creds == null)
        {
            return;
        }

        SecAttribute[] attributes = creds.get_attributes(
            new AttributeType[]{ type } );

        if (attributes.length != 0)
        {
            attrib_mgr.removeAttribute(attributes[0]);
        }
    }
}
