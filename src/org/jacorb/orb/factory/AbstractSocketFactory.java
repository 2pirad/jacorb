/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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
 *
 */

package org.jacorb.orb.factory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.omg.CORBA.TIMEOUT;

/**
 * @author Alphonse Bendt
 * @version $Id: AbstractSocketFactory.java,v 1.5 2009-03-30 15:01:46 alexander.bykov Exp $
 */

public abstract class AbstractSocketFactory implements SocketFactory, Configurable
{
    protected Logger logger;

    public void configure(Configuration configuration) throws ConfigurationException
    {
        org.jacorb.config.Configuration config = (org.jacorb.config.Configuration) configuration;

        logger = config.getNamedLogger("jacorb.orb.socketfactory");
    }

    public final Socket createSocket(String host, int port, int timeout) throws UnknownHostException, IOException
    {
    	try
    	{
    		return doCreateSocket(host, port, timeout);
    	}
    	catch(SocketTimeoutException e)
    	{
    		throw new TIMEOUT(e.toString());
    	}
    }

    protected abstract Socket doCreateSocket(String host, int port, int timeout) throws IOException, UnknownHostException;
}
