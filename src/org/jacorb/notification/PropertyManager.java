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

import java.util.Map;
import org.omg.CORBA.Any;
import org.omg.CosNotification.Property;
import java.util.Iterator;
import java.util.Hashtable;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynAny;
import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;
import org.apache.log.Logger;
import org.apache.log.Hierarchy;

/**
 * PropertyManager.java
 *
 *
 * Created: Tue Feb 04 14:47:50 2003
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: PropertyManager.java,v 1.1 2003-04-12 21:04:54 alphonse.bendt Exp $
 */

public class PropertyManager implements Cloneable {

    Logger logger_ = 
	Hierarchy.getDefaultHierarchy().getLoggerFor(getClass().getName());

    private boolean dirty_ = true;
    private Property[] asArray_;

    PropertyManager defaultManager_;
    Map properties_;
    DynAnyFactory dynAnyFactory_;
    ApplicationContext appContext_;

    public PropertyManager(ApplicationContext appContext) {
	this(appContext, new Hashtable());
    }

    public PropertyManager(ApplicationContext appContext, Map props) {
	properties_ = props;
	appContext_ = appContext;
	dynAnyFactory_ = appContext.getDynAnyFactory();
    }

    public void setDefaultManager(PropertyManager parent) {
	defaultManager_ = parent;
    }

    public void setProperty(String name, Any value) {
	synchronized(this) {
	    properties_.put(name, value);
	    dirty_ = true;
	}
    }

    public Any getProperty(String name) {
	if (properties_.containsKey(name)) {
	    return (Any)properties_.get(name); 
	} else if (defaultManager_ != null) {
	    return defaultManager_.getProperty(name);
	} else {
	    return null;
	}
    }

    public Property[] toArray() {
	if (dirty_) {
	    synchronized(properties_) {
		if (dirty_) {
		    asArray_ = new Property[properties_.size()];
		    
		    Iterator _i = properties_.keySet().iterator();
		    int x = 0;
		    while (_i.hasNext()) {
			String _key = (String)_i.next();
			asArray_[x++] = new Property(_key, (Any)properties_.get(_key));
		    }
		    dirty_ = false;
		}
	    }
	}
	return asArray_;
    }

    public Object clone() {
	try {
	    PropertyManager _r = new PropertyManager(appContext_);
	    
	    Property[] _arr;
	    if (defaultManager_ != null) {
		_arr = defaultManager_.toArray();

		for (int x=0; x<_arr.length; ++x) {
		    Any _orig = _arr[x].value;
		    DynAny _dynAny = dynAnyFactory_.create_dyn_any(_orig);
		    DynAny _dynCopy = _dynAny.copy();
		    Any _copy = _dynCopy.to_any();
		    _r.setProperty(_arr[x].name, _copy);
		}
	    }

	    _arr = toArray();
	    for (int x=0; x<_arr.length; ++x) {
		Any _orig = _arr[x].value;
		DynAny _dynAny = dynAnyFactory_.create_dyn_any(_orig);
		DynAny _dynCopy = _dynAny.copy();
		Any _copy = _dynCopy.to_any();
		_r.setProperty(_arr[x].name, _copy);
	    }
	    
	    return _r;
	} catch (InconsistentTypeCode e) {
	    throw new RuntimeException();
	}
    }

    public String toString() {
	return "PropertyManager//" + properties_.toString();
    }

}// PropertyManager
