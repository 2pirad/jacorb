package org.jacorb.config;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 2009 Gerald Brose.
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

import org.jacorb.orb.ORB;

/**
 * Can be subclassed to provide initialization of a backend logging system
 * based on parameters from the JacORB configuration.
 *
 * @author Andre Spiegel <spiegel@gnu.org>
 * @version $Id: LoggingInitializer.java,v 1.2 2009-05-22 20:41:37 andre.spiegel Exp $
 */
public abstract class LoggingInitializer
{
    public static final String ATTR_LOG_VERBOSITY = "jacorb.log.default.verbosity";
    public static final String ATTR_LOG_FILE      = "jacorb.logfile";
    public static final String ATTR_LOG_APPEND    = "jacorb.logfile.append";
    
    /**
     * If the given filename contains the string "$implname", replaces
     * that string with the value of the configuration attribute
     * "jacorb.implname", if that is set, or the serverId of the ORB
     * otherwise.  Also, as a special case, if $implname appears at the
     * end of the filename, the extension .log is added to the resulting
     * string.
     */
    protected String substituteImplname (String filename,
                                         Configuration config)
    {
        if (!filename.contains ("$implname"))
        {
            return filename;
        }
        else
        {
            String serverId = "jacorb"; // reasonable default
            ORB orb = config.getORB();
            if (orb != null) serverId = orb.getServerIdString();
            String implName = config.getAttribute ("jacorb.implname", serverId);
            if (filename.endsWith ("$implname"))
            {
                return filename.substring (0, filename.length()-9)
                       + implName + ".log";
            }
            else
            {
                return filename.replace ("$implname", implName);
            }
        }
    }
    
    /**
     * Implement this method to provide initialization for a given logging
     * backend.  The method should check itself whether the chosen logging
     * backend is active (by verifying that the corresponding SLF4J adapter
     * class is on the classpath).
     */
    public abstract void init (Configuration config);            
}
