package org.jacorb.notification;

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

import org.omg.CosNotifyFilter.FilterFactoryPOA;
import org.omg.CosNotifyFilter.Filter;
import org.omg.CosNotifyFilter.InvalidGrammar;
import org.omg.CosNotifyFilter.MappingFilter;
import org.omg.CORBA.Any;
import org.omg.PortableServer.POA;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.jacorb.notification.interfaces.Disposable;
import org.omg.PortableServer.POAHelper;
import org.apache.log.Priority;
import java.io.IOException;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

/**
 * FilterFactoryImpl.java
 *
 *
 * Created: Sat Oct 12 17:25:43 2002
 *
 * @author Alphonse Bendt
 * @version $Id: FilterFactoryImpl.java,v 1.6 2003-06-05 13:04:09 alphonse.bendt Exp $
 */

public class FilterFactoryImpl extends FilterFactoryPOA implements Disposable {

    public static String CONSTRAINT_GRAMMAR = "EXTENDED_TCL";

    protected ApplicationContext applicationContext_;

    public FilterFactoryImpl() throws InvalidName, IOException, AdapterInactive {
	super();

	//	EventChannelFactoryImpl.setLogFile("filterImpl.log", "org.jacorb.notification", Priority.DEBUG);

	final ORB _orb = ORB.init(new String[0], null);
	POA _poa = POAHelper.narrow(_orb.resolve_initial_references("RootPOA"));
	applicationContext_ = new ApplicationContext(_orb, _poa, true);

	_this(_orb);

	_poa.the_POAManager().activate();

	Thread t = new Thread(new Runnable() {
		public void run() {
		    _orb.run();
		}
	    });
	t.setDaemon(true);
	t.start();
    }

    public FilterFactoryImpl(ApplicationContext applicationContext) throws InvalidName {
	super();

	applicationContext_ = applicationContext;
    }

    public Filter create_filter(String grammar) throws InvalidGrammar {
	if (CONSTRAINT_GRAMMAR.equals(grammar)) {
	    Filter _filter;

	    FilterImpl _filterServant = new FilterImpl(CONSTRAINT_GRAMMAR, 
						       applicationContext_);

	    _filterServant.init();
	    _filter = _filterServant._this(applicationContext_.getOrb());
	    
	    return _filter;
	}
	throw new InvalidGrammar();
    }

    public MappingFilter create_mapping_filter(String grammar, 
					       Any any) throws InvalidGrammar {
	return null;
    }

    public void dispose() {
    }

}
