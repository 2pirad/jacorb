package org.jacorb.test.bugs.bugjac257;


/**
 * This is a very simple hello world server.
 *
 * @author Nick Cross
 * @version $Id: JAC257Impl.java,v 1.1 2006-06-30 13:25:44 alphonse.bendt Exp $
 */
public class JAC257Impl extends JAC257POA
{
    /**
     * <code>hello</code> prints out the parameter.
     *
     * @param in a <code>String</code> value
     */
    public void hello (String in)
    {
        System.err.println ("Received " + in);
    }

}
