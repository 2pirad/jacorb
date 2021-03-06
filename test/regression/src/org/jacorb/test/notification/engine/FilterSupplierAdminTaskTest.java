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

package org.jacorb.test.notification.engine;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jacorb.notification.engine.AbstractFilterTask;
import org.jacorb.notification.engine.FilterSupplierAdminTask;

/**
 * @author Alphonse Bendt
 * @version $Id: FilterSupplierAdminTaskTest.java,v 1.3 2006-02-25 14:21:30 alphonse.bendt Exp $
 */
public class FilterSupplierAdminTaskTest extends AbstractFilterTaskTestCase
{
    protected AbstractFilterTask newObjectUnderTest()
    {
        return new FilterSupplierAdminTask(mockTaskFactory_, mockTaskExecutor_);
    }

    public static Test suite()
    {
        return new TestSuite(FilterSupplierAdminTaskTest.class);
    }
}