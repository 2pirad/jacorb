package org.jacorb.security.level2;

import org.omg.SecurityLevel2.Current;
import org.jacorb.util.Debug;
/**
 *
 * @author Nicolas Noffke
 * $Id: ServerInitializer.java,v 1.5 2002-03-19 09:25:41 nicolas Exp $
 */

public class ServerInitializer 
  extends org.omg.CORBA.LocalObject 
    implements org.omg.PortableInterceptor.ORBInitializer
{

    public ServerInitializer() 
    {
    }

    // implementation of ORBInitializerOperations interface
    /**
     * Registers the Interceptor with a codec and a slot id.
     */
    public void post_init(org.omg.PortableInterceptor.ORBInitInfo info) 
    {
        try
        {
            Current current = 
                (Current) info.resolve_initial_references("SecurityCurrent");

            info.add_server_request_interceptor
                (new ServerAccessDecisionInterceptor(current));
        }catch (Exception e)
        {
            Debug.output(Debug.SECURITY | Debug.IMPORTANT, e);
        }
    }

    /**
     *
     * @param info <description>
     */
    public void pre_init(org.omg.PortableInterceptor.ORBInitInfo info)
    {
    
    }

} // ServerInitializer






