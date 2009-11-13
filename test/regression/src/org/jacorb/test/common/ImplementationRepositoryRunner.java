package org.jacorb.test.common;

import org.jacorb.imr.ImplementationRepositoryImpl;

/**
 * starts the ImplementationRepository and prints out its IOR
 * in a format understandable to ClientServerSetup.
 *
 * @author Alphonse Bendt
 * @version $Id: ImplementationRepositoryRunner.java,v 1.3 2009-11-13 15:31:11 alexander.bykov Exp $
 */
public class ImplementationRepositoryRunner
{
    public static void main(String[] args)
    {
        ImplementationRepositoryImpl.main(new String[] {"-printIOR"});
    }
}
