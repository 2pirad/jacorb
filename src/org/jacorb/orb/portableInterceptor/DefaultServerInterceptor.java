package org.jacorb.orb.portableInterceptor;

import org.omg.PortableInterceptor.*;

/**
 * DefaultServerInterceptor.java
 *
 * A simple base class for user-defined server interceptors
 *
 * @author Gerald Brose.
 * @version $Id: DefaultServerInterceptor.java,v 1.2 2001-11-19 09:42:47 jacorb Exp $
 */

public abstract class DefaultServerInterceptor
    extends org.jacorb.orb.LocalityConstrainedObject 
    implements ServerRequestInterceptor
{

    // InterceptorOperations interface
    public abstract String name();

    public void destroy()
    {
    }

    public void receive_request_service_contexts( ServerRequestInfo ri ) 
        throws ForwardRequest
    {
    }

    public void receive_request( ServerRequestInfo ri )
        throws ForwardRequest
    {
    }

    public void send_reply(ServerRequestInfo ri)
    {
    }

    public void send_exception(ServerRequestInfo ri)
        throws ForwardRequest
    {
    }

    public void send_other(ServerRequestInfo ri) 
        throws ForwardRequest
    {
    }

}






