package org.jacorb.test.notification;

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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jacorb.notification.CollectionsWrapper;
import org.jacorb.notification.JacORBCollections;
import org.jacorb.notification.JDK13Collections;
import java.util.List;
import java.util.Iterator;

/**
 * @author Alphonse Bendt
 * @version $Id: CollectionsWrapperTest.java,v 1.1 2003-07-16 00:09:09 alphonse.bendt Exp $
 */

public class CollectionsWrapperTest extends TestCase 
{

    public void testCollectionsWrapper() throws Exception {
	String o = "testling";

	List list = CollectionsWrapper.singletonList(o);

	assertTrue(list.size() == 1);

	assertEquals(o, list.get(0));

	Iterator i = list.iterator();

	while (i.hasNext()) {
	    assertEquals(o, i.next());
	}
    }

    public void testJacORBCollections() throws Exception {
	String o = "testling";

	JacORBCollections collections = new JacORBCollections();
	
	List list = collections.singletonList(o);

	assertTrue(list.size() == 1);

	assertEquals(o, list.get(0));

	Iterator i = list.iterator();

	while (i.hasNext()) {
	    assertEquals(o, i.next());
	}
    }

    public void testJDKCollections() throws Exception {
	String o = "testling";

	JDK13Collections collections = new JDK13Collections();
	
	List list = collections.singletonList(o);

	assertTrue(list.size() == 1);

	assertEquals(o, list.get(0));

	Iterator i = list.iterator();

	while (i.hasNext()) {
	    assertEquals(o, i.next());
	}
    }

    /** 
     *
     * @param name test name
     */
    public CollectionsWrapperTest (String name)
    {
	super(name);
    }

    /**
     * @return a <code>TestSuite</code>
     */
    public static Test suite()
    {
	TestSuite suite = new TestSuite(CollectionsWrapperTest.class);
	
	return suite;
    }

    /** 
     * Entry point 
     */ 
    public static void main(String[] args) 
    {
	junit.textui.TestRunner.run(suite());
    
    }
}
