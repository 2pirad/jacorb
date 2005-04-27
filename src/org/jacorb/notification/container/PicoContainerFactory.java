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

import java.lang.reflect.Constructor;

import org.apache.avalon.framework.logger.Logger;
import org.jacorb.notification.MessageFactory;
import org.jacorb.notification.engine.ConfigurablePushTaskExecutorFactory;
import org.jacorb.notification.engine.DefaultTaskProcessor;
import org.jacorb.notification.engine.PushTaskExecutorFactory;
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
import org.picocontainer.defaults.ConstructorInjectionComponentAdapter;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapterFactory;
import org.picocontainer.defaults.DefaultPicoContainer;

/**
 * @author Alphonse Bendt
 * @version $Id: PicoContainerFactory.java,v 1.3 2005-04-27 10:37:14 alphonse.bendt Exp $
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

        _container.registerComponent(new CachingComponentAdapter(new RepositoryComponentAdapter()));

        _container.registerComponent(new CachingComponentAdapter(
                new PushTaskExecutorFactoryComponentAdapter(
                        new ConstructorInjectionComponentAdapter(PushTaskExecutorFactory.class,
                                ConfigurablePushTaskExecutorFactory.class))));

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

    /**
     * helper method for easier debugging of unresolved dependencies.
     * 
     * DO NOT DELETE even if method is not referenced.
     */
    public static void dumpDependencies(PicoContainer container, Class clazzToBeCreated)
    {
        try
        {
            Constructor[] ctors = clazzToBeCreated.getConstructors();

            StringBuffer b = new StringBuffer();
            for (int i = 0; i < ctors.length; i++)
            {
                Constructor constructor = ctors[i];

                b.append(constructor);
                b.append("\n");
                Class[] params = constructor.getParameterTypes();

                for (int j = 0; j < params.length; j++)
                {
                    Class param = params[j];

                    boolean resolvable = container.getComponentInstanceOfType(param) != null;
                    b.append(j);
                    b.append(": ");
                    b.append(param);
                    b.append(" -> ");
                    b.append(resolvable);
                    b.append("\n");
                }
            }

            System.err.println(b.toString());
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}