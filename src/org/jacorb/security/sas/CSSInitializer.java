package org.jacorb.security.sas;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 2002-2002 Gerald Brose
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

import java.io.*;
import org.omg.PortableInterceptor.*;
import org.omg.PortableInterceptor.ORBInitInfoPackage.*;
import org.omg.SecurityReplaceable.*;
import org.omg.Security.*;
import org.jacorb.util.*;
import org.omg.IOP.*;
import org.omg.IOP.CodecFactoryPackage.*;

import org.jacorb.util.Environment;

/**
 * This initializes the SAS Client Security Service (CSS) Interceptor
 *
 * @author David Robison
 * @version $Id: CSSInitializer.java,v 1.1 2002-09-10 16:24:05 david.robison Exp $
 */

public class CSSInitializer
        extends org.omg.CORBA.LocalObject
        implements ORBInitializer
{
    /**
    * This method registers the interceptors.
    */
    public void post_init( ORBInitInfo info )
    {
        try
        {
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, (byte) 1, (byte) 0);
            Codec codec = info.codec_factory().create_codec(encoding);
            info.add_client_request_interceptor(new CSSInvocationInterceptor(codec));
        }
        catch (DuplicateName duplicateName)
        {
            Debug.output( Debug.SECURITY | Debug.IMPORTANT, duplicateName);
        }
        catch (UnknownEncoding unknownEncoding)
        {
            Debug.output( Debug.SECURITY | Debug.IMPORTANT, unknownEncoding);
        }
    }

    public void pre_init(ORBInitInfo info)
    {
    }
}






