/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2002 Gerald Brose
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
package org.jacorb.util.tracing;

import java.util.*;
/**
 * TraceTreeNode.java
 *
 *
 * Created: Tue Jul 25 13:06:22 2000
 *
 * @author Nicolas Noffke
 * $Id: TraceTreeNode.java,v 1.6 2002-10-05 13:59:41 andre.spiegel Exp $
 */

public class TraceTreeNode
{
    protected Vector subtraces = null;

    protected int tracer_id = 0;
    protected String operation = null;
    protected long client_time = 0;
    protected long server_time = 0;


    public TraceTreeNode(int tracer_id)
    {
        this.tracer_id = tracer_id;
        subtraces = new Vector();
    }

//      public TraceTreeNode(int tracer_id,
//                           long client_time,
//                           long server_time)
//      {
//          this(tracer_id);

//          client_time = client_time;
//          server_time = server_time;
//      }    
} // TraceTreeNode






