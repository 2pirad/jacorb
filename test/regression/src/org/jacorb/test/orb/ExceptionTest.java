package org.jacorb.test.orb;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jacorb.test.*;
import org.jacorb.test.common.*;

public class ExceptionTest extends ClientServerTestCase
{
    private ExceptionServer server;

    public ExceptionTest(String name, ClientServerSetup setup)
    {
        super(name, setup);
    }

    public void setUp() throws Exception
    {
        server = ExceptionServerHelper.narrow( setup.getServerObject() );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Client/server exception tests");
        ClientServerSetup setup =
            new ClientServerSetup(suite,
                                  "org.jacorb.test.orb.ExceptionServerImpl");

        suite.addTest(new ExceptionTest("testRuntimeException", setup));
        
        return setup;
    }    
    
    public void testRuntimeException()
    {
        try
        {
            server.throwRuntimeException("sample message");
            fail("should have raised a CORBA SystemException");
        }
        catch (org.omg.CORBA.SystemException ex)
        {
            assertEquals("Server-side Exception: java.lang.RuntimeException: sample message", ex.getMessage());
        }
    }
}
