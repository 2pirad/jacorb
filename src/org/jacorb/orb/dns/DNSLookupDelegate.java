package org.jacorb.orb.dns;

import java.net.InetAddress;

/**
 * DNSLookupDelegate.java
 *
 *
 * Created: Thu Apr  5 10:54:29 2002
 *
 * @author Nicolas Noffke
 * @version $Id: DNSLookupDelegate.java,v 1.2 2002-03-19 09:25:29 nicolas Exp $
 */

public interface DNSLookupDelegate  
{                    
    public String inverseLookup( String ip );

    public String inverseLookup( InetAddress addr );

} // DNSLookupDelegate
