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

package org.jacorb.notification.container;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.Logger;
import org.jacorb.notification.MessageFactory;
import org.jacorb.notification.conf.Attributes;
import org.jacorb.notification.conf.Default;
import org.jacorb.notification.engine.DefaultTaskProcessor;
import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.filter.ETCLEvaluator;
import org.jacorb.notification.filter.impl.DefaultETCLEvaluator;
import org.jacorb.notification.impl.DefaultMessageFactory;
import org.jacorb.notification.impl.PoolingEvaluationContextFactory;
import org.jacorb.notification.interfaces.EvaluationContextFactory;
import org.jacorb.orb.ORB;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.CachingComponentAdapter;
import org.picocontainer.defaults.ComponentAdapterFactory;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapterFactory;
import org.picocontainer.defaults.DefaultPicoContainer;

/**
 * @author Alphonse Bendt
 * @version $Id: PicoContainerFactory.java,v 1.1 2005-02-13 23:58:53 alphonse.bendt Exp $
 */
public class PicoContainerFactory
{
    public static MutablePicoContainer createRootContainer(ORB orb)
    {
        return createRootContainer(null, orb);
    }

    public static MutablePicoContainer createRootContainer(PicoContainer parent, ORB orb)
    {
        final org.jacorb.config.Configuration config = orb.getConfiguration();
        final Logger _logger = config.getNamedLogger(PicoContainerFactory.class.getName());

        final MutablePicoContainer _container;

        if (parent == null)
        {
            ComponentAdapterFactory _componentAdapterFactory = new ConstructorInjectionComponentAdapterFactory();

            _container = new DefaultPicoContainer(_componentAdapterFactory);

            _container.registerComponentInstance(ComponentAdapterFactory.class,
                    _componentAdapterFactory);

            _logger.debug("Created Top Level Container");
        }
        else
        {
            ComponentAdapterFactory _componentAdapterFactory = (ComponentAdapterFactory) parent
                    .getComponentInstance(ComponentAdapterFactory.class);

            _container = new DefaultPicoContainer(_componentAdapterFactory, parent);

            _logger.debug("Created Container with Parent");
        }

        // register existing orb
        _container.registerComponentInstance(org.omg.CORBA.ORB.class, orb);

        // register services that need to be looked up via orb using custom componentadapter
        _container.registerComponent(new CachingComponentAdapter(new POAComponentAdapter()));

        _container.registerComponent(new CachingComponentAdapter(
                new DynAnyFactoryComponentAdapter()));

        _container.registerComponent(new CachingComponentAdapter(
                new ConfigurationComponentAdapter()));

        _container.registerComponent(new CachingComponentAdapter(
                new FilterFactoryComponentAdapter()));

        // register core services

        // etcl evaluator

        ComponentAdapter _evaluatorAdapter = newComponentAdapter(_container, ETCLEvaluator.class,
                DefaultETCLEvaluator.class);

        _container.registerComponent(_evaluatorAdapter);

        // message factory

        ComponentAdapter _messageFactoryAdapter = newComponentAdapter(_container,
                MessageFactory.class, DefaultMessageFactory.class);

        _container.registerComponent(_messageFactoryAdapter);

        // taskprocessor
        
        ComponentAdapter _taskProcessorAdapter = newComponentAdapter(_container,
                TaskProcessor.class, DefaultTaskProcessor.class);

        _container.registerComponent(_taskProcessorAdapter);

        // evaluation context factory

        ComponentAdapter _evaluationContextAdapter = newComponentAdapter(_container,
                EvaluationContextFactory.class, PoolingEvaluationContextFactory.class);

        _container.registerComponent(_evaluationContextAdapter);

        return _container;
    }

    public static MutablePicoContainer createChildContainer(MutablePicoContainer parent)
    {
        MutablePicoContainer child = parent.makeChildContainer();

        return child;
    }

    /**
     * create a ComponentAdapter using the ComponentAdapterFactory registered in the provided
     * container. the resulting ComponentAdapter is wrapped in a CachingComponentAdapter.
     */
    private static ComponentAdapter newComponentAdapter(PicoContainer container, Class service,
            Class implementation)
    {
        ComponentAdapterFactory _adapterFactory = (ComponentAdapterFactory) container
                .getComponentInstance(ComponentAdapterFactory.class);

        return new CachingComponentAdapter(_adapterFactory.createComponentAdapter(service,
                implementation, null));
    }

    public static ComponentAdapter newDeliverTaskExecutorComponentAdapter(PicoContainer container)
    {
        final Configuration config = (Configuration) container.getComponentInstance(Configuration.class);
    
        final String _threadPolicy = config.getAttribute(Attributes.THREADPOLICY,
                Default.DEFAULT_THREADPOLICY);
    
        if ("ThreadPool".equalsIgnoreCase(_threadPolicy))
        {
            return new CachingComponentAdapter(new ThreadPoolTaskExecutorComponentAdapter());
        }
        else if ("ThreadPerProxy".equalsIgnoreCase(_threadPolicy))
        {
            ComponentAdapter adapter = new PerProxyPushTaskExecutorComponentAdapter();
            
            return new NonCachingRememberingComponentAdapter(adapter);
        }
        else
        {
            throw new IllegalArgumentException("The specified value: \"" + _threadPolicy
                    + "\" specified in property: \"" + Attributes.THREADPOLICY + "\" is invalid");
        }
    }
}