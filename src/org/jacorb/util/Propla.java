package org.jacorb.util;
/**
 * The JacORB Properties Launcher<br>
 * This class does the following:
 * <OL>
 * <LI> Copy the properties of the passed in properties 
 * file to the system properties.</LI>
 * <LI>Call the passed in classes main method with the additional args.</LI>
 * </OL>
 * The reason for that is that usually in a complex setup the different 
 * processes have a large common set, and a small individual set of 
 * properties. With this class, you don't have to edit the global properties
 * each time a different process is started, but can keep the individual 
 * properties on a separate file for each different process.
 * <P>Created: Mon Apr  2 11:35:24 2001
 *
 * @author Nicolas Noffke
 * $Id: Propla.java,v 1.1 2001-04-02 10:22:37 noffke Exp $
 */
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

public class Propla 
{
        
    public static void main( String[] args ) 
    {
        if( args.length < 2 )
        {
            System.out.println("Usage: jaco org.jacorb.util.Propla <properties-file> <class name> [<classes args>]*");
            System.exit( -1 );        
        }
        
        try
        {
            File prop_file = new File( args[ 0 ] );

            if( ! prop_file.exists() )
            {
                System.out.println("Properties file " + 
                                   prop_file.getAbsolutePath() + 
                                   " doesn't exist!");
                System.exit( -1 );        
            }

            if( prop_file.isDirectory() )
            {
                System.out.println("Properties file " + 
                                   prop_file.getAbsolutePath() + 
                                   " is a directory!");
                System.exit( -1 );        
            }

            Properties props = new Properties();
            props.load( new FileInputStream( prop_file ));
            
            Properties sys_props = System.getProperties();

            //this is strictly for jdk1.1 compatibility
            //otherwise putAll() would be more convenient
            for( Enumeration keys = props.keys();
                 keys.hasMoreElements(); )
            {
                String key = (String) keys.nextElement();

                sys_props.put( key, props.getProperty( key ));
            }

            Class c = Class.forName( args[1] );
            Method main = c.getDeclaredMethod( "main", 
                                               new Class[]{ String[].class } );
            String[] new_args = new String[ args.length - 2 ];
            System.arraycopy( args, 2, new_args, 0, new_args.length );

            main.invoke( null, new Object[]{ new_args } );
        }
        catch( Exception e )
        {
            Debug.output( 1, e );                        
        }

        System.exit( 0 );
    }    
} // Propla
