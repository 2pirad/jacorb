package org.jacorb.notification.filter.etcl;

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

import org.jacorb.notification.filter.EvaluationContext;
import org.jacorb.notification.filter.EvaluationException;
import org.jacorb.notification.filter.EvaluationResult;
import org.jacorb.notification.filter.ParseException;
import org.jacorb.notification.interfaces.Message;

/**
 * @author Alphonse Bendt
 * @version $Id: PropertyShorthandNode.java,v 1.5 2004-06-03 23:11:55 alphonse.bendt Exp $
 */

public class PropertyShorthandNode extends AbstractTCLNode
{
    String value_;

    ETCLComponentName shorthandVariableHeader_;

    ETCLComponentName shorthandFilterableData_;

    ETCLComponentName shorthandDefault_;

    ETCLComponentName shorthandDefaultAny_;

    ////////////////////////////////////////

    public PropertyShorthandNode(String value)
    {
        try {
            value_ = value;

            shorthandVariableHeader_ =
                (ETCLComponentName)TCLParser.parse("$.header.variable_header(" + value + ")");

            shorthandVariableHeader_.acceptInOrder(new TCLCleanUp());

            shorthandFilterableData_ =
                (ETCLComponentName)TCLParser.parse("$.filterable_data(" + value + ")");

            shorthandFilterableData_.acceptInOrder(new TCLCleanUp());

            shorthandDefault_ = (ETCLComponentName)TCLParser.parse("$." + value );
            shorthandDefault_.acceptInOrder(new TCLCleanUp());

            shorthandDefaultAny_ = (ETCLComponentName)TCLParser.parse("$(" + value + ")");
            shorthandDefaultAny_.acceptInOrder(new TCLCleanUp());

        } catch (ParseException e) {
            throw new RuntimeException();
        } catch (VisitorException e) {
            throw new RuntimeException();
        }

    }


    public EvaluationResult evaluate(EvaluationContext context) throws EvaluationException {

        Message _event = context.getCurrentMessage();
        EvaluationResult _res = null;

        try {
            _res = _event.extractVariableHeader(context,
                                                shorthandVariableHeader_,
                                                value_);

        } catch (EvaluationException e) {
            // can be safely ignored
            // three more methods will be tried ...
        }

        if (_res == null) {
            try {
                _res = _event.extractFilterableData(context,
                                                    shorthandFilterableData_,
                                                    value_);
            } catch (EvaluationException e) {
                // can be safely ignored
                // two more methods will be tried ...
            }

            if (_res == null) {
                _res = extractDefaultValue(context);
            }

            if (_res == null) {
                _res = extractDefaultAnyValue(context);
            }

            if (_res == null) {
                throw new EvaluationException("the shorthand property $" + value_ + " does not exist");
            }
        }

        return _res;
    }


    public EvaluationResult extractDefaultValue(EvaluationContext context) {
        try {
            return context.getCurrentMessage().extractValue(context, shorthandDefault_);
        } catch (Exception e) {
            return null;
        }
    }


    public EvaluationResult extractDefaultAnyValue(EvaluationContext context) {
        try {
            return context.getCurrentMessage().extractValue(context, shorthandDefaultAny_);
        } catch (Exception e) {
            return null;
        }
    }


    public String toString() {
        return "PropertyShorthandNode: " + value_;
    }


    public void acceptPostOrder( AbstractTCLVisitor visitor ) throws VisitorException
    {
        if (getFirstChild() != null) {
            ( ( AbstractTCLNode ) getFirstChild() ).acceptPostOrder( visitor );
        }
    }


    public void acceptPreOrder( AbstractTCLVisitor visitor ) throws VisitorException
    {
        ( ( AbstractTCLNode ) getFirstChild() ).acceptPreOrder( visitor );
    }


    public void acceptInOrder( AbstractTCLVisitor visitor ) throws VisitorException
    {
        ( ( AbstractTCLNode ) getFirstChild() ).acceptInOrder( visitor );
    }
}
