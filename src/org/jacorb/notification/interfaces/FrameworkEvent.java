package org.jacorb.notification.interfaces;

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

/**
 * Baseclass for Framework Events.
 *
 *
 * Created: Wed Feb 12 15:11:59 2003
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: FrameworkEvent.java,v 1.1 2003-04-12 21:17:41 alphonse.bendt Exp $
 */

public class FrameworkEvent {
    
    private Object source_;

    public FrameworkEvent(Object source) {
	source_ = source;
    }

    public Object getSource() {
	return source_;
    }
    
}// FrameworkEvent
