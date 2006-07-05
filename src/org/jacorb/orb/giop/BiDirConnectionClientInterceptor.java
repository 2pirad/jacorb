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

package org.jacorb.orb.giop;

import org.jacorb.orb.*;
import org.jacorb.orb.portableInterceptor.*;

import org.omg.PortableInterceptor.*;
import org.omg.IOP.*;
import org.omg.IIOP.*;

/**
 * @author Nicolas Noffke
 * @version $Id: BiDirConnectionClientInterceptor.java,v 1.11 2006-07-05 09:18:48 alphonse.bendt Exp $
 */
public class BiDirConnectionClientInterceptor
    extends DefaultClientInterceptor
{
    private static final String name = "BiDirConnectionClientInterceptor";

    private final ORB orb;

    private ServiceContext bidir_ctx = null;

    public BiDirConnectionClientInterceptor( ORB orb )
    {
        this.orb = orb;
    }

    public String name()
    {
        return name;
    }

    public void destroy()
    {
        // nothing to do
    }

    public void send_request( ClientRequestInfo ri )
        throws ForwardRequest
    {
        //only send a BiDir service context if our orb allows it, and
        //the connection was initiated in this process

        if( orb.useBiDirGIOP() &&
            ((ClientRequestInfoImpl) ri).connection.isClientInitiated() )
        {
            if( bidir_ctx == null )
            {
                BasicAdapter ba = orb.getBasicAdapter();

                ListenPoint lp = new ListenPoint( ba.getAddress(),
                                                  (short) ba.getPort() );

                ListenPoint[] points = null;
                if( ba.hasSSLListener() )
                {
                    ListenPoint ssl_lp =
                        new ListenPoint( ba.getAddress(),
                                         (short) ba.getSSLPort() );

                    points = new ListenPoint[]{ lp, ssl_lp };
                }
                else
                {
                    points = new ListenPoint[]{ lp };
                }

                BiDirIIOPServiceContext b =
                    new BiDirIIOPServiceContext( points );
                org.omg.CORBA.Any any = orb.create_any();
                BiDirIIOPServiceContextHelper.insert( any, b );

                final CDROutputStream cdr_out = new CDROutputStream();

                try
                {
                    cdr_out.beginEncapsulatedArray();
                    BiDirIIOPServiceContextHelper.write( cdr_out, b );

                    bidir_ctx = new ServiceContext( BI_DIR_IIOP.value,
                            cdr_out.getBufferCopy() );
                }
                finally
                {
                    cdr_out.close();
                }
            }

            ri.add_request_service_context( bidir_ctx, true );

            //if this connection isn't "bidir'ed" yet, do so now
            GIOPConnection conn = ((ClientRequestInfoImpl) ri).connection.getGIOPConnection();
            if(conn.getRequestListener() instanceof
               NoBiDirClientRequestListener)
            {
                conn.setRequestListener(orb.getBasicAdapter().getRequestListener());
            }
        }
    }
}
