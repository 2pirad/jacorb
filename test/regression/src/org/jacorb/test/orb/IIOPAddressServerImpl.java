package org.jacorb.test.orb;

import org.omg.PortableServer.*;

import org.jacorb.orb.IIOPAddress;
import org.jacorb.config.Configuration;

import org.jacorb.test.*;

import org.apache.avalon.framework.configuration.Configurable;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPAddressServerImpl.java,v 1.1.4.1 2004-04-14 13:10:24 phil.mesnier Exp $
 */
public class IIOPAddressServerImpl extends IIOPAddressServerPOA
    implements Configurable
{
    private Configuration config = null;

    public void configure (org.apache.avalon.framework.configuration.Configuration c)
    {
        config = (org.jacorb.config.Configuration)c;
    }

    public void setSocketAddress(String host, int port)
    {
        config.setAttribute ("OAIAddr",host);
        if (port != -1)
            config.setAttribute ("OAPort", Integer.toString(port));
    }

    public void clearSocketAddress()
    {
        config.setAttribute ("OAIAddr", null);
        config.setAttribute ("OAPort", null);
    }

    public void setIORAddress(String host, int port)
    {
        config.setAttribute ("jacorb.ior_proxy_host", host);
        if (port != -1)
            config.setAttribute ("jacorb.ior_proxy_port",Integer.toString(port));
    }

    public void clearIORAddress()
    {
        config.setAttribute ("jacorb.ior_proxy_host", null);
        config.setAttribute ("jacorb.ior_proxy_port", null);
    }

    public void addAlternateAddress(String host, int port)
    {
        IIOPAddressInterceptor.alternateAddresses.add (new IIOPAddress (host, port));
    }

    public void clearAlternateAddresses()
    {
        IIOPAddressInterceptor.alternateAddresses.clear();
    }

    /**
     * Returns a sample object, using a new POA so that the address settings
     * we made are incorporated into the IOR.
     */
    public Sample getObject()
    {
    	try
        {
            SampleImpl result = new SampleImpl();
            POA poa = _default_POA().create_POA("poa-" + System.currentTimeMillis(),
                                                null, null);
            poa.the_POAManager().activate();
            poa.activate_object(result);
            org.omg.CORBA.Object obj = poa.servant_to_reference (result);
            return SampleHelper.narrow (obj);
    	}
    	catch (Exception ex)
    	{
            throw new RuntimeException ("Exception creating result object: "
                                        + ex);
    	}
    }
}
