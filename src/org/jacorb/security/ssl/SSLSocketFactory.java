
/*
 *        Written for JacORB - a free Java ORB
 *
 *   Copyright (C) 2000 Gerald Brose
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

package org.jacorb.security.ssl;

/**
 * @author Andr'e Benvenuti, Gerald Brose.
 * @version $Id: SSLSocketFactory.java,v 1.10 2001-07-29 17:22:57 jacorb Exp $
 * 
 * We follow the design of socket factories in package javax.net 
 * and javax.net.ssl.* Because this package doesn't exist in the JDK yet we 
 * don't extend its classes, but we are fully compatible.
 *
 * The basic idea is to provide one extra constructor that sets up
 * a default SSL configuration that is implicitly used by all other 
 * constructors. The default SSL credentials can also be changed by
 * calling setDefaultSSLContext().
 * 
 * After creating a new SSL socket, this factory swaps roles for
 * the SSL handshake, i.e. any client socket created takes the
 * server role in the handshake, so the actual server need not
 * authenticate.
 */

import org.jacorb.security.level2.*;
import org.jacorb.security.util.*;
import org.jacorb.util.*;
import iaik.security.ssl.*;

public class SSLSocketFactory 
    implements org.jacorb.orb.factory.SocketFactory 
{
    private String[] default_cs = null;
    
    private boolean isRoleChange; // rt

    private CurrentImpl securityCurrent = null;

    private org.jacorb.orb.ORB orb = null;

    private SSLContext default_context = null;

    public SSLSocketFactory( org.jacorb.orb.ORB orb ) 
    {
        this.orb = orb;

	isRoleChange = Environment.changeSSLRoles();

	CipherSuite[] cs = SSLSetup.getCipherSuites();
        
	default_cs = new String[ cs.length ];
	for ( int i = 0; i < cs.length; i++ )
        {
	    default_cs[ i ] = cs[ i ].toString();
        }
    }


    /** 
     * @returns a socket connected to a ServerSocket on the named host, 
     * at the given port. This socket is configured using the socket 
     * options established for this factory. This constructor also sets
     * the default SSL context data to be used in other socket creation
     * operations.
     * 
     * Parameters:
     * 	host - the server host
     * 	port - the server port
     *  chain - a chain of X509 certificates to be used in SSL 
     *          connection setup
     *  key - the private key for the first cert in the chain.
     *
     * @throws:
     * 	java.io.IOException - if the connection can't be established
     * 	java.net.UnknownHostException - if the host is not known
     */

    public java.net.Socket createSocket( String host, 
                                         int port )
	throws java.io.IOException, java.net.UnknownHostException
    {       
        SSLSocket sock = new SSLSocket( host, port, getDefaultContext() );

        // rt: switch to server mode
        if( isRoleChange ) 
        {
            Debug.output(1, "SSLSocket switch to server mode...");
	    sock.setUseClientMode( false );
	}
		
        return sock;
    }

    private org.jacorb.security.level2.KeyAndCert[] getSSLCredentials()
    {
        CurrentImpl  securityCurrent = null;        

        try
        {
            securityCurrent = (CurrentImpl)
                orb.resolve_initial_references("SecurityCurrent");
        }
        catch ( org.omg.CORBA.ORBPackage.InvalidName in )
        {
            Debug.output( 1, "Unable to obtain Security Current. Giving up" );
            
            System.exit( -1 );
        }

        return securityCurrent.getSSLCredentials();
    }        

    private SSLContext getDefaultContext()
    {
        
        if( default_context != null )
        {
            return default_context;
        }

	if( isRoleChange ) 
        {
	    SSLServerContext ctx = new SSLServerContext();

            //the server always has to have certificates
            org.jacorb.security.level2.KeyAndCert[] kac = 
                getSSLCredentials();
            
            for( int i = 0; i < kac.length; i++ )
            {
		ctx.addServerCredentials( kac[i].chain,  
                                          kac[i].key );
	    }
            
            if( (Environment.requiredBySSL() & 0x20) != 0 )
            {
                //required: establish trust in target (the SSL client
                //in this case)--> force other side to authenticate
                ctx.setRequestClientCertificate( true );

		String[] trusteeFileNames = 
		    Environment.getPropertyValueList( "jacorb.security.trustees" );

		if( trusteeFileNames.length == 0 )
		{
		    Debug.output( 1, "WARNING: No trusted certificates specified. This will accept all peer certificate chains!" );
		}
		
		for( int i = 0; i < trusteeFileNames.length; i++ )
		{
		    ctx.addTrustedCertificate( CertUtils.readCertificate( trusteeFileNames[i] ));
		}
	    }

            default_context = ctx;
	}
	else 
        {
	    SSLClientContext ctx = new SSLClientContext();

            //only add own credentials, if establish trust in client
            //is supported
            if(( (byte) Environment.supportedBySSL() & 0x40) != 0 ) 
            {            
                org.jacorb.security.level2.KeyAndCert[] kac = 
                    getSSLCredentials();

                for( int i = 0; i < kac.length; i++ )
                {
                    ctx .addClientCredentials( kac[i].chain,  
                                               kac[i].key );
                }
            }

	    //always adding trusted certificates, since in SSL, the
	    //server must always authenticate
            String[] trusteeFileNames = 
                Environment.getPropertyValueList( "jacorb.security.trustees" );
	    
            if( trusteeFileNames.length == 0 )
            {
                Debug.output( 1, "WARNING: No trusted certificates specified. This will accept all peer certificate chains!" );
            }
	    
            for( int i = 0; i < trusteeFileNames.length; i++ )
            {
                ctx.addTrustedCertificate( CertUtils.readCertificate( trusteeFileNames[i] ));
            }        

            default_context = ctx;
	}


	if( "on".equals( Environment.getProperty( "jacorb.security.iaik_debug", "off" )))
	    default_context.setDebugStream( System.out );

        return default_context;
    }
    
    /**
     * Returns the list of cipher suites which are enabled by 
     * default. Unless a different list is enabled, handshaking 
     * on an SSL connection will use one of these cipher suites. 
     * The minimum quality of service for these defaults requires 
     * confidentiality protection and server authentication.
     *
     * @returns:
     *	array of the cipher suites enabled by default
     */

    public String[] getDefaultCipherSuites()
    {
        return default_cs;
    }

    /**
     * Returns the names of the cipher suites which could be enabled 
     * for use on an SSL connection.
     * Normally, only a subset of these will actually be enabled by 
     * default, since this list may include
     * cipher suites which do not meet quality of service 
     * requirements for those defaults.
     * Such cipher suites are useful in specialized applications.
     *
     * @returns:
     * 	an array of cipher suite names
     */
    public String[] getSupportedCipherSuites()
    {
	CipherSuite [] suites = CipherSuite.getDefault();
	java.lang.String lst [] = new String[ suites.length ];
	for ( int i = 0; i < lst.length; i++ )
	    lst [ i ] = suites[ i ].toString ();
	return lst;
    }

    public boolean isSSL ( java.net.Socket s )
    { 
        return ( s instanceof SSLSocket); 
    }
}

