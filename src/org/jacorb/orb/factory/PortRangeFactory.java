package org.jacorb.orb.factory;

/*
 *        Written for JacORB - a free Java ORB
 *
 *   Copyright (C) 2000-2004 Nicolas Noffke, Gerald Brose.
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

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * @author Steve Osselton
 * @version $Id: PortRangeFactory.java,v 1.6 2006-07-25 15:43:20 alphonse.bendt Exp $
 */
public class PortRangeFactory
    implements Configurable
{
    private int portMin;
    private int portMax;
    protected org.jacorb.config.Configuration configuration;

    protected int getPortProperty(String name)
        throws ConfigurationException
    {
        int port = configuration.getAttributeAsInteger(name);

        // Check sensible port number
        if (port < 0)
        {
           port += 65536;
        }
        if ((port <= 0) || (port > 65535))
        {
            throw new ConfigurationException("PortRangeFactory: " + name + " invalid port number");
        }

        return port;
    }

    public void configure(Configuration config) throws ConfigurationException
    {
        configuration = (org.jacorb.config.Configuration) config;
    }

    protected void setPortMin(int portMin)
    {
        this.portMin = portMin;
    }

    protected void setPortMax(int portMax)
    {
        this.portMax = portMax;
    }

    protected int getPortMin()
    {
        return portMin;
    }

    protected int getPortMax()
    {
        return portMax;
    }
}
