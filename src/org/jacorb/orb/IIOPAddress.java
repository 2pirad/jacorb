package org.jacorb.orb;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPAddress.java,v 1.2 2003-03-31 16:02:26 andre.spiegel Exp $
 */
public class IIOPAddress 
{
	private String host;
	private int port;
	
	public IIOPAddress (String host, int port)
	{
		this.host = host;
		this.port = port;
	}

	public String getHost()
	{
		return host;
	}

	public int getPort()
	{
		return port;
	}
	
    public boolean equals (Object other)
    {
        if (other instanceof IIOPAddress)
        {
            IIOPAddress x = (IIOPAddress)other;
            return this.host.equals (x.host) && this.port == x.port;
        }
        else
            return false;
    }
    
	public String toString()
	{
		return host + ":" + port;
	}
}
