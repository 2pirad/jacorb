package org.jacorb.orb.connection;

import java.io.IOException;

/**
 * StreamClosedException.java
 *
 *
 * Created: Thu Oct  4 15:50:30 2001
 *
 * @author Nicolas Noffke
 * @version $Id: StreamClosedException.java,v 1.1 2001-10-04 13:52:46 jacorb Exp $
 */

public class StreamClosedException 
    extends IOException 
{
    public StreamClosedException( String reason )
    {
        super( reason );
    }
    
}// StreamClosedException
