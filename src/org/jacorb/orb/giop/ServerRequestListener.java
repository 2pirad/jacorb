/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2001  Gerald Brose.
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
package org.jacorb.orb.connection;

import org.jacorb.orb.dsi.ServerRequest;
import org.jacorb.orb.*;

import org.jacorb.poa.*;
import org.jacorb.poa.util.POAUtil;

import org.jacorb.util.*;

import java.io.IOException;
import java.util.StringTokenizer;

import org.omg.GIOP.ReplyStatusType_1_2;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.CompletionStatus;

/**
 * ServerRequestListener.java
 *
 *
 * Created: Sun Aug 12 22:26:25 2001
 *
 * @author Nicolas Noffke
 * @version $Id: ServerRequestListener.java,v 1.1.2.2 2001-09-05 09:50:56 jacorb Exp $
 */

public class ServerRequestListener 
    implements RequestListener 
{
    private ORB orb = null;
    private POA rootPOA = null;

    public ServerRequestListener( org.omg.CORBA.ORB orb, 
                                  org.omg.PortableServer.POA rootPOA )
    {
        this.orb = (ORB) orb;
        this.rootPOA = (POA) rootPOA;
    }
    
    public void requestReceived( byte[] request,
                                 GIOPConnection connection )
    {
        if( Environment.enforceSSL() && 
            ! connection.isSSL()) 
        {
            int giop_minor = Messages.getGIOPMinor( request );
            
            ReplyOutputStream out = 
                new ReplyOutputStream( Messages.getRequestId( request ),
                                       ReplyStatusType_1_2.SYSTEM_EXCEPTION,
                                       giop_minor );
        
            SystemExceptionHelper.write( out, 
                  new NO_PERMISSION( 3, CompletionStatus.COMPLETED_NO ));

            try
            {
                connection.sendMessage( out );
            }
            catch( IOException e )
            {
                Debug.output( 1, e );
            }        
            
            return;
        } 

        RequestInputStream in = 
            new RequestInputStream( orb, request );

        ServerRequest server_request = 
            new ServerRequest( orb, in, connection );

        orb.getBasicAdapter().replyPending();

        /*
        // devik: look for codeset context if not negotiated yet
        if( !connection.isTCSNegotiated() )
        {
            // look for codeset service context
            connection.setServerCodeSet( request.getServiceContext() );
        }
        */

        deliverRequest( server_request );
    }

    public void locateRequestReceived( byte[] request,
                                       GIOPConnection connection )
    {
        /*
        LocateRequest server_request = 
            new LocateRequest( orb, request, connection );

        deliverRequest( server_request );
        */
    }

    
    public void cancelRequestReceived( byte[] request,
                                       GIOPConnection connection )
    {

    }

    
    public void fragmentReceived( byte[] fragment,
                                  GIOPConnection connection )
    {

    }

    private void deliverRequest( ServerRequest request )
    {
        POA tmp_poa = rootPOA;
        
        try
        {
            String poa_name = POAUtil.extractPOAName( request.objectKey() );

            /*
             * strip scoped poa name (first part of the object key
             * before "::", will be empty for the root poa 
             */
            
            StringTokenizer strtok = 
                new StringTokenizer( poa_name, 
                                     POAConstants.OBJECT_KEY_SEPARATOR );

            String scopes[]  = new String[ strtok.countTokens() ];

            for( int i = 0; strtok.hasMoreTokens(); i++ )
            {
                scopes[i] = strtok.nextToken();
            }

            for( int i = 0; i < scopes.length; i++)
            {
                if( scopes[i].equals(""))
                    break;
                
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
                    tmp_poa = tmp_poa._getChildPOA( scopes[i] );
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
                    
                    String [] rest_of_name = new String[scopes.length - i];
                    for( int j = 0; j < i; j++ )
                    {
                        rest_of_name[j] = scopes[j+i];
                    }

                    request.setRemainingPOAName(rest_of_name);

                    break;
                }           
            }
              

            if( tmp_poa == null )
            {
                throw new Error("request POA null!");
            }
            else
            {
                /* hand over to the POA */
                tmp_poa._invoke( request );
            }
            
        }
        catch( org.omg.PortableServer.POAPackage.WrongAdapter wa )
        {
            // unknown oid (not previously generated)
            request.setSystemException( new org.omg.CORBA.OBJECT_NOT_EXIST("unknown oid") );
            request.reply();
        }
        catch( org.omg.CORBA.SystemException one )
        {
            request.setSystemException( one );
            request.reply();
        }
        catch( Throwable th )
        {
            request.setSystemException( new org.omg.CORBA.UNKNOWN( th.toString()) );
            request.reply();
            th.printStackTrace(); // TODO
        }                       
    }
}// ServerRequestListener













