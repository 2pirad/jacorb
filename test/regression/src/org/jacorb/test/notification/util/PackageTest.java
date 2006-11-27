package org.jacorb.test.notification.util;

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

import org.jacorb.test.notification.util.regexp.JDK14PatternWrapperTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * PackageTest.java
 *
 * @author Alphonse Bendt
 * @version $Id: PackageTest.java,v 1.6 2006-11-27 14:45:19 alphonse.bendt Exp $
 */

public class PackageTest extends TestCase
{
    public PackageTest(String name)
    {
        super(name);
    }

    public static Test suite() throws Exception
    {
        TestSuite _suite = new TestSuite("Tests in Package org.jacorb.test.notification.util");

        _suite.addTest(WeakCacheWildcardMapTest.suite());
        _suite.addTest(DefaultWildcardMapTest.suite());
        _suite.addTest(CachingWildcardMapTest.suite());
        _suite.addTest(ThreadPoolTest.suite());
        _suite.addTest(ObjectPoolTest.suite());
        _suite.addTest(EventTypeUtilTest.suite());
        _suite.addTestSuite(WeakHashSetTest.class);
        _suite.addTest(LogUtilTest.suite());
        _suite.addTest(DisposableManagerTest.suite());
        _suite.addTest(JDK14PatternWrapperTest.suite());

        return _suite;
    }
}