package org.jacorb.notification.filter;

/**
 * @author Alphonse Bendt
 * @version $Id: PropertyDoesNotExistException.java,v 1.1 2004-06-29 13:58:38 alphonse.bendt Exp $
 */
public class PropertyDoesNotExistException extends EvaluationException {

    public PropertyDoesNotExistException(String name) {
        super("the property $" + name + " does not exist");
    }

}
