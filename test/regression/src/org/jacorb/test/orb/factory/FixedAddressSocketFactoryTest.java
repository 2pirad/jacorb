package org.jacorb.test.orb.factory;

import java.net.InetAddress;
import java.net.Socket;

import org.easymock.MockControl;
import org.jacorb.config.Configurable;
import org.jacorb.config.Configuration;
import org.jacorb.orb.factory.FixedAddressSocketFactory;
import org.jacorb.orb.factory.SocketFactory;

import org.jacorb.test.common.NullLogger;

/**
 * @author Alphonse Bendt
 * @version $Id: FixedAddressSocketFactoryTest.java,v 1.7 2009-05-14 16:48:42 andre.spiegel Exp $
 */
public class FixedAddressSocketFactoryTest extends AbstractSocketFactoryTestCase
{
    protected SocketFactory newObjectUnderTest()
    {
        return new FixedAddressSocketFactory();
    }

    public void testSetLocalhost() throws Exception
    {
        Socket socket = objectUnderTest.createSocket(hostname, serverPort);
        checkSocketIsConnected(socket);
    }

    public void testSetLocalhost2() throws Exception
    {
        Socket socket = objectUnderTest.createSocket(hostname, serverPort, 1000);
        checkSocketIsConnected(socket);
    }

    public void testSetHostname() throws Exception
    {
        Socket socket = objectUnderTest.createSocket(hostname, serverPort);
        checkSocketIsConnected(socket);
    }

    public void testSetHostname2() throws Exception
    {
        Socket socket = objectUnderTest.createSocket(hostname, serverPort, 1000);
        checkSocketIsConnected(socket);
    }

    protected void configureObjectUnderTest(String name, Configurable configurable) throws Exception
    {
        MockControl configControl = MockControl
            .createControl(Configuration.class);
        Configuration configMock = (Configuration) configControl.getMock();
        configControl.expectAndReturn(configMock.getLogger("jacorb.orb.socketfactory"), new NullLogger());

        if (name.startsWith("testSetLocalhost"))
        {
            configControl.expectAndReturn(configMock.getAttribute("OAIAddr", ""), "localhost");
        }
        else if (name.startsWith("testSetHostname"))
        {
            configControl.expectAndReturn(configMock.getAttribute("OAIAddr", ""), InetAddress.getLocalHost().getCanonicalHostName());
        }
        else
        {
            configControl.expectAndReturn(configMock.getAttribute("OAIAddr", ""), "");
        }

        configControl.replay();

        configurable.configure(configMock);

        configControl.verify();
    }
}
