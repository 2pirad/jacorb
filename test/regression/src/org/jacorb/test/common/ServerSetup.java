/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2006 The JacORB project.
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

package org.jacorb.test.common;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.jacorb.test.common.launch.JacORBLauncher;
import org.jacorb.test.common.launch.Launcher;

/**
 * @author Alphonse Bendt
 * @version $Id: ServerSetup.java,v 1.2 2006-11-30 13:13:50 alphonse.bendt Exp $
 */
public class ServerSetup extends TestSetup
{
    private static final Comparator comparator = new JacORBVersionComparator();

    private static class ProcessShutdown extends Thread
    {
        // only hold a weak reference to the process to
        // allow it to be gc'ed
        private final WeakReference processRef;

        public ProcessShutdown(Process process)
        {
            processRef = new WeakReference(process);
        }

        public void run()
        {
            Process process = (Process) processRef.get();
            if (process != null)
            {
                try
                {
                    process.destroy();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private final Properties serverOrbProperties = new Properties();

    private final String servantName;
    private final long testTimeout;
    private final String testServer;

    private Process serverProcess;
    private StreamListener outListener, errListener;
    private String serverIOR;

    protected String outName = "OUT";
    protected String errName = "ERR";

    protected final List serverArgs = new ArrayList();

    public ServerSetup(Test test, String testServer, String servantName, Properties optionalProperties)
    {
        super(test);

        this.testServer = getTestServer(testServer);
        this.servantName = servantName;
        testTimeout = getTestTimeout();

        if (optionalProperties != null)
        {
            serverOrbProperties.putAll(optionalProperties);
        }

        serverArgs.add(servantName);
    }

    public ServerSetup(Test test, String servantName)
    {
        this(test, null, servantName, null);
    }

    public static long getTestTimeout()
    {
        return TestUtils.getSystemPropertyAsLong("jacorb.test.timeout", 15000);
    }

    private String getTestServer(String optionalTestServer)
    {
        if (optionalTestServer == null)
        {
            String serverVersion = System.getProperty("jacorb.test.server.version", "cvs");
            if (comparator.compare (serverVersion, "2.2") >= 0)
            {
                return "org.jacorb.test.common.TestServer";
            }
            return "org.jacorb.test.common.TestServer_before_2_2";
        }
        return optionalTestServer;
    }


    public void setUp() throws Exception
    {
        initSecurity();

        String serverVersion = System.getProperty ("jacorb.test.server.version", "cvs");
        boolean coverage = TestUtils.getSystemPropertyAsBoolean("jacorb.test.coverage", false);

        Properties serverProperties = new Properties();
        serverProperties.setProperty("jacorb.log.default.verbosity", "0");
        serverProperties.putAll (serverOrbProperties);
        serverProperties.put ("jacorb.implname", servantName);

        if (coverage)
        {
            String outDir = System.getProperty("jacorb.test.outdir");
            serverProperties.put ("emma.coverage.out.file", outDir + "/coverage-server.ec");
            serverProperties.put("emma.verbosity.level", System.getProperty("emma.verbosity.level", "quiet") );
        }

        final Launcher launcher = JacORBLauncher.getLauncher (serverVersion,
                    coverage,
                    System.getProperty("java.class.path"),
                    serverProperties,
                    getTestServerMain(),
                    getServerArgs());

        serverProcess = launcher.launch();

        // add a shutdown hook to ensure that the server process
        // is shutdown even if this JVM is going down unexpectedly
        Runtime.getRuntime().addShutdownHook(new ProcessShutdown(serverProcess));

        outListener = new StreamListener (serverProcess.getInputStream(), outName);
        errListener = new StreamListener (serverProcess.getErrorStream(), errName);
        outListener.start();
        errListener.start();
        serverIOR = outListener.getIOR(testTimeout);

        if (serverIOR == null)
        {
            String exc = errListener.getException(1000);

            String details = dumpStreamListener();

            fail("could not access IOR for Server.\nServant: " + servantName + "\nTimeout: " + testTimeout + " millis.\nThis maybe caused by: " + exc + '\n' + details);
        }
    }

    protected String[] getServerArgs()
    {
        return (String[])serverArgs.toArray(new String[serverArgs.size()]);
    }

    public void tearDown() throws Exception
    {
        if (serverProcess != null)
        {
            serverProcess.destroy();
            serverProcess.waitFor();
            serverProcess = null;

            outListener.setDestroyed();
            errListener.setDestroyed();

            outListener = null;
            errListener = null;

            serverIOR = null;
        }
    }

    public String getServerIOR()
    {
        return serverIOR;
    }

    protected String getTestServerMain()
    {
        return testServer;
    }

    /**
     * <code>initSecurity</code> adds security properties if so configured
     * by the environment. It is possible to turn this off for selected tests
     * either by overriding this method or by setting properties for checkProperties
     * to handle.
     *
     * @exception IOException if an error occurs
     */
    protected void initSecurity() throws IOException
    {
        if (isSSLEnabled())
        {
            // In this case we have been configured to run all the tests
            // in SSL mode. For simplicity, we will use the demo/ssl keystore
            // and properties (partly to ensure that they always work)

            Properties serverProps = CommonSetup.loadSSLProps("jsse_server_props", "jsse_server_ks");

            serverOrbProperties.putAll(serverProps);
        }
    }

    /**
     * check if SSL testing is disabled for this setup
     */
    public boolean isSSLEnabled()
    {
        final String sslProperty = System.getProperty("jacorb.test.ssl");
        final boolean useSSL = TestUtils.getStringAsBoolean(sslProperty);

        return useSSL && !isPropertySet(CommonSetup.JACORB_REGRESSION_DISABLE_SECURITY);
    }

    private boolean isPropertySet(String property)
    {
        return TestUtils.getStringAsBoolean(serverOrbProperties.getProperty(property, "false"));
    }

    private String dumpStreamListener()
    {
        StringBuffer details = new StringBuffer();
        details.append(outListener.toString());
        details.append(errListener.toString());
        return details.toString();
    }
}
