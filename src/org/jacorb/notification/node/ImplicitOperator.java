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

import org.omg.CORBA.Any;
import org.jacorb.notification.evaluate.DynamicEvaluator;
import org.jacorb.notification.evaluate.EvaluationException;
import org.jacorb.notification.EvaluationContext;

/**
 * Operator.java
 *
 *
 * Created: Thu Oct 24 11:31:29 2002
 *
 * @author Alphonse Bendt
 * @version $Id: ImplicitOperator.java,v 1.5 2003-06-05 13:04:08 alphonse.bendt Exp $
 */

public interface ImplicitOperator {
    Any evaluateImplicit(EvaluationContext context, Any value) throws EvaluationException;
}
