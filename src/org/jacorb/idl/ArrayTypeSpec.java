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
 * @author Gerald Brose (C)
 * @version $Id: ArrayTypeSpec.java,v 1.1.1.1 2001-03-17 18:08:18 brose Exp $
 *
 */

import java.io.*;

class ArrayTypeSpec 
    extends TemplateTypeSpec 
{
    ArrayDeclarator declarator = null;
    String typename = null;
    String dimensionStr = "";
    int [] dims;
    int my_dim = 0;
    String typeSig;

    private boolean written = false;

    public ArrayTypeSpec(int num, TypeSpec elem, 
			 ArrayDeclarator ad, String pack_name)
    {
	super(num);
	declarator = ad;
//  	try 
//  	{
//  	    declarator.parse();
//  	} 
//  	catch ( ParseError p )
//  	{
//  	    p.printStackTrace();
//  	}
	name = declarator.name();
	set_token( ad.get_token());
	setEnclosingSymbol( ad.getEnclosingSymbol());
	this.pack_name = pack_name;
	type_spec = elem;
    }

    /**
     * private constructor, only to be called from public constructor for
     * multi-dimensional arrays, i.e. nested arrays. Used to create
     * nested ArrayTypeSpecs
     */

    private ArrayTypeSpec(int num, TypeSpec elem, 
			  ArrayDeclarator ad, String pack_name, int my_dim)
    {
	super(num);
	declarator = ad;
	name =  declarator.name();
	dims = declarator.dimensions();
	set_token( ad.get_token());
	setEnclosingSymbol( ad.getEnclosingSymbol());
	this.pack_name = pack_name;
	this.my_dim = my_dim; 
	if( dims.length > my_dim + 1 )
	    type_spec = new ArrayTypeSpec( new_num(), elem, ad, pack_name, my_dim + 1 );		
	else
	    type_spec = elem;		
	// needs to be done here because nested array type specs are not parsed
	StringBuffer sb = new StringBuffer();
	for( int i = my_dim; i < dims.length; i++ )
	    sb.append("[]");
	dimensionStr = sb.toString();
    }

    /**
     * clone this ArrayTypeSpec. The cloned object will not be parsed again.
     */

    public Object clone()
    {
	System.out.println("clone");
	ArrayTypeSpec st = new ArrayTypeSpec(new_num(), type_spec, declarator, pack_name);
	st.dims = this.dims;
	st.included = this.included;
	st.typedefd = this.typedefd;
	st.inhibitionFlag = this.inhibitionFlag;
	st.dims = dims;
	st.my_dim = my_dim; 
	st.dimensionStr = this.dimensionStr;
	st.set_token( get_token() );
	st.setEnclosingSymbol( getEnclosingSymbol());
	return st;
    }

    public void setEnclosingSymbol( IdlSymbol s )
    {
	if( enclosing_symbol != null && enclosing_symbol != s )
	    throw new RuntimeException("Compiler Error: trying to reassign container for " + name );
	enclosing_symbol = s;
    }

    /**
     * @returns the TypeSpec for the array's elemements
     */

    public TypeSpec elementTypeSpec()
    {
	TypeSpec t = type_spec;
	while( !(t instanceof ArrayTypeSpec ) && t.type_spec != null )
	    t = t.type_spec;
	return t;
    }

    public TypeSpec typeSpec()
    {
	return this;
    }

    public void setPackage( String s)
    {
        s = parser.pack_replace(s);
	throw new RuntimeException("ArrayTypeSpec.setPackage should never be called!");
    }

    /**
     *	we have to be able to distinguish between explicitly typedef'd
     *	type names and anonymously defined type names
     */

    public void markTypeDefd()
    {
	typedefd = true;
    }

    public String typeName()
    {
	// return elementTypeSpec().typeName() + "[]";
	return elementTypeName() + "[]";
    }


    public String elementTypeName()
    {
	if( type_spec.typeSpec() instanceof ScopedName )
	{
	    return ((ScopedName)type_spec.typeSpec()).resolvedTypeSpec().typeName();
	}
	else
	{
	    return type_spec.typeName();
	}
    }

    public String typeSignature()
    {
	if( typeSig == null )
	{
	    typeSig = typeName();
	    while( typeSig.endsWith("[]"))
	    {
		typeSig = "[" + typeSig.substring(0, typeSig.length()-3);
	    }
	}
	return typeSig;
    }

    public void parse() 
        throws ParseException
    {
	dims = declarator.dimensions();
	if( dims.length > 1 )
	{
	    type_spec = new ArrayTypeSpec( new_num(), type_spec, declarator, pack_name, 1 );
	}
	else if( type_spec.typeSpec() instanceof ScopedName )
	{
	    TypeSpec ts = ((ScopedName)type_spec.typeSpec()).resolvedTypeSpec();
	    if( ts != null ) 
		type_spec = ts;
	}

	StringBuffer sb = new StringBuffer();
	for( int i = my_dim; i < dims.length; i++ )
	    sb.append("[]");
	dimensionStr = sb.toString();

	try
	{
	    if( !typedefd )
		NameTable.define( full_name(), "type" );
	    if( !NameTable.defined(typeName(), "type" ))
		NameTable.define( typeName(), "type" );
	} 
	catch ( NameAlreadyDefined n )
	{
	   parser.fatal_error("Name " + full_name() + " already defined.", null);
	}
    }

    /**
     * @returns a string for an expression of type TypeCode that describes this type
     */
    
    public String getTypeCodeExpression()
    {
	String originalType =  "org.omg.CORBA.ORB.init().create_array_tc("+ dims[my_dim] + "," 
	    + elementTypeSpec().getTypeCodeExpression() +")";

	return originalType;
    }

    public String helperName()
    {	
	return ScopedName.unPseudoName(full_name()) + "Helper";
    }

    public String holderName()
    {
	return ScopedName.unPseudoName(full_name()) + "Holder";
    }

    public String className()
    {
	String fullName;

	if( pack_name.length() > 0 )
	    fullName =  ScopedName.unPseudoName( pack_name +"." + name );
	else
	    fullName =  ScopedName.unPseudoName( name ) ;

	String cName;
	if( fullName.indexOf('.') > 0 )
	{
	    pack_name = fullName.substring( 0, fullName.lastIndexOf('.'));
	    cName = fullName.substring( fullName.lastIndexOf('.') + 1 );
	} 
	else 
	{
	    pack_name = "";
	    cName  = fullName;
	}
	return cName;
	
    }

    int length()
    { 
	return dims[my_dim];
    }

    public String printReadExpression(String streamname)
    {
	return helperName() +  ".read(" + streamname +")"; 
    }

    public String printReadStatement(String var_name, String streamname)
    {
	StringBuffer sb = new StringBuffer();
	String type = typeName();

	sb.append(var_name + " = new " + type.substring(0,type.indexOf("[")));
	sb.append("[" + length() + "]");

	sb.append( type.substring(type.indexOf(']')+1) +";\n");


	//	for( int i = my_dim+1; i < dims.length; i++ )
	//  sb.append("[]");
	//	sb.append(";\n");
	
	if( elementTypeSpec() instanceof BaseType )
	{
	    String _tmp =  elementTypeSpec().printReadExpression( streamname );
	    sb.append("\t\t" + _tmp.substring(0,_tmp.indexOf("(")) + "_array("+var_name+",0,"+length()+");");
	}
	else 
	{
	    char idx_variable = 'i';
	    String indent = "";
	    if( var_name.endsWith("]"))
	    {
		idx_variable = (char)( var_name.charAt( var_name.length()-2 ) + 1);
		indent = "    ";
	    }
	    sb.append("\t\t"+indent+"for(int "+idx_variable+"=0;"+
		      idx_variable+"<"+length()+";"+idx_variable+"++)\n\t\t"+indent+"{\n");
	    
	    sb.append("\t\t\t"+indent+ 
		      elementTypeSpec().printReadStatement(var_name +
							   "["+idx_variable+"]", streamname) + "\n");
	    sb.append("\t\t"+indent+"}\n");
	}
	return sb.toString();
    }
      
    public String printWriteStatement(String var_name, String streamname)
    {
//  	if( !typedefd )
//  	{
	    StringBuffer sb = new StringBuffer();
	    String type = typeName();
	    sb.append("if("+var_name+".length<"+length()+")\n\t\tthrow new org.omg.CORBA.MARSHAL(\"Incorrect array size \"+"+var_name+".length+\", expecting " + length()+"\");\n");
	    
	    if( elementTypeSpec() instanceof BaseType )
	    {
		String _tmp =  elementTypeSpec().printWriteStatement(var_name, streamname);
		sb.append( "\t\t" + _tmp.substring(0,_tmp.indexOf("(")) + 
			   "_array("+var_name+",0,"+length()+");");
	    }
	    else
	    {
		char idx_variable = 'i';
		String indent = "";
		if( var_name.endsWith("]"))
		{
		    idx_variable = (char)( var_name.charAt( var_name.length()-2 ) + 1);
		    indent = "    ";
		}
		sb.append("\t\t"+indent+"for( int "+idx_variable+"=0; "+idx_variable+"<"+length()+";"+idx_variable+"++)\n\t\t"+indent+"{\n");
		sb.append("\t\t\t"+indent+ elementTypeSpec().printWriteStatement(var_name 
							   + "["+idx_variable+"]",streamname) + "\n");
		sb.append("\t\t"+indent+"}\n");
	    }
	    return sb.toString();
//  	}
//  	else
//  	    return helperName() + ".write(" + streamname +","+var_name+");";
    }


    private void printHolderClass( String className, PrintWriter ps )
    {
	if( !pack_name.equals(""))
	    ps.println("package " + pack_name + ";\n" );
	
	String type = typeName();
	
	ps.println("public class " + className + "Holder");
	ps.println("\timplements org.omg.CORBA.portable.Streamable");
	
	ps.println("{");
	ps.println("\tpublic " + type + " value;");
	ps.println("\tpublic " + className + "Holder()");
	ps.println("\t{");
	ps.println("\t}");

	ps.println("\tpublic " + className+ "Holder(" + type +" initial)\n\t{");
	ps.println("\t\tvalue = initial;");
	ps.println("\t}");

	ps.println("\tpublic org.omg.CORBA.TypeCode _type()");
	ps.println("\t{");
	ps.println("\t\treturn " + className + "Helper.type();");
	ps.println("\t}");

	TypeSpec m = type_spec;
	
	ps.println("\tpublic void _read(org.omg.CORBA.portable.InputStream _in)");
	ps.println("\t{");
	ps.println("\t\tvalue = " + className + "Helper.read(_in);");
	ps.println("\t}");
	
	ps.println("\tpublic void _write(org.omg.CORBA.portable.OutputStream _out)");
	ps.println("\t{");
	ps.println("\t\t" + className + "Helper.write(_out,value);");
	ps.println("\t}");

	ps.println("}");	
    }


    private void printHelperClass(String className, PrintWriter ps)
    {
	if( !pack_name.equals(""))
	    ps.println("package " + pack_name + ";" );

	String type = typeName();

	ps.println("public class " + className + "Helper");
	ps.println("{");

	ps.println("\tprivate static org.omg.CORBA.TypeCode _type = "+getTypeCodeExpression()+";");
	ps.println("\tpublic " + className + "Helper ()");
	ps.println("\t{");
	ps.println("\t}");

	TypeSpec.printHelperClassMethods(className, ps, type);
	printIdMethod(ps);

	/* read */

	ps.println("\tpublic static " + type + " read(org.omg.CORBA.portable.InputStream _in)");
	ps.println("\t{");

	ps.print("\t\t" + type + " result = new " + type.substring(0, type.indexOf(']'))+length()+"]");
	ps.println( type.substring(type.indexOf(']')) +"; // " + type);

		    //	for( int i = my_dim+1; i < dims.length; i++ )
		    //	    ps.print("[]");
		    //	ps.print(";\n");

	if( elementTypeSpec() instanceof BaseType )
	{
	    String _tmp =  elementTypeSpec().printReadExpression("_in");
	    ps.println("\t\t" + _tmp.substring(0,_tmp.indexOf("(")) + "_array(result,0," +length() + ");");
	}
	else
	{
	    ps.println("\t\tfor( int i = 0; i < " + length() + "; i++ )\n\t\t{");
	    ps.println("\t\t\t" + elementTypeSpec().printReadStatement("result[i]","_in") + "\n\t\t}");
	}
	ps.println("\t\treturn result;");
	ps.println("\t}");

	/* write */

	ps.println("\tpublic static void write(org.omg.CORBA.portable.OutputStream out, " + type + " s)");
	ps.println("\t{");
	if( declarator.dimensions()[0] != 0 )
	{
	    ps.println("\t\tif( s.length != " + declarator.dimensions()[0] + ")");
	    ps.println("\t\t\tthrow new org.omg.CORBA.MARSHAL(\"Incorrect array size\");");
	}
	if( elementTypeSpec() instanceof BaseType )
	{
	    String _tmp =  elementTypeSpec().printWriteStatement("s","out");
	    ps.println("\t\t" + _tmp.substring(0,_tmp.indexOf("(")) + "_array(s,0," + length()+");");
	}
	else
	{
	    ps.println("\t\tfor( int i = 0; i < s.length; i++ )\n\t\t{");
	    ps.println("\t\t\t" + elementTypeSpec().printWriteStatement("s[i]","out") + "\n\t\t}");
	}
	ps.println("\t}");
	ps.println("}");	
    }


    public void print(PrintWriter _ps)
    {
	if( included && !generateIncluded() )
	    return; // no code generation

	try
	{
	    // only generate class files for explicitly
	    // defined sequence types, i.e. for typedef'd ones
	    
	    if( (!written) && typedefd )
	    { 
				// write holder file
		
		String className = className();
		String path = parser.out_dir+fileSeparator+pack_name.replace('.',fileSeparator);
		File dir = new File( path );
		if( !dir.exists() )
		{
		    if( !dir.mkdirs())
		    {
			System.err.println("Unable to create " + path );
			System.exit(1);
		    }
		}
		
		String fname = className + "Holder.java";
		PrintWriter ps = new PrintWriter(new java.io.FileWriter(new File(dir,fname)));
		printHolderClass( className, ps );
		ps.close();

		fname = className + "Helper.java";
		ps = new PrintWriter(new java.io.FileWriter(new File(dir,fname)));
		printHelperClass( className, ps );
		ps.close();

		written = true;
	    }
	} 
	catch ( java.io.IOException i )
	{
	    System.err.println("File IO error");
	    i.printStackTrace();
	}
    }
	
    public String toString()
    {
	return typeName();
    }



}














