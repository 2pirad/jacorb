package org.jacorb.transaction;

import org.omg.PortableInterceptor.*;
import org.omg.CosTransactions.*;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TransactionService;
import org.omg.IOP.Codec;
/**
 * This interceptor transfers the propagation context
 * from the corresponding service context to a slot
 * in the PICurrent.
 *
 * @author Nicolas Noffke
 * @version $Id: ServerContextTransferInterceptor.java,v 1.6 2002-03-19 09:25:49 nicolas Exp $
 */

public class ServerContextTransferInterceptor 
  extends org.omg.CORBA.LocalObject 
    implements ServerRequestInterceptor{

    private Codec codec = null;
    private int slot_id = -1;
  
    public ServerContextTransferInterceptor(Codec codec, int slot_id) {
        this.codec = codec;
        this.slot_id = slot_id;
    }

    // implementation of org.omg.PortableInterceptor.InterceptorOperations interface
    public String name() 
    {
        return "ServerContextTransferInterceptor";
    }

    public void destroy()
    {
    }

    /**
     * Put the propagation context from the service context
     * into the PICurrent.
     */
    public void receive_request_service_contexts(ServerRequestInfo ri) 
        throws ForwardRequest{
        try{
            ServiceContext ctx = ri.get_request_service_context(TransactionService.value);

            ri.set_slot(slot_id, codec.decode(ctx.context_data));
        }catch (Exception e){
            org.jacorb.util.Debug.output(2, e);
        }
    }

    public void receive_request(ServerRequestInfo ri)
        throws ForwardRequest{
    }

    public void send_reply(ServerRequestInfo ri){
    }

    public void send_exception(ServerRequestInfo ri)
        throws ForwardRequest{
    }

    public void send_other(ServerRequestInfo ri) 
        throws ForwardRequest{
    }
} // ServerContextTransferInterceptor






