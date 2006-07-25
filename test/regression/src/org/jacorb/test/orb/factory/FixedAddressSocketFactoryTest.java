package org.jacorb.test.orb.factory;

import org.jacorb.orb.factory.FixedAddressSocketFactory;
import org.jacorb.orb.factory.SocketFactory;

/**
 * @author Alphonse Bendt
 * @version $Id: FixedAddressSocketFactoryTest.java,v 1.1 2006-07-25 15:43:21 alphonse.bendt Exp $
 */
public class FixedAddressSocketFactoryTest extends AbstractSocketFactoryTest
{
    protected SocketFactory newObjectUnderTest()
    {
        return new FixedAddressSocketFactory();
    }
}
