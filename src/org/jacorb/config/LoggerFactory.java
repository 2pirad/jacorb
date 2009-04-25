package org.jacorb.config;

/*
 *        JacORB - a free Java ORB
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

import org.apache.avalon.framework.logger.Logger;

/**
 * Shields JacORB from details of creating Avalon loggers for a
 * specific logging backend.
 *
 * @author Gerald Brose 
 * @version $Id: LoggerFactory.java,v 1.7 2009-04-25 10:04:52 andre.spiegel Exp $
 * @since JacORB 2.0 beta 3
 */

public interface LoggerFactory extends Configurable
{
    /**
     * @return the name of the actual logging mechanism, e.g., "logkit"
     */
    String getLoggingBackendName();

    /**
     * @return a console Logger for a given name
     */
    Logger getNamedLogger(String name);

    /**
     * @return a console Logger for a given name
     */
    Logger getNamedRootLogger(String name);

    /**
     * set the file name and max file size for logging to a file
     */ 
    void setDefaultLogFile(String fileName, long maxLogSize)
        throws java.io.IOException;
}
