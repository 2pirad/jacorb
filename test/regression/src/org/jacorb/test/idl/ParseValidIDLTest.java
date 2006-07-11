/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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

package org.jacorb.test.idl;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jacorb.test.common.TestUtils;

/**
 * this test will try to process and compile all idl files included
 * in the directory <TEST_HOME>/idl/compiler/succeed.
 * this test assumes the idl files to be correct and
 * will fail if JacIDL or javac causes an error during processing.
 *
 * additionally if you'd like to verify the compiled classes
 * you can optionally define a method that must adhere to the following
 * signature: public void verify_<FILENAME>(ClassLoader cl) {...}
 * (dots in filename will be converted to _). inside the method you can then
 * load and inspect the compiled classes.
 *
 * @author Alphonse Bendt
 * @version $Id: ParseValidIDLTest.java,v 1.8 2006-07-11 10:34:18 alphonse.bendt Exp $
 */
public class ParseValidIDLTest extends AbstractIDLTestcase
{
    public ParseValidIDLTest(File file)
    {
        super("testCanParseValidIDL", file);
    }

    /**
     * this is the main test method. it will be invoked
     * for every .idl file that is found in the source directory
     */
    public void testCanParseValidIDL() throws Exception
    {
        runJacIDL(false);
        ClassLoader cl = compileGeneratedSources(false);

        invokeVerifyMethod(cl);

        // if a test fails the directory
        // will not be deleted. this way
        // the contents can be inspected.
        deleteRecursively(dirCompilation);
        deleteRecursively(dirGeneration);
    }


    /**
     * related to RT#1445. forward declarations in idl led
     * to incorrectly generated classes.
     */
    public void verify_rt1445_idl(ClassLoader cl) throws Exception
    {
        Class nodeClazz = cl.loadClass("tree.Node");

        nodeClazz.getDeclaredField("name");
        nodeClazz.getDeclaredField("description");
        nodeClazz.getDeclaredField("children");
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();

        final String dir = TestUtils.testHome() + "/idl/compiler/succeed";

        // TODO this test currently fails during the nightly build.
        // by adding it here explicitely we ensure that it is run
        // first in this suite. if the problem does not occur anymore
        // this comment and the following line may be removed (alphonse)
        suite.addTest(suite(dir, ParseValidIDLTest.class, "basetypes.idl"));

        suite.addTest(suite(dir, ParseValidIDLTest.class));

        return suite;
    }
}
