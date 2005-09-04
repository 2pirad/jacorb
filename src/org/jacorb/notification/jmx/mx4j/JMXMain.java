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

package org.jacorb.notification.jmx.mx4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.management.Attribute;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.notification.jmx.JMXManageableMBeanProvider;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;
import org.tanukisoftware.wrapper.jmx.WrapperManagerMBean;

/**
 * MX4J specific startup class for JMX-enabled Notification Service
 * 
 * @author Alphonse Bendt
 * @version $Id: JMXMain.java,v 1.2 2005-09-04 18:22:37 alphonse.bendt Exp $
 */
public class JMXMain implements WrapperListener
{
    public static final String DEFAULT_DOMAIN = "NotificationService";

    private ObjectName notificationServiceName_;

    private final List connectors_ = new ArrayList();

    ORB orb_;

    private MBeanServer mbeanServer_;

    private Logger logger_;

    private JMXMain()
    {
        super();
    }

    private void stopConnectors()
    {
        for (Iterator i = connectors_.iterator(); i.hasNext();)
        {
            ObjectName name = (ObjectName) i.next();
            try
            {
                mbeanServer_.invoke(name, "stop", null, null);
            } catch (Exception e)
            {
                logger_.warn("Unable to stop Connnector " + name, e);
            }
        }
    }

    private void startHTTPConnector() throws Exception
    {
        ObjectName name = new ObjectName("connectors:protocol=http");
        mbeanServer_.createMBean("mx4j.tools.adaptor.http.HttpAdaptor", name, null);
        mbeanServer_.setAttribute(name, new Attribute("Port", new Integer(8001)));
        mbeanServer_.setAttribute(name, new Attribute("Host", "localhost"));
        mbeanServer_.invoke(name, "start", null, null);

        ObjectName processorName = new ObjectName("Server:name=XSLTProcessor");
        mbeanServer_.createMBean("mx4j.tools.adaptor.http.XSLTProcessor", processorName, null);

        mbeanServer_.setAttribute(name, new Attribute("ProcessorName", processorName));
    }

    private void startIIOPConnector() throws Exception, IOException
    {
        JMXServiceURL _nameServiceURL = new JMXServiceURL(
                "service:jmx:iiop://localhost/jndi/COSNotification");

        HashMap _environment = new HashMap();
        _environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");

        org.omg.CORBA.Object _nameService = orb_.resolve_initial_references("NameService");
        String _nameServiceIOR = orb_.object_to_string(_nameService);
        _environment.put(Context.PROVIDER_URL, _nameServiceIOR);

        // this property is ignored by current 3.0.1 release of MX4J
        // need to use CVS version of MX4J otherwise JMXConnectorServer will use
        // the wrong orb.
        // (see
        // http://sourceforge.net/tracker/index.php?func=detail&aid=1164309&group_id=47745&atid=450647)
        _environment.put("java.naming.corba.orb", orb_);

        // Create the JMXCconnectorServer
        JMXConnectorServer _connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
                _nameServiceURL, _environment, mbeanServer_);

        // Register the JMXConnectorServer in the MBeanServer
        ObjectName _connectorServerName = ObjectName.getInstance("connectors:protocol=iiop");
        mbeanServer_.registerMBean(_connectorServer, _connectorServerName);

        _connectorServer.start();

        connectors_.add(_connectorServerName);
    }

    private void startRMIConnector() throws Exception
    {
        ObjectName _nameServiceName = ObjectName.getInstance("naming:type=rmiregistry");
        mbeanServer_.createMBean("mx4j.tools.naming.NamingService", _nameServiceName, null);
        WrapperManager.log(WrapperManager.WRAPPER_LOG_LEVEL_INFO, "Starting NamingService");

        mbeanServer_.invoke(_nameServiceName, "start", null, null);
        int namingPort = ((Integer) mbeanServer_.getAttribute(_nameServiceName, "Port")).intValue();

        String jndiPath = "/jndi/COSNotification";

        JMXServiceURL address = new JMXServiceURL(
                "service:jmx:rmi://localhost/jndi/rmi://localhost:" + namingPort + jndiPath);

        JMXConnectorServer _connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(
                address, null, mbeanServer_);

        ObjectName _connectorServerName = ObjectName.getInstance("connectors:protocol=rmi");
        mbeanServer_.registerMBean(_connectorServer, _connectorServerName);

        _connectorServer.start();

        connectors_.add(_connectorServerName);
    }

    public Integer start(String[] args)
    {
        try
        {
            initORB(args);

            notificationServiceName_ = ObjectName.getInstance(DEFAULT_DOMAIN
                    + ":type=EventChannelFactory");

            mbeanServer_ = MBeanServerFactory.createMBeanServer();

            registerNotificationService(args);

            registerWrapperManager();

            startIIOPConnector();

            startHTTPConnector();

            startRMIConnector();

            return null;
        } catch (Exception e)
        {
            if (logger_ != null)
            {
                logger_.error("Unable to Start Service", e);
            }

            WrapperManager.log(WrapperManager.WRAPPER_LOG_LEVEL_FATAL, "Unable to Start Service"
                    + e);

            stopConnectors();

            orb_.shutdown(true);

            throw new RuntimeException(e);
        }
    }

    private void registerNotificationService(String[] args) throws NotCompliantMBeanException,
            InstanceAlreadyExistsException, InstanceNotFoundException, MBeanException,
            ReflectionException
    {
        MX4JCOSNotificationServiceMBean _notificationService = new MX4JCOSNotificationService(orb_,
                mbeanServer_, new JMXManageableMBeanProvider(DEFAULT_DOMAIN), args);

        StandardMBean _mbean = new StandardMBean(_notificationService,
                MX4JCOSNotificationServiceMBean.class);

        mbeanServer_.registerMBean(_mbean, notificationServiceName_);

        mbeanServer_.invoke(notificationServiceName_, "start", null, null);
    }

    private void initORB(String[] args) throws InvalidName, AdapterInactive
    {
        orb_ = ORB.init(args, null);
        logger_ = ((org.jacorb.orb.ORB) orb_).getConfiguration().getNamedLogger(
                getClass().getName());

        Thread _orbRunner = new Thread("ORB-Thread")
        {
            public void run()
            {
                orb_.run();
            }
        };

        POA _poa = POAHelper.narrow(orb_.resolve_initial_references("RootPOA"));
        _poa.the_POAManager().activate();

        _orbRunner.start();
    }

    public int stop(int code)
    {
        try
        {
            mbeanServer_.invoke(notificationServiceName_, "stop", null, null);

            mbeanServer_.unregisterMBean(notificationServiceName_);

            stopConnectors();

            orb_.shutdown(true);
        } catch (Exception e)
        {
            logger_.error("Error while stopping Service", e);

            WrapperManager
                    .log(WrapperManager.WRAPPER_LOG_LEVEL_ERROR, "Unable to Stop Service" + e);

            return 1;
        }

        return 0;
    }

    public void controlEvent(int event)
    {
        if (WrapperManager.isControlledByNativeWrapper())
        {
            // The Wrapper will take care of this event
            return;
        }

        // We are not being controlled by the Wrapper, so
        // handle the event ourselves.

        if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT)
                || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT)
                || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT))
        {
            org.tanukisoftware.wrapper.WrapperManager.stop(0);
        }
    }

    private void registerWrapperManager() throws JMException
    {
        if (!WrapperManager.isControlledByNativeWrapper())
        {
            return;
        }

        WrapperManager.log(WrapperManager.WRAPPER_LOG_LEVEL_INFO,
                "Registering WrapperManager MBean");

        ObjectName wrapperManagerName = ObjectName.getInstance(DEFAULT_DOMAIN
                + ":service=WrapperManager");

        StandardMBean wrapperManagerBean = new StandardMBean(
                new org.tanukisoftware.wrapper.jmx.WrapperManager(), WrapperManagerMBean.class);

        mbeanServer_.registerMBean(wrapperManagerBean, wrapperManagerName);
    }

    public static void main(String[] args) throws Exception
    {
        JMXMain main = new JMXMain();

        WrapperManager.start(main, args);
    }
}