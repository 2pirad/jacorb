package org.jacorb.orb.giop;

/*
 *        JacORB - a free Java ORB
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

import org.jacorb.util.threadpool.Consumer;

/**
 * MessageReceptor.java
 *
 *
 * Created: Sat Aug 18 10:52:45 2002
 *
 * @author Nicolas Noffke
 * @version $Id: MessageReceptor.java,v 1.10 2004-05-06 12:40:00 nicolas Exp $
 */

public class MessageReceptor 
    implements Consumer  
{
    public MessageReceptor()
    {        
    }
    
    // implementation of org.jacorb.util.threadpool.Consumer interface
    
    /**
     *
     * @param job <description>
     */
    public void doWork( Object job ) 
    {
        try
        {
            ((GIOPConnection) job).receiveMessages();
        }
        catch( Exception e )
        {
        }
    }    
}// MessageReceptor
