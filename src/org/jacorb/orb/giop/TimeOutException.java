package org.jacorb.orb.connection;

import java.io.IOException;

/**
 * TimeOutException.java
 *
 *
 * Created: Thu Oct  4 15:50:30 2002
 *
 * @author Nicolas Noffke
 * @version $Id: TimeOutException.java,v 1.4 2002-03-19 09:25:28 nicolas Exp $
 */

public class TimeOutException 
    extends IOException 
{
    public TimeOutException( String reason )
    {
        super( reason );
    }
    
}// TimeOutException
