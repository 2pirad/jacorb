package org.jacorb.notification;

import org.omg.CORBA.Any;
import org.omg.CosNotification.StructuredEvent;
import org.omg.CosNotification.Property;
import org.jacorb.notification.node.ComponentOperator;
import org.jacorb.notification.evaluate.DynamicEvaluator;
import org.jacorb.notification.evaluate.ResultExtractor;
import org.jacorb.notification.node.TCLNode;
import org.jacorb.notification.node.EvaluationResult;
import org.jacorb.notification.node.IdentValue;
import org.jacorb.notification.node.DotOperator;
import org.jacorb.notification.node.UnionPositionOperator;
import org.jacorb.notification.node.ComponentPositionOperator;
import org.jacorb.notification.node.ImplicitOperator;
import org.jacorb.notification.node.ImplicitOperatorNode;
import org.jacorb.notification.engine.Destination;
import org.jacorb.notification.evaluate.EvaluationException;
import org.omg.CORBA.TypeCodePackage.BadKind;
import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.jacorb.notification.node.ArrayOperator;
import org.jacorb.notification.node.AssocOperator;
import org.omg.CosNotification.EventType;
import org.omg.CosNotification.FixedEventHeader;
import org.omg.CosNotification.EventHeader;
import org.omg.CORBA.ORB;
import org.omg.CosNotification.StructuredEventHelper;
import org.apache.log4j.Logger;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;

/*
 *        JacORB - a free Java ORB
 */

/**
 * NotificationEvent.java
 *
 *
 * Created: Tue Oct 22 20:16:33 2002
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: NotificationEvent.java,v 1.1 2002-12-03 23:23:02 alphonse.bendt Exp $
 */

public abstract class NotificationEvent {

    public static final int TYPE_ANY = 0;
    public static final int TYPE_STRUCTURED = 1;
    public static final int TYPE_TYPED = 2;

    DynamicEvaluator dynamicEvaluator_;
    ResultExtractor resultExtractor_;
    Logger logger_;
    ORB orb_;

    public Destination[] hops_ = new Destination[2];

    EventTypeIdentifier getEventTypeIdentifier() {
        return null;
    }

    public abstract EvaluationResult evaluate(ComponentOperator c) throws EvaluationException;
    public abstract EvaluationResult hasDefault(ComponentOperator c) throws EvaluationException;
    public abstract EvaluationResult testExists(ComponentOperator c) throws EvaluationException;
    public abstract String getConstraintKey();
    public abstract Any toAny();
    public abstract StructuredEvent toStructuredEvent();
    public abstract int getType();

    static boolean DEBUG = false;

    void debug(String msg) {
	if (DEBUG) {
	    System.err.println("[NotificationEvent] " +msg);
	}
    }

    protected NotificationEvent(ORB orb, ResultExtractor resultExtractor, DynamicEvaluator dynamicEvaluator, Logger logger) {
	resultExtractor_ = resultExtractor;
	orb_ = orb;
	dynamicEvaluator_ = dynamicEvaluator;
	logger_ = logger;
    }

}// NotificationEvent

