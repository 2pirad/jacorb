package org.jacorb.test.common;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Utility class used to setup JUnit-TestSuite
 * 
 * @author Alphonse Bendt
 * @version $Id: TestUtils.java,v 1.4 2005-12-04 22:19:27 alphonse.bendt Exp $
 */

public class TestUtils
{
    private static final String[] STRING_ARRAY_TEMPLATE = new String[0];

    private static String testHome = null;

    /**
     * this method returns a List of all public Methods which Names start with the Prefix "test" and
     * accept no Parameters e.g:
     * 
     * <ul>
     * <li>testOperation
     * <li>testSomething
     * </ul>
     * 
     */
    public static String[] getTestMethods(Class clazz)
    {
        return getTestMethods(clazz, "test");
    }

    public static String[] getTestMethods(Class clazz, String prefix)
    {
        Method[] methods = clazz.getMethods();

        List result = new ArrayList();

        for (int x = 0; x < methods.length; ++x)
        {
            if (methods[x].getName().startsWith(prefix))
            {
                if (methods[x].getParameterTypes().length == 0)
                {
                    result.add(methods[x].getName());
                }
            }
        }

        return (String[]) result.toArray(STRING_ARRAY_TEMPLATE);
    }

    /**
     * Returns the name of the home directory of this regression suite.
     */
    public static String testHome()
    {
        if (testHome == null)
        {
            URL url = TestUtils.class.getResource("/.");
            String result = url.toString();
            if (result.matches("file:/.*?/classes/"))
                // strip the leading "file:" and the trailing
                // "/classes/" from the result
                result = result.substring(5, result.length() - 9);
            else
                throw new RuntimeException("cannot find test home");
            testHome = result;
        }
        return testHome;
    }

    public static Test suite(Class testClazz, Class testSetupClazz, String suiteName, String testMethodPrefix)
            throws Exception
    {
        TestSuite suite = new TestSuite(suiteName);

        TestSetup setup = newSetup(suite, testSetupClazz);

        String[] testMethods = getTestMethods(testClazz, testMethodPrefix);

        addToSuite(suite, setup, testClazz, testMethods);

        return setup;
    }

    private static TestSetup newSetup(Test suite, Class testSetupClazz) throws Exception
    {
        Constructor ctor = testSetupClazz.getConstructor(new Class[] { Test.class });
        return (TestSetup) ctor.newInstance(new Object[] { suite });
    }

    private static void addToSuite(TestSuite suite, TestSetup setup, Class clazz,
            String[] testMethods) throws Exception
    {
        Constructor _ctor = clazz.getConstructor(new Class[] { String.class, setup.getClass() });

        for (int x = 0; x < testMethods.length; ++x)
        {
            suite.addTest((Test) _ctor.newInstance(new Object[] { testMethods[x], setup }));
        }
    }
}