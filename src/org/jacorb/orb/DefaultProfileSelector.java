package org.jacorb.orb;


/*
 *        JacORB  - a free Java ORB
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

import java.util.*;
import org.omg.ETF.Profile;
import org.jacorb.orb.connection.ClientConnectionManager;

/**
 * @author <a href="mailto:spiegel@gnu.org">Andre Spiegel</a>
 * @version $Id: DefaultProfileSelector.java,v 1.2 2003-08-11 09:34:06 andre.spiegel Exp $
 */
public class DefaultProfileSelector implements ProfileSelector
{

    public Profile selectProfile (List profiles, ClientConnectionManager ccm) 
    {
        if (profiles.size() > 0)
            return (Profile)profiles.get(0);
        else
            return null;
    }

}
