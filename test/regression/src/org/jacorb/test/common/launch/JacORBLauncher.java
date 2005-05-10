package org.jacorb.test.common.launch;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 2005  Gerald Brose.
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
 *   Software Foundation, 51 Franklin Street, Fifth Floor, Boston,
 *   MA 02110-1301, USA.
 */

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;

import org.jacorb.test.common.*;

/**
 * A JacORBLauncher runs a given main class against a specified
 * JacORB version.  The class JacORBLauncher itself is an abstract
 * superclass for specific launchers that work with a given JacORB
 * installation.  To use, call JacORBLauncher.getLauncher(), then
 * invoke the launch() method on the resulting object.
 * 
 * @author Andre Spiegel spiegel@gnu.org
 * @version $Id: JacORBLauncher.java,v 1.1 2005-05-10 13:57:56 andre.spiegel Exp $
 */
public abstract class JacORBLauncher
{
    private static Map        launchers;
    private static Properties testProperties;
    private static List       versions;
    
    protected String jacorbHome;
    protected boolean coverage;
    
    protected JacORBLauncher (String jacorbHome, boolean coverage)
    {
        this.jacorbHome = jacorbHome;
        this.coverage = coverage;
    }
    
    public abstract Process launch (String classpath,
                                    Properties props,
                                    String mainClass,
                                    String[] args);
    
    public String getJacorbHome() 
    {
        return jacorbHome;
    }
    
    public List propsToArgList (Properties props)
    {
        List result = new ArrayList();
        
        if (props == null) return result;

        for (Iterator i = props.keySet().iterator(); i.hasNext();)
        {
            String key = (String)i.next();
            String value = props.getProperty(key);
            result.add ("-D" + key + "=" + value);
        }
        
        return result;
    }
    
    public String[] toStringArray (List l)
    {
        return ((String[])l.toArray (new String[0]));
    }

    /**
     * Loads and returns the properties defined in the file test.properties
     * in the regression suite.
     */
    public static Properties getTestProperties()
    {
        if (testProperties == null)
        {
            try
            {
                InputStream in = new FileInputStream
                (
                    TestUtils.testHome() + "/test.properties"
                );
                testProperties = new Properties();
                testProperties.load (in);
            }
            catch (IOException ex)
            {
                testProperties = null;
                throw new RuntimeException (ex);
            }
        }
        return testProperties;
    }
    
    /**
     * Returns a list of all the available JacORB versions.
     */
    public static List getVersions()
    {
        if (versions == null)
        {
            versions = new ArrayList();
            for (int i=0; ; i++)
            {
                String key = "jacorb.test.jacorb_version." + i + ".id";
                String value = getTestProperties().getProperty(key);
                if (value == null) break;
                versions.add (value);
            }
        }
        return versions;
    }

    /**
     * Returns a launcher for the specified JacORB version.
     * If coverage is true, sets up the launcher to that
     * coverage information will be gathered.
     */
    public static JacORBLauncher getLauncher (String version,
                                              boolean coverage)
    {
        int index = getVersions().indexOf (version);
        if (index == -1) throw new RuntimeException
        (
            "JacORB version " + version + " not available"
        );
        String key = "jacorb.test.jacorb_version." + index + ".home";
        String home = getTestProperties().getProperty(key);
        if (home == null)
        {
            if (version.equals("cvs"))
                home = getCVSHome();
            else
                throw new RuntimeException
                (
                    "No home directory for JacORB version " + version
                );
        }
        key = "jacorb.test.jacorb_version." + index + ".launcher";
        String launcherClassName = getTestProperties().getProperty(key);
        if (launcherClassName == null) throw new RuntimeException
        (
            "No launcher class defined for JacORB version " + version
        );
        try
        {
            Class launcherClass = Class.forName (launcherClassName);
            Constructor c = launcherClass.getConstructor
            (
                new Class[] { java.lang.String.class,
                              boolean.class }
            );
            return (JacORBLauncher)c.newInstance
            (
                new Object[] { home, new Boolean(coverage) }
            );
        }
        catch (Exception ex)
        {
            throw new RuntimeException (ex);
        }
    }
    
    private static String getCVSHome()
    {
        String testHome = TestUtils.testHome();
        Pattern homePattern = Pattern.compile 
        (
            "^(.*?)/test/regression"
        );
        Matcher m = homePattern.matcher (testHome);
        if (m.matches())
            return m.group(1);
        else
            throw new RuntimeException ("couldn't find CVS home: "
                                        + testHome);
    }
    
}
