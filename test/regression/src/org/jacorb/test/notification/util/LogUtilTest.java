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

package org.jacorb.test.notification.util;

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.notification.util.LogUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Alphonse Bendt
 * @version $Id: LogUtilTest.java,v 1.1 2005-02-14 00:17:38 alphonse.bendt Exp $
 */
public class LogUtilTest extends TestCase
{
    /**
     * Constructor for LogUtilTest.
     * 
     * @param name
     */
    public LogUtilTest(String name)
    {
        super(name);
    }

    public void testGetLogger()
    {
        Logger _logger = LogUtil.getLogger("name");
        assertNotNull(_logger);
    }

    /**
     * @return
     */
    public static Test suite()
    {
        return new TestSuite(LogUtilTest.class);
    }
}