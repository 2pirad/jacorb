package org.jacorb.notification.filter;

/**
 * @author Alphonse Bendt
 * @version $Id: PropertyDoesNotExistException.java,v 1.2 2005-05-01 21:51:46 alphonse.bendt Exp $
 */
public class PropertyDoesNotExistException extends EvaluationException
{
    private static final long serialVersionUID = 1L;

    public PropertyDoesNotExistException(String name)
    {
        super("the property $" + name + " does not exist");
    }
}
