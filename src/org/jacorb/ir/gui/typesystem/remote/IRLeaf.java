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

package org.jacorb.ir.gui.typesystem.remote;


import org.jacorb.ir.gui.typesystem.*;
import javax.swing.tree.*;

/**
 * Abstrakte Oberklasse f�r Elemente des IR, zu denen kein entsprechendes IRObject existiert
 * oder dieses nicht von Contained erbt,
 * die aber dennoch in unserem Tree auftauchen sollen (z.B. StructMember).
 * Wird instantiiert von den Klassen, die entsprechende members() Operation besitzen (z.B. StructDef)
 * (Weitere Methoden k�men hinzu, wenn das Editieren des IR unterst�tzt w�rde)
 */
public abstract class IRLeaf extends org.jacorb.ir.gui.typesystem.TypeSystemNode {


/**
 * IRLeaf constructor comment.
 */
protected IRLeaf() {
	super();
}
}











