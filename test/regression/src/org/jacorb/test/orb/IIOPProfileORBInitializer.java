
package org.jacorb.test.orb;

import org.omg.CORBA.*;
import org.omg.PortableInterceptor.*;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPProfileORBInitializer.java,v 1.1 2003-12-16 13:38:04 andre.spiegel Exp $
 */
public class IIOPProfileORBInitializer
    extends LocalObject
    implements ORBInitializer
{
    public void pre_init (ORBInitInfo info)
    {
        try
        {
            info.add_ior_interceptor (new IIOPProfileInterceptor());
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
