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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CosNotification.BestEffort;
import org.omg.CosNotification.ConnectionReliability;
import org.omg.CosNotification.DiscardPolicy;
import org.omg.CosNotification.EventReliability;
import org.omg.CosNotification.MaxConsumers;
import org.omg.CosNotification.MaxEventsPerConsumer;
import org.omg.CosNotification.MaxQueueLength;
import org.omg.CosNotification.MaxSuppliers;
import org.omg.CosNotification.MaximumBatchSize;
import org.omg.CosNotification.OrderPolicy;
import org.omg.CosNotification.PacingInterval;
import org.omg.CosNotification.Persistent;
import org.omg.CosNotification.Priority;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.PropertyError;
import org.omg.CosNotification.PropertyRange;
import org.omg.CosNotification.QoSError_code;
import org.omg.CosNotification.RejectNewEvents;
import org.omg.CosNotification.StartTime;
import org.omg.CosNotification.StartTimeSupported;
import org.omg.CosNotification.StopTime;
import org.omg.CosNotification.StopTimeSupported;
import org.omg.CosNotification.Timeout;
import org.omg.CosNotification.UnsupportedAdmin;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotification.AnyOrder;
import org.omg.CosNotification.DeadlineOrder;
import org.omg.CosNotification.FifoOrder;
import org.omg.CosNotification.PriorityOrder;
import org.omg.CosNotification.LifoOrder;

/**
 * PropertyValidator.java
 *
 *
 * Created: Tue Jan 28 17:51:30 2003
 *
 * @author Alphonse Bendt
 * @version $Id: PropertyValidator.java,v 1.3 2003-08-25 21:00:46 alphonse.bendt Exp $
 */

public class PropertyValidator {

    static final PropertyError[] PROPERTY_ERROR_ARRAY_TEMPLATE =
        new PropertyError[0];

    static HashSet sQoSPropertyNames_;
    static HashSet sAdminPropertyNames_;

    static Any connectionReliabilityLowValue_;
    static Any connectionReliabilityHighValue_;

    static Any orderPolicyLowValue_;
    static Any orderPolicyHighValue_;

    static Any discardPolicyLowValue_;
    static Any discardPolicyHighValue_;

    static ORB orb_ = ORB.init();

    public static void checkQoSPropertySeq(Property[] p) throws UnsupportedQoS {
        Vector _errList = new Vector();

        for (int x=0; x<p.length; ++x) {

            if (!isQoSProperty(p[x].name)) {
                _errList.add(new PropertyError(QoSError_code.BAD_PROPERTY,
                                               p[x].name,
                                               new PropertyRange(orb_.create_any(),
                                                                 orb_.create_any())));

            } else if (ConnectionReliability.value.equals(p[x].name)) {

                switch(p[x].value.extract_short()) {
                case BestEffort.value:
                    // fallthrough
                case Persistent.value:
                    break;
                default:
                    _errList.add(new PropertyError(QoSError_code.BAD_VALUE,
                                                   p[x].name,
                                                   new PropertyRange(connectionReliabilityLowValue_,
                                                                     connectionReliabilityHighValue_)));
                }
            } else if (EventReliability.value.equals(p[x].name)) {
                switch(p[x].value.extract_short()) {
                case BestEffort.value:
                    // fallthrough
                case Persistent.value:
                    break;
                default:
                    _errList.add(new PropertyError(QoSError_code.BAD_VALUE,
                                                   p[x].name,
                                                   new PropertyRange(connectionReliabilityLowValue_,
                                                                     connectionReliabilityHighValue_)));
                }
            } else if (OrderPolicy.value.equals(p[x].name)) {
                switch(p[x].value.extract_short()) {
                case AnyOrder.value:
                    break;
                case FifoOrder.value:
                    break;
                case PriorityOrder.value:
                    break;
                case DeadlineOrder.value:
                    break;
                default:
                    _errList.add(new PropertyError(QoSError_code.BAD_VALUE,
                                                   p[x].name,
                                                   new PropertyRange(orderPolicyLowValue_,
                                                                     orderPolicyHighValue_)));
                }
            } else if (DiscardPolicy.value.equals(p[x].name)) {
                switch(p[x].value.extract_short()) {
                case AnyOrder.value:
                    break;
                case FifoOrder.value:
                    break;
                case LifoOrder.value:
                    break;
                case PriorityOrder.value:
                    break;
                case DeadlineOrder.value:
                    break;
                default:
                    _errList.add(new PropertyError(QoSError_code.BAD_VALUE,
                                                   p[x].name,
                                                   new PropertyRange(discardPolicyLowValue_,
                                                                     discardPolicyHighValue_)));
                }
            }
        }

        if (!_errList.isEmpty()) {
            PropertyError[] _ex = (PropertyError[])_errList.toArray(PROPERTY_ERROR_ARRAY_TEMPLATE);
            throw new UnsupportedQoS(_ex);
        }
    }

    public static void checkAdminPropertySeq(Property[] p) throws UnsupportedAdmin {
        Vector _errList = new Vector();

        for (int x=0; x<p.length; ++x) {
            if (!isAdminProperty(p[x].name)) {
                _errList.add(new PropertyError(QoSError_code.BAD_PROPERTY,
                                               p[x].name,
                                               new PropertyRange(orb_.create_any(), orb_.create_any())));
            }
        }

        if (_errList.size() > 0) {
            PropertyError[] _ex = (PropertyError[])_errList.toArray(PROPERTY_ERROR_ARRAY_TEMPLATE);
            throw new UnsupportedAdmin(_ex);
        }
    }

    public static Map getUniqueProperties(Property[] p) {
        Map _ret = new Hashtable();

        for (int x=0; x<p.length; ++x) {
            _ret.put(p[x].name, p[x].value);
        }

        return _ret;
    }

    public static Map validateAdminPropertySeq(Property[] p) throws UnsupportedAdmin {
        Hashtable _ret = null;

        return _ret;
    }

    static boolean isAdminProperty(String name) {
        return sAdminPropertyNames_.contains(name);
    }

    static boolean isQoSProperty(String name) {
        return sQoSPropertyNames_.contains(name);
    }

    static {
        connectionReliabilityHighValue_ = orb_.create_any();
        connectionReliabilityHighValue_.insert_short(Persistent.value);

        connectionReliabilityLowValue_ = orb_.create_any();
        connectionReliabilityLowValue_.insert_short(BestEffort.value);

        orderPolicyLowValue_ = orb_.create_any();
        orderPolicyLowValue_.insert_short(AnyOrder.value);

        orderPolicyHighValue_ = orb_.create_any();
        orderPolicyHighValue_.insert_short(DeadlineOrder.value);

        discardPolicyLowValue_ = orb_.create_any();
        discardPolicyLowValue_.insert_short(AnyOrder.value);

        discardPolicyHighValue_ = orb_.create_any();
        discardPolicyHighValue_.insert_short(DeadlineOrder.value);


        sQoSPropertyNames_ = new HashSet();

        sQoSPropertyNames_.add(ConnectionReliability.value);
        sQoSPropertyNames_.add(EventReliability.value);
        sQoSPropertyNames_.add(Priority.value);
        sQoSPropertyNames_.add(OrderPolicy.value);
        sQoSPropertyNames_.add(Timeout.value);
        sQoSPropertyNames_.add(DiscardPolicy.value);
        sQoSPropertyNames_.add(PacingInterval.value);
        sQoSPropertyNames_.add(StartTime.value);
        sQoSPropertyNames_.add(StartTimeSupported.value);
        sQoSPropertyNames_.add(StopTime.value);
        sQoSPropertyNames_.add(StopTimeSupported.value);
        sQoSPropertyNames_.add(MaximumBatchSize.value);
        sQoSPropertyNames_.add(MaxEventsPerConsumer.value);

        sAdminPropertyNames_ = new HashSet();

        sAdminPropertyNames_.add(MaxQueueLength.value);
        sAdminPropertyNames_.add(MaxConsumers.value);
        sAdminPropertyNames_.add(MaxSuppliers.value);
        sAdminPropertyNames_.add(RejectNewEvents.value);
    }
}
