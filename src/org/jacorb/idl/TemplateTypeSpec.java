/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2000  Gerald Brose.
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
 * @version $Id: TemplateTypeSpec.java,v 1.2 2001-03-17 18:43:47 brose Exp $
 */


class TemplateTypeSpec 
    extends SimpleTypeSpec 
{
    protected boolean typedefd = false;

    public TemplateTypeSpec(int num) 
    {
	super(num);
    }

    public void parse()
        throws ParseException
    {
	type_spec.parse();
    }

    /**
     *	we have to be able to distinguish between explicitly typedef'd
     *	type names and anonymously defined type names
     */

    public void markTypeDefd()
    {
	typedefd = true;
    }


    public boolean basic()
    {
	return true;
    } 
}















