package org.jacorb.security.sas;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 2002-2003 Gerald Brose
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

import org.jacorb.util.Debug;
import org.omg.IOP.CodecFactoryPackage.UnknownEncoding;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitializer;
import org.omg.PortableInterceptor.ORBInitInfoPackage.DuplicateName;

/**
 * This initializes the SAS Target Security Service (TSS) Interceptor
 *
 * @author David Robison
 * @version $Id: SASTargetInitializer.java,v 1.4 2003-08-27 16:58:51 david.robison Exp $
 */

public class SASTargetInitializer
        extends org.omg.CORBA.LocalObject
        implements ORBInitializer
{
    public static final int SecurityAttributeService = 15;

    public static int sasPrincipalNamePIC = (-1);

    /**
    * This method registers the interceptors.
    */
    public void post_init( ORBInitInfo info )
    {
        // install the TSS interceptor
        try
        {
            sasPrincipalNamePIC = info.allocate_slot_id();
            info.add_server_request_interceptor(new SASTargetInterceptor(info));
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
}    // SAS setup Initializer
