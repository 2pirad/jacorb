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

package org.jacorb.idl;

/**
 * @author Gerald Brose
 * @version $Id: ParseException.java,v 1.9 2003-09-03 16:00:16 brose Exp $
 *
 * Thrown by the IDL compiler when it encounters fatal errors
 */

public class ParseException
    extends RuntimeException
{
    /** remember the error position  */
    private PositionInfo position = null;

    public ParseException()
    {
    }

    public ParseException( String reason )
    {
        super( reason );
    }

    public ParseException( String reason, PositionInfo pos )
    {
        super( reason );
        position = pos;
    }

    public String getMessage()
    {
        return "IDL Parse error in " + 
            ( position != null ?  position.toString() : "" ) + 
            ": " + super.getMessage();
    }
    


}








