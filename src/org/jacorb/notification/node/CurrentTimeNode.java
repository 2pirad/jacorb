package org.jacorb.notification.node;

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

import org.jacorb.notification.EvaluationContext;
import org.jacorb.notification.evaluate.EvaluationException;
import org.jacorb.util.Time;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.TimeBase.UtcT;
import org.omg.TimeBase.UtcTHelper;

/**
 * CurrentTimeNode.java
 *
 *
 * Created: Tue Apr 29 11:55:47 2003
 *
 * @author Alphonse Bendt
 * @version $Id: CurrentTimeNode.java,v 1.1 2003-06-05 13:04:09 alphonse.bendt Exp $
 */

public class CurrentTimeNode extends ComponentName {
    
    public static final String SHORT_NAME = "curtime";
    static final String COMP_NAME = "$curtime";

    ORB orb_;

    public CurrentTimeNode(ORB orb) {
	orb_ = orb;
    }
    
    public EvaluationResult evaluate( EvaluationContext context )
	throws DynamicTypeException,
	       EvaluationException {

	EvaluationResult _result = new EvaluationResult();
	
	UtcT _curtime = Time.corbaTime();

	Any _curAny = orb_.create_any();

	UtcTHelper.insert(_curAny, _curtime);

	_result.addAny(_curAny);

	return _result;
    }

    public String toString() {
	return COMP_NAME;
    }

} // CurrentTimeNode
