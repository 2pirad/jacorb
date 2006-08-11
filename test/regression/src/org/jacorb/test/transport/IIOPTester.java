package org.jacorb.test.transport;

import junit.framework.Assert;

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.transport.iiop.Current;
import org.jacorb.transport.iiop.CurrentHelper;
import org.omg.CORBA.ORB;


public class IIOPTester implements AbstractTester {
    public void test_transport_current(ORB orb, Logger logger) {

        try {
            // Get the Current object.
            Object tcobject = orb.resolve_initial_references ("JacOrbIIOPTransportCurrent");

            Current tc = CurrentHelper.narrow (tcobject);
            
            logger.info("TC: ["+tc.id()+"] from="+tc.local_host() +":"+tc.local_port() +", to="
                        +tc.remote_host()+":"+tc.remote_port());

            logger.info("TC: ["+tc.id()+"] sent="+tc.messages_sent ()+"("+tc.bytes_sent ()+")"
                        +", received="+tc.messages_received ()+"("+tc.bytes_received ()+")");
        }
        catch (Exception ex) {
            ex.printStackTrace ();
            Assert.fail ("Unexpected exception" + ex);
        }
    }


}
