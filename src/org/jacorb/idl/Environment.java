package org.jacorb.idl;

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


public final class Environment
{
    private static int verbosity = 0;

    public static final void verbosityLevel( int level )
    {
        verbosity = level;
    }

    public static final boolean isEnabled( int msg_level )
    {
        return msg_level <= verbosity;
    }

    public static final void output( int msg_level, String msg )
    {
        if( msg_level > verbosity )
            return;

        System.out.println( "   [ " + msg + " ]" );
    }

    public static final void output( int msg_level, Throwable t )
    {
        if( msg_level > verbosity )
            return;

        System.out.println( "## Exception ##" );
        t.printStackTrace();
    }


}

