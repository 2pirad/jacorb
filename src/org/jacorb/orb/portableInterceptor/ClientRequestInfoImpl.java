package org.jacorb.orb.portableInterceptor;

import org.omg.IOP.*;
import org.omg.CORBA.*;
import org.omg.PortableInterceptor.*;
import org.omg.Dynamic.Parameter;

import java.util.*;

import org.jacorb.util.Debug;

/**
 * This class represents the type of info object,
 * that will be passed to the ClientRequestInterceptors.<br>
 * See PI Spec p.5-46ff
 *
 * @author Nicolas Noffke
 * @version $Id: ClientRequestInfoImpl.java,v 1.5.4.1 2001-08-22 07:22:22 jacorb Exp $
 */

public class ClientRequestInfoImpl
    extends RequestInfoImpl 
    implements ClientRequestInfo  
{
    //from ClientRequestInfo
    public org.omg.CORBA.Object target = null;
    public org.omg.CORBA.Object effective_target = null;
    public TaggedProfile effective_profile = null;
    public Any received_exception = null;
    public String received_exception_id = null;
    public TaggedComponent[] effective_components = null;
    public org.jacorb.orb.Delegate delegate = null;
    public org.jacorb.orb.ORB orb = null;

    public org.jacorb.orb.connection.RequestOutputStream request_os = null;
    public org.jacorb.orb.connection.ReplyInputStream reply_is = null;

    public ClientRequestInfoImpl() 
    {
        super();
    }

    public void setRequest(org.jacorb.orb.dii.Request request)
    {    
        arguments = new org.omg.Dynamic.Parameter[request.arguments.count()];
        for (int i = 0; i < arguments.length; i++)
        {
            try
            {
                NamedValue value = request.arguments.item(i);
	
                ParameterMode mode = null;
                if (value.flags() == ARG_IN.value)
                    mode = ParameterMode.PARAM_IN;
                else if (value.flags() == ARG_OUT.value)
                    mode = ParameterMode.PARAM_OUT;
                else if (value.flags() == ARG_INOUT.value)
                    mode = ParameterMode.PARAM_INOUT;
	
                arguments[i] = new org.omg.Dynamic.Parameter(value.value(), mode);
            }
            catch (Exception e)
            {
                Debug.output(Debug.INFORMATION | Debug.INTERCEPTOR, e);
            }
        }
        //exceptions will be set when available
    }

    /**
     * This method builds an array of ServiceContexts.
     * The last ServiceContext is a dummy object for
     * data aligning purposes.
     */

    public Enumeration getRequestServiceContexts()
    {
        return request_ctx.elements(); 
    }

    // implementation                        of
    // org.omg.PortableInterceptor.RequestInfoOperations interface

    public Parameter[] arguments() 
    {
        if (! (caller_op == ClientInterceptorIterator.SEND_REQUEST) &&
            ! (caller_op == ClientInterceptorIterator.RECEIVE_REPLY))
            throw new BAD_INV_ORDER("The attribute \"arguments\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        if (arguments == null)
            throw new NO_RESOURCES("Stream-based skeletons/stubs do not support this op", 
                                   1, CompletionStatus.COMPLETED_MAYBE);
        else
            return arguments;
    }

    public TypeCode[] exceptions() 
    {
        if (caller_op == ClientInterceptorIterator.SEND_POLL)
            throw new BAD_INV_ORDER("The attribute \"exceptions\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        if (exceptions == null)
            throw new NO_RESOURCES("Stream-based skeletons/stubs do not support this op", 
                                   1, CompletionStatus.COMPLETED_MAYBE);
        else
            return exceptions;
    }

    public Any result() 
    {
        if (caller_op != ClientInterceptorIterator.RECEIVE_REPLY)
            throw new BAD_INV_ORDER("The attribute \"result\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        if (result == null)
            throw new NO_RESOURCES("Stream-based skeletons/stubs do not support this op", 
                                   1, CompletionStatus.COMPLETED_MAYBE);
        else
            return result;
    }

    public short sync_scope() 
    {
        if (caller_op == ClientInterceptorIterator.SEND_POLL)
            throw new BAD_INV_ORDER("The attribute \"sync_scope\" is currently invalid!", 
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return org.omg.Messaging.SYNC_WITH_TRANSPORT.value;
    }
 
    public short reply_status() 
    {
        if ((caller_op == ClientInterceptorIterator.SEND_REQUEST) ||
            (caller_op == ClientInterceptorIterator.SEND_POLL))
            throw new BAD_INV_ORDER("The attribute \"reply_status\" is currently invalid!",
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return reply_status;
    }

    public org.omg.CORBA.Object forward_reference() 
    {
        if (! (caller_op != ClientInterceptorIterator.RECEIVE_OTHER) ||
            ((reply_status != LOCATION_FORWARD_PERMANENT.value) &&
             (reply_status != LOCATION_FORWARD.value)))
            throw new BAD_INV_ORDER("The attribute \"forward_reference\" is currently " + 
                                    "invalid!", 10, CompletionStatus.COMPLETED_MAYBE);

        return forward_reference;
    }

    public ServiceContext get_request_service_context(int id) 
    {
        if (caller_op == ClientInterceptorIterator.SEND_POLL)
            throw new BAD_INV_ORDER("The attribute \"operation_context\" is currently " + 
                                    "invalid!", 10, CompletionStatus.COMPLETED_MAYBE);

        return super.get_request_service_context(id);
    }

    public ServiceContext get_reply_service_context(int id) 
    {
        if ((caller_op == ClientInterceptorIterator.SEND_REQUEST) ||
            (caller_op == ClientInterceptorIterator.SEND_POLL))
            throw new BAD_INV_ORDER("The attribute \"reply_status\" is currently invalid!",
                                    10, CompletionStatus.COMPLETED_MAYBE);

        return super.get_reply_service_context(id);
    }
  
    // implementation of ClientRequestInfoOperations interface
    public org.omg.CORBA.Object target() 
    {
        return target;
    }
  
    public org.omg.CORBA.Object effective_target() 
    {
        return effective_target;
    }

    public TaggedProfile effective_profile() {
        return effective_profile;
    }
  
    public Any received_exception() 
    {
        if (caller_op != ClientInterceptorIterator.RECEIVE_EXCEPTION)
            throw new BAD_INV_ORDER("The attribute \"received_exception\" is currently " + 
                                    "invalid!", 10, CompletionStatus.COMPLETED_MAYBE);

        return received_exception;
    }

    public String received_exception_id() 
    {
        if (caller_op != ClientInterceptorIterator.RECEIVE_EXCEPTION)
            throw new BAD_INV_ORDER("The attribute \"received_exception_id\" is " + 
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        return received_exception_id;
    }
   
    public TaggedComponent get_effective_component(int id) 
    {
        if (caller_op == ClientInterceptorIterator.SEND_POLL)
            throw new BAD_INV_ORDER("The operation \"get_effective_component\" is " + 
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        for(int _i = 0; _i < effective_components.length; _i++)
            if (effective_components[_i].tag == id)
                return effective_components[_i];
    
        throw new BAD_PARAM("No TaggedComponent with id " + id + " found", 
                            25, CompletionStatus.COMPLETED_MAYBE); 
    }
  
    public TaggedComponent[] get_effective_components(int id) 
    {
        if (caller_op == ClientInterceptorIterator.SEND_POLL)
            throw new BAD_INV_ORDER("The operation \"get_effective_components\" is " + 
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        Vector _store = new Vector();
        for(int _i = 0; _i < effective_components.length; _i++)
            if (effective_components[_i].tag == id)
                _store.addElement(effective_components[_i]);

        if (_store.size() == 0)
            throw new BAD_PARAM("No TaggedComponents with id " + id + " found", 
                                25, CompletionStatus.COMPLETED_MAYBE);
        else
        {
            TaggedComponent[] _result = new TaggedComponent[_store.size()];
            for (int _i = 0; _i < _result.length; _i++)
                _result[_i] = (TaggedComponent) _store.elementAt(_i);

            return _result;
        }
    }
  
    /**
     * WARNING: This method relies on the DomainService to be available.
     * Make shure that the DS is running, if you want to call this method.
     */

    public Policy get_request_policy(int type) 
    {
        if (caller_op == ClientInterceptorIterator.SEND_POLL)
            throw new BAD_INV_ORDER("The operation \"get_request_policy\" is currently " + 
                                    "invalid!", 10, CompletionStatus.COMPLETED_MAYBE);

        if (! orb.hasPolicyFactoryForType(type))
            throw new INV_POLICY("No PolicyFactory for type " + type + 
                                 " has been registered!", 1, 
                                 CompletionStatus.COMPLETED_MAYBE);
        try
        {
            return delegate.get_policy_no_intercept(target, type);
        }
        catch(INV_POLICY _e)
        {
            _e.minor = 1;
            throw _e;
        }
    }
 
    public void add_request_service_context(ServiceContext service_context, 
                                            boolean replace) 
    {

        if (caller_op != ClientInterceptorIterator.SEND_REQUEST)
            throw new BAD_INV_ORDER("The operation \"add_request_service_context\" is " + 
                                    "currently invalid!", 10, 
                                    CompletionStatus.COMPLETED_MAYBE);

        Integer _id = new Integer(service_context.context_id);

        if (! replace && request_ctx.containsKey(_id))
            throw new BAD_INV_ORDER("The ServiceContext with id " + _id.toString() 
                                    + " has already been set!", 11, 
                                    CompletionStatus.COMPLETED_MAYBE);

        request_ctx.put(_id, service_context);
    }

} // ClientRequestInfoImpl






