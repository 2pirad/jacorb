package org.jacorb.test.bugs.bugjac294;

import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.ORB;

import junit.framework.TestCase;

/**
 * @author Alphonse Bendt
 * @version $Id: BugJac294Test.java,v 1.2 2006-07-13 10:43:51 alphonse.bendt Exp $
 */
public class BugJac294Test extends TestCase
{
    public void testStringToObject() throws Exception
    {
        ORB orb = ORB.init(new String[0], null);

        try
        {
            try
            {
                orb.string_to_object("bogus ior");
                fail();
            }
            catch (BAD_PARAM e)
            {
                // expected
                assertEquals(10, e.minor);
            }
        }
        finally
        {
            orb.shutdown(true);
        }
    }
}
