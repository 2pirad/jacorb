package org.jacorb.test.bugs.bugjac181;

import java.util.Properties;

import junit.framework.TestCase;

import org.jacorb.orb.factory.SocketFactoryManager;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class BugJac181ServerListenerTest extends TestCase
{
    private ORB clientORB;
    private ORB serverORB;
    private JAC181 server;

    protected void setUp() throws Exception
    {
        TCPListener.reset();

        Properties serverProps = new Properties();

        serverProps.put(SocketFactoryManager.TCP_LISTENER, TCPListener.class.getName());

        clientORB = ORB.init(new String[0], null);
        serverORB = ORB.init(new String[0], serverProps);

        POA rootPOA = POAHelper.narrow(serverORB.resolve_initial_references("RootPOA"));
        rootPOA.the_POAManager().activate();

        org.omg.CORBA.Object obj = rootPOA.servant_to_reference(new JAC181Impl());
        String objString = serverORB.object_to_string(obj);
        server = JAC181Helper.narrow(clientORB.string_to_object(objString));
    }

    protected void tearDown() throws Exception
    {
        clientORB.shutdown(true);
        serverORB.shutdown(true);
    }

    public void testListener() throws Exception
    {
        server.ping2();

        Thread.sleep(1000);

        assertTrue("No open message from listener", TCPListener.isListenerOpen());

        server._release();

        Thread.sleep(1000);

        assertTrue("No close message from listener", TCPListener.isListenerClose());

        assertTrue(TCPListener.isEventOfCorrectType());
    }
}
