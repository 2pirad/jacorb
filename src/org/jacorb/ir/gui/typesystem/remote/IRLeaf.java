
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





