package org.jacorb.util;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2007 Gerald Brose.
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

/**
 * Holds the release date and version of JacORB. An attempt to bring more
 * maintainability to the versioning.
 * @author Gerald Brose
 * @version $Id: Version.java,v 1.38 2009-05-27 18:17:40 andre.spiegel Exp $
 */
public final class Version
{
    public static final String version = "2.3.1";
    public static final String date = "27-May-2009";
    public static final String longVersion = version + ", " + date;
    public static final String yearString = "1997-2009";
}
