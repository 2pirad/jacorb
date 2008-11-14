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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.log.Hierarchy;
import org.apache.log.LogTarget;
import org.apache.log.format.PatternFormatter;
import org.apache.log.output.io.WriterTarget;
import org.apache.log.output.io.rotate.RevolvingFileStrategy;
import org.apache.log.output.io.rotate.RotateStrategyBySize;
import org.apache.log.output.io.rotate.RotatingFileTarget;

/**
 * JacORB logger factory that creates named Avalon loggers with logkit
 * as the underlying log mechanism. <BR> The semantics here is that any
 * names logger retrieved using the logkit naming conventions for
 * nested loggers will inherit its parent loggers log target e.g., a
 * logger retrieved for <code>jacorb.naming</code>
 * inherits the root logger's target (ie. <code>System.err</code>, or
 * a file.
 * <P>
 * Log priorities for new loggers are also inherited from the parent
 * logger. If no parent logger is available the priority defaults to the
 * value of this factory's <code>defaultPriority</code> field. The
 * priorities for loggers can be set via
 * configuration properties that have the same name as the requested
 * logger, plus a suffix of <code>.log.verbosity</code>.
 * <P>
 * The priority for all loggers that do not have a specific <name>.log.verbosity
 * prop defined will be set to the value of the jacorb.log.default.verbosity
 * property, if it's set. If not, the default is 0.
 *
 * @author Gerald Brose
 * @version $Id: LogKitLoggerFactory.java,v 1.7 2008-11-14 08:55:24 nick.cross Exp $
 * @since JacORB 2.0 beta 3
 */

public class LogKitLoggerFactory
    implements LoggerFactory
{
    private final static String DEFAULT_LOG_PATTERN =
        "[%.20{category}] %.7{priority} : %{message}\\n%{throwable}";

    private final static String name = "logkit";
    private PatternFormatter logFormatter = null;

    /** default priority for loggers created with this factory */
    private int defaultPriority = 0;

    /** cache of created loggers */
    private final Map namedLoggers = new HashMap();

    /**  append to a log file or overwrite ?*/
    private boolean append = false;

    /** the writer for console logging */
    private Writer consoleWriter;

    /** this target for console logging */
    private LogTarget consoleTarget;

    /** The default log file */
    private LogTarget defaultTarget = null;

    private Configuration configuration = null;

    public void configure(org.apache.avalon.framework.configuration.Configuration configuration)
        throws org.apache.avalon.framework.configuration.ConfigurationException
    {
        this.configuration = (Configuration)configuration;

        defaultPriority =
            configuration.getAttributeAsInteger("jacorb.log.default.verbosity", 0);

        append =
            configuration.getAttribute("jacorb.logfile.append","off").equals("on");

        logFormatter =
            new PatternFormatter(configuration.getAttribute("jacorb.log.default.log_pattern",
                                                            DEFAULT_LOG_PATTERN));
        switch (defaultPriority)
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                break;
            default:
                throw new ConfigurationException("'" + defaultPriority + "' is an illegal"
                                                 + " value for the property jacorb.log.default.verbosity. Valid values are [0-4]");
        }
        consoleWriter = new OutputStreamWriter(System.err);
        consoleTarget = new WriterTarget(consoleWriter, logFormatter);
    }

    /**
     * @return the name of the actual, underlying logging mechanism,
     * here "logkit"
     */

    public final String getLoggingBackendName()
    {
        return name;
    }

    /**
     * @param name the name of the logger, which also functions
     *        as a log category
     * @return a Logger for a given name
     */

    public Logger getNamedLogger(String name)
    {
        return getNamedLogger(name, null);
    }

    /**
     * @param name - the name of the logger, which also functions
     *        as a log category
     * @return a root console logger for a logger name hierarchy
     */

    public Logger getNamedRootLogger(String name)
    {
        return getNamedLogger(name, consoleTarget);
    }


    /**
     * @param name - the name of the logger, which also functions
     *        as a log category
     * @param logFileName - the name of the file to log to
     * @param maxLogSize - maximum size of the log file. When this size is reached
     *        the log file will be rotated and a new log file created. A value of 0
     *        means the log file size is unlimited.
     *
     * @return the new logger.
     */

    public Logger getNamedLogger(String name, String logFileName, long maxLogSize)
        throws IOException
    {
        if (name == null)
            throw new IllegalArgumentException("Log file name must not be null!");

        FileOutputStream logStream =
            new FileOutputStream(logFileName, append);

        LogTarget target = null;
        if (maxLogSize == 0)
        {
            // no log file rotation
            Writer logWriter = new OutputStreamWriter(logStream);
            target = new WriterTarget(logWriter, logFormatter);
        }
        else
        {
            // use log file rotation
            target =
                new RotatingFileTarget(append,
                                       logFormatter,
                                       new RotateStrategyBySize(maxLogSize * 1000),
                                       new RevolvingFileStrategy(new File(logFileName), 10000));
        }
        return getNamedLogger(name, target);
    }


    /**
     * @param name the name of the logger, which also functions
     *        as a log category
     * @param target the log target for the new logger. If null, the new logger
     *        will log to System.err
     * @return the logger
     */
    private Logger getNamedLogger(String name, LogTarget target)
    {
        Object o = namedLoggers.get(name);

        if ( o != null )
        {
            return (Logger)o;
        }

        org.apache.log.Logger logger =
            Hierarchy.getDefaultHierarchy().getLoggerFor(name);

        int priority = getPriorityForNamedLogger(name);

        logger.setPriority(intToPriority(priority));

        if (target != null )
        {
            // LogTarget was provided by caller
            logger.setLogTargets( new LogTarget[] { target } );
        }
        else
        {
            // use default LogTarget
            if (defaultTarget == null)
            {
                logger.setLogTargets(new LogTarget[] { consoleTarget } );
            }
            else
            {
                logger.setLogTargets(new LogTarget[] { defaultTarget } );
            }
        }

        Logger result = new LogKitLogger(logger);

        namedLoggers.put(name, result);

        return result;
    }


    /**
     * return the priority for a named logger. if no priority is
     * specified for the requested name the priority of the first
     * matching parent logger is returned.
     * If there's no parent logger the factory default is returned.
     *
     * @param name the name of a logger
     * @return the priority for that logger
     */
    private int getPriorityForNamedLogger(String name)
    {
        String prefix = name;

        while (!prefix.equals(""))
        {
            try
            {
                int priorityForLogger =
                    configuration.getAttributeAsInteger( prefix + ".log.verbosity");
                if (priorityForLogger > 4)
                {
                    priorityForLogger = 4;
                }
                else if (priorityForLogger < 0)
                {
                    priorityForLogger = 0;
                }
                return priorityForLogger;
            }
            catch( ConfigurationException ce )
            {
            }

            if (prefix.lastIndexOf(".") >= 0)
            {
                prefix = prefix.substring(0, prefix.lastIndexOf("."));
            }
            else
            {
                prefix = "";
            }
        }

        // nothing found, return default
        return defaultPriority;
    }


    /**
     * <code>intToPriority</code> returns the priority level for a given integer.
     *
     * @param priority an <code>int</code> value
     * @return an <code>org.apache.log.Priority</code> value
     */
    private static org.apache.log.Priority intToPriority(int priority)
    {
        switch (priority)
        {
            case 4 :
                return org.apache.log.Priority.DEBUG;
            case 3 :
                return org.apache.log.Priority.INFO;
            case 2 :
                return org.apache.log.Priority.WARN;
            case 1 :
                return org.apache.log.Priority.ERROR;
            case 0 :
            default :
                return org.apache.log.Priority.FATAL_ERROR;
        }
    }

    public void setDefaultLogFile(String fileName, long maxLogSize) throws java.io.IOException
    {
        FileOutputStream logStream =
            new FileOutputStream(fileName, append);

        if (maxLogSize == 0)
        {
            // no log file rotation
            Writer logWriter = new OutputStreamWriter(logStream);
            defaultTarget = new WriterTarget(logWriter, logFormatter);
        }
        else
        {
            // use log file rotation
            defaultTarget =
                new RotatingFileTarget(append,
                                       logFormatter,
                                       new RotateStrategyBySize(maxLogSize * 1000),
                                       new RevolvingFileStrategy(new File(fileName), 10000));
        }
    }
}
