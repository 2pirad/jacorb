package org.jacorb.notification.node;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2003 Gerald Brose
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

import antlr.BaseAST;
import antlr.Token;
import antlr.collections.AST;
import org.omg.CORBA.TCKind;
import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.jacorb.notification.EvaluationContext;
import org.jacorb.notification.evaluate.EvaluationException;
import java.lang.reflect.Field;
import org.jacorb.notification.parser.TCLParserTokenTypes;
import org.apache.log.Hierarchy;
import org.apache.log.Logger;

/**
 * Base Class for TCLTree Nodes.
 *
 * @author Alphonse Bendt
 * @version $Id: TCLNode.java,v 1.5 2003-04-12 21:04:53 alphonse.bendt Exp $
 */

public abstract class TCLNode extends BaseAST implements TCLParserTokenTypes
{

    private int astNodeType_;
    private TCKind tcKind_;
    private String name_;

    protected Logger logger_ =
        Hierarchy.getDefaultHierarchy().getLoggerFor( getClass().getName() );


    ////////////////////////////////////////////////////////////
    // Constructor

    public TCLNode( Token tok )
    {
        super();
        setType( tok.getType() );
    }

    protected TCLNode()
    {
        super();
    }

    //////////////////////////////////////////////////

    /**
     * Evaluate this Node.
     *
     * @param context an <code>EvaluationContext</code> value contains
     * all context information necessary for the evaluation
     * @return an <code>EvaluationResult</code> value
     * @exception DynamicTypeException 
     * if an dynamic type error occurs during the evaluation 
     * e.g. the attempt to add a string and a number
     * @exception InconsistentTypeCode if an error occurs
     * @exception InvalidValue if an error occurs
     * @exception TypeMismatch if an error occurs
     * @exception EvaluationException 
     * these errors mostly occur if e.g. an expression contains a reference 
     * to a non-existent struct member.
     */
    public EvaluationResult evaluate( EvaluationContext context )
    throws DynamicTypeException,
                InconsistentTypeCode,
                InvalidValue,
                TypeMismatch,
                EvaluationException
    {
        return null;
    }

    /**
     * accept a visitor for traversal Inorder
     * 
     * @param visitor 
     */
    public abstract void acceptInOrder( TCLVisitor visitor ) 
	throws VisitorException;

    /**
     * accept a visitor for traversal in Preorder. the root node is
     * visited before the left and the right subtrees are visited.
     * 
     * @param visitor 
     */
    public abstract void acceptPreOrder( TCLVisitor visitor ) 
	throws VisitorException;

    /**
     * accept a visitor for traversal in Postorder. the right and left
     * subtrees are visited before the root node is visited.
     * 
     * @param visitor 
     */
    public abstract void acceptPostOrder( TCLVisitor visitor ) 
	throws VisitorException;

    ////////////////////////////////////////////////////////////

    public String getName()
    {
        return name_;
    }

    void setName( String name )
    {
        name_ = name;
    }

    /**
     * Check wether this node has a Sibling.
     *
     * @return true, if this node has a Sibling
     */
    public boolean hasNextSibling()
    {
        return ( getNextSibling() != null );
    }

    /**
     * get the AST Token Type of this nodes sibling
     * 
     * @return a AST Token Type
     */
    public int getNextType()
    {
        TCLNode _next = ( TCLNode ) getNextSibling();

        return _next.getType();
    }

    protected void setKind( TCKind kind )
    {
        tcKind_ = kind;
    }

    /**
     * Return the Runtimetype of this node.
     * If the Runtime type cannot be guessed statically this Method
     * returns null. 
     *
     * @return a <code>TCKind</code> value or null if the Runtimetype
     * cannot be determined 
     * statically.
     */
    public TCKind getKind()
    {
        return tcKind_;
    }

    public void printToStringBuffer( StringBuffer buffer )
    {
        if ( getFirstChild() != null )
        {
            buffer.append( " (" );
        }

        buffer.append( " " );
        buffer.append( toString() );

        if ( getFirstChild() != null )
        {
            buffer.append( ( ( TCLNode ) getFirstChild() ).toStringList() );
        }

        if ( getFirstChild() != null )
        {
            buffer.append( " )" );
        }
    }

    /**
     * create a visualization of this node and all its children.
     * 
     * @return a String representation of this Node and all its children
     */
    public String toStringTree()
    {
        StringBuffer _buffer = new StringBuffer();

        printToStringBuffer( _buffer );

        return _buffer.toString();
    }

    /**
     * Access the left child. This method returns null if this node
     * has no left child
     * 
     * @return the left Child or null.
     */
    public TCLNode left()
    {
        return ( TCLNode ) getFirstChild();
    }

    /**
     * Access the right child. This method returns null if this node
     * has no right child
     * 
     * @return the right Child or null.
     */
    public TCLNode right()
    {
        return ( TCLNode ) getFirstChild().getNextSibling();
    }

    ////////////////////////////////////////////////////////////

    public boolean isStatic()
    {
        return false;
    }

    public boolean isNumber()
    {
        return false;
    }

    public boolean isString()
    {
        return false;
    }

    public boolean isBoolean()
    {
        return false;
    }

    /**
     * Get the AST Token Type for this node.
     * 
     * @return the AST Token Type value
     * @see org.jacorb.notification.parser.TCLParserTokenTypes
     */
    public int getType()
    {
        return astNodeType_;
    }

    /**
     * Set AST Token Type for this node.
     * 
     * @param type must be a valid TCLTokenType.
     * @see org.jacorb.notification.parser.TCLParserTokenTypes
     */
    public void setType( int type )
    {
        astNodeType_ = type;
    }

    /**
     * converts an int tree token type to a name.
     * Does this by reflecting on nsdidl.IDLTreeTokenTypes,
     * and is dependent on how ANTLR 2.00 outputs that class. 
     * stolen from http://www.codetransform.com/
     */
    public static String getNameForType( int t )
    {
        try
        {
            Field[] _fields = TCLParserTokenTypes.class.getDeclaredFields();

            if ( t - 2 < _fields.length )
            {
                return _fields[ t -2 ].getName();
            }
        }
        catch ( Exception e )
        {
            System.out.println( e );
        }

        return "unfoundtype: " + t;
    }

    /**
     * satisfy abstract method from BaseAST. Not used.
     */
    public void initialize( int t, String txt )
    {
        // no op
    }

    /**
     * satisfy abstract method from BaseAST. Not used.
     */
    public void initialize( AST t )
    {
        // no op
    }

    /**
     * satisfy abstract method from BaseAST. Not used.
     */
    public void initialize( Token tok )
    {
        // no op
    }
}
