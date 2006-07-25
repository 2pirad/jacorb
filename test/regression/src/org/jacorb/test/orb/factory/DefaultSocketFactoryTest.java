package org.jacorb.test.orb.factory;

import org.jacorb.orb.factory.DefaultSocketFactory;
import org.jacorb.orb.factory.SocketFactory;

/**
 * @author Alphonse Bendt
 * @version $Id: DefaultSocketFactoryTest.java,v 1.1 2006-07-25 15:43:21 alphonse.bendt Exp $
 */
public class DefaultSocketFactoryTest extends AbstractSocketFactoryTest
{
    protected SocketFactory newObjectUnderTest()
    {
        return new DefaultSocketFactory();
    }
}
