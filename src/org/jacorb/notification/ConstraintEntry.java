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

import org.jacorb.notification.filter.FilterConstraint;
import org.jacorb.notification.filter.FilterUtils;

import org.omg.CosNotification.EventType;
import org.omg.CosNotifyFilter.ConstraintInfo;

/**
 * @author Alphonse Bendt
 * @version $Id: ConstraintEntry.java,v 1.6 2004-01-23 19:41:53 alphonse.bendt Exp $
 */

public class ConstraintEntry
{
    private FilterConstraint filterConstraint_;

    private ConstraintInfo constraintInfo_;

    private int constraintId_;

    ////////////////////////////////////////

    ConstraintEntry( int constraintId,
                     FilterConstraint constraintEvaluator,
                     ConstraintInfo constraintInfo )
    {
        constraintId_ = constraintId;
        filterConstraint_ = constraintEvaluator;
        constraintInfo_ = constraintInfo;
    }

    ////////////////////////////////////////

    static class EventTypeWrapper implements EventTypeIdentifier
    {
        EventType et_;
        String constraintKey_;

        EventTypeWrapper( EventType et )
        {
            et_ = et;

            constraintKey_ =
                FilterUtils.calcConstraintKey( et_.domain_name, et_.type_name );
        }

        public String toString()
        {
            return constraintKey_;
        }
    }

    ////////////////////////////////////////

    EventTypeIdentifier getEventTypeIdentifier( int index )
    {
        return new EventTypeWrapper( constraintInfo_.constraint_expression.event_types[ index ] );
    }

    int getEventTypeCount()
    {
        return constraintInfo_.constraint_expression.event_types.length;
    }

    int getConstraintId()
    {
        return constraintId_;
    }

    ConstraintInfo getConstraintInfo()
    {
        return constraintInfo_;
    }

    public FilterConstraint getFilterConstraint()
    {
        return filterConstraint_;
    }
}
