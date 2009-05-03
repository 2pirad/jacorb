package org.jacorb.orb.giop;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2004 Gerald Brose.
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

import java.io.IOException;
import java.util.List;
import org.jacorb.config.*;
import org.slf4j.Logger;
import org.jacorb.orb.ORB;
import org.jacorb.orb.SystemExceptionHelper;
import org.jacorb.orb.dsi.ServerRequest;
import org.jacorb.poa.POA;
import org.omg.CONV_FRAME.CodeSetContext;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.GIOP.LocateStatusType_1_2;
import org.omg.GIOP.ReplyStatusType_1_2;

/**
 * @author Nicolas Noffke
 * @version $Id: ServerRequestListener.java,v 1.29 2009-05-03 21:35:56 andre.spiegel Exp $
 */
public class ServerRequestListener
    implements RequestListener, Configurable
{
    private final ORB orb;
    private final POA rootPOA;

    /** the configuration object  */
    private Logger logger = null;
    private boolean require_ssl = false;

    public ServerRequestListener( ORB orb,
                                  POA rootPOA )
    {
        this.orb = orb;
        this.rootPOA = rootPOA;
    }

    public void configure(Configuration myConfiguration)
        throws ConfigurationException
    {
        org.jacorb.config.Configuration configuration = (org.jacorb.config.Configuration)myConfiguration;
        logger =
            configuration.getLogger("jacorb.giop.server.listener");

        boolean supportSSL =
            configuration.getAttribute("jacorb.security.support_ssl","off").equals("on");

        if( supportSSL )
        {
            int required =
                configuration.getAttributeAsInteger("jacorb.security.ssl.server.required_options",16);

            //if we require EstablishTrustInTarget or
            //EstablishTrustInClient, SSL must be used.
            require_ssl = supportSSL && (required & 0x60) != 0;
        }
    }

    public void requestReceived( byte[] request,
                                 GIOPConnection connection )
    {
        RequestInputStream inputStream = new RequestInputStream( orb, request );

        if( require_ssl && ! connection.isSSL() )
        {
            ReplyOutputStream out =
                new ReplyOutputStream( inputStream.req_hdr.request_id,
                                       ReplyStatusType_1_2.SYSTEM_EXCEPTION,
                                       inputStream.getGIOPMinor(),
                                       false,
                                       logger); //no locate reply

            logger.debug("About to reject request because connection is not SSL.");

            SystemExceptionHelper.write( out,
                                         new NO_PERMISSION( 3, CompletionStatus.COMPLETED_NO ));

            try
            {
                connection.sendReply( out );
            }
            catch( IOException e )
            {
                logger.warn("IOException",e);
            }

            return;
        }

        //only block timeouts, if a reply needs to be sent
        if( Messages.responseExpected( inputStream.req_hdr.response_flags ))
        {
            connection.incPendingMessages();
        }

        if( ! connection.isTCSNegotiated() )
        {
            //If GIOP 1.0 is used don't check for a codeset context
            if( inputStream.getGIOPMinor() == 0 )
            {
                connection.markTCSNegotiated();
            }
            else
            {
                CodeSetContext ctx = CodeSet.getCodeSetContext( inputStream.req_hdr.service_context );

                if( ctx != null )
                {
                    connection.setCodeSets( ctx.char_data, ctx.wchar_data );
                    connection.markTCSNegotiated();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Received CodeSetContext. Using " +
                                     CodeSet.csName( ctx.char_data ) +
                                     " as TCS and " +
                                     CodeSet.csName( ctx.wchar_data ) +
                                     " as TCSW" );
                    }
                }
            }
        }

        inputStream.setCodeSet( connection.getTCS(), connection.getTCSW() );

        inputStream.updateMutatorConnection(connection);

        ServerRequest server_request = null;

        try
        {
            server_request =
            new ServerRequest( orb, inputStream, connection );
        }
        catch( org.jacorb.poa.except.POAInternalError pie )
        {
            logger.warn("Received a request with a non-jacorb object key" );

            if( inputStream.isLocateRequest() )
            {
                LocateReplyOutputStream lr_out =
                new LocateReplyOutputStream(inputStream.req_hdr.request_id,
                                            LocateStatusType_1_2._UNKNOWN_OBJECT,
                                            inputStream.getGIOPMinor() );

                try
                {
                    connection.sendReply( lr_out );
                }
                catch( IOException e )
                {
                    logger.warn("IOException",e);
                }
            }
            else
            {
                ReplyOutputStream out =
                    new ReplyOutputStream( inputStream.req_hdr.request_id,
                                           ReplyStatusType_1_2.SYSTEM_EXCEPTION,
                                           inputStream.getGIOPMinor(),
                                           false,
                                           logger );//no locate reply

                SystemExceptionHelper.write( out,
                                             new OBJECT_NOT_EXIST( 0, CompletionStatus.COMPLETED_NO ));

                try
                {
                    connection.sendReply( out );
                }
                catch( IOException e )
                {
                    logger.warn("IOException",e);
                }
            }

            return;
        }

        deliverRequest( server_request );
    }

    public void locateRequestReceived ( byte[] request,
                                        GIOPConnection connection )
    {
        //for the time being, map to normal request
        requestReceived( request, connection );
    }


    public void cancelRequestReceived( byte[] request,
                                       GIOPConnection connection )
    {
        // nothing to do
    }

    private void deliverRequest( ServerRequest request )
    {
        POA tmp_poa = rootPOA;
        String res;
        List scopes;

        try
        {
            // This is similar to code within ORB::findPOA but
            // sufficiently different that it is reproduced here.
           String refImplName = "";
           final String orbImplName = orb.getImplName();
           final String orbServerId = orb.getServerIdString();

           try
           {
              refImplName =
                 org.jacorb.poa.util.POAUtil.extractImplName( request.objectKey() );
           }
           catch( org.jacorb.poa.except.POAInternalError pie )
           {
              logger.debug
                  ("serverRequestListener: reference generated by foreign POA");
           }
           if( !(orbImplName.equals(refImplName)) &&
               !(orbServerId.equals(refImplName)))
           {
               if (logger.isDebugEnabled())
               {
                   logger.debug
                   (
                      "serverRequestListener: impl_name mismatch (refImplName: " +
                      refImplName +
                      " and orbServerId " +
                      orbServerId +
                      " and orbImplName " +
                      orbImplName
                   );
               }
              throw new org.omg.PortableServer.POAPackage.WrongAdapter();
           }

            // Get cached scopes from ServerRequest
            scopes = request.getScopes();

            for( int i = 0; i < scopes.size(); i++)
            {
                res = ((String)scopes.get (i));

                if( res.equals(""))
                {
                    break;
                }

                /* the following is a call to a method in the private
                   interface between the ORB and the POA. It does the
                   necessary synchronization between incoming,
                   potentially concurrent requests to activate a POA
                   using its adapter activator. This call will block
                   until the correct POA is activated and ready to
                   service requests. Thus, concurrent calls
                   originating from a single, multi-threaded client
                   will be serialized because the thread that accepts
                   incoming requests from the client process is
                   blocked. Concurrent calls from other destinations
                   are not serialized unless they involve activating
                   the same adapter.
                */

                try
                {
                    tmp_poa = tmp_poa._getChildPOA( res );
                }
                catch ( org.jacorb.poa.except.ParentIsHolding p )
                {
                    /* if one of the POAs is in holding state, we
                       simply deliver deliver the request to this
                       POA. It will forward the request to its
                       child POAs if necessary when changing back
                       to active For the POA to be able to forward
                       this request to its child POAa, we need to
                       supply the remaining part of the child's
                       POA name */

                    String [] rest_of_name = new String[scopes.size () - i];
                    for( int j = 0; j < i; j++ )
                    {
                        rest_of_name[j] = (String)scopes.get( j+i );
                    }

                    request.setRemainingPOAName(rest_of_name);

                    break;
                }
            }

            if( tmp_poa == null )
            {
                throw new org.omg.CORBA.INTERNAL("Request POA null!");
            }
            /* hand over to the POA */
            tmp_poa._invoke( request );
        }
        catch( org.omg.PortableServer.POAPackage.WrongAdapter e )
        {
            // unknown oid (not previously generated)
            request.setSystemException( new org.omg.CORBA.OBJECT_NOT_EXIST("unknown oid") );
            request.reply();
        }
        catch( org.omg.CORBA.SystemException e )
        {
            request.setSystemException( e );
            request.reply();
        }
        catch( RuntimeException e )
        {
            request.setSystemException( new org.omg.CORBA.UNKNOWN( e.toString()) );
            request.reply();
            logger.warn("unexpected exception",e);
        }
    }
}
