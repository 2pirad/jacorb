package org.jacorb.test.transport;

import junit.framework.Assert;

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.transport.Current;
import org.jacorb.transport.CurrentHelper;
import org.omg.CORBA.ORB;


public class DefaultTester implements AbstractTester {
    
   /* (non-Javadoc)
     * @see org.jacorb.test.transport.AbstractTester#test_transport_current(org.omg.CORBA.ORB, org.apache.avalon.framework.logger.Logger)
     */
    public void test_transport_current(ORB orb, Logger logger) {
    
        try {
            // Get the Current object.
            Object tcobject = orb.resolve_initial_references ("JacOrbTransportCurrent");
    
            Current tc = CurrentHelper.narrow (tcobject);
    
            logger.info ("TC: [" + tc.id () + "] sent="
                            + tc.messages_sent () + "(" + tc.bytes_sent ()
                            + ")" + ", received=" + tc.messages_received ()
                            + "(" + tc.bytes_received () + ")");
        }
        catch (Exception ex) {
            ex.printStackTrace ();
            Assert.fail ("Unexpected exception" + ex);
        }
    }

}
