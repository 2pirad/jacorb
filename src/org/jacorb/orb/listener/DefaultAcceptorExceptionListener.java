/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) The JacORB project, 1997-2006.
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

package org.jacorb.orb.listener;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.jacorb.orb.iiop.IIOPListener;
import org.jacorb.util.ObjectUtil;

/**
 * The JacORB default implementation of interface
 * <code>AcceptorExceptionListener</code>. It will shutdown the ORB on all
 * Errors and for SSLExceptions that are thrown on the first loop.
 *
 * @author Nick Cross
 * @version $Id: DefaultAcceptorExceptionListener.java,v 1.2 2006-06-29 13:20:58 alphonse.bendt Exp $
 */
public class DefaultAcceptorExceptionListener
    implements AcceptorExceptionListener, Configurable
{
    /**
     * <code>sslException</code> is a cached class name for ssl exceptions.
     */
    private Class sslException;

    /**
     * <code>logger</code> is the logger.
     */
    private Logger logger;

    /**
     * Creates a new <code>DefaultAcceptorExceptionListener</code> instance.
     *
     */
    public void configure(Configuration configuration)
    {
        try
        {
            String exceptionClass = configuration.getAttribute("javax.net.ssl.SSLException");
            sslException = ObjectUtil.classForName(exceptionClass);
        }
        catch(ClassNotFoundException e)
        {
        }
        catch (ConfigurationException e)
        {
            sslException = null;
        }
        {
            sslException = null;
        }

        logger = ((org.jacorb.config.Configuration)configuration).getNamedLogger("jacorb.orb.iiop");
    }

    /**
     * Throwable <code>th</code> has been caught by the acceptor thread.
     *
     * @param e an <code>AcceptorExceptionEvent</code> value
     */
    public void exceptionCaught(AcceptorExceptionEvent e)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Caught acceptor event: " + e);
        }

        if ((e.getException() instanceof Error) ||
            (
                ! ((IIOPListener.Acceptor)e.getSource()).getAcceptorSocketLoop()
                && (sslException != null && sslException.isInstance(e.getException()))
            )
           )
        {
            logger.fatalError("fatal exception. will shutdown orb", e.getException());

            e.getORB().shutdown(true);
        }
    }
}
