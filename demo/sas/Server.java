package demo.sas;

import java.io.*;

import org.omg.PortableServer.POA;
import org.omg.Security.*;
import org.jacorb.security.sas.*;
import org.omg.CORBA.ORB;
import org.jacorb.util.*;

/**
 * This is the server part of the sas demo. It demonstrates
 * how to get access to the certificates that the client sent
 * for mutual authentication. The certificate chain can be
 * accessed via the Security Level 2 interfaces.
 *
 * @author Nicolas Noffke
 * @version $Id: Server.java,v 1.7 2002-12-12 13:56:54 david.robison Exp $
 */

public class Server
    extends SASDemoPOA
{

    private ORB orb;

    public Server(ORB orb)
    {
        this.orb = orb;
    }

    /**
     * This method is from the IDL--interface. It prints out the
     * received client cert (if available).
     */
    public void printSAS()
    {
        try
        {
            org.omg.PortableInterceptor.Current current = (org.omg.PortableInterceptor.Current) orb.resolve_initial_references( "PICurrent" );
            org.omg.CORBA.Any anyName = current.get_slot( org.jacorb.security.sas.SASTargetInitializer.sasPrincipalNamePIC );
            String name = anyName.extract_string();
            System.out.println("printSAS for user " + name);
        }
        catch (Exception e)
        {
            System.out.println("printSAS Error: " + e);
        }
    }

    public static void main( String[] args )
    {
        if( args.length != 1 )
	{
            System.out.println( "Usage: java demo.sas.GssUpServer <ior_file>" );
            System.exit( -1 );
        }

        try
        {
            ORB orb = ORB.init( args, null );
            POA poa = (POA) orb.resolve_initial_references( "RootPOA" );
            poa.the_POAManager().activate();
            org.omg.CORBA.Object demo = poa.servant_to_reference( new Server( orb ));
            PrintWriter pw = new PrintWriter( new FileWriter( args[ 0 ] ));
            pw.println( orb.object_to_string( demo ));
            pw.flush();
            pw.close();
	    orb.run();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}
