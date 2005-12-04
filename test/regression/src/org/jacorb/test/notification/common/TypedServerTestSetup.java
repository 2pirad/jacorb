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

package org.jacorb.test.notification.common;

import java.util.Properties;

import junit.framework.Test;

import org.jacorb.test.common.ClientServerSetup;

/**
 * setup class for TypedEventChannel integration tests.
 * 
 * @author Alphonse Bendt
 * @version $Id: TypedServerTestSetup.java,v 1.1 2005-12-04 22:19:27 alphonse.bendt Exp $
 */
public class TypedServerTestSetup extends ClientServerSetup
{
    private final static String IGNORED = "ignored";

    public TypedServerTestSetup(Test test)
    {
        super(test, IGNORED);
    }

    public TypedServerTestSetup(Test test, Properties clientOrbProperties,
            Properties serverOrbProperties)
    {
        super(test, IGNORED, clientOrbProperties, serverOrbProperties);
    }

    public String getTestServerMain()
    {
        return TypedServerTestRunner.class.getName();
    }
}
