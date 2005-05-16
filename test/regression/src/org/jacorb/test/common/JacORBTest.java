package org.jacorb.test.common;

/**
 * @author Andre Spiegel spiegel@gnu.org
 * @version $Id: JacORBTest.java,v 1.1 2005-05-16 11:36:00 andre.spiegel Exp $
 */
public interface JacORBTest
{
    public boolean isApplicableTo (String clientVersion, String serverVersion);
}
