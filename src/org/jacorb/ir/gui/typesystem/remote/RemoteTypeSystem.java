package org.jacorb.ir.gui.typesystem.remote;

/*
 *        JacORB  - a free Java ORB
 *
 *   Copyright (C) 1997-2002  Gerald Brose.
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

import org.omg.CORBA.*;
import javax.swing.tree.*;
import javax.swing.table.*;

import java.util.*;

import org.jacorb.ir.gui.typesystem.*;
import org.jacorb.util.Debug;

/**
 * @author Joerg v. Frantzius, Gerald Brose.
 * $Id: RemoteTypeSystem.java,v 1.8 2002-10-06 11:02:16 andre.spiegel Exp $
 */

public class RemoteTypeSystem 
    extends TypeSystem 
{
    Repository rep;
    ORB orb = ORB.init( new String[0], null);
    private static Hashtable knownIRObjects = new Hashtable();

    private static String test = "";
    private static int test2;

    /**
     */

    public RemoteTypeSystem () 
    {
	try 
        {
            this.rep  = 
                RepositoryHelper.narrow( 
                   orb.resolve_initial_references("InterfaceRepository"));
	}
	catch (Exception e) 
        {
            e.printStackTrace();
	}		
    }

    /**
     * @param ior java.lang.String
     */

    public RemoteTypeSystem (String ior) 
    {
	org.omg.CORBA.Object obj = orb.string_to_object(ior);
        try
        {
            rep = RepositoryHelper.narrow(obj);
        }
        catch( org.omg.CORBA.BAD_PARAM bp )
        {
            System.out.println("IOR is not a Repository, sorry.");
            System.exit(0);
	}	
    }

    /**
     *  Creates a TreeModel that contains only root enth�lt. To expand
     *  nodes,     the    TreeExpansionListener    returned    from
     *  getTreeExpansionListener(treeModel)  needs  to be  registered
     *  with JTree.
     * @return javax.swing.tree.DefaultTreeModel
     * @param root org.jacorb.ir.gui.typesystem.ModelParticipant 
     */

    public DefaultTreeModel createTreeModelRoot() 
    {
	if (treeModel!=null) 
        {
            return treeModel;
	}	
	else 
        {
            IRRepository startNode  = new IRRepository(rep); 
            treeModel = 
                ModelBuilder.getSingleton().createTreeModelRoot(startNode);
            return treeModel;
	}	
    }

    /**
     * @return org.jacorb.ir.gui.typesystem.TypeSystemNode
     * @param irNode org.omg.CORBA.IRObject
     */

    public static TypeSystemNode createTypeSystemNode(java.lang.Object obj) 
    {
        Debug.myAssert( obj != null, 
                        "A reference from the Repository is null... (but it should not)");
	IRObject irObject = null;
	TypeSystemNode result = null;

	System.out.flush();

	// Typ-Unterscheidung f�r obj vornehmen und korrespondierendes
        // org.jacorb.ir.gui.typesystem-Objekt erzeugen.
	// knownIRObjects: zu jedem Objekt des IR wird das 
        // korrespondierende org.jacorb.ir.gui.typesystem-Objekt
	// festgehalten, damit letzteres nicht mehrfach f�r 
        /// das selbe IR-Objekt erzeugt wird
	// (die Abbildung von IR-Objekten auf 
        // org.jacorb.ir.gui.typesystem-Objekte wird sozusagen injektiv gehalten)

	// Je nach Typ wird ein anderer Hashcode verwendet, 
        // um das obj in knownIRObjects abzulegen:
	// die von Object geerbte hashcode() Methode reicht 
        // hier nicht, weil sie equals() verwendet und
	// diese Methode nicht f�r alle m�glichen Typen von
        ///  obj korrekt redefiniert wurde (testet nur auf
	// Objekt-Identit�t)

	if ( obj instanceof IRObject ) 
        {
            try
            {
                irObject = IRObjectHelper.narrow((org.omg.CORBA.Object)obj);
            }
            catch( org.omg.CORBA.BAD_PARAM bp )
            {
            }
        }
        if( irObject != null )
        {
            // insbesondere "echte" IRObjects k�nnen beim Aufbau 
            // des Trees mehrmals referenziert
            // und dieser Methode als Argument �bergeben werden
            // if (knownIRObjects.get(ORB.init().object_to_string((org.omg.CORBA.Object)irObject))!=null) {
            //			return (TypeSystemNode)knownIRObjects.get(ORB.init().object_to_string((org.omg.CORBA.Object)irObject));
            //		}

            result = (TypeSystemNode)knownIRObjects.get(irObject);

            if( result != null ) 
            {
                Debug.output(2, result.getInstanceNodeTypeName()+" "+
                             result.getAbsoluteName()+" (cached)");
                return result;
            }

            // try again using Repository-ID
            try
            {
                Contained contained = 
                    ContainedHelper.narrow((org.omg.CORBA.Object)irObject);

                result = (TypeSystemNode)knownIRObjects.get(contained.id());
                if (result != null) 
                {
                    Debug.output(2, 
                                 result.getInstanceNodeTypeName()+" "+
                                 result.getAbsoluteName()+" (cached by id)");
                    return result;
                }	
            }
            catch( org.omg.CORBA.BAD_PARAM bp )
            {}

            try
            {	
                switch(irObject.def_kind().value()) 
                {	
                    // create IRObjects
                case DefinitionKind._dk_Module:
                    result = new IRModule(irObject);		
                    break;
                case DefinitionKind._dk_Interface:
                    result = new IRInterface(irObject);
                    break;
                case DefinitionKind._dk_Constant:
                    result = new IRConstant(irObject);
                    break;
                case DefinitionKind._dk_Attribute:
                    result = new IRAttribute(irObject);
                    break;
                case DefinitionKind._dk_Operation:
                    result = new IROperation(irObject);
                    break;
                    /*   Typedef   ist   eine  abstrakte   Oberklasse,
			theoretisch   d�rfte   es   kein  Objekt   mit
			DefinitionKind._dk_Typedef      geben     case
			DefinitionKind._dk_Typedef:   result   =   new
			IRTypedef(irObject); break; */
                case DefinitionKind._dk_Exception:
                    result = new IRException(irObject);
                    break;
                case DefinitionKind._dk_Struct:
                    result = new IRStruct(irObject);
                    break;				
                case DefinitionKind._dk_Union:
                    result = new IRUnion(irObject);
                    break;				
                case DefinitionKind._dk_Primitive:
                    result = new IRPrimitive(irObject);
                    break;
                case DefinitionKind._dk_Fixed:
                    result = new IRFixed(irObject);
                    break;		
                case DefinitionKind._dk_String:
                    result = new IRString(irObject);
                    break;		
                case DefinitionKind._dk_Wstring:
                    result = new IRWstring(irObject);
                    break;	
                case DefinitionKind._dk_Alias:
                    result = new IRAlias(irObject);
                    break;				
                case DefinitionKind._dk_Sequence:
                    result = new IRSequence(irObject);
                    break;				
                case DefinitionKind._dk_Enum:
                    result = new IREnum(irObject);
                    break;				
                case DefinitionKind._dk_Array:
                    result = new IRArray(irObject);
                    break;					
                case DefinitionKind._dk_ValueBox:
                    result = new IRValueBox(irObject);
                    break;
                case DefinitionKind._dk_Value:
                    result = new IRValue(irObject);
                    break;
                case DefinitionKind._dk_ValueMember:
                    result = new IRValueMember(irObject);
                    break;
                default:
                    System.out.println("Unknown/senseless DefinitionKind returned from Repository: "+irObject.def_kind().value());
                    break;
                } // switch		
            }
            catch( Exception exc )
            {
                Debug.output( 3, exc );
            }

            if ( result instanceof IRInterface && 
                 ((IRInterface)result).getName().equals("Container")) 
            {
                int nanu = ((IRNode)result).irObject.hashCode();
                int nanu2 = irObject.hashCode();
                if (test.equals(((IRInterface)result).getAbsoluteName())) 
                {
                    System.out.println("bug!");
                }	
                test = ((IRInterface)result).getAbsoluteName();
                test2=((IRNode)result).irObject.hashCode();
            }

            if (result != null) 
            {
                // knownIRObjects.put(ORB.init().object_to_string((org.omg.CORBA.Object)irObject),result);
                knownIRObjects.put(irObject,result);

                if (knownIRObjects.get(irObject) == null) 
                {
                    System.out.println( "wasislos?");
                }	

                if (result instanceof IRNode && 
                    (!((IRNode)result).repositoryID.equals(""))) 
                {
                    knownIRObjects.put(((IRNode)result).repositoryID,result);
                }	
            }	
	}	// if (irObjectHelper.narrow...)
	else 
        {	
            // kein IRObject sondern lokales Objekt
            // members von Structs, Unions und Enums k�nnen nicht
            // von anderen IRObjects referenziert werden,
            // wir wollen trotzdem f�r m�gliche mehrfache Aufrufe 
            // das selbe org.jacorb.ir.gui.typesystem-Objekt zur�ckgeben
            if (knownIRObjects.get(obj)!=null) {
                return (TypeSystemNode)knownIRObjects.get(obj);
            }	

            if (obj instanceof StructMember) 
            {
                // als Hash-Key nehmen wir einen IR-weit eindeutigen String
                StructMember structMember = (StructMember)obj;
                if (knownIRObjects.get("structmember" + structMember.name + 
                                       structMember.type.kind().toString())!=null) 
                {
                    return (TypeSystemNode)knownIRObjects.get("structmember" + 
                                                              structMember.name + 
                                                              structMember.type.kind().toString());
                }	
                result = new IRStructMember((StructMember)obj);
                knownIRObjects.put("structmember" + structMember.name + 
                                   structMember.type.kind().toString(),result);
            }	
            else if (obj instanceof UnionMember) 
            {
                UnionMember unionMember = (UnionMember)obj;
                if (knownIRObjects.get("unionmember" + 
                                       unionMember.name + 
                                       unionMember.type.kind().toString())!=null) 
                {
                    return (TypeSystemNode)knownIRObjects.get("unionmember" + 
                                                              unionMember.name + 
                                                              unionMember.type.kind().toString());
                }	
                result = new IRUnionMember((UnionMember)obj);
                knownIRObjects.put("unionmember" + unionMember.name + 
                                   unionMember.type.kind().toString(),result);
            }
            else 	
                if (obj instanceof ParameterDescription) 
                {
                    ParameterDescription parDesc = (ParameterDescription)obj;
                    if (knownIRObjects.get("parameter" + parDesc.name + 
                                           parDesc.type.kind().toString())!=null) 
                    {
                        return (TypeSystemNode)knownIRObjects.get("parameter" + 
                                                                  parDesc.name + 
                                                                  parDesc.type.kind().toString());
                    }	
                    result = new IRParameter(parDesc);
                    knownIRObjects.put("parameter" + parDesc.name + 
                                       parDesc.type.kind().toString(),result);
                }
                else if (obj instanceof String) 
                {
                    if (knownIRObjects.get((String)obj)!=null) 
                    {
                        return (IREnumMember)knownIRObjects.get((String)obj);
                    }	
                    result = new IREnumMember((String)obj);
                    knownIRObjects.put(obj,result);
                }	
	}	// else (obj war kein IRObject)
        
        if( result != null )
        {
            Debug.output( 2, result.getInstanceNodeTypeName()+" "+
                          result.getAbsoluteName());
        }
        else
            Debug.output( 2, "result is null ");
	return result;
    }

    /**
     * @return TableModel
     * @param node org.jacorb.ir.gui.typesystem.TypeSystemNode
     */

    public DefaultTableModel getTableModel(DefaultMutableTreeNode treeNode) 
    {
	DefaultTableModel tableModel = new DefaultTableModel();
	java.lang.Object[] colIdentifiers = {"Item","Type","Name"};

	tableModel.setColumnIdentifiers(colIdentifiers);

	if (treeNode!=null) 
        {
            if (treeNode.getUserObject() instanceof AbstractContainer) 
            {
                for (int i=0; i<treeModel.getChildCount(treeNode); i++) 
                {
                    TypeSystemNode childNode = 
                        (TypeSystemNode)((DefaultMutableTreeNode)(treeNode.getChildAt(i))).getUserObject();
                    String type = "";
                    if (childNode instanceof TypeAssociator) 
                    {
                        type = ((TypeAssociator)childNode).getAssociatedType();
                    }	
                    java.lang.Object[] row = {new NodeMapper(childNode,childNode.getInstanceNodeTypeName()),
                                              new NodeMapper(childNode,type),
                                              new NodeMapper(childNode,childNode.getName())};
                    tableModel.addRow(row);
                }
            }
	}
	return tableModel;
    }	

    /**
     * @return javax.swing.event.TreeExpansionListener
     * @param treeModel javax.swing.tree.DefaultTreeModel
     */
    public javax.swing.event.TreeExpansionListener getTreeExpansionListener(TreeModel treeModel) {
	return ModelBuilder.getSingleton().getTreeExpansionListener(treeModel);
    }

    /**
     * @return javax.swing.tree.TreeModel
     * @param rootModPart org.jacorb.ir.gui.typesystem.ModelParticipant
     */

    public TreeModel getTreeModel() 
    {
	if (treeModel!=null) 
        {
            return treeModel;
	}	
	else 
        {
            try 
            {
                IRRepository startNode 	= new IRRepository(rep); 
                treeModel = ModelBuilder.getSingleton().buildTreeModelAsync(startNode);
                return treeModel;
            }
            catch (Exception e) 
            {
                e.printStackTrace();
            }		
	}	
	return null;
    }

    /**
     * @param args java.lang.String[]
     */

    public static void main(String args[]) 
    {
	TreeModel dummy = 	new RemoteTypeSystem().getTreeModel();
	return;
    }

}











