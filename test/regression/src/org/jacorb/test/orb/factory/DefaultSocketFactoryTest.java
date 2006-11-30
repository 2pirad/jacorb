package org.jacorb.test.orb.factory;

import org.jacorb.orb.factory.DefaultSocketFactory;
import org.jacorb.orb.factory.SocketFactory;

/**
 * @author Alphonse Bendt
 * @version $Id: DefaultSocketFactoryTest.java,v 1.2 2006-11-30 13:40:35 alphonse.bendt Exp $
 */
public class DefaultSocketFactoryTest extends AbstractSocketFactoryTestCase
{
    protected SocketFactory newObjectUnderTest()
    {
        return new DefaultSocketFactory();
    }
}
