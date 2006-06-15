/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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
 *
 */
package org.jacorb.orb.portableInterceptor;

import org.omg.PortableInterceptor.*;
import org.omg.CORBA.UserException;

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.orb.SystemExceptionHelper;

/**
 * This class is an iterator over an array
 * of ClientRequestInterceptors.
 *
 * @author Nicolas Noffke
 * @version  $Id: ClientInterceptorIterator.java,v 1.13 2006-06-15 13:51:46 alphonse.bendt Exp $
 */

public class ClientInterceptorIterator
    extends RequestInterceptorIterator
{
    public static final short SEND_REQUEST = 0;
    public static final short SEND_POLL = 1;
    public static final short RECEIVE_REPLY = 2;
    public static final short RECEIVE_EXCEPTION = 3;
    public static final short RECEIVE_OTHER = 4;

    private ClientRequestInfoImpl info = null;
    private final Logger logger;

    public ClientInterceptorIterator(Logger logger, Interceptor[] interceptors)
    {
        super(interceptors);

        this.logger = logger;
    }

    public void iterate(ClientRequestInfoImpl info, short op)
    throws UserException
    {
        this.info = info;
        this.op = op;

        // ok, op <= SEND_POLL is more efficient but
        // less understandable
        setDirection((op == SEND_REQUEST) || (op == SEND_POLL));

        iterate();

        //propagate last exception upwards
        if (interceptor_ex != null)
        {
            if (interceptor_ex instanceof ForwardRequest)
            {
                throw (ForwardRequest) interceptor_ex;
            }
            throw (org.omg.CORBA.SystemException) interceptor_ex;
        }
    }

    /**
     * Iterates over the enumeration, i.e. calls "op" on
     * nextElement() until !hasMoreElements().
     */
    protected void invoke(Interceptor interceptor)
    throws UserException
    {
        info.caller_op = op;

        try
        {
            switch (op)
            {
                case SEND_REQUEST :
                    ((ClientRequestInterceptor) interceptor).send_request(info);
                    break;
                case SEND_POLL :
                    ((ClientRequestInterceptor) interceptor).send_poll(info);
                    break;
                case RECEIVE_REPLY :
                    ((ClientRequestInterceptor) interceptor).receive_reply(info);
                    break;
                case RECEIVE_EXCEPTION :
                    ((ClientRequestInterceptor) interceptor).receive_exception(info);
                    break;
                case RECEIVE_OTHER :
                    ((ClientRequestInterceptor) interceptor).receive_other(info);
            }
        }
        catch (ForwardRequest _fwd)
        {
            reverseDirection();
            op = RECEIVE_OTHER;

            info.reply_status = LOCATION_FORWARD.value;

            info.forward_reference = _fwd.forward;
            interceptor_ex = _fwd;
        }
        catch (org.omg.CORBA.SystemException _sysex)
        {
            reverseDirection();
            op = RECEIVE_EXCEPTION;
            interceptor_ex = _sysex;

            SystemExceptionHelper.insert(info.received_exception, _sysex);

            try
            {
                info.received_exception_id = SystemExceptionHelper.type(_sysex).id();
            }
            catch(org.omg.CORBA.TypeCodePackage.BadKind _bk)
            {
            }
        }
        catch (Exception e)
        {
            logger.error("unexpected exception during invoke", e);
        }

        info.caller_op = op;
    }
} // ClientInterceptorIterator


