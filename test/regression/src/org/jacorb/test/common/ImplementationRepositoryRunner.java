package org.jacorb.test.common;

import org.jacorb.imr.ImplementationRepositoryImpl;

/**
 * starts the ImplementationRepository and prints out its IOR
 * in a format understandable to ClientServerSetup.
 *
 * @author Alphonse Bendt
 * @version $Id: ImplementationRepositoryRunner.java,v 1.1 2006-07-17 10:37:53 alphonse.bendt Exp $
 */
public class ImplementationRepositoryRunner
{
    public static void main(String[] args)
    {
        ImplementationRepositoryImpl.main(new String[] {"-printIOR"});
    }
}
