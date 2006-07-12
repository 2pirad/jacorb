package org.jacorb.test.bugs.bugjac330;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Alphonse Bendt
 * @version $Id: BugJac330Suite.java,v 1.1 2006-07-12 11:34:43 alphonse.bendt Exp $
 */
public class BugJac330Suite
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Tests for Bug JAC 330");

        suite.addTestSuite(MultipleServerTest.class);
        suite.addTest(BugJac330Test.suite());

        return suite;
    }
}
