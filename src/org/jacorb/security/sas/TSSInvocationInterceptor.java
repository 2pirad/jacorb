package org.jacorb.security.sas;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 2000-2002 Nicolas Noffke, Gerald Brose.
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

import java.io.*;
import org.omg.SecurityReplaceable.*;
import org.omg.Security.*;
import org.ietf.jgss.*;

import org.omg.PortableInterceptor.*;
import org.omg.CORBA.ORBPackage.*;
import org.omg.CORBA.Any;
import org.omg.CORBA.*;

import org.jacorb.util.*;
import org.jacorb.orb.portableInterceptor.ServerRequestInfoImpl;
import org.jacorb.orb.*;
import org.omg.IOP.*;
import org.omg.GIOP.*;
import org.omg.CSI.*;
import org.jacorb.orb.connection.*;
import org.jacorb.orb.dsi.ServerRequest;

/**
 * This is the SAS Target Security Service (TSS) Interceptor
 *
 * @author David Robison
 * @version $Id: TSSInvocationInterceptor.java,v 1.10 2002-10-17 19:35:54 david.robison Exp $
 */

public class TSSInvocationInterceptor
    extends org.omg.CORBA.LocalObject
    implements ServerRequestInterceptor
{
    private static final String DEFAULT_NAME = "TSSInvocationInterceptor";
    private static final int SecurityAttributeService = 15;

    private static GSSCredential myCredential = null;
    private static boolean context_stateful = true;
    private static short targetRequires = (short)0;
    private static boolean use_ssl = false;

    private String name = null;
    private org.jacorb.orb.ORB orb = null;
    private Codec codec = null;
    private int sourceNameSlotID = -1;
    private int contextMsgSlotID = -1;
    private int sasReplySlotID = -1;

    public TSSInvocationInterceptor(org.jacorb.orb.ORB orb, Codec codec, int sourceNameSlotID, int contextMsgSlotID, int sasReplySlotID)
    {
        this.orb = orb;
        this.codec = codec;
        this.sourceNameSlotID = sourceNameSlotID;
        this.contextMsgSlotID = contextMsgSlotID;
        this.sasReplySlotID = sasReplySlotID;
        name = DEFAULT_NAME;
        context_stateful = Boolean.valueOf(org.jacorb.util.Environment.getProperty("jacorb.security.sas.tss.stateful", "true")).booleanValue();

        // see what transport modes are required
        String targetRequiresNames = org.jacorb.util.Environment.getProperty( "jacorb.security.sas.tss.target_requires", "" );
        java.util.StringTokenizer nameTokens = new java.util.StringTokenizer(targetRequiresNames, ":;, ");
        while (nameTokens.hasMoreTokens())
        {
          String token = nameTokens.nextToken();
          if (token.equals("Integrity"))                   targetRequires |= org.omg.CSIIOP.Integrity.value;
          else if (token.equals("Confidentiality"))        targetRequires |= org.omg.CSIIOP.Confidentiality.value;
          else if (token.equals("EstablishTrustInTarget")) targetRequires |= org.omg.CSIIOP.EstablishTrustInTarget.value;
          else if (token.equals("EstablishTrustInClient")) targetRequires |= org.omg.CSIIOP.EstablishTrustInClient.value;
          else if (token.equals("IdentityAssertion"))      targetRequires |= org.omg.CSIIOP.IdentityAssertion.value;
          else if (token.equals("DelegationByClient"))     targetRequires |= org.omg.CSIIOP.DelegationByClient.value;
          else org.jacorb.util.Debug.output(1, "Unknown SAS Association Taken: " + token);
        }
        use_ssl =(targetRequires & EstablishTrustInTarget.value) != 0 || (targetRequires & EstablishTrustInClient.value) != 0;
    }

    public String name()
    {
        return name;
    }

    public void destroy()
    {
    }

    public static void setMyCredential(GSSCredential cred) {
        myCredential = cred;
    }

    public void receive_request( ServerRequestInfo ri )
        throws ForwardRequest
    {
        //System.out.println("receive_request");
    }


    public void receive_request_service_contexts( ServerRequestInfo ri )
        throws ForwardRequest
    {
        //System.out.println("receive_request_service_contexts");
        if (ri.operation().equals("_is_a")) return;
        if (ri.operation().equals("_non_existent")) return;
        if (ri.operation().equals("subscription_change")) return;
        GIOPConnection connection = ((ServerRequestInfoImpl) ri).request.getConnection();

        // verify SSL requirements
        if (use_ssl && !connection.isSSL())
        {
            Debug.output(1, "SSL required for operation " + ri.operation());
            throw new org.omg.CORBA.NO_PERMISSION("SSL Required!", MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
        }

        // parse service context
        SASContextBody contextBody = null;
        long client_context_id = 0;
        byte[] contextToken = null;
        GSSManager gssManager = TSSInitializer.gssManager;
        try
        {
            ServiceContext ctx = ri.get_request_service_context(SecurityAttributeService);
            Any ctx_any = codec.decode( ctx.context_data );
            contextBody = SASContextBodyHelper.extract(ctx_any);
        }
        catch (Exception e)
        {
            Debug.output(1, "Could not parse service context for operation " + ri.operation() + ": " + e);
            throw new org.omg.CORBA.NO_PERMISSION("SAS Error parsing service context: " + e, MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
        }

        // process MessageInContext
        if (contextBody.discriminator() == MTMessageInContext.value)
        {
            MessageInContext msg = null;
            try
            {
                msg = contextBody.in_context_msg();
                client_context_id = msg.client_context_id;
                contextToken = connection.getSASContext(msg.client_context_id);
            }
            catch (Exception e)
            {
                Debug.output(1, "Could not parse service MessageInContext: " + e);
                throw new org.omg.CORBA.NO_PERMISSION("SAS Error parsing MessageInContext: " + e, MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
            }
            if (contextToken == null)
            {
                Debug.output(1, "Could not parse service MessageInContext: " + msg.client_context_id);
                throw new org.omg.CORBA.NO_PERMISSION("SAS Error parsing MessageInContext", MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
            }
        }

        // process EstablishContext
        if (contextBody.discriminator() == MTEstablishContext.value)
        {
            EstablishContext msg = null;
            try
            {
                msg = contextBody.establish_msg();
                client_context_id = msg.client_context_id;

                // verify context
                Oid myMechOid = myCredential.getMechs()[0];
                GSSContext context = gssManager.createContext(myCredential);
                context.acceptSecContext(msg.client_authentication_token, 0, msg.client_authentication_token.length);
                GSSName sourceName = context.getSrcName();
                contextToken = sourceName.toString().getBytes();
            }
            catch (Exception e)
            {
                Debug.output(1, "Could not parse service EstablishContext: " + e);
                throw new org.omg.CORBA.NO_PERMISSION("SAS Error parsing EstablishContext: " + e, MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
            }
            if (contextToken == null)
            {
                Debug.output(1, "Could not parse service EstablishContext: " + msg.client_context_id);
                throw new org.omg.CORBA.NO_PERMISSION("SAS Error parsing EstablishContext", MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
            }

            // cache context
            if (context_stateful) connection.cacheSASContext(msg.client_context_id, contextToken, msg);
        }

        // set slots
        try
        {
            Any source_any = orb.create_any();
            source_any.insert_string(new String(contextToken));
            Any msg_any = orb.create_any();
            EstablishContextHelper.insert(msg_any, connection.getSASContextMsg(client_context_id));
            ri.set_slot( sourceNameSlotID, source_any);
            ri.set_slot( contextMsgSlotID, msg_any);
            ri.set_slot( sasReplySlotID, makeCompleteEstablishContext(client_context_id));
        }
        catch (Exception e)
        {
            Debug.output(1, "Error insert service context into slots: " + e);
            try { ri.set_slot( sasReplySlotID, makeContextError(client_context_id, 1, 1, contextToken)); } catch (Exception ee) {}
            throw new org.omg.CORBA.NO_PERMISSION("SAS Error insert service context into slots: " + e, MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_NO);
        }
    }

    public void send_reply( ServerRequestInfo ri )
    {
        //System.out.println("send_reply");
        Any slot_any = null;
        try {
            slot_any = ri.get_slot(sasReplySlotID);
        }
        catch (Exception e)
        {
            Debug.output(2, "No SAS reply found");
        }
        if (slot_any == null) return;

        try
        {
            ri.add_reply_service_context(new ServiceContext(SecurityAttributeService, codec.encode( slot_any ) ), true);
        }
        catch (Exception e)
        {
            Debug.output(1, "Error setting reply service context:" + e);
            throw new org.omg.CORBA.NO_PERMISSION("SAS Error setting reply service contex: " + e, MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_MAYBE);
        }
    }

    public void send_exception( ServerRequestInfo ri )
        throws ForwardRequest
    {
        //System.out.println("send_exception");
        try
        {
            ri.add_reply_service_context(new ServiceContext(SecurityAttributeService, codec.encode( ri.get_slot(sasReplySlotID) ) ), true);
        }
        catch (Exception e)
        {
            Debug.output(1, "Error setting reply service context:" + e);
            throw new org.omg.CORBA.NO_PERMISSION("SAS Error setting reply service context: " + e, MinorCodes.SAS_TSS_FAILURE, CompletionStatus.COMPLETED_MAYBE);
        }
    }

    public void send_other( ServerRequestInfo ri )
        throws ForwardRequest
    {
        //System.out.println("send_other");
    }

    private Any makeCompleteEstablishContext(long client_context_id) {
        CompleteEstablishContext msg = new CompleteEstablishContext();
        msg.client_context_id = client_context_id;
        msg.context_stateful = context_stateful;
        msg.final_context_token = new byte[0];
        SASContextBody contextBody = new SASContextBody();
        contextBody.complete_msg(msg);
        Any any = orb.create_any();
        SASContextBodyHelper.insert( any, contextBody );
        return any;
    }

    private Any makeContextError(long client_context_id, int major_status, int minor_status, byte[] error_token) {
        ContextError msg = new ContextError();
        msg.client_context_id = client_context_id;
        msg.error_token = error_token;
        msg.major_status = major_status;
        msg.minor_status = minor_status;
        SASContextBody contextBody = new SASContextBody();
        contextBody.error_msg(msg);
        Any any = orb.create_any();
        SASContextBodyHelper.insert( any, contextBody );
        return any;
    }
}
