package org.jacorb.orb;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jacorb.config.*;
import org.slf4j.Logger;
import org.jacorb.imr.ImRAccessImpl;
import org.jacorb.ir.RepositoryID;
import org.jacorb.orb.giop.ClientConnection;
import org.jacorb.orb.giop.ClientConnectionManager;
import org.jacorb.orb.giop.LocateReplyInputStream;
import org.jacorb.orb.giop.LocateRequestOutputStream;
import org.jacorb.orb.giop.ReplyInputStream;
import org.jacorb.orb.giop.ReplyPlaceholder;
import org.jacorb.orb.giop.RequestOutputStream;
import org.jacorb.orb.portableInterceptor.ClientInterceptorIterator;
import org.jacorb.orb.portableInterceptor.ClientRequestInfoImpl;
import org.jacorb.orb.util.CorbaLoc;
import org.jacorb.poa.util.POAUtil;
import org.jacorb.util.ObjectUtil;
import org.jacorb.util.Time;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INTERNAL;
import org.omg.CORBA.INV_OBJREF;
import org.omg.CORBA.OBJ_ADAPTER;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TIMEOUT;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA.portable.ServantObject;
import org.omg.GIOP.LocateStatusType_1_2;
import org.omg.Messaging.RELATIVE_REQ_TIMEOUT_POLICY_TYPE;
import org.omg.Messaging.RELATIVE_RT_TIMEOUT_POLICY_TYPE;
import org.omg.Messaging.REPLY_END_TIME_POLICY_TYPE;
import org.omg.Messaging.REPLY_START_TIME_POLICY_TYPE;
import org.omg.Messaging.REQUEST_END_TIME_POLICY_TYPE;
import org.omg.Messaging.REQUEST_START_TIME_POLICY_TYPE;
import org.omg.Messaging.SYNC_NONE;
import org.omg.Messaging.SYNC_SCOPE_POLICY_TYPE;
import org.omg.Messaging.SYNC_WITH_SERVER;
import org.omg.Messaging.SYNC_WITH_TARGET;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import org.omg.PortableInterceptor.SUCCESSFUL;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ServantActivator;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;
import org.omg.TimeBase.UtcT;

/**
 * JacORB implementation of CORBA object reference
 *
 * @author Gerald Brose
 * @version $Id: Delegate.java,v 1.152 2009-08-04 14:13:56 alexander.bykov Exp $
 *
 */

public final class Delegate
    extends org.omg.CORBA_2_3.portable.Delegate
    implements Configurable
{
    // WARNING: DO NOT USE _pior DIRECTLY, BECAUSE THAT IS NOT MT
    // SAFE. USE getParsedIOR() INSTEAD, AND KEEP A METHOD-LOCAL COPY
    // OF THE REFERENCE.
    private ParsedIOR _pior = null;
    private org.omg.IOP.IOR ior = null;
    private ClientConnection connection = null;
    private String objectReference = null;

    /* save original ior for fallback */
    private ParsedIOR piorOriginal = null;

    /* save iors to detect and prevent locate forward loop */
    private ParsedIOR piorLastFailed = null;

    /* flag to indicate if this is the delegate for the ImR */
    private boolean isImR = false;

    private boolean bound = false;
    private org.jacorb.poa.POA poa;

    private final org.jacorb.orb.ORB orb;
    private Logger logger = null;

    /** set after the first attempt to determine whether
        this reference is to a local object */
    private boolean resolved_locality = false;

    private final Set     pending_replies      = new HashSet();
    private final Barrier pending_replies_sync = new Barrier();

    private final java.lang.Object bind_sync = new java.lang.Object();

    private boolean locate_on_bind_performed = false;

    private ClientConnectionManager conn_mg = null;

    private Map policy_overrides;

    private boolean doNotCheckExceptions = false; //Setting for Appligator

    private CookieHolder cookie = null;

    private String invokedOperation = null;

    /**
     * <code>localInterceptors</code> stores the ClientInterceptorHandler that is
     * currently in use during an interceptor invocation. It is held within a
     * thread local to prevent thread interaction issues.
     */
    private static final ThreadLocal localInterceptors = new ThreadLocal();

    /** the configuration object for this delegate */
    private Configuration configuration = null;

    /** configuration properties */
    private boolean useIMR;
    private boolean locateOnBind;

    /**
     * 03-09-04: 1.5.2.2
     *
     * boolean threadlocal to ensure that
     * after servant_preinvoke has returned null the
     * next call to is_local will return false
     * so that the stub will choose the non-optimized path
     */
    private static final ThreadLocal ignoreNextCallToIsLocal = new ThreadLocal()
    {
        protected java.lang.Object initialValue()
        {
            return Boolean.FALSE;
        }
    };

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
     * have a look at the comment for operation bind().
     */


    /* constructors: */

    private Delegate(ORB orb)
    {
        super();

        this.orb = orb;
    }

    public Delegate ( org.jacorb.orb.ORB orb, ParsedIOR pior )
    {
        this(orb);
        _pior = pior;

        checkIfImR( _pior.getTypeId() );
        conn_mg = orb.getClientConnectionManager();
    }

    public Delegate( org.jacorb.orb.ORB orb, String object_reference )
    {
        this(orb);

        if ( object_reference.indexOf( "IOR:" ) != 0 )
        {
            throw new org.omg.CORBA.INV_OBJREF( "Not an IOR: " +
                                                object_reference );
        }

        this.objectReference = object_reference;
        conn_mg = orb.getClientConnectionManager();
    }

    public Delegate(org.jacorb.orb.ORB orb, org.omg.IOP.IOR ior)
    {
        this(orb);
        this.ior = ior;
        conn_mg = orb.getClientConnectionManager();
    }

    /**
     * special constructor for appligator
     */
    public Delegate( org.jacorb.orb.ORB orb,
                     String object_reference,
                     boolean _donotcheckexceptions )
    {
        this( orb, object_reference );
        doNotCheckExceptions = _donotcheckexceptions;
    }


    public void configure(Configuration config) throws ConfigurationException
    {
        this.configuration = config;
        logger =
            ((Configuration)config).getLogger("jacorb.orb.delegate");
        useIMR =
            config.getAttribute("jacorb.use_imr","off").equals("on");
        locateOnBind =
            config.getAttribute("jacorb.locate_on_bind","off").equals("on");

        if (objectReference != null)
        {
            _pior = new ParsedIOR( orb, objectReference);
        }
        else if (ior!=null)
        {
            _pior = new ParsedIOR( orb, ior);
        }
        else if (_pior == null )
        {
            throw new ConfigurationException("Neither objectReference nor IOR set!");
        }
        checkIfImR( _pior.getTypeId() );
   }


    public boolean doNotCheckExceptions()
    {
        return doNotCheckExceptions;
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
        if ("IDL:org/jacorb/imr/ImplementationRepository:1.0".equals (typeId))
        {
            isImR = true;
        }
    }


    public int _get_TCKind()
    {
        return org.omg.CORBA.TCKind._tk_objref;
    }

    /**
     * @see #bind(boolean)
     */
    private void bind()
    {
        bind(false);
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
     *
     * @param rebind a <code>boolean</code> value which denotes if rebind
     *               was the caller. If so, we will avoid checking client
     *               protocols as that will have already been done.
     */
    private void bind(boolean rebind)
    {
        synchronized (bind_sync)
        {
            if ( bound )
            {
                return;
            }

            if (!rebind)
            {
                // Check if ClientProtocolPolicy set, if so, set profile
                // selector for IOR that selects effective profile for protocol
                org.omg.RTCORBA.Protocol[] protocols = getClientProtocols();
                if (protocols != null)
                {
                    _pior.setProfileSelector(new SpecificProfileSelector(protocols));
                }
            }

            org.omg.ETF.Profile profile = _pior.getEffectiveProfile();
            if (profile == null)
            {
                throw new org.omg.CORBA.COMM_FAILURE ("no effective profile");
            }

            connection = conn_mg.getConnection(profile);
            bound = true;

            /* The delegate could query the server for the object
             *  location using a GIOP locate request to make sure the
             *  first call will get through without redirections
             *  (provided the server's answer is definite):
             */
            if (( ! locate_on_bind_performed ) &&
                    locateOnBind )
            {
                //only locate once, because bind is called from the
                //switch statement below again.
                locate_on_bind_performed = true;

                try
                {
                    LocateRequestOutputStream lros =
                        new LocateRequestOutputStream
                            ( _pior.get_object_key(),
                              connection.getId(),
                              _pior.getEffectiveProfile().version().minor );

                    LocateReplyReceiver receiver =
                        new LocateReplyReceiver(orb);

                    connection.sendRequest( lros,
                                            receiver,
                                            lros.getRequestId(),
                                            true ); //response expected

                    LocateReplyInputStream lris = receiver.getReply();

                    switch ( lris.rep_hdr.locate_status.value() )
                    {

                    case LocateStatusType_1_2._UNKNOWN_OBJECT :
                        {
                            throw new org.omg.CORBA.UNKNOWN( "Could not bind to object, server does not know it!" );
                        }

                    case LocateStatusType_1_2._OBJECT_HERE :
                        {
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
                            rebind(lris.read_Object());
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
                            throw new IllegalArgumentException( "Unknown reply status for LOCATE_REQUEST: " + lris.rep_hdr.locate_status.value() );
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
                    if (logger.isWarnEnabled())
                    {
                        logger.warn( e.getMessage() );
                    }
                }

            }

            //wake up threads waiting for the pior
            bind_sync.notifyAll();
        }
    }

    public void rebind(org.omg.CORBA.Object obj)
    {
        String object_reference = orb.object_to_string(obj);

        if (object_reference.indexOf( "IOR:" ) == 0)
        {
            rebind(new ParsedIOR( orb, object_reference));
        }
        else
        {
            throw new INV_OBJREF ("Not an IOR: " + object_reference);
        }
    }

    public void rebind(ParsedIOR ior)
    {
        synchronized ( bind_sync )
        {
            // Do the ParsedIORs currently match.
            final ParsedIOR originalIOR = getParsedIOR();
            boolean originalMatch = originalIOR.equals(ior);

            // Check if ClientProtocolPolicy set, if so, set profile
            // selector for IOR that selects effective profile for protocol
            org.omg.RTCORBA.Protocol[] protocols = getClientProtocols();

            if (protocols != null)
            {
                ior.setProfileSelector(new SpecificProfileSelector(protocols));
            }

            // While the target override may have altered the effective profile so that
            // the IORs are now equal if the original ones do not match we still have to
            // disconnect so that the connection is made with the correct effective profile.
            if (originalMatch && ior.equals(originalIOR))
            {
                //already bound to target so just return
                return ;
            }

            if (piorLastFailed != null && piorLastFailed.equals(ior))
            {
                //we've already failed to bind to the ior
                throw new org.omg.CORBA.TRANSIENT();
            }

            if (piorOriginal == null)
            {
                //keep original pior for fallback
                piorOriginal = _pior;
            }

            _pior = ior;

            if (connection != null)
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
        checkORB();

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

    public boolean equals(java.lang.Object obj)
    {
        return ( obj instanceof org.omg.CORBA.Object &&
                 toString().equals( obj.toString() ) );
    }

    public boolean equals( org.omg.CORBA.Object self, java.lang.Object obj )
    {
        return equals( obj );
    }

    /**
     * Called when a client-side stub is garbage-collected.
     * In effect, this method causes the Delegate to unregister
     * itself from the underlying GIOPConnection.  If there are
     * no other Delegates using that connection, it will be
     * closed and disposed of altogether.
     */
    protected void finalize() throws Throwable
    {
        try
        {
            release(null);
        }
        finally
        {
            super.finalize();
        }
    }

    public org.omg.CORBA.DomainManager[] get_domain_managers
    ( org.omg.CORBA.Object self )
    {
        return null;
    }

    /**
     * The get_policy operation returns the policy object of the
     * specified type, which applies to this object. It returns the
     * effective Policy for the object reference. The effective Policy
     * is the one that would be used if a request were made.  This
     * Policy is determined first by obtaining the effective override
     * for the PolicyType as returned by get_client_policy. The
     * effective override is then compared with the Policy as
     * specified in the IOR.
     * <p>
     * The effective Policy is determined by reconciling the effective
     * override and the IOR-specified Policy. If the two policies
     * cannot be reconciled, the standard system exception INV_POLICY
     * is raised with standard minor code 1. The absence of a Policy
     * value in the IOR implies that any legal value may be used.
     */

    public org.omg.CORBA.Policy get_policy( org.omg.CORBA.Object self,
                                            int policy_type )
    {
        Policy result = get_client_policy(policy_type);
        if (result != null)
        {
            // TODO: "reconcile" with server-side policy
            return result;
        }
        // if not locally overridden, ask the server
        return get_policy( self,
                           policy_type,
                           request( self, "_get_policy", true ) );
    }

    /**
     * Gets the effective overriding policy with the given type from
     * the client-side, or null if this policy type is unset.
     *
     * (Implementation is incomplete, we don't check PolicyCurrent, i.e.
     * at the thread-level)
     */

    public org.omg.CORBA.Policy get_client_policy(int policy_type)
    {
        Policy result = null;

        if (policy_overrides != null)
        {
            Integer key = ObjectUtil.newInteger(policy_type);
            result = (Policy)policy_overrides.get(key);
        }

        if ( result == null )
        {
            // no override at the object level for this type, now
            // check at the thread level, ie PolicyCurrent.
            // TODO: currently not implemented

            // check at the ORB-level
            org.omg.CORBA.PolicyManager policyManager = orb.getPolicyManager();
            if (policyManager != null)
            {
                Policy[] orbPolicies = policyManager.get_policy_overrides (new int[] {policy_type});
                if (orbPolicies!= null && orbPolicies.length == 1)
                {
                    result = orbPolicies[0];
                }
            }
        }

        return result;
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
            catch ( RemarshalException r ) // NOPMD
            {
                // Ignored
            }
            catch ( ApplicationException _ax )
            {
                String _id = _ax.getId();
                throw new INTERNAL( "Unexpected exception " + _id );
            }

        }
    } // get_policy

    public UtcT getRequestEndTime()
    {
        Policy policy = get_client_policy(REQUEST_END_TIME_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.RequestEndTimePolicy)policy).end_time();
        }
        return null;
    }

    public UtcT getReplyEndTime()
    {
        Policy policy = get_client_policy (REPLY_END_TIME_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.ReplyEndTimePolicy)policy).end_time();
        }
        return null;
    }

    public UtcT getRequestStartTime()
    {
        Policy policy = get_client_policy (REQUEST_START_TIME_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.RequestStartTimePolicy)policy).start_time();
        }
        return null;
    }

    public UtcT getReplyStartTime()
    {
        Policy policy = get_client_policy (REPLY_START_TIME_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.ReplyStartTimePolicy)policy).start_time();
        }
        return null;
    }

    public long getRelativeRoundtripTimeout()
    {
        Policy policy = get_client_policy (RELATIVE_RT_TIMEOUT_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.RelativeRoundtripTimeoutPolicy)policy)
                                                            .relative_expiry();
        }
        return -1;
    }

    public long getRelativeRequestTimeout()
    {
        Policy policy = get_client_policy (RELATIVE_REQ_TIMEOUT_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.RelativeRequestTimeoutPolicy)policy)
                                                            .relative_expiry();
        }
        return -1;
    }

    public short getSyncScope()
    {
        Policy policy = get_client_policy (SYNC_SCOPE_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.Messaging.SyncScopePolicy)policy).synchronization();
        }
        return org.omg.Messaging.SYNC_WITH_TRANSPORT.value;
    }

    public org.omg.RTCORBA.Protocol[] getClientProtocols ()
    {
        Policy policy = get_client_policy(org.omg.RTCORBA.CLIENT_PROTOCOL_POLICY_TYPE.value);
        if (policy != null)
        {
            return ((org.omg.RTCORBA.ClientProtocolPolicy)policy).protocols ();
        }
        return null;
    }

    /**
     * @deprecated Deprecated by CORBA 2.3
     */
    public org.omg.CORBA.InterfaceDef get_interface( org.omg.CORBA.Object self )
    {
        return org.omg.CORBA.InterfaceDefHelper.narrow( get_interface_def( self ) ) ;
    }

    public org.omg.CORBA.Object get_interface_def (org.omg.CORBA.Object self)
    {
        checkORB();

        // If local object call _interface directly

        if (is_really_local (self))
        {
            org.omg.PortableServer.Servant servant;
            org.omg.CORBA.portable.ServantObject so;

            so = servant_preinvoke (self, "_interface", java.lang.Object.class);

            // If preinvoke returns null POA spec, 11.3.4 states OBJ_ADAPTER
            // should be thrown.
            if (so == null )
            {
                throw new OBJ_ADAPTER ( "Servant from pre_invoke was null" );
            }
            try
            {
                servant = (org.omg.PortableServer.Servant) so.servant;
                orb.set_delegate (servant);
                return servant._get_interface_def();
            }
            finally
            {
                servant_postinvoke (self, so);
            }
        }

        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;

        while (true)
        {
            try
            {
                os = request(self, "_interface", true);
                is = invoke(self, os);
                return is.read_Object();
            }
            catch (RemarshalException re) // NOPMD
            {
                // Ignored
            }
            catch (Exception ex)
            {
                return null;
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
            return getParsedIOR().getIOR();
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
                    // ignored
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
        return poa;
    }

    /**
     */

    public org.omg.CORBA.portable.ObjectImpl getReference( org.jacorb.poa.POA _poa )
    {
        if ( _poa != null )
        {
            poa = _poa;
        }

        org.omg.CORBA.portable.ObjectImpl reference =
            new org.jacorb.orb.Reference( typeId() );

        reference._set_delegate( this );

        return reference;
    }

    public int hash( org.omg.CORBA.Object self, int x )
    {
        checkORB();

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
     * Invokes an asynchronous operation using this object reference by
     * sending the request marshalled in the OutputStream.  The reply
     * will be directed to the supplied ReplyHandler.
     */
    public void invoke( org.omg.CORBA.Object self,
                        org.omg.CORBA.portable.OutputStream os,
                        org.omg.Messaging.ReplyHandler replyHandler )
      throws ApplicationException, RemarshalException
    {
        // discard result, it is always null
        invoke_internal (self, os, replyHandler, true);
    }

    /**
     * Invokes a synchronous operation using this object reference
     * by sending the request marshalled in the OutputStream.
     * @return the reply, if a reply is expected for this request.
     * If no reply is expected, returns null.
     */
    public org.omg.CORBA.portable.InputStream invoke
                                       ( org.omg.CORBA.Object self,
                                         org.omg.CORBA.portable.OutputStream os )
      throws ApplicationException, RemarshalException
    {
        return invoke_internal (self, os, null, false);
    }

    /**
     * Internal implementation of both invoke() methods.  Note that
     * the boolean argument <code>async</code> is necessary to differentiate
     * between synchronous and asynchronous calls, because the ReplyHandler
     * can be null even for an asynchronous call.
     */
    private org.omg.CORBA.portable.InputStream invoke_internal
                               ( org.omg.CORBA.Object self,
                                 org.omg.CORBA.portable.OutputStream os,
                                 org.omg.Messaging.ReplyHandler replyHandler,
                                 boolean async )
        throws ApplicationException, RemarshalException
    {
        checkORB();

        RequestOutputStream ros      = (RequestOutputStream)os;
        ReplyReceiver       receiver = null;
        final ClientInterceptorHandler interceptors = new ClientInterceptorHandler
        (
            (ClientInterceptorHandler)localInterceptors.get(),
            orb,
            ros,
            self,
            this,
            piorOriginal,
            connection
        );

        orb.notifyTransportListeners (connection.getGIOPConnection());

        if (orb.hasRequestInterceptors())
        {
            localInterceptors.set(interceptors);

            try
            {
                interceptors.handle_send_request();
            }
            catch (RuntimeException e)
            {
                // If we are throwing a system exception then this will disrupt the call path.
                // Therefore nullify localInterceptors so it doesn't appear we are still in an
                // interceptor call. RemarshalExceptions are explicitely not caught, because in
                // that case, localInterceptors must stay set

                localInterceptors.set(null);
                throw e;
            }
        }
        else
        {
            interceptors.handle_send_request();
        }


        try
        {
            if ( !ros.response_expected() )  // oneway op
            {
                invoke_oneway (ros, interceptors);
                return null;
            }
            // response expected, synchronous or asynchronous
            receiver = new ReplyReceiver(this, ros.operation(), ros.getReplyEndTime(),
                    interceptors, replyHandler);
            receiver.configure(configuration);

            // Store the receiver in pending_replies, so in the
            // case of a LocationForward a RemarshalException can
            // be thrown to *all* waiting threads.

            synchronized (pending_replies)
            {
                pending_replies.add(receiver);
            }

            ClientConnection cltconn = null;
            synchronized (bind_sync)
            {
                if (ros.getConnection() != connection)
                {
                    logger.debug("invoke: RemarshalException");

                    // RequestOutputStream has been created for
                    // another connection, so try again
                    throw new RemarshalException();
                }
                cltconn = connection;
            }
            // Use the local copy of the client connection to avoid trouble
            // with something else affecting the real connection.
            cltconn.sendRequest(ros, receiver, ros.requestId(), true);
        }
        catch ( org.omg.CORBA.SystemException cfe )
        {
            logger.debug("invoke: SystemException");

            if( !async )
            {
               // Remove ReplyReceiver to break up reference cycle
               // Otherwise gc will not detect this Delegate and
               // will never finalize it.
               synchronized (pending_replies)
               {
                   pending_replies.remove (receiver);
               }
            }

            interceptors.handle_receive_exception ( cfe );

            // The exception is a TRANSIENT, so try rebinding.
            if ( cfe instanceof org.omg.CORBA.TRANSIENT && try_rebind() )
            {
                throw new RemarshalException();
            }

            throw cfe;
        }
        finally
        {
            if (orb.hasRequestInterceptors())
            {
                localInterceptors.set(null);
            }
        }

        if ( !async && receiver != null )
        {
            // Synchronous invocation, response expected.
            // This call blocks until the reply arrives.
            org.omg.CORBA.portable.InputStream is = receiver.getReply();

            ((CDRInputStream)is).updateMutatorConnection (connection.getGIOPConnection());

            return is;
        }

        return null;
    }

    private void invoke_oneway (RequestOutputStream ros,
                                ClientInterceptorHandler interceptors)
        throws RemarshalException, ApplicationException
    {
        switch (ros.syncScope())
        {
            case SYNC_NONE.value:
                RequestOutputStream copy = new RequestOutputStream(ros);
                passToTransport (copy);
                interceptors.handle_receive_other (SUCCESSFUL.value);
                break;

            case SYNC_WITH_TRANSPORT.value:
                connection.sendRequest (ros, false);
                interceptors.handle_receive_other (SUCCESSFUL.value);
                break;

            case SYNC_WITH_SERVER.value:
            case SYNC_WITH_TARGET.value:
                ReplyReceiver rcv = new ReplyReceiver (this,
                                                       ros.operation(),
                                                       ros.getReplyEndTime(),
                                                       interceptors,
                                                       null);
                rcv.configure(configuration);

                connection.sendRequest (ros, rcv, ros.requestId(), true);
                ReplyInputStream in = rcv.getReply();
                interceptors.handle_receive_reply (in);
                break;

            default:
                throw new org.omg.CORBA.MARSHAL
                    ("Illegal SYNC_SCOPE: " + ros.syncScope(),
                     0, CompletionStatus.COMPLETED_MAYBE);
        }
    }

    private void passToTransport (final RequestOutputStream ros)
    {
        new Thread (new Runnable()
        {
            public void run()
            {
                connection.sendRequest (ros, false);
            }
        },
        "PassToTransport").start();
    }

    private boolean try_rebind()
    {
        synchronized ( bind_sync )
        {
            if( logger.isDebugEnabled())
            {
                logger.debug("Delegate.try_rebind" );
            }

            if ( piorOriginal != null )
            {
                if( logger.isDebugEnabled())
                {
                    logger.debug("Delegate: falling back to original IOR");
                }

                //keep last failed ior to detect forwarding loops
                piorLastFailed = getParsedIOR();

                //rebind to the original ior
                rebind( piorOriginal );

                //clean up and start fresh
                piorOriginal = null;
                piorLastFailed = null; // supplied byte Kevin Heifner, OCI

                return true;
            }
            else if ( useIMR && ! isImR )
            {
                Integer orbTypeId = getParsedIOR().getORBTypeId();

                // only lookup ImR if IOR is generated by JacORB
                if ( orbTypeId == null ||
                     orbTypeId.intValue() != ORBConstants.JACORB_ORB_ID )
                {
                    if( logger.isDebugEnabled())
                    {
                        logger.debug("Delegate: foreign IOR detected" );
                    }
                    return false;
                }

                if( logger.isDebugEnabled())
                {
                    logger.debug("Delegate: JacORB IOR detected" );
                }

                byte[] object_key = getParsedIOR().get_object_key();

                // No backup IOR so it may be that the ImR is down
                // Attempt to resolve the ImR again to see if it has
                // come back up at a different address

                if( logger.isDebugEnabled())
                {
                    logger.debug("Delegate: attempting to contact ImR" );
                }

                ImRAccess imr = null;

                try
                {
                    imr = ImRAccessImpl.connect(orb);
                }
                catch ( Exception e )
                {
                    if( logger.isDebugEnabled())
                    {
                        logger.debug("Delegate: failed to contact ImR" );
                    }
                    return false;
                }

                //create a corbaloc URL to use to contact the server
                StringBuffer corbaloc = new StringBuffer( "corbaloc:iiop:" );

                corbaloc.append( imr.getImRHost() );
                corbaloc.append( ':' );
                corbaloc.append( imr.getImRPort() );
                corbaloc.append( '/' );
                corbaloc.append( CorbaLoc.parseKey( object_key ) );

                //rebind to the new IOR
                rebind( new ParsedIOR( orb, corbaloc.toString()));

                //clean up and start fresh
                piorOriginal = null;
                piorLastFailed = null; //***

                return true;
            }
            else
            {
                return false;
            }
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
            rebind(fwd.forward);
            throw new RemarshalException();
        }
        catch ( org.omg.CORBA.UserException ue )
        {
            if (logger.isErrorEnabled())
            {
                logger.error( ue.getMessage() );
            }
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
        {
            return true;
        }

        /*   The Ids in ObjectImpl will at least contain the type id
             found in the object reference itself.
        */
        String[] ids = ( ( org.omg.CORBA.portable.ObjectImpl ) self )._ids();

        /* the last id will be CORBA.Object, and we know that already... */
        for ( int i = 0; i < ids.length - 1; i++ )
        {
            if ( ids[ i ].equals( logical_type_id ) )
            {
                return true;
            }
        }

        /* ok, we could not affirm by simply looking at the locally available
           type ids, so ask the object itself */

        // If local object call _is_a directly
        if (is_really_local(self))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Located " + self + " on local POA; assuming local.");
            }

            org.omg.PortableServer.Servant servant;
            org.omg.CORBA.portable.ServantObject so;

            so = servant_preinvoke (self, "_is_a", java.lang.Object.class);

            // If preinvoke returns null POA spec, 11.3.4 states OBJ_ADAPTER
            // should be thrown.
            if (so == null )
            {
                throw new OBJ_ADAPTER( "Servant from pre_invoke was null" );
            }

            try
            {
                servant = (org.omg.PortableServer.Servant)so.servant;
                orb.set_delegate(servant);
                return servant._is_a(logical_type_id);
            }
            finally
            {
                servant_postinvoke(self, so);
            }
        }
        // The check below avoids trying to load a stub for CORBA.Object.
        // (It would be faster to check that ids.length > 1, but Sun's
        // CosNaming JNDI provider calls _is_a() on some weird ObjectImpl
        // instances whose _ids() method returns an array of length two,
        // containing two Strings equal to "IDL:omg.org/CORBA/Object:1.0".)
        if (!ids[0].equals("IDL:omg.org/CORBA/Object:1.0"))
        {
            // Try to avoid remote call - is it a derived type?
            try
            {
                // Retrieve the local stub for the object in question. Then call the _ids method
                // and see if any match the logical_type_id otherwise fall back to remote.

                final String classname = RepositoryID.className( ids[0], "Stub", null );

                int lastDot = classname.lastIndexOf( '.' );
                StringBuffer buffer = new StringBuffer( classname.substring( 0, lastDot + 1) );
                buffer.append( '_' );
                buffer.append( classname.substring( lastDot + 1 ) );

                // This will only work if there is a correspondence between the Java class
                // name and the Repository ID. If prefixes have been using then this mapping
                // may have been lost.

                // First, search with stub name
                // if not found, try with the 'org.omg.stub' prefix to support package
                // with javax prefix
                Class stub=null;
                try
                {
                    stub = ObjectUtil.classForName( buffer.toString());
                }
                catch (ClassNotFoundException e)
                {
                    stub = ObjectUtil.classForName("org.omg.stub."+buffer.toString());
                }

                Method idm = stub.getMethod ( "_ids", (Class[]) null );
                String newids[] = (String[] )idm.invoke( stub.newInstance(), (java.lang.Object[]) null );

                for ( int i = 0; i < newids.length ; i++ )
                {
                    if (newids[i].equals( logical_type_id ) )
                    {
                        return true;
                    }
                }
            }
            // If it fails fall back to a remote call.
            catch (ClassNotFoundException e) // NOPMD
            {
                // ignore
            }
            catch (IllegalArgumentException e) // NOPMD
            {
                // ignore
            }
            catch (SecurityException e) // NOPMD
            {
                // ignore
            }
            catch (NoSuchMethodException e) // NOPMD
            {
                // ignore
            }
            catch (IllegalAccessException e) // NOPMD
            {
                // ignore
            }
            catch (InvocationTargetException e) // NOPMD
            {
                // ignore
            }
            catch (InstantiationException e) // NOPMD
            {
                // ignore
            }
            catch (SystemException e) // NOPMD
            {
                // ignore
            }
            logger.debug("trying is_a remotely");
        }

        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;

        while (true)
        {
            try
            {
                os = request(self, "_is_a", true);
                os.write_string(logical_type_id);
                is = invoke(self, os);
                return is.read_boolean();
            }
            catch (RemarshalException re) // NOPMD
            {
                // Ignored
            }
            catch (ApplicationException ax)
            {
                throw new INTERNAL("Unexpected exception " + ax.getId());
            }
        }
    }

    public boolean is_equivalent(org.omg.CORBA.Object self,
                                 org.omg.CORBA.Object obj)
    {
        checkORB();

        boolean result = true;

        if (self != obj)
        {
            ParsedIOR pior1 = new ParsedIOR( orb, obj.toString() );
            ParsedIOR pior2 = new ParsedIOR( orb, self.toString() );
            result = pior2.getIDString().equals( pior1.getIDString() );
        }

        return result;
    }

    public String getIDString()
    {
        return getParsedIOR().getIDString();
    }

    /**
     * @return true if this object lives on a local POA and
     * interceptors are not installed. When interceptors are
     * installed this returns false so that stubs do not call
     * direct to implementation, avoiding installed interceptors.
     */

    public boolean is_local(org.omg.CORBA.Object self)
    {
        if (ignoreNextCallToIsLocal.get() == Boolean.TRUE)
        {
            ignoreNextCallToIsLocal.set(Boolean.FALSE);
            return false;
        }

        if (localInterceptors.get() == null && orb.hasRequestInterceptors())
        {
            return false;
        }

        return is_really_local(self);
    }

    /**
     * @return true if this object lives on a local POA
     */

    private boolean is_really_local(org.omg.CORBA.Object self)
    {
        if (poa == null)
        {
            resolvePOA(self);
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
        // If local object call _non_existent directly

        if (is_really_local(self))
        {
            org.omg.PortableServer.Servant servant;
            org.omg.CORBA.portable.ServantObject so;

            so = servant_preinvoke(self, "_non_existent", java.lang.Object.class);

            try
            {
                servant = (org.omg.PortableServer.Servant)so.servant;
                orb.set_delegate(servant);
                return servant._non_existent();
            }
            finally
            {
                servant_postinvoke(self, so);
            }
        }

        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;

        while (true)
        {
            try
            {
                os = request(self, "_non_existent", true);
                is = invoke(self, os);
                return is.read_boolean();
            }
            catch (RemarshalException re) // NOPMD
            {
                // ignored
            }
            catch (ApplicationException e)
            {
                throw new INTERNAL( "Unexpected exception " + e.getId() );
            }
        }
    }

    public org.omg.CORBA.Object get_component (org.omg.CORBA.Object self)
    {
        // If local object call _get_component directly

        if (is_really_local(self))
        {
            org.omg.PortableServer.Servant servant;
            org.omg.CORBA.portable.ServantObject so;

            so = servant_preinvoke(self, "_get_component", java.lang.Object.class);

            try
            {
                servant = (org.omg.PortableServer.Servant)so.servant;
                orb.set_delegate(servant);
                return servant._get_component();
            }
            finally
            {
                servant_postinvoke(self, so);
            }
        }

        org.omg.CORBA.portable.OutputStream os;
        org.omg.CORBA.portable.InputStream is;

        while (true)
        {
            try
            {
                os = request(self, "_get_component", true);
                is = invoke(self, os);
                return is.read_Object();
            }
            catch (RemarshalException re) // NOPMD
            {
                // ignored
            }
            catch (ApplicationException e)
            {
                throw new INTERNAL( "Unexpected exception " + e.getId() );
            }
        }
    }

    public org.omg.CORBA.ORB orb( org.omg.CORBA.Object self )
    {
        return orb;
    }

    /**
     * Called to indicate that this Delegate will no longer be used by
     * the client.  The Delegate unregisters itself from the underlying
     * GIOPConnection.  If there are no other Delegates using that
     * connection, it will be closed and disposed of altogether.
     */
    public void release( org.omg.CORBA.Object self )
    {
        synchronized ( bind_sync )
        {
            if (!bound)
            {
                return;
            }


            if ( connection != null )
            {
                conn_mg.releaseConnection( connection );
                connection = null;
            }
            bound = false;

            // Call using string rather than this to prevent data race
            // warning.
            orb._release( getParsedIOR().getIORString() );

            if ( logger.isDebugEnabled() )
            {
                logger.debug("Delegate released!");
            }
        }
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
                // ignored
            }
        }
        Time.waitFor (getReplyStartTime());
    }

    public org.omg.CORBA.Request request( org.omg.CORBA.Object self,
                                                       String operation )
    {
        orb.perform_work();

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

    public synchronized org.omg.CORBA.portable.OutputStream request
                                                 ( org.omg.CORBA.Object self,
                                                   String operation,
                                                   boolean responseExpected )
    {
        orb.perform_work();

        // Compute the deadlines for this request based on any absolute or
        // relative timing policies that have been specified.  Compute this
        // now, because it is the earliest possible time, and therefore any
        // relative timeouts will cover the entire invocation.

        UtcT requestEndTime = getRequestEndTime();
        long requestTimeout = getRelativeRequestTimeout();

        if ((requestTimeout != 0) || (requestEndTime != null))
        {
            requestEndTime = Time.earliest(Time.corbaFuture (requestTimeout),
                                           requestEndTime);
            if (Time.hasPassed(requestEndTime))
            {
                throw new TIMEOUT("Request End Time exceeded prior to invocation",
                                  0, CompletionStatus.COMPLETED_NO);
            }
        }

        UtcT replyEndTime     = getReplyEndTime();
        long roundtripTimeout = getRelativeRoundtripTimeout();

        if ((roundtripTimeout != 0) || (replyEndTime != null))
        {
            replyEndTime = Time.earliest(Time.corbaFuture (roundtripTimeout),
                                         replyEndTime);
            if (Time.hasPassed(replyEndTime))
            {
                throw new TIMEOUT("Reply End Time exceeded prior to invocation",
                                  0, CompletionStatus.COMPLETED_NO);
            }
        }

        synchronized ( bind_sync )
        {
            bind();

            ParsedIOR ior = getParsedIOR();

            RequestOutputStream out =
                new RequestOutputStream( orb,
                                         connection,
                                         connection.getId(),
                                         operation,
                                         responseExpected,
                                         getSyncScope(),
                                         getRequestStartTime(),
                                         requestEndTime,
                                         replyEndTime,
                                         ior.get_object_key(), ior.getEffectiveProfile().version().minor );

            // CodeSets are only negotiated once per connection,
            // not for each individual request
            // (CORBA 3.0, 13.10.2.6, second paragraph).
            if (!connection.isTCSNegotiated())
            {
                connection.setCodeSet(ior);
            }

            //Setting the codesets not until here results in the
            //header being writtend using the default codesets. On the
            //other hand, the server side must have already read the
            //header to discover the codeset service context.
            out.setCodeSets( connection.getTCS(), connection.getTCSW() );

            out.updateMutatorConnection (connection.getGIOPConnection());

            return out;
        }
    }

    /**
     * Overrides servant_postinvoke() in org.omg.CORBA.portable.Delegate<BR>
     * called from generated stubs after a local operation
     */


    public void servant_postinvoke( org.omg.CORBA.Object self, ServantObject servant )
    {
        if (poa != null)
        {
            if ( poa.isUseServantManager() &&
                 ! poa.isRetain() &&
                 cookie != null &&
                 invokedOperation != null )
            {
                // ServantManager is a ServantLocator:
                // call postinvoke
                try
                {
                    byte [] oid =
                    POAUtil.extractOID( getParsedIOR().get_object_key() );
                    org.omg.PortableServer.ServantLocator sl =
                        ( org.omg.PortableServer.ServantLocator ) poa.get_servant_manager();

                    sl.postinvoke( oid, poa, invokedOperation, cookie.value, (Servant)servant.servant );

                    // delete stored values
                    cookie = null;
                    invokedOperation = null;
                }
                catch ( Throwable e )
                {
                    if (logger.isWarnEnabled())
                    {
                        logger.warn( e.getMessage() );
                    }
                }

            }
            poa.removeLocalRequest();
        }
        orb.getPOACurrent()._removeContext( Thread.currentThread() );
    }

    /**
     * Overrides servant_preinvoke() in org.omg.CORBA.portable.Delegate<BR>
     * called from generated stubs before a local operation
     */

    public ServantObject servant_preinvoke( org.omg.CORBA.Object self,
                                            String operation,
                                            Class expectedType )
    {
        if (poa == null)
        {
            resolvePOA(self);
        }

        if (poa != null)
        {
            // remember that a local request is outstanding. On
            //  any exit through an exception, this must be cleared again,
            // otherwise the POA will hangon destruction (bug #400).
            poa.addLocalRequest();

            ServantObject so = new ServantObject();

            try
            {
                if ( ( poa.isRetain() && !poa.isUseServantManager() ) ||
                     poa.useDefaultServant() )
                {
                    // no ServantManagers, but AOM use
                    try
                    {
                        so.servant = poa.reference_to_servant( self );
                    }
                    catch( WrongAdapter e )
                    {
                        //  exit on an error condition, but need to clean up first (added to fix bug #400)
                        poa.removeLocalRequest();
                        throw new OBJ_ADAPTER( "WrongAdapter caught when converting servant to reference. " + e );
                    }
                    catch( WrongPolicy e )
                    {
                        //  exit on an error condition, but need to clean up first (added to fix bug #400)
                        poa.removeLocalRequest();
                        throw new OBJ_ADAPTER("WrongPolicy caught" + e );
                    }
                    catch( ObjectNotActive e )
                    {
                        //  exit on an error condition, but need to clean up first (added to fix bug #400)
                        poa.removeLocalRequest();
                        throw new org.omg.CORBA.OBJECT_NOT_EXIST();
                    }
                }
                else if ( poa.isUseServantManager() )
                {
                    byte [] oid =
                    POAUtil.extractOID( getParsedIOR().get_object_key() );
                    org.omg.PortableServer.ServantManager sm =
                        poa.get_servant_manager();

                    if ( poa.isRetain() )
                    {
                        // ServantManager is a ServantActivator. Use the AOM to
                        // incarnate this or return the servant. It will correctly
                        // synchrnoize the requests.
                        so.servant = poa._incarnateServant(oid, (ServantActivator)sm);
                    }
                    else
                    {
                        // ServantManager is a ServantLocator:
                        // locate a servant

                        org.omg.PortableServer.ServantLocator sl =
                            ( org.omg.PortableServer.ServantLocator ) sm;

                        // store this for postinvoke

                        cookie =
                           new org.omg.PortableServer.ServantLocatorPackage.CookieHolder();

                        invokedOperation = operation;

                        boolean ok = false;

                        try
                        {
                            so.servant =
                                sl.preinvoke( oid, poa, operation, cookie );
			    ok = true;
                        }
                        finally
                        {
                            if (!ok)
                            {
                                // error condition: need to clean up before
                                // propagating the exception (added to fix
                                // bug #400)
                                poa.removeLocalRequest();
                            }
                        }
                    }
                }
                else
                {
                    throw new INTERNAL("Internal error: we should not have gotten to this piece of code!");
                }
            }
            catch( WrongPolicy e )
            {
                //  exit on an error condition, but need to clean up first (added to fix bug #400)
                poa.removeLocalRequest();
                throw new OBJ_ADAPTER( "WrongPolicy caught" + e );
            }
            catch( org.omg.PortableServer.ForwardRequest e )
            {
                if( logger.isDebugEnabled() )
                {
                    logger.debug("Caught forwardrequest to " + e.forward_reference + " from " + self );
                }
                return servant_preinvoke(e.forward_reference, operation, expectedType);
            }

            if ( !expectedType.isInstance( so.servant ) )
            {
                if( logger.isWarnEnabled() )
                {
                    logger.warn("Expected " + expectedType +
                                " got " + so.servant.getClass() );
                }

                ignoreNextCallToIsLocal.set(Boolean.TRUE);

                poa.removeLocalRequest();
                return null;
            }
            orb.getPOACurrent()._addContext(
                    Thread.currentThread(),
                    new org.jacorb.poa.LocalInvocationContext(
                            orb,
                            poa,
                            getObjectId(),
                            ( org.omg.PortableServer.Servant ) so.servant
                    )
            );
            return so;
        }
        if (logger.isWarnEnabled())
        {
            logger.warn("No POA! servant_preinvoke returns null");
        }
        return null;
    }

    public String toString()
    {
        synchronized ( bind_sync )
        {
            if ( piorOriginal != null )
            {
                return piorOriginal.getIORString();
            }
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

    public org.omg.CORBA.Object set_policy_override( org.omg.CORBA.Object self,
                                                     org.omg.CORBA.Policy[] policies,
                                                     org.omg.CORBA.SetOverrideType set_add )
    {
        if (policy_overrides == null)
        {
            policy_overrides = new HashMap();
        }
        if ( set_add == org.omg.CORBA.SetOverrideType.SET_OVERRIDE )
        {
            policy_overrides.clear();
        }

        for ( int i = 0; i < policies.length; i++ )
        {
            policy_overrides.put(ObjectUtil.newInteger( policies[ i ].policy_type() ), policies[ i ] );
        }

        return self;
    }

    public String get_codebase( org.omg.CORBA.Object self )
    {
        return getParsedIOR().getCodebaseComponent();
    }

    public Set get_pending_replies()
    {
        return pending_replies;
    }

    public void replyDone (ReplyPlaceholder placeholder)
    {
        synchronized (pending_replies)
        {
            pending_replies.remove (placeholder);
        }
    }

    public void lockBarrier()
    {
        pending_replies_sync.lockBarrier();
    }

    public void waitOnBarrier()
    {
        pending_replies_sync.waitOnBarrier();
    }

    public void openBarrier()
    {
        pending_replies_sync.openBarrier();
    }

    /**
     * Call work_pending as that does a simple boolean check to establish
     * if the ORB has been shutdown - otherwise it throws BAD_INV_ORDER.
     */
    private void checkORB()
    {
        orb.work_pending();
    }

    private static class Barrier
    {
        private boolean is_open = true;

        public synchronized void waitOnBarrier()
        {
            while (! is_open)
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
