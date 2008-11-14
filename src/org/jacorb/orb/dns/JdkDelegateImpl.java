package org.jacorb.orb.dns;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2004 Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.avalon.framework.logger.Logger;


/**
 * A DNS Delegate that resolves names using the JDK methods.
 * This only works reliably since JDK 1.4.
 * 
 * @author <a href="mailto:spiegel@gnu.org">Andre Spiegel</a>
 * @version $Id: JdkDelegateImpl.java,v 1.4 2008-11-14 08:55:31 nick.cross Exp $
 */

public class JdkDelegateImpl 
    implements DNSLookupDelegate
{
    Logger logger;

    JdkDelegateImpl(Logger logger)
    {
        this.logger = logger;
    }

    public String inverseLookup(String ip)
    {
        try
        {
            return InetAddress.getByName(ip).getHostName();
        }
        catch (UnknownHostException e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Could not resolve IP " + ip);
            }
            return null;
        }
    }

    public String inverseLookup (InetAddress addr)
    {
        return addr.getHostName();
    }

}
