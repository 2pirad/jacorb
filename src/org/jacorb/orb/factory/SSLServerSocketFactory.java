package org.jacorb.orb.factory;

/* 
 * 
 * @author Nicolas Noffke
 * $Id: SSLServerSocketFactory.java,v 1.4 2002-03-19 09:25:32 nicolas Exp $
 */

import java.net.*;

public interface SSLServerSocketFactory
    extends ServerSocketFactory
{
    public void switchToClientMode( Socket socket );
    
    public boolean isSSL( ServerSocket socket );
}













