package org.jacorb.test.orb;

import org.jacorb.test.SamplePOA;

/**
 * @author Andre Spiegel
 * @version $Id: SampleImpl.java,v 1.1 2003-04-09 09:12:40 andre.spiegel Exp $
 */
public class SampleImpl extends SamplePOA
{
    public int ping (int data)
    {
        return data+1;
    }
}
