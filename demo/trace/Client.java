package demo.trace;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.*;
/**
 * Client.java
 *
 *
 * Created: Mon Jul 24 14:58:02 2000
 *
 * @author Nicolas Noffke
 * $Id: Client.java,v 1.1 2001-03-17 18:07:42 brose Exp $
 */

public class Client {
    public static void main(String[] args) {
        try
        {
            ORB orb = ORB.init(args, null);

            NamingContextExt nc = NamingContextExtHelper.narrow
                (orb.resolve_initial_references("NameService"));

	    org.omg.CORBA.Object o = nc.resolve( nc.to_name("relay_server.trace_demo") );
            FooRelayServer server = FooRelayServerHelper.narrow( o );

            System.out.println("Client will call relay server");
            server.anotherOperation("Hello relay server");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
} // Client
