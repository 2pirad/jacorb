package org.jacorb.notification;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2003 Gerald Brose
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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.EventChannelEvent;
import org.jacorb.notification.interfaces.EventChannelEventListener;
// import org.jacorb.notification.persistence.EventChannelStore;
// import org.jacorb.notification.persistence.PersistenceException;
import org.jacorb.notification.util.PatternWrapper;
import org.jacorb.util.Debug;

import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNotification.BestEffort;
import org.omg.CosNotification.ConnectionReliability;
import org.omg.CosNotification.EventReliability;
import org.omg.CosNotification.Persistent;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.PropertyError;
import org.omg.CosNotification.PropertyRange;
import org.omg.CosNotification.QoSError_code;
import org.omg.CosNotification.UnsupportedAdmin;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ChannelNotFound;
import org.omg.CosNotifyChannelAdmin.EventChannel;
import org.omg.CosNotifyChannelAdmin.EventChannelFactory;
import org.omg.CosNotifyChannelAdmin.EventChannelFactoryHelper;
import org.omg.CosNotifyChannelAdmin.EventChannelFactoryPOA;
import org.omg.CosNotifyFilter.FilterFactory;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import org.apache.avalon.framework.logger.Logger;

/**
 * <code>EventChannelFactoryImpl</code> is a implementation of
 * the  <code>EventChannelFactory</code> interface which defines operations
 * for creating and managing new Notification Service style event
 * channels. It supports a routine that creates new instances of
 * Notification Service event channels and assigns unique numeric
 * identifiers to them. In addition the
 * <code>EventChannelFactory</code> interface supports a routing,
 * which can return the unique identifiers assigned to all event
 * channels created by a given instance of
 * <code>EventChannelFactory</code>, and another routine which, given
 * the unique identifier of an event channel created by a target
 * <code>EventChannelFactory</code> instance, returns the object
 * reference of that event channel.<br>
 *
 * @author Alphonse Bendt
 * @version $Id: EventChannelFactoryImpl.java,v 1.24 2004-01-16 22:28:37 alphonse.bendt Exp $
 */

public class EventChannelFactoryImpl
    extends EventChannelFactoryPOA
    implements Disposable
{
    interface ShutdownCallback
    {
        void needTime( int time );
        void shutdownComplete();
    }

    ////////////////////////////////////////

    private static final Object[] INTEGER_ARRAY_TEMPLATE = new Integer[ 0 ];

    private static final String NOTIFICATION_SERVICE = "NotificationService";

    private static final String EVENTCHANNEL_FACTORY_POA_NAME = "NotificationPOA";

    private static final String OBJECT_NAME = "_factory";

    private static final String PERSISTENT_EVENTCHANNEL_POA_NAME = "PersistentNotificationPOA";

    ////////////////////////////////////////

    protected EventChannelFactory thisFactory_;

    protected FilterFactory defaultFilterFactory_;

    protected FilterFactoryImpl defaultFilterFactoryServant_;

    protected ApplicationContext applicationContext_;

    protected ChannelContext channelContextTemplate_;

    protected int counter_ = 0;

    protected Map allChannels_;

    protected Logger logger_ = Debug.getNamedLogger(getClass().getName() );

    protected String ior_;

    protected Vector listEventChannelEventListener_ = new Vector();

    protected String corbaLoc_;

    private POA eventChannelFactoryPOA_;

    private POA persistentEventChannelPOA_;

    //    private EventChannelStore eventChannelStore_;

    ////////////////////////////////////////

    private POA getPersistentChannelPOA() {
        try {
            if (persistentEventChannelPOA_ == null) {
                ORB _orb = applicationContext_.getOrb();

                POA _rootPOA = POAHelper.narrow( _orb.resolve_initial_references( "RootPOA" ) );

                org.omg.CORBA.Policy[] _policies =
                    new org.omg.CORBA.Policy [] {
                        _rootPOA.create_id_assignment_policy( IdAssignmentPolicyValue.USER_ID ),
                        _rootPOA.create_lifespan_policy(LifespanPolicyValue.PERSISTENT)
                    };

                persistentEventChannelPOA_ = _rootPOA.create_POA( PERSISTENT_EVENTCHANNEL_POA_NAME,
                                                             _rootPOA.the_POAManager(),
                                                             _policies );

                for ( int x = 0; x < _policies.length; ++x )
                    {
                        _policies[ x ].destroy();
                    }

                persistentEventChannelPOA_.the_POAManager().activate();
            }

            return persistentEventChannelPOA_;
        } catch (Exception e) {
            logger_.fatalError("unable to create Persistent POA", e);

            throw new RuntimeException();
        }
    }

    private EventChannelFactoryImpl( final ORB _orb ) throws Exception
    {
        POA _rootPOA = POAHelper.narrow( _orb.resolve_initial_references( "RootPOA" ) );

        applicationContext_ = new ApplicationContext( _orb, _rootPOA, true );

        org.omg.CORBA.Policy[] _policies =
            new org.omg.CORBA.Policy [] {
                _rootPOA.create_id_assignment_policy( IdAssignmentPolicyValue.USER_ID )
            };

        eventChannelFactoryPOA_ = _rootPOA.create_POA( EVENTCHANNEL_FACTORY_POA_NAME,
                                                _rootPOA.the_POAManager(),
                                                _policies );

        for ( int x = 0; x < _policies.length; ++x )
        {
            _policies[ x ].destroy();
        }

        byte[] oid = ( OBJECT_NAME.getBytes() );

        eventChannelFactoryPOA_.activate_object_with_id( oid, this );
        thisFactory_ = EventChannelFactoryHelper.narrow( eventChannelFactoryPOA_.id_to_reference( oid ) );

        initialize();

        _rootPOA.the_POAManager().activate();
        eventChannelFactoryPOA_.the_POAManager().activate();

        Thread t = new Thread(
                       new Runnable()
                       {
                           public void run()
                           {
                               _orb.run();
                           }
                       }
                   );

        t.setDaemon( false );
        t.start();

        ior_ = _orb.object_to_string( eventChannelFactoryPOA_.id_to_reference( oid ) );
        corbaLoc_ = getCorbaLoc( eventChannelFactoryPOA_.the_name(), oid );

        logger_.info( "EventChannelFactory - ready" );
    }

    ////////////////////////////////////////

    /**
     * The <code>create_channel</code> operation is invoked to create
     * a new instance of the Notification Service style event
     * channel. This operation accepts two input parameters. The first
     * input parameter is a list of name-value pairs, which specify the
     * initial QoS property settings for the new channel. The second
     * input parameter is a list of name-value pairs, which specify
     * the initial administrative property settings for the new
     * channel. <br> If no implementation of the
     * <code>EventChannel</code> Interface exists that can support all
     * of the requested administrative property settings, the
     * <code>UnsupportedAdmin</code> exception is raised This
     * exception contains as data a sequence of data structures, each
     * identifies the name of an administrative property in the input
     * list whose requested setting could not be satisfied, along with
     * an error code and a range of settings for the property which
     * could be satisfied. The meanings of the error codes that might
     * be returned are described in <a
     * href="%%%NOTIFICATION_SPEC_URL%%%">Notification Service
     * Specification</a> Table 2-5 on page 2-46.<br>
     * If neither of these exceptions is raised, the
     * <code>create_channel</code> operation will return a reference
     * to a new Notification Service style event channel. In addition,
     * the operation assigns to this new event channel a numeric
     * identifier, which is unique among all event channels created by
     * the target object. This numeric identifier is returned as an
     * output parameter.
     *
     * @param qualitiyOfServiceProperties a list of name-value pairs,
     * which specify the initial QoS property settings for the new channel
     * @param administrativeProperties a list of name-value pairs,
     * which specify the initial administrative property settings for
     * the new channel
     * @param channelIdentifier, a reference to the new event channel
     * @return a newly created event channel
     * @exception UnsupportedAdmin if no implementation supports the
     * requested administrative settings
     * @exception UnsupportedQoS if no implementation supports the
     * requested QoS settings
     */
    public EventChannel create_channel( Property[] qualitiyOfServiceProperties,
                                        Property[] administrativeProperties,
                                        IntHolder channelIdentifier )
        throws UnsupportedAdmin, UnsupportedQoS
    {
        try
        {
            EventChannel _channel = null;

            // create identifier
            int _identifier = createChannelIdentifier();
            channelIdentifier.value = _identifier;
            Integer _key = new Integer( _identifier );

            EventChannelImpl _channelServant = create_channel_servant( _identifier,
                                                                       qualitiyOfServiceProperties,
                                                                       administrativeProperties );

            eventChannelServantCreated( _channelServant );

            _channel = _channelServant.getEventChannel();

            logger_.info( "created new EventChannel with id: " + _identifier );

            allChannels_.put( _key, _channelServant );

            return _channel;
        }
        catch ( WrongPolicy e )
        {
            logger_.fatalError( "create_channel()", e );
        }
        catch ( ObjectNotActive e )
        {
            logger_.fatalError( "create_channel()", e );
        }
        catch ( ServantAlreadyActive e )
        {
            logger_.fatalError( "create_channel()", e );
        }
        catch (PersistenceException e) {
            logger_.fatalError("error creating persistent channel", e);
        }

        throw new RuntimeException();
    }

    protected void eventChannelServantCreated( EventChannelImpl servant )
    {
        EventChannelEvent _event = new EventChannelEvent( servant );

        Iterator _i = listEventChannelEventListener_.iterator();

        while ( _i.hasNext() )
        {
            ( ( EventChannelEventListener ) _i.next() ).actionEventChannelCreated( _event );
        }
    }

    public EventChannelImpl create_channel_servant( int key,
                                                    Property[] qualitiyOfServiceProperties,
                                                    Property[] administrativeProperties )

        throws UnsupportedAdmin,
               UnsupportedQoS,
               ObjectNotActive,
               WrongPolicy,
               ServantAlreadyActive,
               PersistenceException
    {
        if (logger_.isDebugEnabled() ) {
            logger_.debug( "create_channel_servant " + key );
        }

        PropertyValidator.checkAdminPropertySeq( administrativeProperties );
        PropertyValidator.checkQoSPropertySeq( qualitiyOfServiceProperties );

        Map _uniqueAdminProperties =
            PropertyValidator.getUniqueProperties( administrativeProperties );

        Map _uniqueQoSProperties =
            PropertyValidator.getUniqueProperties( qualitiyOfServiceProperties );

        logger_.info( "uniqueQoSProps: " + _uniqueQoSProperties );

        if ( _uniqueQoSProperties.containsKey( EventReliability.value ) )
        {
            logger_.info( "try to set EventReliability" );

            short _eventReliabilty =
                ( ( Any ) _uniqueQoSProperties.get( EventReliability.value ) ).extract_short();

            switch ( _eventReliabilty )
            {

                case BestEffort.value:
                    break;

                case Persistent.value:
                    throwPersistentNotSupported( EventReliability.value );

                default:
                    throwBadValue( EventReliability.value );

            }
        }

        short _connectionReliability = BestEffort.value;

        if ( _uniqueQoSProperties.containsKey( ConnectionReliability.value ) )
        {
            _connectionReliability =
                ( ( Any ) _uniqueQoSProperties.get( ConnectionReliability.value ) ).extract_short();

            switch ( _connectionReliability )
            {
                case BestEffort.value:
                    break;

                case Persistent.value:
                    break;
                    //                    throwPersistentNotSupported( ConnectionReliability.value );

                default:
                    throwBadValue( ConnectionReliability.value );

            }
        }

        ChannelContext _channelContext;

        _channelContext = ( ChannelContext ) channelContextTemplate_.clone();

        if (_connectionReliability == Persistent.value ) {
            _channelContext.setPOA(getPersistentChannelPOA());
        } else {
            _channelContext.setPOA(applicationContext_.getPoa());
        }

        // create new servant
        EventChannelImpl _channelServant =
            new EventChannelImpl( key,
                                  applicationContext_,
                                  _channelContext,
                                  _uniqueQoSProperties,
                                  _uniqueAdminProperties );

        if (_connectionReliability == Persistent.value) {
            makePersistent(_channelServant);
        }

        return _channelServant;
    }

    private void makePersistent(EventChannelImpl channel) throws PersistenceException {
        logger_.info("The creation of a persistent Channel has been requested");

        if (eventChannelStore_ != null) {
            eventChannelStore_.storePersistentChannel(channel);
        }
    }

    void removeEventChannelServant( int id )
    {
        EventChannelImpl _channel =
            ( EventChannelImpl ) allChannels_.remove( new Integer( id ) );

        if (!listEventChannelEventListener_.isEmpty())
        {
            EventChannelEvent _event =
                new EventChannelEvent( _channel );

            Iterator i = listEventChannelEventListener_.iterator();

            while ( i.hasNext() )
            {
                ( ( EventChannelEventListener ) i.next() ).actionEventChannelDestroyed( _event );
            }
        }
    }

    protected int createChannelIdentifier()
    {
        return ++counter_;
    }

    /**
     * The <code>get_all_channels</code> operation returns a sequence
     * of all of the unique numeric identifiers corresponding to
     * Notification Service event channels, which have been created by
     * the target object.
     *
     * @return an <code>int[]</code> value
     */
    public int[] get_all_channels()
    {
        Integer[] _keys = ( Integer[] ) allChannels_.keySet().toArray( INTEGER_ARRAY_TEMPLATE );

        int[] _ret = new int[ _keys.length ];

        for ( int x = _keys.length - 1; x >= 0; --x )
        {
            _ret[ x ] = _keys[ x ].intValue();
        }

        return _ret;
    }

    /**
     * The <code>get_event_channel</code> operation accepts as input
     * a numeric value that is supposed to be the unique identifier of
     * a Notification Service event channel, which has been created by
     * the target object. If this input value does not correspond to
     * such a unique identifier, the <code>ChannelNotFound</code>
     * exception is raised. Otherwise, the operation returns the
     * object reference of the Notification Service event channel
     * corresponding to the input identifier.
     *
     * @param n an <code>int</code> the unique identifier of a
     * Notification Service event channel
     * @return an <code>EventChannel</code> corresponding to the input identifier
     * @exception ChannelNotFound if the input value does not
     * correspond to a Notification Service event channel
     */
    public EventChannel get_event_channel( int n ) throws ChannelNotFound
    {
        return ( ( EventChannelImpl ) allChannels_.get( new Integer( n ) ) ).getEventChannel();
    }


    public void addEventChannelEventListener( EventChannelEventListener listener )
    {
        listEventChannelEventListener_.add( listener );
    }

    public void removeEventChannelEventListener( EventChannelEventListener listener )
    {
        listEventChannelEventListener_.remove( listener );
    }

    public void shutdown( ShutdownCallback cb )
    {
        int numberOfChannels = allChannels_.size();

        // estimate 1000ms shutdowntime per channel
        cb.needTime( numberOfChannels * 1000 );

        logger_.info( "NotificationService is going down" );
        dispose();
        logger_.info( "NotificationService down" );

        cb.shutdownComplete();
    }

    public void dispose()
    {
        listEventChannelEventListener_.clear();

        Iterator _i = allChannels_.entrySet().iterator();

        while ( _i.hasNext() )
        {
            EventChannelImpl _ec = ( EventChannelImpl ) ( ( Map.Entry ) _i.next() ).getValue();
            _i.remove();
            _ec.dispose();
        }

        defaultFilterFactoryServant_.dispose();

        applicationContext_.dispose();

        applicationContext_.getOrb().shutdown( true );
    }

    private void initialize() throws InvalidName
    {
        allChannels_ = new Hashtable();

        defaultFilterFactoryServant_ = new FilterFactoryImpl( applicationContext_ );

        logger_.info( "create Default Filter Factory" );
        defaultFilterFactory_ = defaultFilterFactoryServant_._this( applicationContext_.getOrb() );

        channelContextTemplate_ = new ChannelContext();
        channelContextTemplate_.setDefaultFilterFactory( defaultFilterFactory_ );
        channelContextTemplate_.setEventChannelFactoryServant( this );
        channelContextTemplate_.setEventChannelFactory( thisFactory_ );
    }

    public String getCorbaLoc()
    {
        return corbaLoc_;
    }

    private String getCorbaLoc( String poaName, byte[] id )
    {
        StringBuffer _corbaLoc = new StringBuffer( "corbaloc::" );

        _corbaLoc.append( getLocalAddress() );
        _corbaLoc.append( ":" );

        _corbaLoc.append( getLocalPort() );
        _corbaLoc.append( "/" );
        _corbaLoc.append( NOTIFICATION_SERVICE );

        return _corbaLoc.toString();
    }

    private int getLocalPort()
    {
        return ( ( org.jacorb.orb.ORB ) applicationContext_.getOrb() ).getBasicAdapter().getPort();
    }

    private String getLocalAddress()
    {
        return ( ( org.jacorb.orb.ORB ) applicationContext_.getOrb() ).getBasicAdapter().getAddress();
    }

    private String objectIdToHexString( byte[] objectId )
    {
        StringBuffer buffer = new StringBuffer();

        for ( int x = 0; x < objectId.length; ++x )
        {
            buffer.append( "%" );
            String hexString = Integer.toHexString( objectId[ x ] ).toUpperCase();

            if ( hexString.length() == 1 )
            {
                buffer.append( "0" );
            }

            buffer.append( hexString );
        }

        return buffer.toString();
    }

    public String getIOR()
    {
        return ior_;
    }

    public EventChannelFactory getEventChannelFactory()
    {
        return thisFactory_;
    }

    public ApplicationContext getApplicationContext()
    {
        return applicationContext_;
    }

    void throwPersistentNotSupported( String property ) throws UnsupportedQoS
    {
        Any _lowVal = applicationContext_.getOrb().create_any();
        Any _highVal = applicationContext_.getOrb().create_any();

        _lowVal.insert_short( BestEffort.value );
        _highVal.insert_short( BestEffort.value );

        UnsupportedQoS _e =
            new UnsupportedQoS( new PropertyError[] {new PropertyError( QoSError_code.UNSUPPORTED_VALUE,
                                property,
                                new PropertyRange( _lowVal, _highVal ) ) } );

        throw _e;
    }

    void throwBadValue( String property ) throws UnsupportedQoS
    {
        Any _lowVal = applicationContext_.getOrb().create_any();
        Any _highVal = applicationContext_.getOrb().create_any();

        _lowVal.insert_short( BestEffort.value );
        _highVal.insert_short( BestEffort.value );

        UnsupportedQoS _e =
            new UnsupportedQoS( "The specified Property Value is not supported",
                                new PropertyError[] {
                                    new PropertyError( QoSError_code.BAD_VALUE,
                                                       property,
                                                       new PropertyRange( _lowVal, _highVal ) ) } );
        throw _e;
    }

    public POA _default_POA()
    {
        return eventChannelFactoryPOA_;
    }

    private static void help()
    {
        System.out.println( "Usage: ntfy [-printIOR] [-printCorbaloc] " +
                            "[-writeIOR <filename>] " +
                            "[-registerName <nameId>[.<nameKind>]] " +
                            "[-port <oaPort>] [-channels <channels>] [-help]" );

        System.exit( 0 );
    }

    public static EventChannelFactoryImpl newFactory() throws Exception
    {
        return newFactory( new String[ 0 ] );
    }

    public static EventChannelFactoryImpl newFactory( String[] args ) throws Exception
    {
        boolean doHelp = false;
        boolean doPrintIOR = false;
        boolean doPrintCorbaloc = false;
        String iorFileName = null;
        String oaPort = null;
        int channels = 0;
        String nameId = null;
        String nameKind = "";

        Logger _logger = Debug.getNamedLogger( EventChannelFactoryImpl.class.getName() + ".init" );

        // force Classloader to load Class PatternWrapper.
        // PatternWrapper may cause a ClassNotFoundException if
        // running on < JDK1.4 and gnu.regexp is NOT installed.
        // Therefor the Error should occur as _early_ as possible.
        PatternWrapper.class.getName();

        try
        {
            // process arguments

            for ( int i = 0; i < args.length; i++ )
            {
                if ( args[ i ].equals( "-printIOR" ) )
                {
                    doPrintIOR = true;
                }
                else if ( args[ i ].equals( "-printCorbaloc" ) )
                {
                    doPrintCorbaloc = true;
                }
                else if ( args[ i ].equals( "-help" ) )
                {
                    doHelp = true;
                }
                else if ( args[ i ].equals( "-port" ) )
                {
                    oaPort = args[ ++i ];
                }
                else if ( args[ i ].equals( "-channels" ) )
                {
                    channels = Integer.parseInt( args[ ++i ] );
                }
                else if ( args[ i ].equals( "-writeIOR" ) )
                {
                    iorFileName = args[ ++i ];
                }
                else if ( args[ i ].equals( "-registerName" ) )
                {
                    String name = args[ ++i ];

                    int index = name.indexOf( "." );

                    if ( name.lastIndexOf( "." ) != index )
                    {
                        throw new IllegalArgumentException( name +
                                                            ": argument to -registerName should be " +
                                                            "<nameId> or <nameId>.<nameKind>" );
                    }

                    if ( index != -1 )
                    {
                        nameId = name.substring( 0, index );
                        nameKind = name.substring( index + 1 );
                    }
                    else
                    {
                        nameId = name;
                    }
                }
                else
                {
                    System.out.println( "Unknown argument: " + args[ i ] );

                    help();
                }
            }
        }
        catch ( ArrayIndexOutOfBoundsException e )
        {
            doHelp = true;
        }

        if ( doHelp )
        {
            help();
            System.exit( 0 );
        }

        // set ORB properties
        String standardImplName = "Notification";

        Properties props = new Properties();

        props.put( "jacorb.implname", standardImplName );

        props.put( "jacorb.orb.objectKeyMap." + NOTIFICATION_SERVICE,
                   standardImplName + "/" + EVENTCHANNEL_FACTORY_POA_NAME + "/" + OBJECT_NAME );

        if ( oaPort != null )
        {
            props.put ( "OAPort", oaPort );
        }

        final ORB _orb = ORB.init( new String[ 0 ], props );

        EventChannelFactoryImpl _factory = new EventChannelFactoryImpl( _orb );

        for ( int i = 0; i < channels; i++ )
        {
            IntHolder ih = new IntHolder();
            _factory.create_channel( new Property[ 0 ], new Property[ 0 ], ih );
        }

        if ( doPrintIOR )
        {
            System.out.println( _factory.getIOR() );
        }

        if ( iorFileName != null )
        {
            _logger.info( "Writing IOR to file:" + iorFileName );

            try
            {
                PrintWriter out = new PrintWriter( new FileWriter( iorFileName ) );
                out.println( _factory.getIOR() );
                out.flush();
                out.close();
            }
            catch ( Exception e )
            {
                _logger.error( "Could not write the IOR to file:" + iorFileName, e );
            }
        }

        if ( nameId != null )
        {
            NamingContext namingContext =
                NamingContextHelper.narrow( _orb.resolve_initial_references( "NameService" ) );

            NameComponent[] name = new NameComponent[] {
                                       new NameComponent( nameId, nameKind )
                                   };

            _logger.info( "namingContext.rebind("
                          + nameId
                          + ( ( nameKind != null && nameKind.length() > 0 ) ? ( "." + nameKind ) : "" )
                          + " => "
                          + _factory.getCorbaLoc()
                          + ")" );

            namingContext.rebind( name, _factory.getEventChannelFactory() );
        }


        if ( doPrintCorbaloc )
        {
            System.out.println( _factory.getCorbaLoc() );
        }

        return _factory;
    }

    public static void main( String[] args ) throws Exception
    {
        newFactory( args );
    }
}

