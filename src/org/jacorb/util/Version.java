package org.jacorb.util;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2002  Gerald Brose.
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
import java.io.*;

/**
 * Holds the release date and version of JacORB. An attempt to bring more maintainability to
 * the versioning.
 * @author Gerald Brose
 * @version $Id: Version.java,v 1.7 2002-05-23 09:58:20 simon.mcqueen Exp $
 */

public final class Version
{
    public static final String shortVersion = "1.4";

    public static final String version = shortVersion + " beta 4";

    public static final String date = "March 2002";

    public static final String longVersion = version + ", " + date;

    public static String get()
    {
        return longVersion;
    }
}










