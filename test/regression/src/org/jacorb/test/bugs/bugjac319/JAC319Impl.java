package org.jacorb.test.bugs.bugjac319;

/**
 * @author Nick Cross
 * @version $Id: JAC319Impl.java,v 1.1 2006-07-10 08:56:00 alphonse.bendt Exp $
 */
public class JAC319Impl extends JAC319POA
{
    public org.omg.CORBA.Object getObject (org.omg.CORBA.Object obj)
    {
        return obj;
    }
}
