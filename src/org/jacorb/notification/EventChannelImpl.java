package org.jacorb.notification;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
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

import java.net.*;
import java.util.*;
import java.util.Hashtable;
import org.jacorb.notification.engine.Engine;
import java.util.Map;
import org.omg.CORBA.ORB;
import org.omg.CORBA.UNKNOWN;
import org.omg.CosNotification.NamedPropertyRangeSeqHolder;
import org.omg.CosNotification.Property;
import org.omg.CosNotification.UnsupportedAdmin;
import org.omg.CosNotification.UnsupportedQoS;
import org.omg.CosNotifyChannelAdmin.ConsumerAdmin;
import org.omg.CosNotifyChannelAdmin.ConsumerAdminHelper;
import org.omg.CosNotifyChannelAdmin.ConsumerAdminPOATie;
import org.omg.CosNotifyChannelAdmin.EventChannel;
import org.omg.CosNotifyChannelAdmin.EventChannelFactory;
import org.omg.CosNotifyChannelAdmin.EventChannelFactoryHelper;
import org.omg.CosNotifyChannelAdmin.EventChannelPOA;
import org.omg.CosNotifyChannelAdmin.InterFilterGroupOperator;
import org.omg.CosNotifyChannelAdmin.ProxyPullConsumer;
import org.omg.CosNotifyChannelAdmin.ProxyPullSupplier;
import org.omg.CosNotifyChannelAdmin.ProxyPushConsumer;
import org.omg.CosNotifyChannelAdmin.ProxyPushSupplier;
import org.omg.CosNotifyChannelAdmin.SupplierAdmin;
import org.omg.CosNotifyChannelAdmin.SupplierAdminHelper;
import org.omg.CosNotifyChannelAdmin.SupplierAdminPOATie;
import org.omg.CosNotifyFilter.FilterFactory;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.apache.log4j.Logger;
import org.omg.CosNotification.StructuredEvent;
import org.omg.PortableServer.POA;
import org.omg.CosNotifyFilter.Filter;
import org.omg.CosNotifyFilter.UnsupportedFilterableData;
import org.omg.CORBA.IntHolder;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.Any;
import org.jacorb.notification.framework.Disposable;
import org.jacorb.notification.framework.EventDispatcher;

/**
 * @author Alphonse Bendt
 * @version $Id: EventChannelImpl.java,v 1.3 2003-01-14 11:46:07 alphonse.bendt Exp $
 */

public class EventChannelImpl extends EventChannelPOA implements Disposable {

    protected ApplicationContext    applicationContext_;
    protected ChannelContext        channelContext_;

    private EventChannel            thisEventChannel_;
    private FilterFactory           defaultFilterFactory_;

    private EventChannelFactoryImpl myFactoryServant_;
    private EventChannelFactory     myFactory_;

    private List allConsumerAdmins_ = new Vector();

    private Map consumerAdminServants_;
    private Map supplierAdminServants_;

    //    private Map allFilters_;

    private List incomingQueue_;

    private ConsumerAdmin defaultConsumerAdmin_;
    private SupplierAdmin defaultSupplierAdmin_;
    protected ConsumerAdminTieImpl defaultConsumerAdminServant_;
    protected SupplierAdminTieImpl defaultSupplierAdminServant_;

    private  int consumerIdPool_ = 0;

    private Any nullAny = null;
    private ORB myOrb_ = null;
    private POA myPoa_ = null;

    private  Logger logger_;

    Logger timeLogger_ = Logger.getLogger("TIME.EventChannel");

    int getAdminId() {
        return ++consumerIdPool_;
    }

    ConsumerAdminTieImpl getConsumerAdminServant() {
	return defaultConsumerAdminServant_;
    }

    SupplierAdminTieImpl getSupplierAdminServant() {
	return defaultSupplierAdminServant_;
    }

    /**
     * The MyFactory attribute is a readonly attribute that maintains
     * the object reference of the event channel factory, which
     * created a given Notification Service EventChannel instance.
     */
    public EventChannelFactory MyFactory() {
        return myFactory_;
    }

    /**
     * The default_consumer_admin attribute is a readonly attribute
     * that maintains a reference to the default ConsumerAdmin
     * instance associated with the target EventChannel instance. Each
     * EventChannel instance has an associated default ConsumerAdmin
     * instance, which exists upon creation of the channel and is
     * assigned the unique identifier of zero. Subsequently, clients
     * can create additional Event Service style ConsumerAdmin
     * instances by invoking the inherited  operation, and additional
     * Notification Service style ConsumerAdmin instances by invoking
     * the new_for_consumers operation defined by the EventChannel
     * interface.
     */
    public ConsumerAdmin default_consumer_admin() {
	return defaultConsumerAdmin_;
    }

    /**
     * The default_supplier_admin attribute is a readonly attribute
     * that maintains a reference to the default SupplierAdmin
     * instance associated with the target EventChannel instance. Each
     * EventChannel instance has an associated default SupplierAdmin
     * instance, which exists upon creation of the channel and is
     * assigned the unique identifier of zero. Subsequently, clients
     * can create additional Event Service style SupplierAdmin
     * instances by invoking the inherited for_suppliers operation,
     * and additional Notification Service style SupplierAdmin
     * instances by invoking the new_for_suppliers operation defined
     * by the EventChannel interface. 
     */
    public SupplierAdmin default_supplier_admin() {
	return defaultSupplierAdmin_;
    }

    /**
     * The default_filter_factory attribute is a readonly attribute
     * that maintains an object reference to the default factory to be
     * used by the EventChannel instance with which it�s associated for
     * creating filter objects. If the target channel does not support
     * a default filter factory, the attribute will maintain the value
     * of OBJECT_NIL.
     */
    public FilterFactory default_filter_factory() {
        return defaultFilterFactory_;
    }

    /**
     * The new_for_consumers operation is invoked to create a new
     * Notification Service style ConsumerAdmin instance. The
     * operation accepts as an input parameter a boolean flag, which
     * indicates whether AND or OR semantics will be used when
     * combining the filter objects associated with the newly created
     * ConsumerAdmin instance with those associated with a supplier
     * proxy, which was created by the ConsumerAdmin during the
     * evaluation of each event against a set of filter objects. The
     * new instance is assigned a unique identifier by the target
     * EventChannel instance that is unique among all ConsumerAdmin
     * instances currently associated with the channel. Upon
     * completion, the operation returns the reference to the new
     * ConsumerAdmin instance as the result of the operation, and the
     * unique identifier assigned to the new ConsumerAdmin instance as
     * the output parameter. 
     */
    public ConsumerAdmin new_for_consumers(InterFilterGroupOperator filterGroupOperator,
                                           IntHolder intHolder) {

        return new_for_consumers_servant(filterGroupOperator, intHolder).getConsumerAdmin();
    }

    ConsumerAdminTieImpl new_for_consumers_servant(InterFilterGroupOperator filterGroupOperator,
						   IntHolder intHolder) {

	intHolder.value = getAdminId();

        ConsumerAdminTieImpl _consumerAdminServant = new ConsumerAdminTieImpl(applicationContext_,
									      channelContext_,
									      intHolder.value,
									      filterGroupOperator);
	
	Integer _key = new Integer(intHolder.value);

	consumerAdminServants_.put(_key, _consumerAdminServant);

	allConsumerAdmins_.add(_consumerAdminServant);

	return _consumerAdminServant;
    }

    public SupplierAdmin new_for_suppliers(InterFilterGroupOperator filterGroupOperator,
                                           IntHolder intHolder) {


        SupplierAdmin _supplierAdmin = new_for_suppliers_servant(filterGroupOperator, intHolder).getSupplierAdmin();

        return _supplierAdmin;
    }
    
    SupplierAdminTieImpl new_for_suppliers_servant(InterFilterGroupOperator filterGroupOperator,
						   IntHolder intHolder) {

	intHolder.value = getAdminId();
	Integer _key = new Integer(intHolder.value);
	
        SupplierAdminTieImpl _supplierAdminServant = new SupplierAdminTieImpl(applicationContext_,
									      channelContext_,
									      intHolder.value,
									      filterGroupOperator);

	supplierAdminServants_.put(_key, _supplierAdminServant);

	return _supplierAdminServant;
    }
	

    public void removeAdmin(AdminBase admin) {
	logger_.debug("removeAdmin");
	Integer _key = admin.getKey();

	if (_key != null) {
	    logger_.debug("removeAdmin(" + _key + ")");
	    logger_.debug("admin: " + admin);

	    if (admin instanceof SupplierAdminTieImpl) {
		supplierAdminServants_.remove(_key);
	    } else if (admin instanceof ConsumerAdminTieImpl) {
		consumerAdminServants_.remove(_key);
	    }
	}

 	if (allConsumerAdmins_.contains(admin)) {
 	    allConsumerAdmins_.remove(admin);
 	}
    }

    public ConsumerAdmin get_consumeradmin(int identifier) {
	if (identifier == 0) {
	    return defaultConsumerAdmin_;
	}

        Integer _key = new Integer(identifier);
        return ((ConsumerAdminTieImpl)consumerAdminServants_.get(_key)).getConsumerAdmin();
    }

    public SupplierAdmin get_supplieradmin(int identifier) {
	if (identifier == 0) {
	    return defaultSupplierAdmin_;
	}

        Integer _key = new Integer(identifier);
        return ((SupplierAdminTieImpl)supplierAdminServants_.get(_key)).getSupplierAdmin();
    }

    public int[] get_all_consumeradmins() {
        return null;
    }

    public int[] get_all_supplieradmins() {
        return null;
    }

    public Property[] get_admin() {
        return null;
    }

    public Property[] get_qos() {
        return null;
    }

    public void set_qos(Property[] qos) throws UnsupportedQoS {}

    public void validate_qos(Property[] qos,
                             NamedPropertyRangeSeqHolder namedPropertySeqHolder) throws UnsupportedQoS {}

    public void set_admin(Property[] adminProps) throws UnsupportedAdmin {}

    /**
     * EventChannel constructor.
     */
    EventChannelImpl(ApplicationContext appContext,
		     ChannelContext channelContext) {

	channelContext_ = channelContext;
	applicationContext_ = appContext;

        myOrb_ = appContext.getOrb();
        myPoa_ = appContext.getPoa();
        myFactory_ = channelContext.getEventChannelFactory();
        myFactoryServant_ = channelContext.getEventChannelFactoryServant();
        defaultFilterFactory_ = channelContext.getDefaultFilterFactory();

	channelContext.setEventChannelServant(this);
	channelContext_.setEventChannel(_this(myOrb_));

	logger_ = Logger.getLogger("EventChannel");

	incomingQueue_ = new Vector();

	supplierAdminServants_ = new Hashtable();
	consumerAdminServants_ = new Hashtable();

	defaultConsumerAdminServant_ = 
	    new ConsumerAdminTieImpl(applicationContext_, 
				     channelContext_);

	allConsumerAdmins_.add(defaultConsumerAdminServant_);

	defaultSupplierAdminServant_ = 
	    new SupplierAdminTieImpl(applicationContext_, 
				     channelContext_);

	ConsumerAdminPOATie _consumerAdmin = new ConsumerAdminPOATie(defaultConsumerAdminServant_);
	SupplierAdminPOATie _supplierAdmin = new SupplierAdminPOATie(defaultSupplierAdminServant_);

	defaultSupplierAdmin_ = _supplierAdmin._this(myOrb_);
	defaultConsumerAdmin_ = _consumerAdmin._this(myOrb_);

        _this_object(myOrb_);
        nullAny = myOrb_.create_any();
        nullAny.type(myOrb_.get_primitive_tc( TCKind.tk_null));

	channelContext.setEngine(new Engine());
	
        try {
            myPoa_ = applicationContext_.getPoa();
            myPoa_.the_POAManager().activate();
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * send the ConsumerAdmin vectors off for destrcution.
     */
    private void consumerAdminDestroy() {
    }

    /**
     * send the SupplierAdmin vectors off for destrcution.
     */
    private void supplierAdminDestroy() {
    }

    /**
     * Iteratre a list and send the servant off to be destroyed.
     */
    private void releaseList(Vector list) {
        for (Enumeration e = list.elements(); e.hasMoreElements(); ) {
            org.omg.PortableServer.Servant servant =
                (org.omg.PortableServer.Servant)e.nextElement();
            releaseServant(servant);
        }
    }

    /**
     * Destroy / deactivate the servant.
     */
    private void releaseServant(org.omg.PortableServer.Servant servant) {
        try {
            servant._poa().deactivate_object( servant._object_id() );
        } catch (org.omg.PortableServer.POAPackage.WrongPolicy wpEx) {
            wpEx.printStackTrace();
        } catch (org.omg.PortableServer.POAPackage.ObjectNotActive onaEx) {
            onaEx.printStackTrace();
        }
    }

    /**
     * Destroy all objects which are managed by the POA.
     */
    public void destroy() {
	dispose();
    }

    public void dispose() {
	logger_.info("dispose()");

	defaultConsumerAdminServant_.dispose();
	defaultSupplierAdminServant_.dispose();

	Iterator _i = consumerAdminServants_.values().iterator();
	while (_i.hasNext()) {
	    ((Disposable)_i.next()).dispose();
	}

	_i = supplierAdminServants_.values().iterator();
	while (_i.hasNext()) {
	    ((Disposable)_i.next()).dispose();
	}

	consumerAdminDestroy();
        supplierAdminDestroy();
	//        releaseServant(this);

	channelContext_.getEngine().shutdown();
    }


    /**
     * Return the consumerAdmin interface (event style)
     */
     public org.omg.CosEventChannelAdmin.ConsumerAdmin for_consumers() {
         try {
	     return org.omg.CosEventChannelAdmin.ConsumerAdminHelper.narrow(defaultConsumerAdmin_);
         } catch( Exception e ) {
             e.printStackTrace();
             return null;
         }
     }

    /**
     * Return the supplierAdmin interface (event style)
     */
    public org.omg.CosEventChannelAdmin.SupplierAdmin for_suppliers() {
        try {
            return org.omg.CosEventChannelAdmin.SupplierAdminHelper.narrow(defaultSupplierAdmin_);
        } catch( Exception e ) {
            e.printStackTrace();
            return null;
        }
    }

    public void dispatchEvent(NotificationEvent event) {
	long _time = System.currentTimeMillis();

	channelContext_.getEngine().dispatchEvent(event);

	timeLogger_.info("push(): " + (System.currentTimeMillis() - _time));
    }

    /**
     * Override this method from the Servant baseclass.  Fintan Bolton
     * in his book "Pure CORBA" suggests that you override this method to
     * avoid the risk that a servant object (like this one) could be
     * activated by the <b>wrong</b> POA object.
     */
    public POA _default_POA() {
        return myPoa_;
    }

     List getAllConsumerAdmins() {
 	return allConsumerAdmins_;
     }
}
