package org.jacorb.security.ssl.sun_jsse;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 2000-2004 Gerald Brose.
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


import java.security.*;
import java.security.cert.*;
import java.io.*;

/**
 * A class with utility methods that help managing a key store.
 *
 * @author Gerald Brose
 * @version $Id: KeyStoreUtil.java,v 1.8 2004-05-06 12:40:01 nicolas Exp $
 */

public class KeyStoreUtil
{
    /**
     * @return - a fully loaded and operational KeyStore
     * @param file_name - a keystore file name to be loaded
     * @param storepass - the password for managing the keystore
     */

    public static KeyStore getKeyStore( String file_name, 
					char[] storepass )
        throws IOException, java.security.GeneralSecurityException
    {
        //try unchanged name first
        File f = new File( file_name );        
        if( ! f.exists() )
        {
            //try to prepend home dir
            String name = 
                System.getProperty( "user.home" ) +
                System.getProperty( "file.separator" ) +
                file_name;

            f = new File( name );
            
            if( ! f.exists() )
            {
                throw new IOException("Unable to find keystore file" + 
                                      (new File( file_name )).getAbsolutePath() );
            }
        }

        FileInputStream in = new FileInputStream( f );
        KeyStore ks = KeyStore.getInstance( "JKS" );	
        ks.load( in, storepass );
        in.close();
        return ks;
    }
}
