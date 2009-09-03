package org.jacorb.test.common;

import org.jacorb.imr.ImplementationRepositoryImpl;

/**
 * starts the ImplementationRepository and prints out its IOR
 * in a format understandable to ClientServerSetup.
 *
 * @author Alphonse Bendt
 * @version $Id: ImplementationRepositoryRunner.java,v 1.2 2009-09-03 12:49:16 alexander.bykov Exp $
 */
public class ImplementationRepositoryRunner
{
    public static void main(String[] args)
    {
        TestServer.startReaperThread();

        ImplementationRepositoryImpl.main(new String[] {"-printIOR"});
    }
}
