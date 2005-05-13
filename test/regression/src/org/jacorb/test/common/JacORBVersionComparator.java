package org.jacorb.test.common;

import java.util.*;

/**
 * A Comparator that compares JacORB versions.  Note that the static array
 * <code>versions</code> must be updated each time a new JacORB version
 * is released. 
 * 
 * @author Andre Spiegel spiegel@gnu.org
 * @version $Id: JacORBVersionComparator.java,v 1.1 2005-05-13 13:13:06 andre.spiegel Exp $
 */
public class JacORBVersionComparator implements Comparator
{
    /**
     * Contains the released versions of JacORB in chronological order.
     * "cvs" must always be the final element.
     */
    private static final String[] versions = 
    { 
        "1.3.30", "1.4.1", "2.0", "2.1", "2.2", "2.2.1", "cvs"
    };
    
    public int compare (Object o1, Object o2)
    {
        if (o1 == null && o2 == null)
        {
            return 0;
        }
        else if (o1 == null)
        {
            // no version is earlier than any version
            return -1;
        }
        else if (o2 == null)
        {
            // any version is later than no version
            return 1;
        }
        else if (o1 instanceof String && o2 instanceof String)
        {
            String s1 = (String)o1;
            String s2 = (String)o2;

            int i1 = versions.length,
                i2 = versions.length;

            for (int i=0; i<versions.length; i++)
            {
                if (s1.equals(versions[i])) i1 = i;
                if (s2.equals(versions[i])) i2 = i;
            }
            
            if      (i1 < i2) return -1;
            else if (i1 > i2) return +1;
            else              return 0;
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
}
