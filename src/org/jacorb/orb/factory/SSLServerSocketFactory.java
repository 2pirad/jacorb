package org.jacorb.orb.factory;

/* 
 * 
 * @author Nicolas Noffke
 * $Id: SSLServerSocketFactory.java,v 1.3 2001-03-19 11:08:51 brose Exp $
 */

import java.net.*;

public interface SSLServerSocketFactory
    extends ServerSocketFactory
{
    public void switchToClientMode( Socket socket );
    
    public boolean isSSL( ServerSocket socket );
}













