package org.jacorb.test.orb.etf;

import org.jacorb.test.common.ClientServerTestCase;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
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
 */

import java.util.*;

import junit.framework.*;

import org.jacorb.test.common.*;
import org.jacorb.test.*;
import org.jacorb.test.orb.etf.wiop.WIOPFactories;

/**
 * @author <a href="mailto:spiegel@gnu.org">Andre Spiegel</a>
 * @version $Id: C_WIOP_S_WIOP.java,v 1.2 2003-08-02 11:41:37 alphonse.bendt Exp $
 */
public class C_WIOP_S_WIOP extends ClientServerTestCase
{
    private BasicServer server = null;
    
    public C_WIOP_S_WIOP (String name, ClientServerSetup setup)
    {
        super (name, setup);
    }
    
    public void setUp() throws Exception
    {
        server = BasicServerHelper.narrow( setup.getServerObject() );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite ("Client: WIOP, Server: WIOP");
        
        Properties props = new Properties();
        props.setProperty("jacorb.transport.factories",
                          "org.jacorb.test.orb.etf.wiop.WIOPFactories");
        
        ClientServerSetup setup = 
          new ClientServerSetup (suite,
                                 "org.jacorb.test.orb.BasicServerImpl",
                                 props, props);
        
        suite.addTest (new C_WIOP_S_WIOP ("testConnection", setup));
        
        return setup;
    }

    public void testConnection()
    {
        server.ping();
        assertTrue (WIOPFactories.transportInUse);
    }

}
