package org.jacorb.orb.factory;

/* 
 * 
 * @author Nicolas Noffke
 * $Id: SSLServerSocketFactory.java,v 1.2 2001-03-17 18:44:59 brose Exp $
 */

import java.net.*;

public interface SSLServerSocketFactory
    extends ServerSocketFactory
{
    public void switchToClientMode( Socket socket );
    
    public boolean isSSL( ServerSocket socket );
}







