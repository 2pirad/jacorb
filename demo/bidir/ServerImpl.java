package demo.bidir;

import org.omg.CosNaming.*;
import org.omg.PortableServer.*;
import org.omg.BiDirPolicy.*;
import org.omg.CORBA.*;

import java.util.Properties;
/**
 * ServerImpl.java
 *
 *
 * Created: Mon Sep  3 19:28:34 2001
 *
 * @author Nicolas Noffke
 * @version $Id: ServerImpl.java,v 1.1 2001-10-04 07:43:34 jacorb Exp $
 */

public class ServerImpl 
    extends ServerPOA 
{
    private ClientCallback ccb = null;

    public ServerImpl()
    {        
    }

    public void register_callback( ClientCallback ccb )
    {
        this.ccb = ccb;
    }
    
    public void callback_hello( String message )
    {
        System.out.println( "Server object received hello message >" + 
                            message + 
                            '<');
        
        ccb.hello( message );
    }

    public static void main( String[] args )
        throws Exception
    {
        Properties props = new Properties();
        props.put( "org.omg.PortableInterceptor.ORBInitializerClass.bidir_init",
                   "org.jacorb.orb.connection.BiDirConnectionInitializer" );

        org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init( args, props );

        Any any = orb.create_any();
        BidirectionalPolicyValueHelper.insert( any, BOTH.value );

        POA root_poa = (POA) orb.resolve_initial_references( "RootPOA" );

        Policy[] policies = new Policy[4];
        policies[0] = 
            root_poa.create_lifespan_policy(LifespanPolicyValue.TRANSIENT);

        policies[1] = 
            root_poa.create_id_assignment_policy(IdAssignmentPolicyValue.SYSTEM_ID);

        policies[2] = 
            root_poa.create_implicit_activation_policy( ImplicitActivationPolicyValue.IMPLICIT_ACTIVATION );

        policies[3] = orb.create_policy( BIDIRECTIONAL_POLICY_TYPE.value,
                                         any );
        
        POA bidir_poa = root_poa.create_POA( "BiDirPOA",
                                             root_poa.the_POAManager(),
                                             policies );
        bidir_poa.the_POAManager().activate();

        org.omg.CORBA.Object o = 
            bidir_poa.servant_to_reference( new ServerImpl() );

        NamingContextExt nc = 
            NamingContextExtHelper.narrow(
                orb.resolve_initial_references( "NameService" ));
        
        NameComponent[] n = nc.to_name( "bidir.example" );

        nc.bind( n, o );

        orb.run();
    }        
}// ServerImpl

