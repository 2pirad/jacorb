package org.jacorb.security.jsse;


/**
 * @author Nicolas Noffke
 * $Id: SSLServerSocketFactory.java,v 1.4 2001-07-16 08:23:25 noffke Exp $
 */
import org.jacorb.util.*;
import org.jacorb.security.util.*;
import org.jacorb.security.level2.*;

import com.sun.net.ssl.*;

import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import javax.net.*;
import java.security.*;

public class SSLServerSocketFactory 
    implements org.jacorb.orb.factory.SSLServerSocketFactory
{
    private ServerSocketFactory factory = null;
    private boolean mutual_auth = false;

    public SSLServerSocketFactory(org.jacorb.orb.ORB orb)
    {
	factory = createServerSocketFactory();

	if( factory == null )
	{
	    Debug.output( 1, "ERROR: Unable to create ServerSocketFactory!" );
	}

	if( (Environment.requiredBySSL() & 0x20) != 0 )
	{
	    //required: establish trust in target
	    //--> force other side to authenticate
	    mutual_auth = true;
	}

    }
           
    public ServerSocket createServerSocket( int port )
        throws IOException
    {
	SSLServerSocket s = (SSLServerSocket) 
	    factory.createServerSocket( port );
	s.setNeedClientAuth( mutual_auth );

	return s;
    }


    public ServerSocket createServerSocket( int port, int backlog ) 
        throws IOException
    {
	SSLServerSocket s = (SSLServerSocket) 
	    factory.createServerSocket( port, backlog );

	s.setNeedClientAuth( mutual_auth );

	return s;
    }

    public ServerSocket createServerSocket (int port,
                                            int backlog,
                                            InetAddress ifAddress)
        throws IOException    
    {
	SSLServerSocket s = (SSLServerSocket) 
	    factory.createServerSocket( port, backlog, ifAddress );
	s.setNeedClientAuth( mutual_auth );

	return s;
    }

    public boolean isSSL( java.net.ServerSocket s )
    { 
        return (s instanceof SSLServerSocket); 
    }

    public void switchToClientMode( java.net.Socket socket )
    {
        // rt: switch to client mode
//         if( Environment.changeSSLRoles())
//         {	
//             ((SSLSocket) socket).setUseClientMode( true );
//         }
    }
    
    private ServerSocketFactory createServerSocketFactory() 
    {
	try 
	{
	    // set up key manager to do server authentication
	    KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
	    KeyStore key_store = KeyStore.getInstance( "JKS" );

            String keystore_location = Environment.keyStore();
            if( keystore_location == null ) 
            {
                System.out.print( "Please enter key store file name: " );
                keystore_location = 
                    (new BufferedReader(new InputStreamReader(System.in))).readLine();
            }

            String keystore_passphrase = 
                Environment.getProperty( "jacorb.security.keystore_password" );
            if( keystore_passphrase == null ) 
            {
                System.out.print( "Please enter store pass phrase: " );
                keystore_passphrase= 
                    (new BufferedReader(new InputStreamReader(System.in))).readLine();
            }

            String alias = Environment.defaultUser();
            if( alias == null ) 
            {
                System.out.print( "Please enter alias  name: " );
                alias = 
                    (new BufferedReader(new InputStreamReader(System.in))).readLine();
            }

            String password = Environment.defaultPassword();
            if ( password == null ) 
            {
                System.out.print( "Please enter password: " );
                password = 
                    (new BufferedReader(new InputStreamReader(System.in))).readLine();
            }

            key_store.load( new FileInputStream( keystore_location ),
                            keystore_passphrase.toCharArray() );

            kmf.init( key_store, keystore_passphrase.toCharArray() );

            TrustManagerFactory tmf = 
		TrustManagerFactory.getInstance( "SunX509" );
	    tmf.init( key_store );

            SSLContext ctx = SSLContext.getInstance( "TLS" );
            ctx.init( kmf.getKeyManagers(), 
		      tmf.getTrustManagers(), 
		      null );

            return ctx.getServerSocketFactory();
	} 
	catch( Exception e ) 
	{
	    Debug.output( 1, e );
	}

	return null;
    }
}
