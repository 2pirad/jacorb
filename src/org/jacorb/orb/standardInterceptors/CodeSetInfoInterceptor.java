package org.jacorb.orb.standardInterceptors;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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


import org.jacorb.config.*;
import org.jacorb.orb.CDROutputStream;
import org.jacorb.orb.ORB;
import org.jacorb.orb.giop.CodeSet;
import org.omg.PortableInterceptor.IORInfo;
import org.omg.PortableInterceptor.IORInterceptor;

/**
 * This interceptor creates a codeset TaggedComponent.
 *
 * @author Nicolas Noffke
 * @version $Id: CodeSetInfoInterceptor.java,v 1.27 2009-04-25 10:10:36 andre.spiegel Exp $
 */

public class CodeSetInfoInterceptor
    extends org.omg.CORBA.LocalObject
    implements IORInterceptor, Configurable
{
    private final org.omg.IOP.TaggedComponent tagc;

    public CodeSetInfoInterceptor(ORB orb)
    {
        super();

        try
        {
            configure(orb.getConfiguration());
        }
        catch (ConfigurationException ex)
        {
            ex.printStackTrace();
            // will have to do with defaults
        }

        // encapsulate it into TaggedComponent
        final CDROutputStream out = new CDROutputStream( orb );
        try
        {
            out.beginEncapsulatedArray();
            org.omg.CONV_FRAME.CodeSetComponentInfoHelper.write( out, CodeSet.getLocalCodeSetComponentInfo() );

            tagc = new org.omg.IOP.TaggedComponent( org.omg.IOP.TAG_CODE_SETS.value,
                    out.getBufferCopy());
        }
        finally
        {
            out.close();
        }
    }


    public void configure(Configuration config)
        throws ConfigurationException
    {
        CodeSet.configure((org.jacorb.config.Configuration)config);
    }

    public String name()
    {
        return "CodeSetInfoComponentCreator";
    }

    public void destroy()
    {
        // nothing to do
    }

    /**
     * Creates default IOR codeset  component.
     */

    public void establish_components( IORInfo info, int [] tags )
    {
        if (tags == null)
        {
            info.add_ior_component_to_profile( tagc,
                    org.omg.IOP.TAG_MULTIPLE_COMPONENTS.value );
            info.add_ior_component_to_profile( tagc,
                    org.omg.IOP.TAG_INTERNET_IOP.value );
        }
        else
        {
            for (int i = 0; i < tags.length; i++)
            {
                info.add_ior_component_to_profile(tagc, tags[i]);
            }
        }
    }

    public void establish_components( IORInfo info )
    {
        info.add_ior_component_to_profile( tagc,
                                           org.omg.IOP.TAG_MULTIPLE_COMPONENTS.value );
        info.add_ior_component_to_profile( tagc,
                                           org.omg.IOP.TAG_INTERNET_IOP.value );
    }
}
