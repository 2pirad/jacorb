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
import org.jacorb.notification.interfaces.Message;
import org.jacorb.notification.parser.TCLParser;

import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;
import org.apache.log.Hierarchy;

/**
 *
 *
 * Created: Thu Apr 10 12:08:42 2003
 *
 * @author Alphonse Bendt
 * @version $Id: EventNameShorthandNode.java,v 1.4 2003-09-12 09:31:42 alphonse.bendt Exp $
 */

public class EventNameShorthandNode extends ComponentName {

    static AbstractTCLNode expandedPath_;
    static final String COMP_NAME = "$.header.fixed_header.event_name";
    public static final String SHORT_NAME = "event_name";

    static {
        try {
            expandedPath_ = TCLParser.parse( COMP_NAME );
            expandedPath_.acceptInOrder( new TCLCleanUp() );
        } catch (Exception e) {
            Hierarchy
                .getDefaultHierarchy()
                .getLoggerFor(EventNameShorthandNode.class.getName())
                .fatalError("No exception should ever occur at this point", e);
        }
    }

    public EventNameShorthandNode() {
        setName("EventNameShorthandNode");
    }

    public String getComponentName() {
        return COMP_NAME;
    }

    public void acceptInOrder(AbstractTCLVisitor v) {

    }

    public void acceptPostOrder(AbstractTCLVisitor v) {

    }

    public void acceptPreOrder(AbstractTCLVisitor v) {

    }

    public EvaluationResult evaluate( EvaluationContext context )

        throws DynamicTypeException,
               EvaluationException {

        logger_.debug("evaluate");

        Message _event = context.getNotificationEvent();
        EvaluationResult _result = new EvaluationResult();

        switch (_event.getType()) {
        case Message.TYPE_ANY:
            _result = expandedPath_.evaluate(context);
            break;
        case Message.TYPE_STRUCTURED:
            String _domainName = _event.toStructuredEvent().header.fixed_header.event_name;
            logger_.debug("Got result: " + _domainName);
            _result.setString(_domainName);
            break;
        default:
            throw new RuntimeException();
        }

        return _result;
    }

    public String toString() {
        return COMP_NAME;
    }

}
