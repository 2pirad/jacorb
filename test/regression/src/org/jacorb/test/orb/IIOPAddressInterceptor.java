package org.jacorb.test.orb;

import java.util.*;

import org.omg.CORBA.*;
import org.omg.IOP.*;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

import org.jacorb.orb.IIOPAddress;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPAddressInterceptor.java,v 1.1 2003-04-09 09:13:08 andre.spiegel Exp $
 */
public class IIOPAddressInterceptor
    extends LocalObject
    implements IORInterceptor
{
	public static List alternateAddresses = new ArrayList();
	
    public void establish_components(IORInfo info)
    {
		for (Iterator i = alternateAddresses.iterator(); i.hasNext();)
		{
			IIOPAddress addr = (IIOPAddress)i.next();
			info.add_ior_component_to_profile
			(
				new TaggedComponent
				(
					TAG_ALTERNATE_IIOP_ADDRESS.value,
					addr.toCDR()
				),
				TAG_INTERNET_IOP.value
			);
		}
    }

    public String name()
    {
		return "IIOPAddressInterceptor";
    }

    public void destroy()
    {
		alternateAddresses.clear();
    }

}
