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
import java.lang.reflect.Method;

import junit.framework.Test;

import org.jacorb.test.common.TestUtils;

/**
 * this test will try to process all idl files included
 * in the directory <TEST_HOME>/idl/compiler/succeed.
 * this test assumes the idl files to be correct and
 * will fail if JacIDL causes an error during processing.
 *
 * additionally if you'd like to verify the compiled classes
 * you can optionally define a method that must adhere to the following
 * signature: public void verify_<FILENAME>(ClassLoader cl) {...}
 * (dots in filename will be converted to _). inside the method you can then
 * load and inspect the compiled classes.
 *
 * @author Alphonse Bendt
 * @version $Id: ParseValidIDLTest.java,v 1.5 2006-05-17 13:23:37 alphonse.bendt Exp $
 */
public class ParseValidIDLTest extends AbstractIDLTestcase
{
    public ParseValidIDLTest(File file)
    {
        super("testCanParseValidIDL", file);
    }

    public void testCanParseValidIDL() throws Exception
    {
        runJacIDL(false);
        ClassLoader cl = compileGeneratedSources(false);

        try
        {
            // test if a verify_ method is available and invoke it
            String file = idlFile.getName().replaceAll("\\.", "_");
            Method method = getClass().getMethod("verify_" + file, new Class[] {ClassLoader.class});
            method.invoke(this, new Object[] {cl});
        }
        catch (NoSuchMethodException e)
        {
            // ignored
        }

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
        final String dir = TestUtils.testHome() + "/idl/compiler/succeed";
        return suite(dir, ParseValidIDLTest.class);
    }
}
