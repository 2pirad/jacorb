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
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jacorb.test.common.StreamListener;
import org.jacorb.test.common.TestUtils;

public class AbstractIDLTestcase extends TestCase
{
    protected final File idlFile;

    /**
     * dir where the idl compiler places generated
     * java source files.
     */
    protected final File dirGeneration;

    /**
     * dir where .class are compiled to.
     */
    protected final File dirCompilation;

    private final String testName;

    public AbstractIDLTestcase(String name, File file)
    {
        // sets testname to a more informative value.
        // this would break JUnit as the testname
        // is used to look up the testmethod to run.
        // for this reason the runTest() method
        // is overriden locally to get back
        // the standard behaviour.
        super(name + ": " + file.getName());

        assertTrue(file.exists());
        assertTrue(file.isFile());

        testName = name;
        idlFile = file;

        dirGeneration = new File(TestUtils.testHome() + "/src-testidl/" + idlFile.getName());
        deleteRecursively(dirGeneration);
        File dirClasses = new File(TestUtils.testHome() + "/classes-testidl");

        dirClasses.mkdir();
        assertTrue(dirClasses.canWrite());
        assertTrue(dirClasses.isDirectory());

        dirCompilation = new File(dirClasses, idlFile.getName());
        deleteRecursively(dirCompilation);
    }

    /**
     * run JacORBs IDL compiler on the current idlFile
     */
    protected void runJacIDL(boolean failureExpected, boolean spawnProcess) throws Exception
    {
        final String details;

        if (spawnProcess)
        {
            details = runJacIDLExtraProcess(failureExpected);
        }
        else
        {
            details = runJacIDLInProcess(failureExpected);
        }

        TestUtils.log("[" + idlFile.getName() + " output]:\n" + details);
    }

    private String runJacIDLInProcess(boolean failureExpected) throws AssertionFailedError
    {
        String[] file = createJacIDLArgs();

        dirGeneration.mkdir();

        StringWriter writer = new StringWriter();
        final String details = writer.toString();
        try
        {
            org.jacorb.idl.parser.compile(file, writer);

            if (failureExpected)
            {
                fail("parsing of " + idlFile.getName() + " should fail.");
            }
        }
        catch (Exception e)
        {
            handleJacIDLFailed(failureExpected, details, e);
        }

        return details;
    }

    private String runJacIDLExtraProcess(boolean failureExpected) throws Exception
    {
        List args = new ArrayList();
        args.add(TestUtils.testHome() + "/../../bin/idl");
        args.addAll(Arrays.asList(createJacIDLArgs()));

        TestUtils.log("Running: " + args);

        Process process = Runtime.getRuntime().exec((String[])args.toArray(new String[args.size()]));

        StreamListener outListener = new StreamListener (process.getInputStream(), "OUT");
        StreamListener errListener = new StreamListener (process.getErrorStream(), "ERR");
        String details = "STDOUT\n" + outListener.getBuffer() + "\nSTDERR:\n" + errListener.getBuffer() + "\n";

        try
        {
            outListener.start();
            errListener.start();
            process.waitFor();

            boolean success = process.exitValue() == 0;

            if (failureExpected)
            {
                assertFalse(success);
            }
            else
            {
                assertTrue(success);
            }
        }
        catch (Exception e)
        {
            handleJacIDLFailed(failureExpected, details, e);
        }

        return details;
    }

    private void handleJacIDLFailed(boolean failureExpected, final String details, Exception e) throws AssertionFailedError
    {
        if (!failureExpected)
        {
            AssertionFailedError error = new AssertionFailedError("parsing of " + idlFile.getName()
                    + " failed: " + details);
            error.initCause(e);
            throw error;
        }
    }

    /**
     * compile all .java files that were
     * generated by a previous IDL run.
     * this method depends on a properly configured
     * environment that has javac available.
     * @return a ClassLoader that can be used to access the compiled classes
     */
    protected ClassLoader compileGeneratedSources(boolean failureExpected) throws IOException
    {
        File[] files = getJavaFiles();
        assertNotNull(files);

        if (files.length == 0)
        {
            return null;
        }

        dirCompilation.mkdir();

        assertTrue(dirCompilation.isDirectory());
        assertTrue(dirCompilation.exists());
        assertTrue(dirCompilation.canWrite());
        File file = new File(dirCompilation, "files.txt");
        file.delete();
        file.createNewFile();

        PrintWriter writer = new PrintWriter(new FileWriter(file));

        for (int i = 0; i < files.length; ++i)
        {
            writer.println(files[i].getAbsolutePath());
        }

        writer.close();

        String javaHome = System.getProperty("java.home");
        String testHome = TestUtils.testHome();
        String classpath = testHome + File.separator + ".." + File.separator + ".." + File.separator + "classes";

        if (javaHome.endsWith("jre"))
        {
            javaHome = javaHome.substring(0, javaHome.length() - 4);
        }
        String cmd = javaHome + "/bin/javac -d " + dirCompilation + " -classpath " + classpath + " @" + file.getAbsolutePath();
        try
        {
            Process proc = Runtime.getRuntime().exec(cmd);

            int exit = proc.waitFor();
            if (failureExpected && exit == 0)
            {
                fail("should fail: " + cmd);
            }

            if (exit != 0)
            {
                InputStream in = proc.getErrorStream();
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
                StringBuffer b = new StringBuffer();

                String line;
                while ((line = reader.readLine()) != null)
                {
                    b.append(line);
                    b.append("\n");
                }

                fail(cmd + "\n" + b.toString());
            }

            return new URLClassLoader(new URL[] {dirCompilation.toURL()});
        }
        catch (Exception e)
        {
            if (!failureExpected)
            {
                AssertionFailedError error = new AssertionFailedError("cmd: " + cmd);
                error.initCause(e);
                throw error;
            }
            return null;
        }
    }

    /**
     * get a list of all .java files that were
     * generated by a previous IDL run.
     */
    protected File[] getJavaFiles()
    {
        return (File[]) getJavaFilesRecursively(dirGeneration).toArray(new File[0]);
    }

    private List getJavaFilesRecursively(File src)
    {
        List result = new ArrayList();
        result.addAll(getJavaFilesInDirectory(src));

        File[] dirs = getSubDirectories(src);

        for (int i = 0; i < dirs.length; ++i)
        {
            result.addAll(getJavaFilesRecursively(dirs[i]));
        }

        return result;
    }

    private File[] getSubDirectories(File src)
    {
        return src.listFiles(new FileFilter()
        {
            public boolean accept(File pathname)
            {
                return pathname.isDirectory();
            }
        });
    }

    private List getJavaFilesInDirectory(File src)
    {
        final File[] fileList = src.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".java");
            }
        });
        assertNotNull(src + " does not exist.", fileList);
        return Arrays.asList(fileList);
    }

    /**
     * build the argument list that will be used to invoke the
     * IDL compiler.
     */
    protected String[] createJacIDLArgs()
    {
        String file[] = new String[] { "-forceOverwrite", "-d", dirGeneration.getAbsolutePath(),
                idlFile.getAbsolutePath() };
        return file;
    }

    protected void runTest() throws Throwable
    {
        assertNotNull(testName);
        Method runMethod = null;
        try
        {
            runMethod = getClass().getMethod(testName, new Class[0]);
        }
        catch (NoSuchMethodException e)
        {
            fail("Method \"" + testName + "\" not found");
        }
        if (!Modifier.isPublic(runMethod.getModifiers()))
        {
            fail("Method \"" + testName + "\" should be public");
        }

        try
        {
            runMethod.invoke(this, new Object[0]);
        }
        catch (InvocationTargetException e)
        {
            e.fillInStackTrace();
            throw e.getTargetException();
        }
        catch (IllegalAccessException e)
        {
            e.fillInStackTrace();
            throw e;
        }
    }

    /**
     * create a test suite for all idl files located in
     * the supplied directory.
     *
     * @param srcDir directory that contains the .idl files
     * @param testClazz must supply a constructor
     * that accepts a file argument.
     */
    protected static Test suite(String srcDir, Class testClazz)
    {
        return suite(srcDir, testClazz, ".idl");
    }

    /**
     * create a test suite for all idl files which names are
     * ending with the specified suffix and which are located in
     * the supplied directory.
     *
     * @param srcDir directory that contains the .idl files.
     * @param testClazz must supply a constructor that accepts a file argument.
     * @param suffix should match all IDL files that should be tested.
     */
    protected static Test suite(final String srcDir, final Class testClazz, final String suffix)
    {
        TestSuite suite = new TestSuite();

        File file = new File(srcDir);
        File[] files = file.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(suffix);
            }
        });

        try
        {
            Constructor ctor = testClazz.getConstructor(new Class[] { File.class });

            for (int i = 0; i < files.length; ++i)
            {
                suite.addTest((Test) ctor.newInstance(new Object[] { files[i] }));
            }

            return suite;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void deleteRecursively(File name)
    {
        if (name.isDirectory())
        {
            File[] subdirs = name.listFiles(new FileFilter()
            {
                public boolean accept(File pathname)
                {
                    return pathname.isDirectory();
                }
            });

            for (int i = 0; i < subdirs.length; ++i)
            {
                deleteRecursively(subdirs[i]);
            }

            File[] files = name.listFiles(new FileFilter()
            {
                public boolean accept(File pathname)
                {
                    return pathname.isFile();
                }
            });

            for (int i = 0; i < files.length; ++i)
            {
                files[i].delete();
            }

            name.delete();
        }

        if (name.isFile())
        {
            name.delete();
        }
    }

    /**
     * search for a method with the signature
     * public void verify_<FILENAME>(ClassLoader cl) {...}
     * (dots in filename will be converted to _) and invoke
     * it with the specified classloader.
     */
    protected void invokeVerifyMethod(ClassLoader cl) throws IllegalAccessException, InvocationTargetException
    {
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
    }
}
