package org.jacorb.orb;

import java.util.Enumeration;

import org.jacorb.orb.connection.*;
import org.jacorb.orb.portableInterceptor.*;
import org.jacorb.util.Debug;

import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.IOP.ServiceContext;
import org.omg.GIOP.ReplyHeader_1_2;
import org.omg.GIOP.ReplyStatusType_1_2;
import org.omg.PortableInterceptor.*;

/**
 * An instance of this class handles all interactions between one particular
 * client request and any interceptors registered for it.
 * 
 * @author Andre Spiegel
 * @version $Id: ClientInterceptorHandler.java,v 1.1.2.2 2002-10-20 14:46:56 andre.spiegel Exp $
 */
public class ClientInterceptorHandler
{
    private ClientRequestInfoImpl info = null;
    
    /**
     * Constructs an interceptor handler for the given parameters.
     * If no interceptors are registered on the client side,
     * the resulting object will be a dummy object that does nothing when
     * invoked.
     */
    public ClientInterceptorHandler 
                      ( org.jacorb.orb.ORB orb,
                        org.jacorb.orb.connection.RequestOutputStream ros,
                        org.omg.CORBA.Object self,
                        org.jacorb.orb.Delegate delegate,
                        org.jacorb.orb.ParsedIOR piorOriginal,
                        org.jacorb.orb.connection.ClientConnection connection )
    {
        if ( orb.hasClientRequestInterceptors() )
        {
            info = new ClientRequestInfoImpl ( orb, ros, self, delegate,
                                               piorOriginal, connection );
        }
    }
    
    public void handle_send_request() throws RemarshalException
    {
        if ( info != null )
        {
            invokeInterceptors ( info, ClientInterceptorIterator.SEND_REQUEST );
            
            // Add any new service contexts to the message
            Enumeration ctx = info.getRequestServiceContexts();
            
            while ( ctx.hasMoreElements() )
            {
                info.request_os.addServiceContext 
                                       ( ( ServiceContext ) ctx.nextElement() );
            }           
        }
    }   

    public void handle_location_forward ( ReplyInputStream     reply,
                                          org.omg.CORBA.Object forward_reference )
        throws RemarshalException
    {
        if ( info != null )
        {
            info.reply_status = LOCATION_FORWARD.value;
            info.setReplyServiceContexts( reply.rep_hdr.service_context );

            info.forward_reference = forward_reference;

            //allow interceptors access to reply input stream
            info.reply_is = reply;

            invokeInterceptors( info,
                                ClientInterceptorIterator.RECEIVE_OTHER );
        }
    }

    public void handle_receive_reply ( ReplyInputStream reply )
        throws RemarshalException
    {
        if ( info != null )
        {                                          
            ReplyHeader_1_2 header = reply.rep_hdr;

            if ( header.reply_status.value() == ReplyStatusType_1_2._NO_EXCEPTION )
            {
                info.reply_status = SUCCESSFUL.value;

                info.setReplyServiceContexts( header.service_context );

                // the case that invoke was called from
                // dii.Request._invoke() will be handled inside
                // of dii.Request._invoke() itself, because the
                // result will first be available there
                if ( info.request_os.getRequest() == null )
                {
                    InterceptorManager manager = info.orb.getInterceptorManager();
                    info.current = manager.getCurrent();

                    //allow interceptors access to reply input stream
                    info.reply_is = reply;

                    invokeInterceptors( info,
                                        ClientInterceptorIterator.RECEIVE_REPLY );
                }
                else
                    info.request_os.getRequest().setInfo( info );
            }
        }
    }
            
    public void handle_receive_other ( short reply_status )
        throws RemarshalException
    {
        if ( info != null )
        {
            info.reply_status = reply_status;
            invokeInterceptors ( info, ClientInterceptorIterator.RECEIVE_OTHER );
        }   
    }

    public void handle_receive_exception ( org.omg.CORBA.SystemException ex )
        throws RemarshalException
    {
        handle_receive_exception ( ex, null );
    }        

    public void handle_receive_exception ( org.omg.CORBA.SystemException ex,
                                           ReplyInputStream reply )
        throws RemarshalException
    {
        if ( info != null )
        {
            SystemExceptionHelper.insert ( info.received_exception, ex );
            try
            {
                info.received_exception_id =
                    SystemExceptionHelper.type ( ex ).id();
            }
            catch ( org.omg.CORBA.TypeCodePackage.BadKind bk )
            {
                Debug.output ( Debug.INTERCEPTOR | Debug.INFORMATION, bk );
            }
            info.reply_status = SYSTEM_EXCEPTION.value;

            if ( reply != null )
            {
                info.setReplyServiceContexts ( reply.rep_hdr.service_context );
                info.reply_is = reply;
            }
            
            invokeInterceptors ( info,
                                 ClientInterceptorIterator.RECEIVE_EXCEPTION );
        }
    }

    public void handle_receive_exception ( ApplicationException ex,
                                           ReplyInputStream reply )
        throws RemarshalException
    {
        if ( info != null )
        {
            info.received_exception_id = ex.getId();
            try
            {
                ApplicationExceptionHelper.insert( info.received_exception, 
                                                   ex );
            }
            catch ( Exception e )
            {
                Debug.output ( Debug.INTERCEPTOR | Debug.INFORMATION, e );
                SystemExceptionHelper.insert ( info.received_exception,
                                               new org.omg.CORBA.UNKNOWN
                                                  ( e.getMessage() ) );
            }
            info.reply_status = USER_EXCEPTION.value;

            try
            {
                reply.reset();
            }
            catch ( Exception e )
            {
                // shouldn't happen anyway
                Debug.output ( Debug.INTERCEPTOR | Debug.INFORMATION, e);
            }                        

            info.setReplyServiceContexts ( reply.rep_hdr.service_context );
            info.reply_is = reply;
            
            invokeInterceptors ( info,
                                 ClientInterceptorIterator.RECEIVE_EXCEPTION );
        }
    }
        
    private void invokeInterceptors( ClientRequestInfoImpl info, short op )
      throws RemarshalException
    {
        ClientInterceptorIterator intercept_iter =
            info.orb.getInterceptorManager().getClientIterator();

        try
        {
            intercept_iter.iterate( info, op );
        }
        catch ( org.omg.PortableInterceptor.ForwardRequest fwd )
        {
            info.delegate.rebind( info.orb.object_to_string( fwd.forward ) );
            throw new RemarshalException();
        }
        catch ( org.omg.CORBA.UserException ue )
        {
            Debug.output( Debug.INTERCEPTOR | Debug.IMPORTANT, ue );
        }
    }

    
}
