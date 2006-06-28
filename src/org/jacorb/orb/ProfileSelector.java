package org.jacorb.orb;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2004 Gerald Brose.
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

import org.omg.ETF.Profile;

import org.jacorb.orb.giop.ClientConnectionManager;

/**
 * A ProfileSelector decides, on the client side, which Profile from
 * an object's IOR should be used to communicate with the object.
 *
 * @author <a href="mailto:spiegel@gnu.org">Andre Spiegel</a>
 * @version $Id: ProfileSelector.java,v 1.4 2006-06-28 12:39:20 alphonse.bendt Exp $
 */
public interface ProfileSelector
{
    Profile selectProfile(List profiles,
                          ClientConnectionManager ccm);
}
