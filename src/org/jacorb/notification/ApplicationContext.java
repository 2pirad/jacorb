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

import org.jacorb.notification.engine.TaskProcessor;
import org.jacorb.notification.evaluate.DynamicEvaluator;
import org.jacorb.notification.evaluate.ResultExtractor;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.interfaces.AbstractPoolable;
import org.jacorb.notification.node.EvaluationResult;
import org.jacorb.notification.queue.EventQueue;
import org.jacorb.notification.queue.EventQueueFactory;
import org.jacorb.notification.util.AbstractObjectPool;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNotification.MaximumBatchSize;
import org.omg.CosNotification.PacingInterval;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynAnyFactoryHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.TimeBase.TimeTHelper;

import org.apache.log.Hierarchy;
import org.apache.log.Logger;

/**
 * ApplicationContext.java
 *
 *
 * Created: Sat Nov 30 16:02:04 2002
 *
 * @author Alphonse Bendt
 * @version $Id: ApplicationContext.java,v 1.9 2003-08-25 21:00:47 alphonse.bendt Exp $
 */

public class ApplicationContext implements Disposable {

    private ORB orb_;
    private POA poa_;
    private TaskProcessor taskProcessor_;
    private AbstractObjectPool evaluationResultPool_;
    private AbstractObjectPool evaluationContextPool_;
    private MessageFactory notificationEventFactory_;
    private DynAnyFactory dynAnyFactory_;
    private ResultExtractor resultExtractor_;
    private DynamicEvaluator dynamicEvaluator_;
    private PropertyManager defaultAdminProperties_;
    private PropertyManager defaultQoSProperties_;
    Logger logger_ = Hierarchy.getDefaultHierarchy().getLoggerFor(getClass().getName());

    private void setup(ORB orb, POA poa, boolean init) throws InvalidName {
        orb_ = orb;
        poa_ = poa;

        dynAnyFactory_ =
            DynAnyFactoryHelper.narrow(orb_.resolve_initial_references("DynAnyFactory"));

        resultExtractor_ = new ResultExtractor(dynAnyFactory_);
        dynamicEvaluator_ = new DynamicEvaluator(orb_, dynAnyFactory_);

        evaluationContextPool_ = new AbstractObjectPool() {
                public Object newInstance() {
                    EvaluationContext _e = new EvaluationContext();
                    _e.setDynamicEvaluator(dynamicEvaluator_);
                    _e.setResultExtractor(resultExtractor_);
                    _e.setDynAnyFactory(dynAnyFactory_);

                    return _e;
                }
                public void activateObject(Object o) {
                    ((AbstractPoolable) o).setObjectPool(this);
                }
                public void passivateObject(Object o) {
                    ((AbstractPoolable) o).reset();
                }
            };
        evaluationContextPool_.init();

        evaluationResultPool_ = new AbstractObjectPool() {
                public Object newInstance() {
                    return new EvaluationResult();
                }
                public void activateObject(Object o) {
                    ((AbstractPoolable) o).setObjectPool(this);
                }
                public void passivateObject(Object o) {
                    ((AbstractPoolable) o).reset();
                }
            };
        evaluationResultPool_.init();

        notificationEventFactory_ = new MessageFactory();
        notificationEventFactory_.init();

        defaultQoSProperties_ = new PropertyManager(this);
        defaultAdminProperties_ = new PropertyManager(this);

        Any _maxBatchSize = orb_.create_any();
        _maxBatchSize.insert_long(1);
        defaultQoSProperties_.setProperty(MaximumBatchSize.value, _maxBatchSize);

        Any _pacingInterval = orb_.create_any();
        TimeTHelper.insert(_pacingInterval, 0);
        defaultQoSProperties_.setProperty(PacingInterval.value, _pacingInterval);

        if (init) {
            init();
        }
    }

    public ApplicationContext(boolean init) throws InvalidName {
        ORB orb = ORB.init(new String[0], null);
        POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

        setup(orb, poa, init);
    }

    public ApplicationContext(ORB orb, POA poa) throws InvalidName {
        setup(orb, poa, false);
    }

    public ApplicationContext(ORB orb, POA poa, boolean init) throws InvalidName {
        setup(orb, poa, init);
    }

    public void init() {
        taskProcessor_ = new TaskProcessor();
    }

    public void dispose() {
        if (taskProcessor_ != null) {
            taskProcessor_.dispose();
            taskProcessor_ = null;
        }


        evaluationResultPool_.dispose();
        evaluationContextPool_.dispose();
        notificationEventFactory_.dispose();

        orb_.shutdown(true);
    }

    /**
     * Get the Orb value.
     * @return the Orb value.
     */
    public ORB getOrb() {
        return orb_;
    }

    /**
     * Set the Orb value.
     * @param newOrb The new Orb value.
     */
    public void setOrb(ORB newOrb) {
        orb_ = newOrb;
    }

    /**
     * Get the Poa value.
     * @return the Poa value.
     */
    public POA getPoa() {
        return poa_;
    }

    /**
     * Set the Poa value.
     * @param newPoa The new Poa value.
     */
    public void setPoa(POA newPoa) {
        poa_ = newPoa;
    }

    public EvaluationResult newEvaluationResult() {
        return (EvaluationResult) evaluationResultPool_.lendObject();
    }

    public EvaluationContext newEvaluationContext() {
        return (EvaluationContext) evaluationContextPool_.lendObject();
    }

    public MessageFactory getMessageFactory() {
        return notificationEventFactory_;
    }

    public DynAnyFactory getDynAnyFactory() {
        return dynAnyFactory_;
    }

    public PropertyManager getDefaultAdminProperties() {
        return defaultAdminProperties_;
    }

    public PropertyManager getDefaultQoSProperties() {
        return defaultQoSProperties_;
    }

    public DynamicEvaluator getDynamicEvaluator() {
        return dynamicEvaluator_;
    }

    public ResultExtractor getResultExtractor() {
        return resultExtractor_;
    }

    public TaskProcessor getTaskProcessor() {
        return taskProcessor_;
    }

    public EventQueue newEventQueue(PropertyManager qosProperties) throws UnsupportedQoS {
        return EventQueueFactory.newEventQueue(qosProperties);
    }

}// ApplicationContext
