package org.jacorb.security.ssl.sun_jsse;

/*
 *       JacORB - a free Java ORB
 *
 *   Copyright (C) 2000-2004 Gerald Brose
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

import org.jacorb.security.level2.*;

import java.net.*;
import java.io.*;
import java.security.*;
import java.util.*;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.configuration.*;

import javax.net.ssl.*;
import javax.net.*;


/**
 * @author Nicolas Noffke
 * $Id: SSLSocketFactory.java,v 1.13 2004-05-06 09:43:19 nicolas Exp $
 */

public class SSLSocketFactory 
    implements org.jacorb.orb.factory.SocketFactory, Configurable
{    
    private SocketFactory factory = null;
    private String[] cipher_suites = null;

    private boolean trusteesFromKS = false;
    private short clientSupportedOptions = 0;
    private String keystore_location = null;
    private String keystore_passphrase = null;
    private Logger logger;

    
    public SSLSocketFactory( org.jacorb.orb.ORB orb ) 
        throws ConfigurationException
    {
        configure( orb.getConfiguration());
    }


    public void configure(Configuration configuration)
        throws ConfigurationException
    {
        logger = 
            ((org.jacorb.config.Configuration)configuration).getNamedLogger("jacorb.security.jsse");

        trusteesFromKS = 
            configuration.getAttributeAsBoolean("jacorb.security.jsse.trustees_from_ks",false);

        keystore_location = 
            configuration.getAttribute("jacorb.security.keystore","UNSET");

        keystore_passphrase = 
            configuration.getAttribute("jacorb.security.keystore_password","UNSET" );

        clientSupportedOptions = 
            Short.parseShort(
                configuration.getAttribute("jacorb.security.ssl.client.supported_options","0"),
                16);

        try
        {
            factory = createSocketFactory();
        }
        catch( Exception e )
        {
            if (logger.isWarnEnabled())
                logger.warn("Exception", e );
        }

        if( factory == null )
        {
            if (logger.isErrorEnabled())
                logger.error("Unable to create SSLSocketFactory!" );
            throw new ConfigurationException("Unable to create ServerSocketFactory!");
        }
	
        // Andrew T. Finnell / Change made for e-Security Inc. 2002
        // We need to obtain all the cipher suites to use from the 
        // properties file.
        String cipher_suite_list = 
            configuration.getAttribute("jacorb.security.ssl.server.cipher_suites", null );
	
        if ( cipher_suite_list != null )
        {
            StringTokenizer tokenizer =
                new StringTokenizer( cipher_suite_list, "," );
        
            // Get the number of ciphers in the list
            int tokens = tokenizer.countTokens();
            
            if ( tokens > 0 )
            {
                // Create an array of strings to store the ciphers
                cipher_suites = new String[tokens];
                
                // This will fill the array in reverse order but that doesn't
                // matter
                while( tokenizer.hasMoreElements() )
                {
                    cipher_suites[--tokens] = tokenizer.nextToken();
                }
            }
        }
    }

    public Socket createSocket( String host, 
                                int port )
        throws IOException, UnknownHostException
    {       
        SSLSocket s = (SSLSocket)factory.createSocket( host, port );
	
        // Andrew T. Finnell
        // We need a way to enable the cipher suites that we would like to use
        // We should obtain these from the properties file
        if( cipher_suites != null )
        {
            s.setEnabledCipherSuites( cipher_suites );
        }

        return s;
    }

    public boolean isSSL ( java.net.Socket s )
    { 
        return ( s instanceof SSLSocket); 
    }

    private SocketFactory createSocketFactory() 
        throws IOException, java.security.GeneralSecurityException
    {
        Security.addProvider( new com.sun.net.ssl.internal.ssl.Provider() );

        KeyManagerFactory kmf = null;
        KeyStore key_store = null;

        if( trusteesFromKS || ( clientSupportedOptions& 0x40) != 0 )
        {            
            key_store = 
                KeyStoreUtil.getKeyStore( keystore_location,
                                          keystore_passphrase.toCharArray() );
            //only add own credentials, if establish trust in
            //client is supported
            if( ( clientSupportedOptions & 0x40) != 0 ) 
            {        
                kmf = KeyManagerFactory.getInstance( "SunX509" );
                kmf.init( key_store, keystore_passphrase.toCharArray() );
            }
        }
        
        TrustManagerFactory tmf = 
            TrustManagerFactory.getInstance( "SunX509" );
        
        if( key_store != null && trusteesFromKS )
        {
            //take trusted certificates from keystore
            if (logger.isInfoEnabled())
                logger.info("Loading certs from keystore " + key_store );
            tmf.init( key_store );
        }
        else
        {
            tmf.init( (KeyStore) null );
        }
        
        SSLContext ctx = SSLContext.getInstance( "TLS" );
        ctx.init( (kmf == null)? null : kmf.getKeyManagers(), 
                  tmf.getTrustManagers(), 
                  null );
        
        return ctx.getSocketFactory();
    }
}


