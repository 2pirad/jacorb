package org.jacorb.orb.iiop;

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

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;

import org.omg.ETF.*;
import org.omg.CSIIOP.*;
import org.omg.SSLIOP.*;

import org.apache.avalon.framework.configuration.*;

import org.jacorb.orb.factory.*;
import org.jacorb.orb.etf.ProtocolAddressBase;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPListener.java,v 1.28 2006-05-22 15:03:50 alphonse.bendt Exp $
 */
public class IIOPListener
    extends org.jacorb.orb.etf.ListenerBase
{
    /** the maximum set of security options supported by the SSL mechanism */
    private static final int MAX_SSL_OPTIONS = Integrity.value |
                                               Confidentiality.value |
                                               DetectReplay.value |
                                               DetectMisordering.value |
                                               EstablishTrustInTarget.value |
                                               EstablishTrustInClient.value;

    /**  the minimum set of security options supported by the SSL mechanism
     *   which cannot be turned off, so they are always supported
     */
    private static final int MIN_SSL_OPTIONS = Integrity.value |
                                               DetectReplay.value |
                                               DetectMisordering.value;


    private SocketFactoryManager socketFactoryManager = null;
    private ServerSocketFactory    serverSocketFactory    = null;
    private SSLServerSocketFactory sslServerSocketFactory = null;

    private SSLAcceptor sslAcceptor = null;
    private LoopbackAcceptor loopbackAcceptor ;

    private boolean supportSSL = false;
    //    private boolean dnsEnabled = false;
    private int serverTimeout = 0;
    private IIOPAddress address = null;
    private IIOPAddress sslAddress = null;
    private int target_supports = 0;
    private int target_requires = 0;
    private boolean generateSSLComponents = true;

    public IIOPListener()
    {
    }

    public void configure(Configuration configuration)
        throws ConfigurationException
    {
        this.configuration = (org.jacorb.config.Configuration)configuration;

        if (orb == null)
        {
            // c.f. with the constructor taking an ORB param.
            this.orb = this.configuration.getORB();
            socketFactoryManager = new SocketFactoryManager(this.orb);
        }

        logger = this.configuration.getNamedLogger("jacorb.iiop.listener");

        socketFactoryManager.configure(configuration);

        String address_str = configuration.getAttribute("OAAddress",null);
        if (address_str != null) {
            ProtocolAddressBase ep = orb.createAddress(address_str);
            if (ep instanceof IIOPAddress)
                address = (IIOPAddress)ep;
        }
        else {
            int oaPort = configuration.getAttributeAsInteger("OAPort",0);
            String oaHost = configuration.getAttribute("OAIAddr","");
            address = new IIOPAddress(oaHost,oaPort);
        }

        if (address != null)
            address.configure (configuration);

        address_str = configuration.getAttribute("OASSLAddress",null);
        if (address_str != null) {
            ProtocolAddressBase ep = orb.createAddress(address_str);
            if (ep instanceof IIOPAddress)
                sslAddress = (IIOPAddress)ep;
        }
        else {
            int sslPort = configuration.getAttributeAsInteger("OASSLPort",0);
            String sslHost = configuration.getAttribute("OAIAddr","");
            sslAddress = new IIOPAddress(sslHost,sslPort);
        }

        if (sslAddress != null)
            sslAddress.configure (configuration);

        serverTimeout =
            configuration.getAttributeAsInteger("jacorb.connection.server.timeout",0);

        supportSSL =
            configuration.getAttribute("jacorb.security.support_ssl","off").equals("on");

        target_supports =
            Integer.parseInt(
                configuration.getAttribute("jacorb.security.ssl.server.supported_options","20"),
                16); // 16 is the base as we take the string value as hex!

        // make sure that the minimum options are always in the set of supported options
        target_supports |= MIN_SSL_OPTIONS;

        target_requires =
            Integer.parseInt(
                configuration.getAttribute("jacorb.security.ssl.server.required_options","0"),
                16);


        generateSSLComponents =
            configuration.getAttribute("jacorb.security.ssl_components_added_by_ior_interceptor","off").equals("off");


        if (!isSSLRequired() ||
            configuration.getAttributeAsBoolean("jacorb.security.ssl.always_open_unsecured_address", false))
        {
            acceptor = new Acceptor("ServerSocketListener");
            ((Acceptor)acceptor).init();
        }

        if (supportSSL)
        {
            sslAcceptor = new SSLAcceptor();
            sslAcceptor.init();
        }

        loopbackAcceptor = new LoopbackAcceptor() ;

        profile = createAddressProfile();

    }


    /**
     * It is possible that connection requests arrive <i>after</i> the
     * initial creation of the Listener instance but <i>before</i> the
     * conclusion of the configuration of the specific address in this
     * plugin. In order to provide a clear end of this configuration state,
     * we added the listen() method. It is called by the ORB when it ready
     * for incoming connection and thus signals the Listener instance to
     * start processing the incoming connection requests. Therefore,
     * a Listener instance shall not deliver incoming connections to the
     * ORB before this method was called.
     */
    public void listen()
    {
        super.listen();

        if (sslAcceptor != null)
            sslAcceptor.start();

        loopbackAcceptor.start() ;
    }

    /**
     * The Listener is instructed to close its address. It shall no
     * longer accept any connection requests and shall close all
     * connections opened by it.
     */
    public void destroy()
    {
        loopbackAcceptor.terminate() ;

        if (sslAcceptor != null)
            sslAcceptor.terminate();

        super.destroy();
    }

    // internal methods below this line

    /**
     * Returns true if this Listener should support SSL connections.
     */
    private boolean isSSLSupported()
    {
        return  supportSSL;
    }

    /**
     * Returns true if this Listener should <i>require</i> SSL, and not
     * offer plain connections.
     */
    private boolean isSSLRequired()
        throws ConfigurationException
    {
        if (isSSLSupported())
        {
            // the following is used as a bit mask to check if any SSL
            // options are required
            return ((target_requires & MAX_SSL_OPTIONS ) != 0);
        }
        return false;
    }

    /**
     * Returns the server timeout that has been specified, or zero if none
     * has been set.
     */
    private int getServerTimeout()
    {
        return serverTimeout;
    }

    private ServerSocketFactory getServerSocketFactory()
    {
        if (serverSocketFactory == null)
        {
            serverSocketFactory =
                socketFactoryManager.getServerSocketFactory();
        }
        return serverSocketFactory;
    }

    /**
     * Returns the SSLServerSocketFactory that has been configured.
     * If no such factory is available, org.omg.CORBA.INITIALIZE() is thrown.
     */
    private SSLServerSocketFactory getSSLServerSocketFactory()
    {
        if (sslServerSocketFactory == null)
        {
            sslServerSocketFactory =
                orb.getBasicAdapter().getSSLSocketFactory();

            if (sslServerSocketFactory == null)
                throw new org.omg.CORBA.INITIALIZE("No SSL server socket factory found");
        }
        return sslServerSocketFactory;
    }

    /**
     * Creates a new IIOPProfile that describes this transport address.
     */
    private IIOPProfile createAddressProfile()
        throws ConfigurationException
    {
        if (acceptor != null)
        {

            if (address.getPort() == 0)
            {
                address.setPort(((Acceptor)acceptor).getLocalAddress().getPort());
            }
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug ("Using port " + address.getPort());
                }
            }
        }
        else if (sslAcceptor == null)
        {
            throw new org.omg.CORBA.INITIALIZE
                ("no acceptors found, cannot create address profile");
        }

        IIOPProfile result = new IIOPProfile(address,null);
        if (sslAcceptor != null && generateSSLComponents)
        {
             result.addComponent (TAG_SSL_SEC_TRANS.value,
                                  createSSL(), SSLHelper.class);
        }

        result.configure(configuration);
        return result;
    }

    private SSL createSSL()
    {
        return new SSL
        (
            (short)target_supports,
            (short)target_requires,
            (short)sslAcceptor.getLocalAddress().getPort()
        );
    }

    /**
     * Creates an ETF connection around a live socket and passes it
     * up to the ORB, using either the upcall mechanism or the
     * polling mechanism.
     */
    private void deliverConnection (Socket socket, boolean isSSL)
    {
        Connection result = null;
        try
        {
            result = createServerConnection(socket, isSSL);
        }
        catch (IOException ex)
        {
            if (logger.isErrorEnabled())
            {
                logger.error("Could not create connection from socket: " + ex);
            }
            return;
        }

        deliverConnection(result);
    }

    /**
     * Template method to create a server-side ETF Connection.
     * This can be overridden by subclasses to pass a different
     * kind of Connection up to the ORB.
     */
    protected Connection createServerConnection (Socket socket,
                                                 boolean is_ssl)
        throws IOException
    {
        ServerIIOPConnection result = new ServerIIOPConnection(socket, is_ssl);
        try
        {
            result.configure(configuration);
        }
        catch( ConfigurationException ce )
        {
            throw new org.omg.CORBA.INTERNAL("ConfigurationException: " + ce.toString());
        }
        return result;
    }

    // Acceptor classes below this line

    protected class Acceptor
        extends org.jacorb.orb.etf.ListenerBase.Acceptor
    {
        protected ServerSocket serverSocket;

        protected boolean terminated = false;

        protected Acceptor(String name)
        {
            super();
            // initialization deferred to init() method due to JDK bug
            setDaemon(true);
            setName(name);
        }

        public void init()
        {
            serverSocket = createServerSocket();

            if( logger.isDebugEnabled() )
            {
                logger.debug( "Created socket listener on " +
                              serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() );
            }
       }

        public void run()
        {
            while (!terminated)
            {
                try
                {
                    Socket socket = serverSocket.accept();
                    setup (socket);
                    deliverConnection (socket);
                }
                catch (Exception e)
                {
                    if (!terminated)
                    {
                        if (logger.isWarnEnabled())
                        {
                            logger.warn(e.toString());
                        }
                    }
                }
            }
            if (logger.isInfoEnabled())
            {
                logger.info( "Listener exited");
            }
        }

        /**
         * Terminates this Acceptor by closing the ServerSocket.
         */
        public void terminate()
        {
            terminated = true;
            try
            {
                serverSocket.close();
            }
            catch (java.io.IOException e)
            {
                if (logger.isWarnEnabled())
                {
                    logger.warn(e.toString());
                }
            }
        }

        public IIOPAddress getLocalAddress()
        {
            IIOPAddress ep = new IIOPAddress
            (
                serverSocket.getInetAddress().toString(),
                serverSocket.getLocalPort()
            );
            if (configuration != null)
                try {
                    ep.configure(configuration);
                }
                catch( ConfigurationException ce) {
                    if (logger.isWarnEnabled())
                        logger.warn("ConfigurationException", ce );
                }
            return ep;
        }

        /**
         * Template method that creates the server socket.
         */
        protected ServerSocket createServerSocket()
        {
            try
            {
                return getServerSocketFactory()
                           .createServerSocket (address.getPort(),
                                                20,
                                                address.getConfiguredHost());
            }
            catch (IOException ex)
            {
                logger.warn("could not create server socket", ex);
                throw new org.omg.CORBA.INITIALIZE ("Could not create server socket");
            }
        }

        /**
         * Template method that sets up the socket right after the
         * connection has been established.  Subclass implementations
         * must call super.setup() first.
         */
        protected void setup(Socket socket)
            throws IOException
        {
             socket.setSoTimeout(serverTimeout);
        }

        protected void deliverConnection(Socket socket)
        {
            IIOPListener.this.deliverConnection (socket, false);
        }
    }

    private class SSLAcceptor
        extends Acceptor
    {
        private SSLAcceptor()
        {
            super("SSLServerSocketListener");
        }

        protected ServerSocket createServerSocket()
        {
            try
            {
                return getSSLServerSocketFactory()
                            .createServerSocket (sslAddress.getPort(),
                                                 20,
                                                 sslAddress.getConfiguredHost());
            }
            catch (IOException e)
            {
                logger.warn("could not create SSL server socket", e);
                throw new org.omg.CORBA.INITIALIZE
                                           ("Could not create SSL server socket");
            }
        }

        protected void deliverConnection(Socket socket)
        {
            IIOPListener.this.deliverConnection (socket, true);
        }
    }

    private class LoopbackAcceptor implements IIOPLoopback
    {
        public void start()
        {
            IIOPLoopbackRegistry.getRegistry().register(getAddress(), this);
        }

        public void terminate()
        {
            IIOPLoopbackRegistry.getRegistry().unregister(getAddress());
        }

        public void initLoopback(final IIOPLoopbackInputStream lis,
                                 final IIOPLoopbackOutputStream los)
        {
            final IIOPLoopbackConnection connection =
                new IIOPLoopbackConnection(lis, los) ;
            deliverConnection(connection);
        }

        private IIOPAddress getAddress()
        {
            final IIOPProfile profile =
                (IIOPProfile)IIOPListener.this.profile;
            return (IIOPAddress)profile.getAddress();
        }
    }
}
