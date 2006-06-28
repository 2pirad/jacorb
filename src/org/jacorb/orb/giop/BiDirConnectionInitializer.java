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

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.orb.*;

import org.omg.PortableInterceptor.*;
import org.omg.IOP.*;
import org.omg.BiDirPolicy.BIDIRECTIONAL_POLICY_TYPE;

/**
 * @author Nicolas Noffke
 * @version $Id: BiDirConnectionInitializer.java,v 1.12 2006-06-28 12:41:43 alphonse.bendt Exp $
 */

public class BiDirConnectionInitializer
  extends org.omg.CORBA.LocalObject
  implements ORBInitializer
{
    public void post_init(ORBInitInfo info)
    {
        final ORB orb = ((org.jacorb.orb.portableInterceptor.ORBInitInfoImpl) info).getORB();
        final Logger logger = orb.getConfiguration().getNamedLogger("org.jacorb.interceptors.ior_init");

        try
        {
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value,
                                             (byte) 1, (byte) 0);
            Codec codec = info.codec_factory().create_codec(encoding);

            info.add_client_request_interceptor( new BiDirConnectionClientInterceptor( orb ));
            info.add_server_request_interceptor( new BiDirConnectionServerInterceptor( orb ));

            info.register_policy_factory( BIDIRECTIONAL_POLICY_TYPE.value,
                                          new BiDirPolicyFactory() );
        }
        catch (Exception e)
        {
            logger.error("BiDirConnectionInitializer.post_init", e);
        }
    }

    public void pre_init(ORBInitInfo info)
    {
        // do nothing
    }
} // BiDirConnectionInitializer
