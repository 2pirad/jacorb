package demo.sas;

import java.io.*;
import java.util.*;
import org.omg.CORBA.*;
import org.jacorb.util.*;

/**
 * This is the client side of the sas demo. It just calls the single
 * operation "printCert()" of the server. As you can see, sas is fully
 * transparent.
 *
 * @author Nicolas Noffke
 * @version $Id: GssUpClient.java,v 1.1 2002-12-12 13:56:54 david.robison Exp $
 */

public class GssUpClient
{
    public static void main( String args[] )
    {
        if( args.length != 3 )
	{
            System.out.println( "Usage: java demo.sas.GssUpClient <ior_file> <username> <password>" );
            System.exit( 1 );
        }

        try
	{
            // initialize the ORB.
            ORB orb = ORB.init( args, null );

            // get the server
            File f = new File( args[ 0 ] );
            if( ! f.exists() )
            {
                System.out.println("File " + args[0] + " does not exist.");

                System.exit( -1 );
            }
            if( f.isDirectory() )
            {
                System.out.println("File " + args[0] + " is a directory.");

                System.exit( -1 );
            }
            BufferedReader br = new BufferedReader( new FileReader( f ));
            org.omg.CORBA.Object obj = orb.string_to_object( br.readLine() );
            br.close();
            SASDemo demo = SASDemoHelper.narrow( obj );

            // set security credentials
            byte[] contextToken = org.jacorb.security.sas.GSSUPNameSpi.encode(args[1], args[2], "");
            org.jacorb.security.sas.SASClientInitializer.setContextToken(contextToken);

            //call single operation
            demo.printSAS();
            demo.printSAS();
            demo.printSAS();

            System.out.println( "Call to server succeeded" );
        }
        catch( Exception ex )
	{
            ex.printStackTrace();
        }
    }
}
