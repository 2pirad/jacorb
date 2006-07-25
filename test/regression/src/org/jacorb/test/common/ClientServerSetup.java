package org.jacorb.test.common;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2005  Gerald Brose.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.omg.CORBA.*;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import junit.framework.*;
import junit.extensions.*;

import org.jacorb.test.common.launch.*;

/**
 * A special TestSetup that creates a separate CORBA server process,
 * and allows JUnit test cases to talk to a CORBA object supplied
 * by that server.
 * <p>
 * A <code>ClientServerSetup</code> should be used together with a
 * {@link ClientServerTestCase}, which provides an easy way so that
 * the individual test cases can actually see the setup.
 * The following example shows how to set this up in the static
 * <code>suite</code> method:
 *
 * <p><blockquote><pre>
 * public class MyTest extends ClientServerTestCase
 * {
 *     ...
 *
 *     public static Test suite()
 *     {
 *         TestSuite suite = new TestSuite ("My CORBA Test");
 *
 *         // Wrap the setup around the suite, specifying
 *         // the name of the servant class that should be
 *         // instantiated by the server process.
 *
 *         ClientServerSetup setup =
 *             new ClientServerSetup (suite,
 *                                    "my.corba.ServerImpl");
 *
 *         // Add test cases, passing the setup as an
 *         // additional constructor parameter.
 *
 *         suite.addTest (new MyTest ("testSomething", setup));
 *         ...
 *
 *         // Return the setup, not the suite!
 *         return setup;
 *     }
 * }
 * </pre></blockquote><p>
 *
 * The individual test cases can then access the setup in a convenient way.
 * For details, see {@link ClientServerTestCase}.
 *
 * @author Andre Spiegel <spiegel@gnu.org>
 * @version $Id: ClientServerSetup.java,v 1.39 2006-07-25 10:54:39 alphonse.bendt Exp $
 */
public class ClientServerSetup extends TestSetup {

    public static final String JACORB_REGRESSION_DISABLE_SECURITY = "jacorb.regression.disable_security";
    public static final String JACORB_REGRESSION_DISABLE_IMR = "jacorb.regression.disable_imr";

    private final long testTimeout;
    protected final String               servantName;
    protected Process                    serverProcess;
    private boolean isProcessDestroyed = false;
    protected StreamListener             outListener, errListener;
    protected org.omg.CORBA.Object       serverObject;
    protected org.omg.CORBA.ORB          clientOrb;
    protected org.omg.PortableServer.POA clientRootPOA;

    private ClientServerSetup imrSetup;
    boolean inInit = false;

    private final Properties clientOrbProperties = new Properties();
    private final Properties serverOrbProperties = new Properties();

    private static final Comparator comparator = new JacORBVersionComparator();

    /**
     * Constructs a new ClientServerSetup that is wrapped
     * around the specified Test.  When the test is run,
     * the setup spawns a server process in which an instance
     * of the class servantName is created and registered
     * with the ORB.
     * @param test The test around which the new setup
     * should be wrapped.
     * @param servantName The fully qualified name of the
     * servant class that should be instantiated in the
     * server process.
     */
    public ClientServerSetup ( Test test, String servantName )
    {
        super ( test );
        this.servantName = servantName;
        clientOrbProperties.put ("org.omg.CORBA.ORBClass",
                                 "org.jacorb.orb.ORB");
        clientOrbProperties.put ("org.omg.CORBA.ORBSingletonClass",
                                 "org.jacorb.orb.ORBSingleton");

        long parseLong = getTestTimeout();
        testTimeout = parseLong;
    }

    public static long getTestTimeout()
    {
        long parseLong;
        try
        {
            parseLong = Long.parseLong(System.getProperty("jacorb.test.timeout"));
        }
        catch (Exception e)
        {
            parseLong = 15000;
        }
        return parseLong;
    }

    public ClientServerSetup( Test test,
                              String servantName,
                              Properties clientOrbProperties,
                              Properties serverOrbProperties )
    {
        this( test, servantName );
        if (clientOrbProperties != null)
        {
            this.clientOrbProperties.putAll (clientOrbProperties);
        }
        if (serverOrbProperties != null)
        {
            this.serverOrbProperties.putAll(serverOrbProperties);
        }
    }

    public void setUp() throws Exception
    {
        initIMR();

        setUpServer();
    }

    private void setUpServer() throws IOException, InvalidName, AdapterInactive
    {
        initSecurity();

        clientOrb = ORB.init (new String[0], clientOrbProperties );
        clientRootPOA = POAHelper.narrow
                          ( clientOrb.resolve_initial_references( "RootPOA" ) );
        clientRootPOA.the_POAManager().activate();

        String serverVersion = System.getProperty ("jacorb.test.server.version",
                                                   "cvs");
        String testID = System.getProperty("jacorb.test.id", "");
        String cs = System.getProperty ("jacorb.test.coverage", "false");
        boolean coverage = TestUtils.isPropertyTrue(cs);
        String outStr = System.getProperty("jacorb.test.outputfile.testname", "false");
        boolean outputFileTestName = TestUtils.isPropertyTrue(outStr);

        Properties serverProperties = new Properties();
        if (serverOrbProperties != null)
        {
            serverProperties.putAll (serverOrbProperties);
        }
        serverProperties.put ("jacorb.implname", servantName);

        JacORBLauncher launcher = JacORBLauncher.getLauncher (serverVersion,
                                                              coverage);

        if (coverage)
        {
            serverProperties.put ("emma.coverage.out.file",
                                  launcher.getJacorbHome() +
                                  "/test/regression/output/" +
                                  (outputFileTestName == true ? "" : testID) +
                                  "/coverage-server.ec");
        }

        serverProcess = launcher.launch
        (
            TestUtils.testHome() + "/classes",
            serverProperties,
            getTestServerMain(),
            new String[] { servantName }
        );

        // add a shutdown hook to ensure that the server process
        // is shutdown even if this JVM is going down unexpectedly
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run()
            {
                if (!isProcessDestroyed)
                {
                    serverProcess.destroy();
                }
            }
        });

        outListener = new StreamListener (serverProcess.getInputStream(),
                                          "OUT");
        errListener = new StreamListener (serverProcess.getErrorStream(),
                                          "ERR");
        outListener.start();
        errListener.start();
        String ior = outListener.getIOR(testTimeout);

        if (ior == null)
        {
            String exc = errListener.getException(1000);

            String details = dumpStreamListener();

            fail("could not access IOR for Server.\nServant: " + servantName + "\nTimeout: " + testTimeout + " millis.\nThis maybe caused by: " + exc + '\n' + details);
        }

        resolveServerObject(ior);
    }

    private String dumpStreamListener()
    {
        StringBuffer details = new StringBuffer();
        details.append(outListener.toString());
        details.append(errListener.toString());
        return details.toString();
    }

    protected void resolveServerObject(String ior)
    {
        serverObject = clientOrb.string_to_object(ior);
    }

    public void tearDown() throws Exception
    {
        clientOrb.shutdown(true);
        serverProcess.destroy();
        isProcessDestroyed = true;
        outListener.setDestroyed();
        errListener.setDestroyed();
        if (imrSetup != null)
        {
            imrSetup.tearDown();
        }
    }

    public String getTestServerMain()
    {
        String serverVersion = System.getProperty ("jacorb.test.server.version",
                                                   "cvs");
        if (comparator.compare (serverVersion, "2.2") >= 0)
        {
            return "org.jacorb.test.common.TestServer";
        }
        return "org.jacorb.test.common.TestServer_before_2_2";
    }

    /**
     * Gets a reference to the object that was instantiated in the
     * server process.
     */
    public org.omg.CORBA.Object getServerObject()
    {
        return serverObject;
    }

    /**
     * Gets the client ORB that is used to communicate with the server.
     */
    public org.omg.CORBA.ORB getClientOrb()
    {
        return clientOrb;
    }

    /**
     * Gets the fully qualified name of the servant class that
     * is instantiated in the server.
     */
    public String getServantName()
    {
        return servantName;
    }

    /**
     * Gets the server process.
     */
    public Process getServerProcess()
    {
        return serverProcess;
    }

    public POA getClientRootPOA()
    {
        return clientRootPOA;
    }

    /**
     * check if SSL testing is disabled for this setup
     */
    private boolean isSSLEnabled()
    {
        return checkProperty(JACORB_REGRESSION_DISABLE_SECURITY);
    }

    /**
     * check is IMR testing is disabled for this setup
     */
    private boolean isIMREnabled()
    {
        return checkProperty(JACORB_REGRESSION_DISABLE_IMR);
    }

    /**
     * <code>checkProperties</code> examines clientOrbProperties and serverOrbProperties
     * for the specified key. If it is found in one of the properties
     * and is set to true this
     * will return false.
     *
     * @return a <code>boolean</code> value
     */
    private boolean checkProperty(String property)
    {
        boolean result = true;

        if (clientOrbProperties != null)
        {
            if (TestUtils.isPropertyTrue(clientOrbProperties.getProperty
                            (property, "false")))
            {
                result = false;
            }
        }
        if (serverOrbProperties != null)
        {
            if (TestUtils.isPropertyTrue(serverOrbProperties.getProperty
                            (property, "false")))
            {
                result = false;
            }
        }
        return result;
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
        final String sslProperty = System.getProperty("jacorb.test.ssl");
        final boolean useSSL = TestUtils.isPropertyTrue(sslProperty);

        if (useSSL && isSSLEnabled())
        {
            // In this case we have been configured to run all the tests
            // in SSL mode. For simplicity, we will use the demo/ssl keystore
            // and properties (partly to ensure that they always work)
            Properties clientProps = loadSSLProps("jsse_client_props", "jsse_client_ks");

            clientOrbProperties.putAll(clientProps);

            Properties serverProps = loadSSLProps("jsse_server_props", "jsse_server_ks");

            serverOrbProperties.putAll(serverProps);
        }
    }

    /**
     * its assumed that the property file and the keystore file
     * are located in the demo/ssl dir.
     */
    private Properties loadSSLProps(String propertyFilename, String keystoreFilename) throws IOException
    {
        final Properties props = new Properties();

        final File file = new File
        (
            TestUtils.testHome()
            + File.separatorChar
            + ".."
            + File.separatorChar
            + ".."
            + File.separatorChar
            + "demo"
            + File.separatorChar
            + "ssl"
            + File.separatorChar
            + propertyFilename
        );

        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        try
        {
            props.load(input);
        }
        finally
        {
            input.close();
        }

        props.put
        (
            "jacorb.security.keystore",
            file.getParent() + File.separatorChar + keystoreFilename
        );

        props.put("jacorb.security.ssl.ssl_listener", SSLListener.class.getName());

        return props;
    }

    /**
     * optionally configures the IMR
     * for a test. the system property
     * "jacorb.test.imr" needs to be set
     * to enable use of the IMR for Client/Server tests.
     */
    private void initIMR() throws Exception
    {
        final String imrProperty = System.getProperty("jacorb.test.imr");
        final boolean useIMR = TestUtils.isPropertyTrue(imrProperty);

        if (useIMR && isIMREnabled())
        {
            final Properties imrServerProps = new Properties();
            File tempFile = File.createTempFile("IMR_Ref", ".ior");
            imrServerProps.put("jacorb.imr.ior_file", tempFile.getAbsolutePath());

            imrSetup = new ClientServerSetup(null, "", null, imrServerProps)
            {
                public String getTestServerMain()
                {
                    return ImplementationRepositoryRunner.class.getName();
                }
            };
            imrSetup.setUpServer();

            final Properties imrProps = new Properties();
            imrProps.put("jacorb.use_imr", "on");
            imrProps.put("ORBInitRef.ImplementationRepository", imrSetup.getClientOrb().object_to_string(imrSetup.getServerObject()));

            clientOrbProperties.putAll(imrProps);
            serverOrbProperties.putAll(imrProps);
        }
    }
}
