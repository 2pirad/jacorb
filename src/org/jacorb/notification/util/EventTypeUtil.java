package org.jacorb.notification.util;

/*
 *        JacORB - a free Java ORB
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

import org.omg.CosNotification.EventType;

/**
 * @author Alphonse Bendt
 * @version $Id: EventTypeUtil.java,v 1.1 2004-02-20 12:47:08 alphonse.bendt Exp $
 */

public class EventTypeUtil
{
    private EventTypeUtil()
    {}

    ////////////////////////////////////////

    public static String toString(EventType et)
    {
        return et.domain_name + "/" + et.type_name;
    }


    public static String toString(EventType[] ets)
    {
        StringBuffer b = new StringBuffer("[");

        for (int x = 0; x < ets.length; ++x)
        {
            b.append(toString(ets[x]));

            if (x < ets.length - 1 )
            {
                b.append(", ");
            }
        }

        b.append("]");

        return b.toString();
    }
}
