package org.jacorb.orb.dsi;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2002  Gerald Brose.
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

import org.jacorb.util.*;
import org.jacorb.orb.*;
import org.jacorb.orb.connection.*;
import org.jacorb.orb.portableInterceptor.*;

import org.omg.GIOP.*;

/**
 * @author Gerald Brose, FU Berlin
 * @version $Id: ServerRequest.java,v 1.11 2002-03-19 09:25:30 nicolas Exp $
 */

public class ServerRequest 
    extends org.omg.CORBA.ServerRequest 
    implements org.omg.CORBA.portable.ResponseHandler
{
    private RequestInputStream in;
    private ReplyOutputStream out;	
    private GIOPConnection connection;
    
    private int status = ReplyStatusType_1_2._NO_EXCEPTION;
    private byte[] oid;
    private org.omg.CORBA.Object reference = null;
    private String[] rest_of_name = null;

    /* is this request stream or DSI-based ? */
    private boolean stream_based; 

    private org.omg.CORBA.SystemException sys_ex;
    private org.omg.PortableServer.ForwardRequest location_forward;
    private org.omg.CORBA.Any  ex;
    private org.omg.CORBA.Any result;
    private org.jacorb.orb.NVList args;

    private org.jacorb.orb.ORB orb;

    private ServerRequestInfoImpl info = null;

    public ServerRequest( org.jacorb.orb.ORB orb, 
                          RequestInputStream in, 
                          GIOPConnection _connection )
    {
	this.orb = orb;
	this.in = in;
	connection = _connection;

	oid = org.jacorb.poa.util.POAUtil.extractOID( in.req_hdr.target.object_key() );
    }

    /* 
     * if this request could not be delivered directly to the correct
     * POA because the POA's adapter activator could not be called
     * when the parent POA was in holding state, the parent will queue
     * the request and later return it to the adapter layer. In order
     * to be able to find the right POA when trying to deliver again,
     * we have to remember the target POA's name 
     */

    public void setRemainingPOAName(String [] r_o_n)
    {
	rest_of_name = r_o_n;
    }

    public String[] remainingPOAName()
    {
	return rest_of_name;
    }
	
    public String operation()
    {
	return in.req_hdr.operation;
    }

    /**
     * The resulting any must be used to create an input stream from
     * which the result value can be read.
     */

    public org.omg.CORBA.Any result()
    {
	if( stream_based )
        {
            org.omg.CORBA.Any any = orb.create_any();

            // create the output stream for the result

            CDROutputStream _out = ((CDROutputStream)any.create_output_stream());

            // get a copy of the content of this reply
            byte[] result_buf = out.getBody();

            // ... and insert it
            _out.setBuffer( result_buf  );
            // important: set the _out buffer's position to the end of the contents!
            _out.skip( result_buf.length );
            return any;
        }
	return result;
    }

    public org.omg.CORBA.NVList arguments()
    {
	if( stream_based )
	    throw new RuntimeException("This ServerRequest is stream-based!");
	return args;
    }

    public org.omg.CORBA.Any except()
    {
	if( stream_based )
	    throw new RuntimeException("This ServerRequest is stream-based!");
	return ex;
    }

    public ReplyStatusType_1_2 status()
    {
	return ReplyStatusType_1_2.from_int( status );
    }

    public org.omg.CORBA.Context ctx()
    {
	return null;
    }

    public void arguments(org.omg.CORBA.NVList p)
    {
	args = (org.jacorb.orb.NVList)p;
	// unmarshal

	if( args != null )
	{
	    in.mark(0);
	    for( java.util.Enumeration e = args.enumerate(); 
		 e.hasMoreElements(); )
	    {
		org.omg.CORBA.NamedValue nv = 
		    (org.omg.CORBA.NamedValue)e.nextElement();
			
		if( nv.flags() != org.omg.CORBA.ARG_OUT.value )
		{ 
		    // out parameters are not received
		    try
		    { 
			nv.value().read_value( in, nv.value().type() );
		    } 
		    catch (Exception ex)
		    {
			throw new org.omg.CORBA.MARSHAL("Couldn't unmarshal object of type "
							+ nv.value().type() + " in ServerRequest.");
		    }
		}
	    }
	    try
	    { 
		in.reset();
	    }
	    catch (Exception ex)
	    {
		throw new org.omg.CORBA.UNKNOWN("Could not reset input stream");
	    }
	    
	    if (info != null)
            {
		//invoke interceptors
		org.omg.Dynamic.Parameter[] params = new org.omg.Dynamic.Parameter[args.count()];
		for (int i = 0; i < params.length; i++)
                {
		    try
                    {
			org.omg.CORBA.NamedValue value = args.item(i);

			org.omg.CORBA.ParameterMode mode = null;
			if (value.flags() == org.omg.CORBA.ARG_IN.value)
			    mode = org.omg.CORBA.ParameterMode.PARAM_IN;
			else if (value.flags() == org.omg.CORBA.ARG_OUT.value)
			    mode = org.omg.CORBA.ParameterMode.PARAM_OUT;
			else if (value.flags() == org.omg.CORBA.ARG_INOUT.value)
			    mode = org.omg.CORBA.ParameterMode.PARAM_INOUT;
		  
			params[i] = new org.omg.Dynamic.Parameter(value.value(), mode);
		    }
                    catch (Exception e)
                    {
			Debug.output(2, e);
		    }
		}

		info.arguments = params;

		ServerInterceptorIterator intercept_iter = 
		    orb.getInterceptorManager().getServerIterator();      
	      
		try
                {
		    intercept_iter.iterate(info, ServerInterceptorIterator.RECEIVE_REQUEST);
		} 
                catch(org.omg.CORBA.UserException ue)
                {
                    if (ue instanceof org.omg.PortableInterceptor.
                        ForwardRequest)
                    {
                        
                        org.omg.PortableInterceptor.ForwardRequest fwd =
                            (org.omg.PortableInterceptor.ForwardRequest) ue;
                        
                        setLocationForward(new org.omg.PortableServer.
                            ForwardRequest(fwd.forward));
                    }    
		} 
                catch (org.omg.CORBA.SystemException _sys_ex) 
                {
		    setSystemException(_sys_ex);
		}
	      
		info = null;
	    }	
	}
    }

    public void set_result(org.omg.CORBA.Any res)
    {
	if( stream_based )
	    throw new RuntimeException("This ServerRequest is stream-based!");
	result = res;
    }

    public void set_exception(org.omg.CORBA.Any ex)
    {
	if( stream_based )
	    throw new RuntimeException("This ServerRequest is stream-based!");
	this.ex = ex;
	status = ReplyStatusType_1_2._USER_EXCEPTION;
    }


    public void reply()
    {
	if( responseExpected() )
	{
	    Debug.output(6,"ServerRequest: reply to " + operation());

	    try 
	    { 
		if( out == null )
		{ 
		    out = 
                        new ReplyOutputStream(
                                 requestId(), 
                                 ReplyStatusType_1_2.from_int(status),
                                 in.getGIOPMinor() );
		}

		/* 
                 * DSI-based servers set results and user exceptions
                 * using anys, so we have to treat this differently 
                 */
		if( !stream_based )
		{
		    if( status == ReplyStatusType_1_2._USER_EXCEPTION )
		    {
			out.write_string( ex.type().id() );
			ex.write_value( out );
		    }
		    else if( status == ReplyStatusType_1_2._NO_EXCEPTION )
		    {
			result.write_value( out );
			if( args != null )
			{
			    for( java.util.Enumeration e = args.enumerate(); 
				 e.hasMoreElements(); )
			    {
				org.jacorb.orb.NamedValue nv = 
				    (org.jacorb.orb.NamedValue)e.nextElement();
				
				if( nv.flags() != org.omg.CORBA.ARG_IN.value )
				{ 
				// in parameters are not returnd
				    try
				    { 
					nv.send( out );
				    } 
				    catch (Exception ex)
				    {
					throw new org.omg.CORBA.MARSHAL("Couldn't return (in)out arg of type "
									+ nv.value().type() + " in ServerRequest.");
				    }
				}
			    }
			}
		    }
		}

		/* 
                 * these two exceptions are set in the same way for
                 * both stream-based and DSI-based servers 
                 */
		if( status == ReplyStatusType_1_2._LOCATION_FORWARD )
		{
		    out.write_Object( location_forward.forward_reference );
		}
		else if( status == ReplyStatusType_1_2._SYSTEM_EXCEPTION )
		{
		    org.jacorb.orb.SystemExceptionHelper.write( out, sys_ex );
		}

		/* 
                 * everything is written to out by now, be it results
                 * or exceptions. 
                 */

		connection.sendMessage( out );
	    }
	    catch ( Exception ioe )
	    {
		Debug.output(2,ioe);
		System.err.println("ServerRequest: Error replying to request!");
	    }
	}
    }

    /* ResponseHandler */

    public org.omg.CORBA.portable.OutputStream createReply()
    {
	stream_based = true;

	if( out != null )
	    throw new Error("Internal: reply already created!");

	if( !stream_based )
	    throw new Error("Internal: ServerRequest not stream-based!");

	out = 
            new ReplyOutputStream(requestId(),
                                  ReplyStatusType_1_2.NO_EXCEPTION,
                                  in.getGIOPMinor() );

	return out;
    }

    public org.omg.CORBA.portable.OutputStream createExceptionReply()
    {
	stream_based = true;

	status = ReplyStatusType_1_2._USER_EXCEPTION;

	out = 
            new ReplyOutputStream(requestId(),
                                  ReplyStatusType_1_2.USER_EXCEPTION,
                                  in.getGIOPMinor() );

	return out;
    }

    /** our own: */

    public void setSystemException(org.omg.CORBA.SystemException s)
    {
	Debug.output(2, s);

	status = ReplyStatusType_1_2._SYSTEM_EXCEPTION;

	/* we need to create a new output stream here because a system exception may
	   have occurred *after* a no_exception request header was written onto the
	   original output stream*/


	out = new ReplyOutputStream(requestId(),
                                    ReplyStatusType_1_2.SYSTEM_EXCEPTION,
                                    in.getGIOPMinor() );
	sys_ex = s;
    }

    public void setLocationForward(org.omg.PortableServer.ForwardRequest r)
    {
	Debug.output(2,"Location Forward");

	status = ReplyStatusType_1_2._LOCATION_FORWARD;

	out = new ReplyOutputStream(requestId(),
                                    ReplyStatusType_1_2.LOCATION_FORWARD,
                                    in.getGIOPMinor() );
	location_forward = r;
    }

    /**
     * @returns the InputStream. This operation sets the
     * request be stream-based, ie. all attempts to extract
     * data using DII-based operations will throw exceptions
     * For internal access to the stream use get_in()
     *
     */

    public org.jacorb.orb.CDRInputStream getInputStream()
    {
	stream_based = true;
	return in;
    }
  
    public ReplyOutputStream getReplyOutputStream()
    {
	if (out == null)
	    createReply();
        
	stream_based = true;
	return out;
    }

    public boolean responseExpected()
    {
	return Messages.responseExpected(in.req_hdr.response_flags);
    }

    public org.omg.CORBA.SystemException getSystemException()
    {
	return sys_ex;
    }

    public int requestId()
    {
	return in.req_hdr.request_id;
    }

    public byte[] objectKey()
    {
	return in.req_hdr.target.object_key();
    }

    public org.omg.IOP.ServiceContext[] getServiceContext()
    {
	return in.req_hdr.service_context;
    }

    public byte[] objectId()
    {
	return oid;
    }

    public boolean streamBased()
    {
	return stream_based;
    }


    public void setReference(org.omg.CORBA.Object o)
    {
	reference = o;
    }

    public org.omg.CORBA.Object getReference()
    {
	return reference;
    }

    /*
    public byte[] getBuffer()
    {
	return in.getBuffer();
    }

    public void updateBuffer( byte[] _buf )
    {
	RequestInputStream rin = 
            new RequestInputStream( orb,_buf);
	//  	byte[] n_oid = org.jacorb.poa.util.POAUtil.extractOID( in.req_hdr.object_key);
	//  	if( oid != n_oid )
	//  	    throw new org.omg.CORBA.UNKNOWN("Invalid message buffer update");
	in = rin;
    }
    */

    public RequestInputStream get_in()
    {
	return in;
    }

    /**
     * If a new output stream has to be created, the request itself isn't fixed
     * to stream-based.
     */

    public ReplyOutputStream get_out()
    {
        if (out == null)
            out = 
                new ReplyOutputStream(requestId(),
                                      ReplyStatusType_1_2.NO_EXCEPTION,
                                      in.getGIOPMinor() );

        return out;
    }

    public void setServerRequestInfo(ServerRequestInfoImpl info)
    {
	this.info = info;
    }

    public org.omg.CORBA.Object getForwardReference()
    {
	if (location_forward != null)
	    return location_forward.forward_reference;
	else
	    return null;
    }

    /*
    public void reply(byte[] buf)
    {
	reply(buf, buf.length);
    }

    public void reply( byte[] buf, int len )
    {
	if( out == null )
	    out = new ReplyOutputStream(new org.omg.IOP.ServiceContext[0],
                                        requestId(), 
                                        ReplyStatusType_1_2.from_int(status),
                                        in.getGIOPMinor() );
        out.setCodeSet( connection.TCS, connection.TCSW );
    

	out.setBuffer(buf);
	//correct the requestId (unsigned long)
	out.setGIOPRequestId(requestId());
	out.setSize(len);
	//	out.insertMsgSize(); stream copied.. not needed
	try
        {
	    connection.sendReply( out );
	}
        catch (Exception ioe)
        {
	    Debug.output(2,ioe);
	    ioe.printStackTrace();
	    System.out.println("ServerRequest: Error replying to request!");
	}
	in.req_hdr.response_flags = 0; 
        // make sure that no
	// other reply is send
    }    
   */
    public GIOPConnection getConnection()
    {
	return connection;
    }

}





