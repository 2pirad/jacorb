package org.jacorb.notification.node;

/**
 * DynamicTypeException.java
 *
 *
 * Created: Sat Jul 06 02:13:01 2002
 *
 * @author <a href="mailto:bendt@inf.fu-berlin.de">Alphonse Bendt</a>
 * @version $Id: DynamicTypeException.java,v 1.1 2002-12-03 23:23:02 alphonse.bendt Exp $
 */

public class DynamicTypeException extends Exception {
    public DynamicTypeException() {
	super();
    }

    public DynamicTypeException(String msg) {
	super(msg);
    }

}// DynamicTypeException
