package org.jacorb.idl;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2001 Gerald Brose.
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

import java.util.Vector;
import java.util.Enumeration;
import java.io.*;

/**
 * @author Gerald Brose
 * @version $Id: AliasTypeSpec.java,v 1.15 2002-02-04 08:39:36 steve.osselton Exp $
 */

public class AliasTypeSpec 
    extends TypeSpec
{
    public TypeSpec originalType = null;
    private boolean written = false;

    public AliasTypeSpec(TypeSpec ts)
    {
	super(IdlSymbol.new_num());
	originalType = ts;	
    }

    public Object clone()
    { 
        AliasTypeSpec alias = new AliasTypeSpec( (TypeSpec)type_spec.clone() );
        alias.name = name;
        alias.pack_name = pack_name;
	return alias;
    }

    public String full_name()
    {
	if( pack_name.length() > 0 )
	{
	    String s = ScopedName.unPseudoName( pack_name+"." + name );
	    if(! s.startsWith("org.omg") )
	    {	
		return omg_package_prefix + s;
	    }
	    else
		return s;		           
	}
	else
	    return ScopedName.unPseudoName( name ) ;    
    }

    public String typeName()
    {	
	return originalType.typeName();
    }

    public String signature()
    {
	throw new java.lang.Error("compiler error, should not be called");
    }

    public TypeSpec typeSpec()
    {
	return this;
    }

    public TypeSpec originalType()
    {
	return originalType;
    }

    public void setPackage( String s )
    {
        //s = parser.pack_replace(s);
	if( pack_name.length() > 0 )
	    pack_name = new String( s + "." + pack_name );
	else
	    pack_name = s;
        pack_name = parser.pack_replace(pack_name);
    }

    public void setEnclosingSymbol( IdlSymbol s )
    {
	if( enclosing_symbol != null && enclosing_symbol != s )
	    throw new RuntimeException("Compiler Error: trying to reassign container for " + name );
	enclosing_symbol = s;
    }

    public boolean basic()
    {
	return false;
    } 


    public void parse() 	 
    {
	if( originalType instanceof TemplateTypeSpec )
	{
	    ((TemplateTypeSpec)originalType).markTypeDefd();	    
	}

//          String typeName = ( pack_name.length() > 0 ? pack_name + "." : "" ) + name;

//  	try
//  	{
//              if( typeName.length() > 0 )                
//                  NameTable.define( typeName , "type" );
//              else 
//                  ;
//  	} 
//  	catch ( NameAlreadyDefined nad )
//  	{
//              Environment.output( 4, nad );
//  	    parser.error("Typedef'd name " + typeName + " already defined", token);
//  	}

	if( originalType instanceof ConstrTypeSpec ||
	    originalType instanceof FixedPointType || 
	    originalType instanceof SequenceType ||
	    originalType instanceof ArrayTypeSpec )
        {
	    originalType.parse();
            if( originalType.typeName().indexOf( '.' ) < 0 )
            {
                String tName = null;
                if( originalType instanceof SequenceType ||
                    originalType instanceof ArrayTypeSpec )
                {
                    tName = originalType.typeName().substring( 0, originalType.typeName().indexOf('['));
                }
                else
                    tName = originalType.typeName();

                addImportedName( tName );
//                  imports.put( tName, "" );
//                  imports.put( tName + "Helper", "" );
            }        
        }

	if( originalType instanceof ScopedName )
        {
	    originalType = ((ScopedName)originalType).resolvedTypeSpec();

            addImportedName( originalType.typeName() );

//              if( originalType.typeName().indexOf( '.' ) < 0 )
//              {
//                  imports.put( originalType.typeName(), "" );
//                  imports.put( originalType.typeName() + "Helper", "" );
//              }
        }


    }

    public String toString()
    {
	return originalType.toString();
    }


    /**
     * @returns a string for an expression of type TypeCode 
     * 			that describes this type
     */

    public String getTypeCodeExpression()
    {
        return "org.omg.CORBA.ORB.init().create_alias_tc( " + 
            full_name() + "Helper.id(),\"" + name + "\"," +
            originalType.getTypeCodeExpression() + ")";
    }

    public String className()
    {
	String fullName = full_name();
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

    public void print(PrintWriter ps)
    {
	setPrintPhaseNames();

	/** no code generation for included definitions */
	if( included && !generateIncluded() )
	    return;

	/** only write once */

	if( written )
        {
	    return;
        }

	written = true;

	try
	{
            if( !(originalType.typeSpec() instanceof TemplateTypeSpec) )
                originalType.print(ps);

	    String className = className();

	    String path = parser.out_dir + fileSeparator + 
		pack_name.replace('.', fileSeparator );

	    File dir = new File( path );
	    if( !dir.exists() )
            {
		if( !dir.mkdirs())
		{
                    org.jacorb.idl.parser.fatal_error( "Unable to create " + path, null );
		}
            }

	    String fname = null;
	    PrintWriter decl_ps = null;
 
	    if( ! originalType.basic() || 
                ( originalType instanceof TemplateTypeSpec && 
                  !(originalType instanceof StringType )))
	    {
		/** print the holder class */

		fname =  className + "Holder.java";
		decl_ps = new PrintWriter( new java.io.FileWriter(new File(dir,fname)));
		printHolderClass( className, decl_ps );
		decl_ps.close();
	    }

	    /** print the helper class */

	    fname = className + "Helper.java";
	    decl_ps = new PrintWriter(new java.io.FileWriter(new File(dir,fname)));
	    printHelperClass( className, decl_ps );
	    decl_ps.close();

	    written = true;
	} 
	catch ( java.io.IOException i )
	{
	    System.err.println("File IO error");
	    i.printStackTrace();
	}
    }

    public String printReadStatement(String varname, String streamname)
    {
	//	return typeName() + "Helper.read(" + Streamname +")" ;


	if( originalType.basic()  &&  !(originalType instanceof TemplateTypeSpec))
	{
	    return originalType.printReadStatement(varname, streamname);
	}
	else
	{
	    return varname + " = " + full_name() + "Helper.read(" + streamname +");" ;
	    //	    return toString() + "Helper.read(" + streamname +")" ;
	}
    }

    public String printReadExpression(String streamname)
    {
	//	return typeName() + "Helper.read(" + Streamname +")" ;


	if( originalType.basic()  &&  
            !(originalType instanceof TemplateTypeSpec))
	{
	    return originalType.printReadExpression(streamname);
	}
	else
	{
	    return full_name() + "Helper.read(" + streamname +")" ;
	    //	    return toString() + "Helper.read(" + streamname +")" ;
	}
    }

    public String printWriteStatement(String var_name, String streamname)
    {
	//return typeName()+"Helper.write(" + streamname +"," + var_name +");";
	if( originalType.basic()  &&  !(originalType instanceof TemplateTypeSpec))
	{
	    return originalType.printWriteStatement( var_name,streamname);
	}
	else
	{
	    return full_name()+"Helper.write(" + streamname +"," + var_name +");";
	}
    }

    private void printClassComment(String className, PrintWriter ps)
    {
	ps.println("/**");
	ps.println(" *\tGenerated from IDL definition of alias " + 
                    "\"" + className + "\"" );
        ps.println(" *\t@author JacORB IDL compiler ");
        ps.println(" */\n");
    }



    public String holderName()
    {
	if( ( originalType.basic() && 
           (  !(originalType instanceof TemplateTypeSpec) ||
              originalType instanceof StringType )
              )
            || originalType instanceof AliasTypeSpec
              )
	{
	    return originalType.holderName();
	}
	else
	{
	    return full_name() + "Holder";
	}
    }


    private void printHolderClass(String className, PrintWriter ps)
    {
	if( !pack_name.equals(""))
	    ps.println("package " + pack_name + ";" );

        printImport( ps );

        printClassComment( className, ps );

	ps.println("final public class " + className + "Holder");
	ps.println("\timplements org.omg.CORBA.portable.Streamable");
	ps.println("{");

	ps.println("\tpublic " + originalType.typeName() + " value;\n");

	ps.println("\tpublic " + className + "Holder ()");
	ps.println("\t{");
	ps.println("\t}");

	ps.println("\tpublic " + className + "Holder (" + originalType.typeName() + " initial)");
	ps.println("\t{");
	ps.println("\t\tvalue = initial;");
	ps.println("\t}");

	ps.println("\tpublic org.omg.CORBA.TypeCode _type()");
	ps.println("\t{");
	ps.println("\t\treturn " + className  + "Helper.type();");
	ps.println("\t}");

	ps.println("\tpublic void _read(org.omg.CORBA.portable.InputStream in)");
	ps.println("\t{");
	ps.println("\t\tvalue = " + className  + "Helper.read(in);");
	ps.println("\t}");

	ps.println("\tpublic void _write(org.omg.CORBA.portable.OutputStream out)");
	ps.println("\t{");
	ps.println("\t\t" + className + "Helper.write(out,value);");
	ps.println("\t}");

	ps.println("}");
    }


    private void printHelperClass(String className, PrintWriter ps)
    {
	if( !pack_name.equals(""))
	    ps.println("package " + pack_name + ";" );

        printImport( ps );

        printClassComment( className, ps );

	ps.println("public class " + className + "Helper");
	ps.println("{");

	ps.println("\tprivate static org.omg.CORBA.TypeCode _type = "+
                   getTypeCodeExpression()+";");

	String type = originalType.typeName();

	ps.println("\tpublic " + className + "Helper ()");
	ps.println("\t{");
	ps.println("\t}");

	ps.println("\tpublic static void insert (org.omg.CORBA.Any any, " + type + " s)");
	ps.println("\t{");
        ps.println("\t\tany.type (type ());");       
        ps.println("\t\twrite (any.create_output_stream (), s);");
        ps.println("\t}");

	ps.println("\tpublic static " + type + " extract (org.omg.CORBA.Any any)");
	ps.println("\t{");
        ps.println("\t\treturn read (any.create_input_stream ());");
	ps.println("\t}");

	ps.println("\tpublic static org.omg.CORBA.TypeCode type ()");
	ps.println("\t{");
	ps.println("\t\treturn _type;");
	ps.println("\t}");

	printIdMethod( ps ); // inherited from IdlSymbol

	if( originalType.basic() || originalType instanceof AnyType )
	{
	    /* read */
	    ps.println("\tpublic static " +type+ " read(org.omg.CORBA.portable.InputStream _in)");
	    ps.println("\t{");	
	    ps.println("\t\t" + type + " _result;");
	    ps.println("\t\t" + originalType.printReadStatement("_result","_in"));
	    ps.println("\t\treturn _result;");
	    ps.println("\t}");

	    /* write */
	    ps.println("\tpublic static void write(org.omg.CORBA.portable.OutputStream _out, " + type + " _s)");
	    ps.println("\t{");
	    ps.println("\t\t" + originalType.printWriteStatement("_s","_out"));
	    ps.println("\t}");
	    ps.println("}");    
	}
	else
	{
	    String helpername = ( originalType instanceof AliasTypeSpec ? 
				  originalType.full_name() : originalType.typeName() ) + "Helper";
	    /* read */
	    ps.println("\tpublic static " +type+ " read(org.omg.CORBA.portable.InputStream _in)");
	    ps.println("\t{");	
	    ps.println("\t\treturn " + helpername +".read(_in);");
	    ps.println("\t}");

	    /* write */
	    ps.println("\tpublic static void write(org.omg.CORBA.portable.OutputStream _out, " + type + " _s)");
	    ps.println("\t{");
	    ps.println("\t\t" +helpername + ".write(_out,_s);");
	    ps.println("\t}");
	    ps.println("}");
	}
    }

}



