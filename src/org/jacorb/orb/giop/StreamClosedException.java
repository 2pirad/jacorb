package org.jacorb.orb.connection;

import java.io.IOException;

/**
 * StreamClosedException.java
 *
 *
 * Created: Thu Oct  4 15:50:30 2002
 *
 * @author Nicolas Noffke
 * @version $Id: StreamClosedException.java,v 1.2 2002-03-19 09:25:28 nicolas Exp $
 */

public class StreamClosedException 
    extends IOException 
{
    public StreamClosedException( String reason )
    {
        super( reason );
    }
    
}// StreamClosedException
