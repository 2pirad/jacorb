package org.jacorb.config;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2004  Gerald Brose.
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

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.*;

import java.io.*;
import java.util.Properties;
import java.util.Iterator;

/**
 * Configuration objects are read-only representations of files with
 * configuration properties. Configuration files for a given name are
 * looked up relative to a configuration directory, with the
 * configuration <name> in the file ${JACORB_CONFIG}/<name>.properties
 * The default value after installation is $JacORB_HOME/etc$.
 *
 * To also support packaged servers in jar files, the configuration
 * file lookup mechanism tries to load properties files from the
 * classpath, if it cannot find them in the config dictionary.
 *
 * Failure to retrieve the configuration file will result in an
 * exception raised by ORB.init().
 *
 * The Configuration object is also used by JacORB components to
 * retreive their Logger objects.
 *
 * 
 * @author Gerald Brose, XTRADYNE Technologies
 * @version $Id: Configuration.java,v 1.1.2.1 2004-03-22 14:17:04 gerald Exp $
 */

public class Configuration
    extends org.apache.avalon.framework.configuration.DefaultConfiguration
{
    private static final String fileSuffix = ".orb.properties";
    private static final String COMMON_PROPS = "common" + fileSuffix;

    private Configuration config;
    private String configName; 

    /** root logger instance for this configuration */
    private Logger logger = null;
    
    /**  logger factory used to create loggers */
    private LoggerFactory loggerFactory = null;

    /**  default class name for logger factory */
    private final String loggerFactoryClzName = 
       "org.jacorb.util.LogKitLoggerFactory";


    /**
     * Create a configuration with a given name and load configuration
     * properties from the file <name>.properties
     */

    Configuration(String name)
        throws ConfigurationException
    {
        super(name);
        init(name, null);
        initLogging();
    }


    /**
     * Create a configuration using the properties passed
     * into ORB.init()
     */

    Configuration(String name, Properties orbProperties)
        throws ConfigurationException
    {
        super(name);
        init(name, orbProperties);
        initLogging();
    }

    /**
     * loads properties from files. 
     *
     * Properties are loaded in the following order, with later properties 
     * overriding earlier ones:
     * 1) System properties (incl. command line)
     * 2) common.orb.properties file
     * 2) specific configuration file for the ORB (if any)
     * 3) the ORB properties set in the client code and passed int through ORB.init(). 
     *    (Note that these will thus always take effect!)
     *
     * @param name the name for the ORB instance, may not be null. For the unspecified
     * case, the name may be "anonymous", int which case no ORB-specific properties file
     * will be looked up.
     */

    private void init(String name, Properties orbProperties)
    {
        String separator = System.getProperty("file.separator");
        String home = System.getProperty("user.home");
        String lib = System.getProperty("java.home");

        // 1) include system properties
        setAttributes( System.getProperties() );

        // 2) look for orb.common.properties

        // look for common properties files in java.home/lib first
        Properties commonProps = 
            loadPropertiesFromFile( lib + separator + "lib" + separator + COMMON_PROPS);

        if (commonProps!= null)
            setAttributes(commonProps);

        // look for common properties files in user.home next
        commonProps = 
            loadPropertiesFromFile( home + separator + COMMON_PROPS );
 
        if (commonProps!= null)
            setAttributes(commonProps);

        // look for common properties files on the classpath next
        commonProps = 
            loadPropertiesFromClassPath( COMMON_PROPS );

        if (commonProps!= null)
            setAttributes(commonProps);

        // 3) look for specific properties file
        if( !name.equals("anonymous"))
        {
            String configDir = 
                System.getProperty("jacorb.config.dir");
            
            if (configDir == null)
                configDir = System.getProperty("jacorb.home");
            
            if (configDir != null )
                configDir += separator + "etc";
            else
            {
                System.err.println("[ jacorb.home unset! Will use '.']");
                configDir = ".";
            }
            
            String propFileName = configDir + separator + name + fileSuffix;
            
            // now load properties file from file system
            Properties orbConfig = loadPropertiesFromFile(propFileName );
            
            if (orbConfig!= null)
            {
                System.out.println("[configuration " + name + " loaded from file]");
                setAttributes(orbConfig);
            }
            
            // now load properties file from classpath
            orbConfig = loadPropertiesFromClassPath( name +  fileSuffix );
            if (orbConfig!= null)
            {
                System.out.println("[configuration " + name + " loaded from classpath]");
                setAttributes(orbConfig);
            }
        }

        // 4) load properties passed to ORB.init(), these will override any
        // settings in config files or system properties!
        if (orbProperties != null)
            setAttributes(orbProperties);
    }

    /**
     * set attributes of this configuration using properties
     */

    void setAttributes(Properties properties)
    {
        System.out.println("--- set Attributes ----");
        for (Iterator iter=properties.keySet().iterator(); iter.hasNext();)
        {
            String key = (String)iter.next();

            System.out.println("setting (" + key + "," +
                               (String)properties.get(key) + ")");

            setAttribute(key, (String)properties.get(key));
        }
    }


    /**
     * Loads properties from a file
     * @param fileName the name of a properties file
     * @return a properties object or null, if fileName not found
     */

    private static Properties loadPropertiesFromFile(String fileName)
    {
        try
        {
            BufferedInputStream bin =
                new BufferedInputStream( new FileInputStream(fileName));
            Properties result = new Properties();
            result.load(bin);
            return result;
        }
        catch( java.io.IOException io )
        {
            io.printStackTrace(); //debug only
            return null;
        }
    }


    /**
     * load properties file from classpath
     * @param name the name of the properties file.
     * @return a properties object or null, if name not found
     */

    private static Properties loadPropertiesFromClassPath(String name)
    {
        Properties result = null;
        try
        {
            java.net.URL url = ClassLoader.getSystemResource(name);
            if (url!=null)           
            {
                result = new Properties();
                result.load( url.openStream() );
            }
        }
        catch (java.io.IOException ioe)
        {
            ioe.printStackTrace(); //debug only
        }
        return result;
    }


    /**
     * Set up JacORB logging. Will create logger factory and root
     * logger object according to configuration parameters. The value
     * of the property <tt>jacorb.log.loggerFactory</tt> determines the
     * logger factory class name that is used to create the root logger.
     *
     * @since 2.0 beta 3
     */

    private void initLogging()
        throws ConfigurationException
    {
        String logFileName = getAttribute("jacorb.logfile");
        String size = getAttribute( "jacorb.logfile.maxLogSize" );
        int maxLogSize = 0;

        if( size != null )
        {
            maxLogSize = Integer.parseInt( size );
        }

        if (logFileName != null && !logFileName.equals(""))
        {
            // Convert $implname postfix to implementation name
            if (logFileName.endsWith("$implname"))
            {
                logFileName = logFileName.substring (0, logFileName.length () - 9);

                if ( getAttribute("jacorb.implname") != null)
                {
                    logFileName += getAttribute("jacorb.implname");
                }
                else
                {
                    // Just in case implname has not been set
                    logFileName += "log";
                }
            }
        }

        String clzName = getAttribute("jacorb.log.loggerFactory");
        Class loggerFactoryClz = null;

        try
        {
            if ( clzName != null)
            {
                loggerFactoryClz = org.jacorb.util.ObjectUtil.classForName(clzName);
            }
            else
            {
                loggerFactoryClz = org.jacorb.util.ObjectUtil.classForName(loggerFactoryClzName);
            }
            loggerFactory = (LoggerFactory)loggerFactoryClz.newInstance();
            loggerFactory.configure( this );
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (loggerFactory == null)
        {
            System.err.println("Configuration Error, could not create logger!");
        }

        if (logFileName != null)
        {
            try
            {
                loggerFactory.setDefaultLogFile(logFileName, maxLogSize);
                //logger = loggerFactory.getNamedRootLogger("jacorb");
                logger = loggerFactory.getNamedLogger("jacorb",logFileName, maxLogSize);
            }
            catch (IOException e)
            {
                logger = loggerFactory.getNamedRootLogger("jacorb");
                if( logger.isErrorEnabled())
                {
                    logger.error("Could not create logger with file target: " + logFileName +
                                 ", falling back to console log!");
                }
            }
        }
        else
        {
            logger = loggerFactory.getNamedRootLogger("jacorb" );
        }
    }

    /**
     * @param name the name of the logger, which also functions
     *        as a log category
     * @return a Logger for a given name
     */

    public Logger getNamedLogger(String name)
    {
        return loggerFactory.getNamedLogger(name);
    }
    

}
