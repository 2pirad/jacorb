package org.jacorb.orb.connection;

import java.io.IOException;

/**
 * TimeOutException.java
 *
 *
 * Created: Thu Oct  4 15:50:30 2001
 *
 * @author Nicolas Noffke
 * @version $Id: TimeOutException.java,v 1.3 2001-10-04 13:51:58 jacorb Exp $
 */

public class TimeOutException 
    extends IOException 
{
    public TimeOutException( String reason )
    {
        super( reason );
    }
    
}// TimeOutException
