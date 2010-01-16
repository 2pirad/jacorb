
package org.jacorb.test.orb;

import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPAddressORBInitializer.java,v 1.2 2010-01-16 16:24:10 alexander.bykov Exp $
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
