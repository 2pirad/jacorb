package org.jacorb.orb;

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

import java.util.*;
import java.io.*;
import java.lang.Object;
import java.net.*;

import org.jacorb.imr.*;
import org.jacorb.util.*;
import org.jacorb.poa.POAConstants;
import org.jacorb.poa.util.POAUtil;
import org.jacorb.orb.connection.*;
import org.jacorb.orb.util.CorbaLoc;
import org.jacorb.orb.portableInterceptor.*;

import org.omg.CORBA.portable.*;
import org.omg.PortableInterceptor.*;
import org.omg.IOP.ServiceContext;
import org.omg.GIOP.*;
import org.omg.CORBA.SystemException;
import org.omg.PortableServer.POAPackage.*;

/**
 * JacORB implementation of CORBA object reference
 *
 * @author Gerald Brose
 * @version $Id: Delegate.java,v 1.57 2002-07-17 13:29:53 steve.osselton Exp $
 *
 */

public final class Delegate
    extends org.omg.CORBA_2_3.portable.Delegate
{
    // WARNING: DO NOT USE _pior DIRECTLY, BECAUSE THAT IS NOT MT
    // SAFE. USE getParsedIOR() INSTEAD, AND KEEP A METHOD-LOCAL COPY
    // OF THE REFERENCE.
    private ParsedIOR _pior = null;
    private ClientConnection connection = null;

    /* save original ior for fallback */
    private ParsedIOR piorOriginal = null;

    /* save iors to detect and prevent locate forward loop */
    private ParsedIOR piorLastFailed = null;

    /* flag to indicate if this is the delegate for the ImR */
    private boolean isImR = false;

    private boolean bound = false;
    private org.jacorb.poa.POA poa;

    private org.jacorb.orb.ORB orb = null;

    /** set after the first attempt to determine whether
        this reference is to a local object */
    private boolean resolved_locality = false;

    private Hashtable pending_replies = new Hashtable();
    private Barrier pending_replies_sync = new Barrier();

    private Object bind_sync = new Object();

    private boolean locate_on_bind_performed = false;

    private ConnectionManager conn_mg = null;

    private Hashtable policy_overrides = new Hashtable();

    private boolean doNotCheckExceptions = false; //Setting for Appligator
    /**
     * A general note on the synchronization concept
     *
     * The main problem that has to be addressed by synchronization
     * means is the case when an object reference is shared by
     * threads, and LocationForwards (e.g. thrown by the ImR) or
     * ForwardRequest (thrown by ClientInterceptors) involved. In
     * these cases, the rebinding to another target can occur while
     * there are still other requests active. Therefore, the act of
     * rebinding must be synchronized, so every thread sees a
     * consistent state.  
     *
     * Synchronization is done via the bind_sync object. Please also
     * have a look at the comment for opration bind().  
     */

    /* constructors: */


    private Delegate ()
    {
    }

    public Delegate ( org.jacorb.orb.ORB orb, ParsedIOR pior )
    {
        this.orb = orb;
        _pior = pior;
        checkIfImR( _pior.getTypeId() );

        conn_mg = orb.getConnectionManager();
    }

    public Delegate ( org.jacorb.orb.ORB orb, String object_reference )
    {
        this.orb = orb;

        if ( object_reference.indexOf( "IOR:" ) == 0 )
        {
            _pior = new ParsedIOR( object_reference );
        }
        else
        {
            throw new org.omg.CORBA.INV_OBJREF( "Not an IOR: " +
                                                object_reference );
        }

        checkIfImR( _pior.getTypeId() );
        conn_mg = orb.getConnectionManager();
    }

    public Delegate ( org.jacorb.orb.ORB orb, org.omg.IOP.IOR _ior )
    {
        this.orb = orb;
        _pior = new ParsedIOR( _ior );
        checkIfImR( _pior.getTypeId() );

        conn_mg = orb.getConnectionManager();
    }

    //special constructor for appligator
    public Delegate( org.jacorb.orb.ORB orb,
                     String object_reference,
                     boolean _donotcheckexceptions )
    {
        this( orb, object_reference );
        doNotCheckExceptions = _donotcheckexceptions;
    }


    /**
     * Method to determine if this delegate is the delegate for the ImR.
     * This information is needed when trying to determine if the ImR has
     * gone down and come back up at a different addresss.  All delegates
     * except the delegate of the ImR itself will try to determine if the
     * ImR has gone down and come back up at a new address if a connection
     * to the ImR can't be made.  If the delegate of the ImR itself has
     * failed to connect then the ImR hasn't come back up!
     */
    private void checkIfImR( String typeId )
    {
        if ( typeId.equals( "IDL:org/jacorb/imr/ImplementationRepository:1.0" ) )
        {
            isImR = true;
        }
    }


    public int _get_TCKind()
    {
        return org.omg.CORBA.TCKind._tk_objref;
    }


    /**
     * This bind is a combination of the old _init() and bind()
     * operations. It first inits this delegate with the information
     * supplied by the (parsed) IOR. Then it requests a new
     * ClientConnection from the ConnectionsManager. This will *NOT*
     * open up a TCP connection, but the connection is needed for the
     * GIOP message ids. The actual TCP connection is automatically
     * opened up by the ClientConnection, when the first request is
     * sent. This has the advantage, that COMM_FAILURES can only occur
     * inside of _invoke, where they get handled properly (falling
     * back, etc.)
     *  */
    private void bind()
    {
        synchronized ( bind_sync )
        {
            if ( bound )
                return ;

            _pior.init();

            connection = conn_mg.getConnection( _pior.getAdPort(),
                                                _pior.useSSL() );

            bound = true;

            /* The delegate could query the server for the object
             *  location using a GIOP locate request to make sure the
             *  first call will get through without redirections
             *  (provided the server's answer is definite): 
             */
            if ( ( ! locate_on_bind_performed ) &&
                    Environment.locateOnBind() )
            {
                //only locate once, because bind is called from the
                //switch statement below again.
                locate_on_bind_performed = true;

                try
                {
                    LocateRequestOutputStream lros =
                        new LocateRequestOutputStream( _pior.get_object_key(),
                                                       connection.getId(),
                                                       ( int ) _pior.getProfileBody().iiop_version.minor );

                    ReplyPlaceholder place_holder = new ReplyPlaceholder();

                    connection.sendRequest( lros,
                                            place_holder,
                                            lros.getRequestId() );


                    LocateReplyInputStream lris =
                        ( LocateReplyInputStream ) place_holder.getInputStream();

                    switch ( lris.rep_hdr.locate_status.value() )
                    {

                    case LocateStatusType_1_2._UNKNOWN_OBJECT :
                        {
                            throw new org.omg.CORBA.UNKNOWN( "Could not bind to object, server does not know it!" );
                        }

                    case LocateStatusType_1_2._OBJECT_HERE :
                        {
                            Debug.output( 3, "object here" );

                            break;
                        }

                    case LocateStatusType_1_2._OBJECT_FORWARD :
                        {
                            //fall through
                        }

                    case LocateStatusType_1_2._OBJECT_FORWARD_PERM :
                        {
                            //_OBJECT_FORWARD_PERM is actually more or
                            //less deprecated
                            Debug.output( 3, "Locate Reply: Forward" );

                            rebind( orb.object_to_string( lris.read_Object() ) );

                            break;
                        }

                    case LocateStatusType_1_2._LOC_SYSTEM_EXCEPTION :
                        {
                            throw SystemExceptionHelper.read( lris );

                            //break;
                        }

                    case LocateStatusType_1_2._LOC_NEEDS_ADDRESSING_MODE :
                        {
                            throw new org.omg.CORBA.NO_IMPLEMENT( "Server responded to LocateRequest with a status of LOC_NEEDS_ADDRESSING_MODE, but this isn't yet implemented by JacORB" );

                            //break;
                        }

                    default :
                        {
                            throw new RuntimeException( "Unknown reply status for LOCATE_REQUEST: " + lris.rep_hdr.locate_status.value() );
                        }

                    }

                }
                catch ( org.omg.CORBA.SystemException se )
                {
                    //rethrow
                    throw se;
                }
                catch ( Exception e )
                {
                    Debug.output( 1, e );
                }

            }

            //wake up threads waiting for the pior
            bind_sync.notifyAll();
        }
    }

    private void rebind( String object_reference )
    {
        synchronized ( bind_sync )
        {
            if ( object_reference.indexOf( "IOR:" ) == 0 )
            {
                rebind( new ParsedIOR( object_reference ) );
            }
            else
            {
                throw new org.omg.CORBA.INV_OBJREF( "Not an IOR: " +
                                                    object_reference );
            }

        }
    }

    private void rebind( ParsedIOR p )
    {
        synchronized ( bind_sync )
        {
            if ( p.equals( _pior ) )
            {
                //already bound to target so just return
                return ;
            }

            if ( piorLastFailed != null && piorLastFailed.equals( p ) )
            {
                //we've already failed to bind to the ior
                throw new org.omg.CORBA.TRANSIENT();
            }

            if ( piorOriginal == null )
            {
                //keep original pior for fallback
                piorOriginal = _pior;
            }

            _pior = p;

            if ( connection != null )
            {
                conn_mg.releaseConnection( connection );
                connection = null;
            }

            //to tell bind() that it has to take action
            bound = false;

            bind();
        }
    }

    public org.omg.CORBA.Request create_request( org.omg.CORBA.Object self,
            org.omg.CORBA.Context ctx,
            java.lang.String operation ,
            org.omg.CORBA.NVList args,
            org.omg.CORBA.NamedValue result )
    {
        bind();

        return new org.jacorb.orb.dii.Request( self,
                                               orb,
                                               connection,
                                               getParsedIOR().get_object_key(),
                                               operation,
                                               args,
                                               ctx,
                                               result );
    }

    public org.omg.CORBA.Request create_request( org.omg.CORBA.Object self,
            org.omg.CORBA.Context ctx,
            String operation,
            org.omg.CORBA.NVList arg_list,
            org.omg.CORBA.NamedValue result,
            org.omg.CORBA.ExceptionList exceptions,
            org.omg.CORBA.ContextList contexts )
    {
        throw new org.omg.CORBA.NO_IMPLEMENT();
    }

    public synchronized org.omg.CORBA.Object duplicate( org.omg.CORBA.Object self )
    {
        return self;
    }

    public boolean equals( java.lang.Object obj )
    {
        return ( obj instanceof org.omg.CORBA.Object &&
                 toString().equals( obj.toString() ) );
    }

    public boolean equals( org.omg.CORBA.Object self, java.lang.Object obj )
    {
        return equals( obj );
    }

    /**
     * 
     */

    public void finalize()
    {
        if ( connection != null )
        {
            conn_mg.releaseConnection( connection );
        }

        orb._release( this );


        Debug.output( 3, " Delegate gc'ed!" );
    }

    public String get_adport()
    {
        return getParsedIOR().getAdPort();
    }



    public org.omg.CORBA.DomainManager[] get_domain_managers
    ( org.omg.CORBA.Object self )
    {
        return null;
    }


    /**
     * this is get_policy without the call to request(), which would
     * invoke interceptors.
     */
    public org.omg.CORBA.Policy get_policy_no_intercept( org.omg.CORBA.Object self,
            int policy_type )
    {
        RequestOutputStream _os = null;

        synchronized ( bind_sync )
        {
            bind();

            ParsedIOR p = getParsedIOR();

            _os =
                new RequestOutputStream( connection,
                                         connection.getId(),
                                         "_get_policy",
                                         true,
                                         p.get_object_key(),
                                         ( int ) p.getProfileBody().iiop_version.minor );

            //Problem: What about the case where different objects
            //that are accessed by the same connection have different
            //codesets?  Is this possible anyway?
            if ( ! connection.isTCSNegotiated() )
            {
                ServiceContext ctx = connection.setCodeSet( p );

                if ( ctx != null )
                {
                    _os.addServiceContext( ctx );
                }

            }

            //Setting the codesets not until here results in the
            //header being writtend using the default codesets. On the
            //other hand, the server side must have already read the
            //header to discover the codeset service context.
            _os.setCodeSet( connection.getTCS(), connection.getTCSW() );

        }

        return get_policy( self, policy_type, _os );
    }



    public org.omg.CORBA.Policy get_policy( org.omg.CORBA.Object self,
                                            int policy_type )
    {
        return get_policy( self,
                           policy_type,
                           request( self, "_get_policy", true ) );
    }



    public org.omg.CORBA.Policy get_policy( org.omg.CORBA.Object self,
                                            int policy_type,
                                            org.omg.CORBA.portable.OutputStream os )
    {
        // ask object implementation
        while ( true )
        {
            try
            {
                os.write_Object( self );
                os.write_long( policy_type );
                org.omg.CORBA.portable.InputStream is = invoke( self, os );
                return org.omg.CORBA.PolicyHelper.narrow( is.read_Object() );
            }
            catch ( RemarshalException r )
            {
            }
            catch ( ApplicationException _ax )
            {
                String _id = _ax.getId();
                throw new RuntimeException( "Unexpected exception " + _id );
            }

        }
    } // get_policy


    /**
     * @deprecated Deprecated by CORBA 2.3
     */

    public org.omg.CORBA.InterfaceDef get_interface( org.omg.CORBA.Object self )
    {
        return org.omg.CORBA.InterfaceDefHelper.narrow( get_interface_def( self ) ) ;
    }

    public org.omg.CORBA.Object get_interface_def (org.omg.CORBA.Object self)
    {
        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;
        org.omg.PortableServer.Servant servant;
        org.omg.CORBA.portable.ServantObject so;

        while (true)
        {
            // If local object call _interface directly

            if (is_really_local (self))
            {
                so = servant_preinvoke (self, "_interface", java.lang.Object.class);

                try
                {
                    servant = (org.omg.PortableServer.Servant) so.servant;
                    return (servant._get_interface_def ());
                }
                finally
                {
                    servant_postinvoke (self, so);
                }
            }
            else
            {
                try
                {
                    os = request (self, "_interface", true);
                    is = invoke (self, os);
                    return is.read_Object ();
                }
                catch (RemarshalException r)
                {
                }
                catch (Exception n)
                {
                    return null;
                }
            }
        }
    }

    ClientConnection getConnection()
    {
        synchronized ( bind_sync )
        {
            bind();

            return connection;
        }
    }

    public org.omg.IOP.IOR getIOR()
    {
        synchronized ( bind_sync )
        {
            if ( piorOriginal != null )
            {
                return piorOriginal.getIOR();
            }
            else
            {
                return getParsedIOR().getIOR();
            }

        }
    }

    public byte[] getObjectId()
    {
        synchronized ( bind_sync )
        {
            bind();

            return POAUtil.extractOID( getParsedIOR().get_object_key() );
        }
    }

    public byte[] getObjectKey()
    {
        synchronized ( bind_sync )
        {
            bind();

            return getParsedIOR().get_object_key();
        }
    }

    public ParsedIOR getParsedIOR()
    {
        synchronized ( bind_sync )
        {
            while ( _pior == null )
            {
                try
                {
                    bind_sync.wait();
                }
                catch ( InterruptedException ie )
                {
                }

            }

            return _pior;
        }
    }

    public void resolvePOA (org.omg.CORBA.Object self)
    {
        if (! resolved_locality)
        {
            resolved_locality = true;
            org.jacorb.poa.POA local_poa = orb.findPOA (this, self);

            if (local_poa != null)
            {
                poa = local_poa;
            }
        }
    }

    public org.jacorb.poa.POA getPOA()
    {
        return ( org.jacorb.poa.POA ) poa;
    }

    /**
     */

    public org.omg.CORBA.portable.ObjectImpl getReference( org.jacorb.poa.POA _poa )
    {
        Debug.output( 3, "Delegate.getReference with POA <" +
                      ( _poa != null ? _poa._getQualifiedName() : " empty" ) + ">" );

        if ( _poa != null )   // && _poa._localStubsSupported())
            poa = _poa;

        org.omg.CORBA.portable.ObjectImpl o =
            new org.jacorb.orb.Reference( typeId() );

        o._set_delegate( this );

        return o;
    }

    public int hash( org.omg.CORBA.Object self, int x )
    {
        return hashCode();
    }

    public int hashCode()
    {
        return getIDString().hashCode();
    }

    public int hashCode( org.omg.CORBA.Object self )
    {
        return hashCode();
    }

    /**
     * invoke an operation using this object reference by sending 
     * the request marshalled in the OutputStream
     */

    public org.omg.CORBA.portable.InputStream invoke( org.omg.CORBA.Object self,
            org.omg.CORBA.portable.OutputStream os )
    throws ApplicationException, RemarshalException
    {
        ClientRequestInfoImpl info = null;
        RequestOutputStream ros = null;
        boolean useInterceptors = orb.hasClientRequestInterceptors ();

        ros = ( RequestOutputStream ) os;

        if ( useInterceptors )
        {
            //set up info object
            info = new ClientRequestInfoImpl();
            info.orb = orb;
            info.operation = ros.operation();
            info.response_expected = ros.response_expected();
            info.received_exception = orb.create_any();

            if ( ros.getRequest() != null )
                info.setRequest( ros.getRequest() );

            info.effective_target = self;

            ParsedIOR pior = getParsedIOR();

            if ( piorOriginal != null )
                info.target = orb._getObject( pior );
            else
                info.target = self;

            info.effective_profile = pior.getEffectiveProfile();

            // bnv: simply call pior.getProfileBody()
            org.omg.IIOP.ProfileBody_1_1 _body = pior.getProfileBody();

            if ( _body != null )
                info.effective_components = _body.components;

            if ( info.effective_components == null )
            {
                info.effective_components = new org.omg.IOP.TaggedComponent[ 0 ];
            }

            info.delegate = this;

            info.request_id = ros.requestId();
            InterceptorManager manager = orb.getInterceptorManager();

            info.current = manager.getCurrent();

            //allow interceptors access to request output stream
            info.request_os = ros;

            //allow (BiDir) interceptor to inspect the connection
            info.connection = connection;

            invokeInterceptors( info, ClientInterceptorIterator.SEND_REQUEST );

            //add service contexts to message
            Enumeration ctx = info.getRequestServiceContexts();

            while ( ctx.hasMoreElements() )
            {
                ros.addServiceContext( ( ServiceContext ) ctx.nextElement() );
            }

        }

        ReplyPlaceholder placeholder = null;

        try
        {
            if ( ros.response_expected() )
            {
                placeholder = new ReplyPlaceholder();

                //store pending replies, so in the case of a
                //LocationForward a RemarshalException can be thrown
                //to *all* waiting threads.

                synchronized ( pending_replies )
                {
                    pending_replies.put( placeholder, placeholder );
                }

                synchronized ( bind_sync )
                {
                    if ( ros.getConnection() == connection )
                    {
                        //RequestOutputStream has been created for
                        //exactly this connection
                        connection.sendRequest( ros,
                                                placeholder,
                                                ros.requestId() );
                    }
                    else
                    {
                        //RequestOutputStream has been created for
                        //other connection, so try again.
                        throw new RemarshalException();
                    }

                }

            }
            else
            {
                connection.sendRequest( ros );
            }

        }
        catch ( org.omg.CORBA.SystemException cfe )
        {
            if ( useInterceptors && ( info != null ) )
            {
                SystemExceptionHelper.insert( info.received_exception, cfe );

                try
                {
                    info.received_exception_id =
                        SystemExceptionHelper.type( cfe ).id();
                }
                catch ( org.omg.CORBA.TypeCodePackage.BadKind _bk )
                {
                    Debug.output( 2, _bk );
                }

                info.reply_status = SYSTEM_EXCEPTION.value;

                invokeInterceptors( info,
                                    ClientInterceptorIterator.RECEIVE_EXCEPTION );
            }

            if ( cfe instanceof org.omg.CORBA.TRANSIENT )
            {
                //if the exception is a TRANSIENT then we may want to retry

                synchronized ( bind_sync )
                {
                    if ( piorOriginal != null )
                    {
                        Debug.output( 2, "Delegate: falling back to original IOR" );

                        //keep last failed ior to detect forwarding loops
                        piorLastFailed = getParsedIOR();

                        //rebind to the original ior
                        rebind( piorOriginal );

                        //clean up and start fresh
                        piorOriginal = null;

                        //now cause this invocation to be repeated by the
                        //caller of invoke(), i.e. the stub
                        throw new RemarshalException();
                    }
                    else if ( Environment.useImR() && ! isImR )
                    {
                        Integer orbTypeId = getParsedIOR().getORBTypeId();

                        // only lookup ImR if IOR is generated by JacORB
                        if ( orbTypeId == null ||
                                orbTypeId.intValue() != ORBConstants.JACORB_ORB_ID )
                        {
                            Debug.output( 2, "Delegate: foreign IOR detected" );
                            throw cfe;
                        }

                        Debug.output( 2, "Delegate: JacORB IOR detected" );

                        byte[] object_key = getParsedIOR().get_object_key();

                        // No backup IOR so it may be that the ImR is down
                        // Attempt to resolve the ImR again to see if it has
                        // come back up at a different address
                        Debug.output( 2, "Delegate: attempting to contact ImR" );

                        ImRAccess imr = null;

                        try
                        {
                            imr = ( ImRAccess ) Class.forName( "org.jacorb.imr.ImRAccessImpl" ).newInstance();
                            imr.connect( orb );
                        }
                        catch ( Exception e )
                        {
                            Debug.output( 2, "Delegate: failed to contact ImR" );
                            throw cfe;
                        }

                        //create a corbaloc URL to use to contact the server
                        StringBuffer corbaloc = new StringBuffer( "corbaloc:iiop:" );

                        corbaloc.append( imr.getImRHost() );

                        corbaloc.append( ":" );

                        corbaloc.append( imr.getImRPort() );

                        corbaloc.append( "/" );

                        corbaloc.append( CorbaLoc.parseKey( object_key ) );

                        //rebind to the new IOR
                        rebind( new ParsedIOR( corbaloc.toString() ) );

                        //clean up and start fresh
                        piorOriginal = null;

                        //now cause this invocation to be repeated by the
                        //caller of invoke(), i.e. the stub
                        throw new RemarshalException();
                    }

                }

            }

            throw cfe;
        }

        /* look at the result stream now */

        if ( placeholder != null )
        {
            //response is expected

            ReplyInputStream rep = null;

            try
            {
                //this blocks until the reply arrives
                rep = ( ReplyInputStream ) placeholder.getInputStream();

                //this will check the reply status and throw arrived
                //exceptions
                if ( !doNotCheckExceptions )
                    rep.checkExceptions();

                if ( useInterceptors && ( info != null ) )
                {
                    ReplyHeader_1_2 _header = rep.rep_hdr;

                    if ( _header.reply_status.value() == ReplyStatusType_1_2._NO_EXCEPTION )
                    {
                        info.reply_status = SUCCESSFUL.value;

                        info.setReplyServiceContexts( _header.service_context );

                        //the case that invoke was called from
                        //dii.Request._invoke() will be handled inside
                        //of dii.Request._invoke() itself, because the
                        //result will first be available there
                        if ( ros.getRequest() == null )
                        {
                            InterceptorManager manager = orb.getInterceptorManager();
                            info.current = manager.getCurrent();

                            //allow interceptors access to reply input stream
                            info.reply_is = rep;

                            invokeInterceptors( info,
                                                ClientInterceptorIterator.RECEIVE_REPLY );
                        }
                        else
                            ros.getRequest().setInfo( info );
                    }

                }

                return rep;
            }
            catch ( RemarshalException re )
            {
                //wait, until the thread that received the actual
                //ForwardRequest rebound this Delegate
                pending_replies_sync.waitOnBarrier();

                throw re;
            }
            catch ( org.omg.PortableServer.ForwardRequest f )
            {
                if ( useInterceptors && ( info != null ) )
                {
                    info.reply_status = LOCATION_FORWARD.value;
                    info.setReplyServiceContexts( rep.rep_hdr.service_context );

                    info.forward_reference = f.forward_reference;

                    //allow interceptors access to reply input stream
                    info.reply_is = rep;

                    invokeInterceptors( info,
                                        ClientInterceptorIterator.RECEIVE_OTHER );
                }

                /* retrieve the forwarded IOR and bind to it */

                //make other threads, that have unreturned replies, wait
                pending_replies_sync.lockBarrier();

                //tell every pending request to remarshal
                //they will be blocked on the barrier
                synchronized ( pending_replies )
                {
                    for ( Enumeration e = pending_replies.elements();
                            e.hasMoreElements(); )
                    {
                        ReplyPlaceholder r =
                            ( ReplyPlaceholder ) e.nextElement();

                        r.retry
                        ();
                    }

                }

                //do the actual rebind
                rebind( orb.object_to_string( f.forward_reference ) );

                //now other threads can safely remarshal
                pending_replies_sync.openBarrier();

                throw new RemarshalException();
            }
            catch ( SystemException _sys_ex )
            {
                if ( useInterceptors && ( info != null ) )
                {
                    info.reply_status = SYSTEM_EXCEPTION.value;

                    info.setReplyServiceContexts( rep.rep_hdr.service_context );

                    SystemExceptionHelper.insert( info.received_exception, _sys_ex );

                    try
                    {
                        info.received_exception_id =
                            SystemExceptionHelper.type( _sys_ex ).id();
                    }
                    catch ( org.omg.CORBA.TypeCodePackage.BadKind _bk )
                    {
                        Debug.output( 2, _bk );
                    }

                    //allow interceptors access to reply input stream
                    info.reply_is = rep;

                    invokeInterceptors( info,
                                        ClientInterceptorIterator.RECEIVE_EXCEPTION );
                }

                throw _sys_ex;
            }
            catch ( ApplicationException _user_ex )
            {
                if ( useInterceptors && ( info != null ) )
                {
                    info.reply_status = USER_EXCEPTION.value;
                    info.setReplyServiceContexts( rep.rep_hdr.service_context );

                    info.received_exception_id = _user_ex.getId();

                    rep.mark( 0 );

                    try
                    {
                        ApplicationExceptionHelper.insert( info.received_exception, _user_ex );
                    }
                    catch ( Exception _e )
                    {
                        Debug.output( 2, _e );

                        SystemExceptionHelper.insert( info.received_exception,
                                                      new org.omg.CORBA.UNKNOWN( _e.getMessage() ) );
                    }

                    try
                    {
                        rep.reset();
                    }
                    catch ( Exception _e )
                    {
                        //shouldn't happen anyway
                        Debug.output( 2, _e );
                    }

                    //allow interceptors access to reply input stream
                    info.reply_is = rep;

                    invokeInterceptors( info,
                                        ClientInterceptorIterator.RECEIVE_EXCEPTION );
                }

                throw _user_ex;
            }
            finally
            {
                //reply returned (with whatever result)
                synchronized ( pending_replies )
                {
                    if ( placeholder != null )
                    {
                        pending_replies.remove( placeholder );
                    }

                }

            }

        }
        else
        {
            if ( useInterceptors && ( info != null ) )
            {
                //oneway call
                info.reply_status = SUCCESSFUL.value;

                invokeInterceptors( info, ClientInterceptorIterator.RECEIVE_OTHER );
            }

            return null; //call was oneway
        }

    }

    public void invokeInterceptors( ClientRequestInfoImpl info, short op )
    throws RemarshalException
    {
        ClientInterceptorIterator intercept_iter =
            orb.getInterceptorManager().getClientIterator();

        try
        {
            intercept_iter.iterate( info, op );
        }
        catch ( org.omg.PortableInterceptor.ForwardRequest fwd )
        {
            rebind( orb.object_to_string( fwd.forward ) );
            throw new RemarshalException();
        }
        catch ( org.omg.CORBA.UserException ue )
        {
            Debug.output( Debug.INTERCEPTOR | Debug.IMPORTANT, ue );
        }
    }

    /**
     * Determines whether the object denoted by self
     * has type logical_type_id or a subtype of it
     */

    public boolean is_a( org.omg.CORBA.Object self, String logical_type_id )
    {
        /* First, try to find out without a remote invocation. */

        /* check most derived type as defined in the IOR first
         * (this type might otherwise not be found if the helper 
         * is consulted and the reference was not narrowed to
         * the most derived type. In this case, the ids returned by
         * the helper won't contain the most derived type
         */

        ParsedIOR pior = getParsedIOR();

        if ( pior.getTypeId().equals( logical_type_id ) )
            return true;

        /*   The Ids in ObjectImpl will at least contain the type id
             found in the object reference itself.
        */
        String[] ids = ( ( org.omg.CORBA.portable.ObjectImpl ) self )._ids();

        /* the last id will be CORBA.Object, and we know that already... */
        for ( int i = 0; i < ids.length - 1; i++ )
        {
            if ( ids[ i ].equals( logical_type_id ) )
                return true;
        }

        /* ok, we could not affirm by simply looking at the locally available
           type ids, so ask the object itself */

        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;
        org.omg.PortableServer.Servant servant;
        org.omg.CORBA.portable.ServantObject so;

        while (true)
        {
            // If local object call _is_a directly

            if (is_really_local (self))
            {

                so = servant_preinvoke (self, "_is_a", java.lang.Object.class);

                try
                {
                    servant = (org.omg.PortableServer.Servant) so.servant;
                    return (servant._is_a (logical_type_id));
                }
                finally
                {
                    servant_postinvoke (self, so);
                }
            }
            else
            {
                try
                {
                    os = request( self, "_is_a", true );
                    os.write_string( logical_type_id );
                    is = invoke( self, os );
                    return is.read_boolean();
                }
                catch ( RemarshalException r )
                {
                }
                catch ( ApplicationException _ax )
                {
                    String _id = _ax.getId();
                    throw new RuntimeException( "Unexpected exception " + _id );
                }
            }
        }
    }

    public boolean is_equivalent ( org.omg.CORBA.Object self,
                                   org.omg.CORBA.Object obj )
    {
        boolean result = true;

        if ( self != obj )
        {
            ParsedIOR pior1 = new ParsedIOR ( obj.toString () );
            ParsedIOR pior2 = new ParsedIOR ( self.toString () );
            result = pior2.getIDString().equals ( pior1.getIDString () );
        }

        return result;
    }

    public String getIDString ()
    {
        return ( getParsedIOR().getIDString () );
    }

    /**
     * @return true if this object lives on a local POA and
     * interceptors are not installed. When interceptors are
     * installed this returns false so that stubs do not call
     * direct to implementation, avoiding installed interceptors.
     */

    public boolean is_local (org.omg.CORBA.Object self)
    {
        if (orb.hasRequestInterceptors ())
        {
            return false;
        }
        return (is_really_local (self));
    }

    /**
     * @return true iff this object lives on a local POA
     */

    private boolean is_really_local (org.omg.CORBA.Object self)
    {
        if (poa == null)
        {
            resolvePOA (self);
        }

        return poa != null;
    }

    public boolean is_nil()
    {
        ParsedIOR pior = getParsedIOR();

        return ( pior.getIOR().type_id.equals( "" ) &&
                 pior.getIOR().profiles.length == 0 );
    }

    public boolean non_existent (org.omg.CORBA.Object self)
    {
        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;
        org.omg.PortableServer.Servant servant;
        org.omg.CORBA.portable.ServantObject so;

        while (true)
        {
            // If local object call _non_existent directly

            if (is_really_local (self))
            {
                so = servant_preinvoke (self, "_non_existent", java.lang.Object.class);

                try
                {
                    servant = (org.omg.PortableServer.Servant) so.servant;
                    return (servant._non_existent ());
                }
                finally
                {
                    servant_postinvoke (self, so);
                }
            }
            else
            {
                try
                {
                    os = request (self, "_non_existent", true);
                    is = invoke (self, os);
                    return is.read_boolean ();
                }
                catch (RemarshalException r)
                {
                }
                catch (Exception n)
                {
                    return true;
                }
            }
        }
    }

    public org.omg.CORBA.ORB orb( org.omg.CORBA.Object self )
    {
        return orb;
    }

    public synchronized void release( org.omg.CORBA.Object self )
    {
    }

    /**
     * releases the InputStream
     */
    public void releaseReply( org.omg.CORBA.Object self,
                              org.omg.CORBA.portable.InputStream is )
    {
        if ( is != null )
        {
            try
            {
                is.close();
            }
            catch ( java.io.IOException io )
            {
            }

        }

    }

    public synchronized org.omg.CORBA.Request request( org.omg.CORBA.Object self,

            String operation )
    {
        synchronized ( bind_sync )
        {
            bind();

            return new org.jacorb.orb.dii.Request( self,
                                                   orb,
                                                   connection,
                                                   getParsedIOR().get_object_key(),
                                                   operation );
        }

    }

    /**
     */

    public synchronized org.omg.CORBA.portable.OutputStream request( org.omg.CORBA.Object self,
            String operation,
            boolean responseExpected )
    {
        // NOTE: When making changes to this method which are outside of the
        // Interceptor-if-statement, please make sure to update
        // get_poliy_no_intercept as well!

        synchronized ( bind_sync )
        {
            bind();

            ParsedIOR p = getParsedIOR();

            RequestOutputStream ros =
                new RequestOutputStream( connection,
                                         connection.getId(),
                                         operation,
                                         responseExpected,
                                         p.get_object_key(),
                                         ( int ) p.getProfileBody().iiop_version.minor );

            //Problem: What about the case where different objects
            //that are accessed by the same connection have different
            //codesets?  Is this possible anyway?
            if ( ! connection.isTCSNegotiated() )
            {
                ServiceContext ctx = connection.setCodeSet( p );

                if ( ctx != null )
                {
                    ros.addServiceContext( ctx );
                }

            }

            //Setting the codesets not until here results in the
            //header being writtend using the default codesets. On the
            //other hand, the server side must have already read the
            //header to discover the codeset service context.
            ros.setCodeSet( connection.getTCS(), connection.getTCSW() );

            return ros;
        }

    }

    /**
     * @overrides servant_postinvoke() in org.omg.CORBA.portable.Delegate<BR>
     * called from generated stubs after a local operation
     */


    public void servant_postinvoke( org.omg.CORBA.Object self, ServantObject servant )
    {
        orb.getPOACurrent()._removeContext( Thread.currentThread() );
        if (poa != null)
        {
            poa.removeLocalRequest ();
        }
    }

    /**
     * @overrides servant_preinvoke() in org.omg.CORBA.portable.Delegate<BR>
     * called from generated stubs before a local operation
     */

    public ServantObject servant_preinvoke( org.omg.CORBA.Object self,
                                            String operation,
                                            Class expectedType )
    {
        if (poa == null)
        {
            resolvePOA (self);
        }

        if (poa != null)
        {
            poa.addLocalRequest ();
            try
            {
                ServantObject so = new ServantObject();

                if ( ( poa.isRetain() && !poa.isUseServantManager() ) ||
                        poa.useDefaultServant() )
                {
                    // no ServantManagers, but AOM use
                    so.servant = poa.reference_to_servant( self );
                }
                else if ( poa.isUseServantManager() )
                {
                    byte [] oid =
                        POAUtil.extractOID( getParsedIOR().get_object_key() );
                    org.omg.PortableServer.ServantManager sm =
                        poa.get_servant_manager();

                    if ( poa.isRetain() )
                    {
                        // ServantManager is a ServantActivator:
                        // see if the servant is already activated
                        try
                        {
                            so.servant = poa.id_to_servant( oid );
                        }
                        catch( ObjectNotActive ona )
                        {
                            // okay, we need to activate it
                            org.omg.PortableServer.ServantActivator sa =
                                ( org.omg.PortableServer.ServantActivator ) sm ;
                            org.omg.PortableServer.ServantActivatorHelper.narrow( sm );
                            so.servant = sa.incarnate( oid, poa );
                        }                        
                    }
                    else
                    {
                        // ServantManager is a ServantLocator: 
                        // locate a servant

                        org.omg.PortableServer.ServantLocator sl =
                            ( org.omg.PortableServer.ServantLocator ) sm;

                        so.servant =
                            sl.preinvoke( oid, poa, operation,
                                  new org.omg.PortableServer.ServantLocatorPackage.CookieHolder() );
                    }
                }
                else
                {
                    System.err.println ("Internal error: we should have gotten to this piece of code!");
                }
                
                if ( !expectedType.isInstance( so.servant ) )
                {
                    Debug.output(1, "Warning: expected " + expectedType + 
                                 " got " + so.servant.getClass() );
                    return null;
                }
                else
                {
                    orb.getPOACurrent()._addContext(
                              Thread.currentThread(),
                              new org.jacorb.poa.LocalInvocationContext(
                                             orb,
                                             poa,
                                             getObjectId(),
                                             ( org.omg.PortableServer.Servant ) so.servant
                                             )
                                  );
                }                
                return so;
            }
            catch ( Throwable e )
            {
                Debug.output( 2, e );
            }

        }
        Debug.output(1, "Internal Warning: no POA! servant_preinvoke returns null ");
        return null;
    }

        /**
         * used only by ORB.getConnection ( Delegate ) when diverting
         * connection to the proxy by Delegate.servant_preinvoke 
         */
        /*
        public void set_adport_and_key( String ap, byte[] _key )
        {
            //adport = ap;
            //object_key = _key;
    }

        public void setIOR(org.omg.IOP.IOR _ior)
        {        
            synchronized( bind_sync )
            {
                _pior = new ParsedIOR( _ior );
                piorOriginal = null;

                bind_sync.notifyAll();
            }     
    }
        */
        public String toString()
        {
            synchronized ( bind_sync )
            {
                if ( piorOriginal != null )
                    return piorOriginal.getIORString();
                else
                    return getParsedIOR().getIORString();
            }

        }

        public String toString( org.omg.CORBA.Object self )
        {
            return toString();
        }

        public String typeId()
        {
            return getParsedIOR().getIOR().type_id;
        }

        public boolean useSSL()
        {
            return getParsedIOR().useSSL();
        }

        public org.omg.CORBA.Object set_policy_override( org.omg.CORBA.Object self,
                org.omg.CORBA.Policy[] policies,
                org.omg.CORBA.SetOverrideType set_add )
        {
            if ( set_add == org.omg.CORBA.SetOverrideType.SET_OVERRIDE )
            {
                policy_overrides.clear();
            }

            for ( int i = 0; i < policies.length; i++ )
            {
                if ( orb.hasPolicyFactoryForType( policies[ i ].policy_type() ) )
                {
                    policy_overrides.put( new Integer( policies[ i ].policy_type() ), policies[ i ] );
                }

            }

            ParsedIOR pior = getParsedIOR();
            org.omg.IOP.IOR ior = orb.createIOR( pior.getIOR().type_id,
                                                 pior.get_object_key(),
                                                 !poa.isPersistent(),
                                                 poa,
                                                 policy_overrides );

            synchronized ( bind_sync )
            {
                _pior = new ParsedIOR( ior );
                getParsedIOR().init();
            }

            return self;
        }

        public String get_codebase( org.omg.CORBA.Object self )
        {
            return getParsedIOR().getCodebaseComponent();
        }

        private class Barrier
        {
            private boolean is_open = true;

            public synchronized void waitOnBarrier()
            {
                while ( ! is_open )
                {
                    try
                    {
                        this.wait();
                    }
                    catch ( InterruptedException e )
                    {
                        //ignore
                    }

                }

            }

            public synchronized void lockBarrier()

            {
                is_open = false;
            }

            public synchronized void openBarrier()
            {
                is_open = true;

                this.notifyAll();
            }
        }
    }
