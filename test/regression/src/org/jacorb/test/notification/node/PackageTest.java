package org.jacorb.test.notification.node;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2003 Gerald Brose
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * PackageTest.java
 *
 *
 *
 * @author Alphonse Bendt
 * @version $Id: PackageTest.java,v 1.2 2003-08-25 21:00:46 alphonse.bendt Exp $
 */

public class PackageTest extends TestCase
{

    public PackageTest( String name )
    {
        super( name );
    }

    public static Test suite() throws Exception
    {
        TestSuite _suite = new TestSuite( "Tests in Package org.jacorb.test.notification.node" );

        _suite.addTest( EvaluationResultTest.suite() );
        _suite.addTest( TCLTest.suite() );

        return _suite;
    }

    public static void main( String[] args ) throws Exception
    {
        junit.textui.TestRunner.run( suite() );
    }
}
