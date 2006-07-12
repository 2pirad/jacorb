package org.jacorb.test.bugs.bugjac330;

import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jacorb.test.BasicServer;
import org.jacorb.test.BasicServerHelper;
import org.jacorb.test.common.ClientServerSetup;
import org.jacorb.test.orb.BasicServerImpl;
import org.omg.CORBA.NO_RESOURCES;
import org.omg.CORBA.ORB;

/**
 * @author Alphonse Bendt
 * @version $Id: MultipleServerTest.java,v 1.1 2006-07-12 11:34:43 alphonse.bendt Exp $
 */
public class MultipleServerTest extends TestCase
{
    private ClientServerSetup setup1;
    private String server1IOR;
    private ClientServerSetup setup2;
    private String server2IOR;
    private ORB orb;

    protected void setUp() throws Exception
    {
        // this is a hack. we need two server objects for this test.
        // as ClientServerTestCase
        // does not support that, we create two ClientServerSetup's here and
        // invoke their lifecyle methods explicitely.
        TestSuite dummySuite = new TestSuite();

        setup1 = new ClientServerSetup(dummySuite, BasicServerImpl.class.getName());
        setup1.setUp();
        server1IOR = setup1.getClientOrb().object_to_string(setup1.getServerObject());

        setup2 = new ClientServerSetup(dummySuite, BasicServerImpl.class.getName());
        setup2.setUp();
        server2IOR = setup2.getClientOrb().object_to_string(setup2.getServerObject());
    }

    protected void tearDown() throws Exception
    {
        setup2.tearDown();
        setup1.tearDown();
        if (orb != null)
        {
            orb.shutdown(true);
        }
    }

    private ORB newORB(Properties props)
    {
        orb = ORB.init(new String[0], props);

        return orb;
    }

    public void testAccessTwoServersAtOnceShouldFail() throws Exception
    {
        Properties props = new Properties();

        props.put("jacorb.connection.client.max_receptor_threads", "1");

        ORB orb = newORB(props);

        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));

        BasicServer server2 = BasicServerHelper.narrow(orb.string_to_object(server2IOR));

        try
        {
            server2.bounce_long(10);
            fail();
        }
        catch (NO_RESOURCES e)
        {
            // expected
        }
    }

    public void testAccessTwoServersOneByOne() throws Exception
    {
        Properties props = new Properties();

        props.put("jacorb.connection.client.max_receptor_threads", "1");

        ORB orb = newORB(props);

        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));
        server1._release();

        // give the ConsumerReceptorThread some time to finish its work
        Thread.sleep(1000);

        BasicServer server2 = BasicServerHelper.narrow(orb.string_to_object(server2IOR));
        assertEquals(10, server2.bounce_long(10));
        server2._release();
    }

    public void testAccessTwoServersAtOnceReleaseTryAgain() throws Exception
    {
        Properties props = new Properties();

        props.put("jacorb.connection.client.max_receptor_threads", "1");

        ORB orb = newORB(props);

        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));
        server1._release();

        BasicServer server2 = BasicServerHelper.narrow(orb.string_to_object(server2IOR));

        try
        {
            server2.bounce_long(10);
            fail();
        }
        catch (NO_RESOURCES e)
        {
            // expected
        }

        server1._release();

        // give the ConsumerReceptorThread some time to finish its work
        Thread.sleep(1000);

        // retry bind
        assertEquals(10, server2.bounce_long(10));
    }

    public void testNoIdleThreads() throws Exception
    {
        Properties props = new Properties();

        props.put("jacorb.connection.client.max_receptor_threads", "1");
        props.put("jacorb.connection.client.max_idle_receptor_threads", "0");

        ORB orb = newORB(props);

        BasicServer server1 = BasicServerHelper.narrow(orb.string_to_object(server1IOR));
        assertEquals(10, server1.bounce_long(10));

        assertTrue(isThereAThreadNamed("ClientMessageReceptor"));

        server1._release();

        Thread.sleep(2000);

        assertFalse(isThereAThreadNamed("ClientMessageReceptor"));
    }

    private boolean isThereAThreadNamed(String name)
    {
        // begin hack.
        // fetch the names of all active threads and see if
        // the name matches.
        int threadCount = Thread.activeCount();
        Thread[] threads = new Thread[threadCount];

        Thread.enumerate(threads);

        for (int i = 0; i < threads.length; i++)
        {
            if (threads[i].getName().indexOf(name) >= 0)
            {
                return true;
            }
        }
        return false;
    }
}
