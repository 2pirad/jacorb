package org.jacorb.orb.portableInterceptor;

import org.omg.IOP.*;
import org.omg.CORBA.*;
import org.omg.CORBA.portable.*;
import org.omg.PortableInterceptor.*;
import org.omg.Dynamic.Parameter;
import org.omg.PortableServer.Servant;

import java.util.Enumeration;

import org.jacorb.orb.dsi.ServerRequest;
import org.jacorb.util.Debug;
/**
 * This class represents the type of info object
 * that will be passed to the ServerRequestInterceptors. <br>
 * See PI Spec p.5-50ff
 *
 * @author Nicolas Noffke
 * @version $Id: ServerRequestInfoImpl.java,v 1.2 2001-03-17 18:45:03 brose Exp $
 */

public class ServerRequestInfoImpl 
    extends RequestInfoImpl 
    implements ServerRequestInfo
{

    //from ServerRequestInfo
    private byte[] adapter_id = null;
    private String target_most_derived_interface = null;

    private Servant servant = null;  
    private org.jacorb.orb.ORB orb = null;

    public ServerRequest request = null;

    public Any sending_exception = null;
    
    public ServerRequestInfoImpl(org.jacorb.orb.ORB orb, ServerRequest request,
                                 Servant servant) {
        super();
    
        this.orb = orb;
        this.request = request;
        this.servant = servant;

        if (servant != null){
            setServant(servant);
        }

        setRequestServiceContexts(request.getServiceContext());

        sending_exception = orb.create_any();
    }

    /**
     * The servant is sometimes not available on calling
     * receive_request_service_contexts (e.g. in case of
     * ServantLocators or ServantActivators).
     */
    public void setServant(Servant servant){
        this.servant = servant;

        adapter_id = ((org.jacorb.poa.POA) servant._poa()).getPOAId();   
        String[] all_ifs = servant._all_interfaces(null, null);
        target_most_derived_interface = all_ifs[0];
    }

    /**
     * Set the sending_exception attribute.
     */
    public void update(){
        if (! request.streamBased()){
            Any user_ex = request.except();
            if (user_ex != null)
                sending_exception = user_ex;
        }

        SystemException sys_ex = request.getSystemException();
        if (sys_ex != null)
            org.jacorb.orb.SystemExceptionHelper.insert(sending_exception, sys_ex);

        forward_reference = request.getForwardReference();
    }

    /**
     * This method builds an array of ServiceContexts.
     * The last ServiceContext is a dummy object for
     * data aligning purposes.
     */
    public ServiceContext[] getReplyServiceContexts(){
        //copying manually for jdk1.1 compatibility
        ServiceContext[] _ctx = new ServiceContext[reply_ctx.size() + 1];
        Enumeration _enum = reply_ctx.elements(); 
        int _i = 0;
        while(_enum.hasMoreElements())
            _ctx[_i++] = (ServiceContext) _enum.nextElement();

        _ctx[_ctx.length - 1] = new ServiceContext(Integer.MAX_VALUE, new byte[0]);

        return _ctx;
    }

    /**
     * returns a reference to the calls target.
     */
    public org.omg.CORBA.Object target()
    {
        return servant._this_object(orb);
    }

    // implementation of RequestInfoOperations interface
    public Parameter[] arguments() {
        if (!(caller_op == ServerInterceptorIterator.RECEIVE_REQUEST) &&
            !(caller_op == ServerInterceptorIterator.SEND_REPLY))
            throw new BAD_INV_ORDER("The attribute \"arguments\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        if (arguments == null)
            throw new NO_RESOURCES("Stream-based skeletons/stubs do not support this op", 
                                   1, CompletionStatus.COMPLETED_MAYBE);
        else
            return arguments;
    }

    public TypeCode[] exceptions() {
        throw new NO_RESOURCES("This feature is not supported on the server side", 
                               1, CompletionStatus.COMPLETED_MAYBE);
    }

    public Any result() {
        if (caller_op != ServerInterceptorIterator.SEND_REPLY)
            throw new BAD_INV_ORDER("The attribute \"result\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        Any result = null;
        try{
            result = request.result();
        }catch(Exception e){
            Debug.output(Debug.INFORMATION | Debug.INTERCEPTOR, e);
        }

        if (result == null)
            throw new NO_RESOURCES("Stream-based skeletons/stubs do not support this op", 
                                   1, CompletionStatus.COMPLETED_MAYBE);
        else
            return result;
    }

    public short sync_scope() {
        return org.omg.Messaging.SYNC_WITH_TRANSPORT.value;
    }
 
    public short reply_status() {
        if ((caller_op == ServerInterceptorIterator.RECEIVE_REQUEST) ||
            (caller_op == ServerInterceptorIterator.RECEIVE_REQUEST_SERVICE_CONTEXTS))
            throw new BAD_INV_ORDER("The attribute \"reply_status\" is currently invalid!",
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return (short) request.status().value();
    }

    public org.omg.CORBA.Object forward_reference() {
        if (! (caller_op != ServerInterceptorIterator.SEND_OTHER) ||
            ((reply_status != LOCATION_FORWARD_PERMANENT.value) && 
             (reply_status != LOCATION_FORWARD.value)))
            throw new BAD_INV_ORDER("The attribute \"forward_reference\" is currently " +
                                    "invalid!", 10, CompletionStatus.COMPLETED_MAYBE);

        return forward_reference;
    }

    public ServiceContext get_reply_service_context(int id) {
        if ((caller_op == ServerInterceptorIterator.RECEIVE_REQUEST) ||
            (caller_op == ServerInterceptorIterator.RECEIVE_REQUEST_SERVICE_CONTEXTS))
            throw new BAD_INV_ORDER("The operation \"get_reply_service_context\" is " + 
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        return super.get_reply_service_context(id);
    }

    public String operation() {
        return request.operation();
    }
  
    public int request_id() {
        return request.requestId();
    }
  
    public boolean response_expected() {
        return request.responseExpected();
    }

    // implementation of ServerRequestInfoOperations interface
    public Any sending_exception() {
        if (caller_op != ServerInterceptorIterator.SEND_EXCEPTION)
            throw new BAD_INV_ORDER("The attribute \"sending_exception\" is " +
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        return sending_exception;
    }

    public byte[] object_id() {
        if (caller_op == ServerInterceptorIterator.RECEIVE_REQUEST_SERVICE_CONTEXTS)
            throw new BAD_INV_ORDER("The attribute \"object_id\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return request.objectId();
    }

    public byte[] adapter_id() {
        if (caller_op == ServerInterceptorIterator.RECEIVE_REQUEST_SERVICE_CONTEXTS)
            throw new BAD_INV_ORDER("The attribute \"adapter_id\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return adapter_id;
    }

    public String target_most_derived_interface() {    
        if (caller_op == ServerInterceptorIterator.RECEIVE_REQUEST_SERVICE_CONTEXTS)
            throw new BAD_INV_ORDER("The attribute \"target_most_derived_interface\" is " +
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        return target_most_derived_interface;
    }

    /**
     * WARNING: This method relies on the DomainService to be available.
     * Make shure that the DS is running, if you want to call this method.
     */
    public Policy get_server_policy(int type) {
        if (! orb.hasPolicyFactoryForType(type))
            throw new INV_POLICY("No PolicyFactory for type " + type + 
                                 " has been registered!", 2, 
                                 CompletionStatus.COMPLETED_MAYBE);

        try{
            org.jacorb.orb.ServantDelegate delegate = (org.jacorb.orb.ServantDelegate) servant._get_delegate();
            return delegate._get_policy(servant._this_object(), type);
        }catch(INV_POLICY _e){
            _e.minor = 2;
            throw _e;
        }
    }

    public void set_slot(int id, Any data) throws InvalidSlot {
        current.set_slot(id, data);
    }
  
    public boolean target_is_a(String id) {
        if (caller_op == ServerInterceptorIterator.RECEIVE_REQUEST_SERVICE_CONTEXTS)
            throw new BAD_INV_ORDER("The operation \"target_is_a\" is currently invalid!",
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return servant._is_a(id);
    }

    public void add_reply_service_context(ServiceContext service_context, 
                                          boolean replace) {

        Integer _id = new Integer(service_context.context_id);

        if (! replace && reply_ctx.containsKey(_id))
            throw new BAD_INV_ORDER("The ServiceContext with id " + _id.toString() 
                                    + " has already been set!", 11, 
                                    CompletionStatus.COMPLETED_MAYBE);

        reply_ctx.put(_id, service_context);
    }
} // ServerRequestInfoImpl
