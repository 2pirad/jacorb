
package org.jacorb.test.orb;

import org.omg.CORBA.*;
import org.omg.PortableInterceptor.*;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPAddressORBInitializer.java,v 1.1 2003-04-09 09:13:31 andre.spiegel Exp $
 */
public class IIOPAddressORBInitializer
    extends LocalObject
    implements ORBInitializer
{
    public void pre_init (ORBInitInfo info)
    {
        try
        {
            info.add_ior_interceptor (new IIOPAddressInterceptor());
        }
        catch (DuplicateName ex)
        {
            throw new RuntimeException (ex.toString());        
        }
    }

    public void post_init (ORBInitInfo info)
    {

    }

}
