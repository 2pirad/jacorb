/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2001  Gerald Brose.
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
 * @version $Id: CharType.java,v 1.5 2001-04-05 09:20:59 jacorb Exp $
 */


class CharType 
    extends BaseType 
    implements SwitchTypeSpec 
{
    private boolean wide = false;

    public CharType(int num) 
    {
	super(num);
    }

    public Object clone()
    {
	CharType s = new CharType(new_num());
	if( wide )
	    s.setWide();
	return s;
    }

    public boolean isWide()
    {
	return wide;
    }

    public void setWide()
    {
	wide = true;
    }

    public String typeName()
    {
	return "char";
    }

    public TypeSpec typeSpec()
    {
	return this;
    }

    public String toString()
    {	
	return typeName();
    }

    public boolean basic()
    {
	return true;
    } 

    public int getTCKind()
    {
	if( wide )
	    return 26;
	else
	    return 9;
    }

    public String holderName()
    {	
	return "org.omg.CORBA.CharHolder";
    }

    public String signature()
    {
	return "C";
    }

    public String printReadExpression(String strname)
    {
	if( wide )
	    return  strname + ".read_wchar()";
	else
	    return strname + ".read_char()";
    }

    public String printWriteStatement(String var_name, String strname)
    {
	if( wide )
	    return strname + ".write_wchar("+var_name+");";
	else
	    return strname + ".write_char("+var_name+");";
    }

    public String printInsertExpression()
    {
	if( wide )
            return "insert_wchar";
        else
            return "insert_char";

    }

    public String printExtractExpression()
    {
	if( wide )
            return "extract_wchar";
        else
            return "extract_char";
    }
}

