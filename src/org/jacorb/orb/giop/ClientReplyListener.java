package org.jacorb.orb.connection;

/**
 * ClientReplyListener.java
 *
 *
 * Created: Sat Aug 18 18:37:56 2001
 *
 * @author Nicolas Noffke
 * @version $Id: ClientReplyListener.java,v 1.1.2.1 2001-08-22 07:22:09 jacorb Exp $ 
 */

public class ClientReplyListener 
    implements ReplyListener
{
    public ClientReplyListener ()
    {
        
    }

    public void replyReceived( byte[] reply,
                               GIOPConnection connection )
    {
        
    }

        
    public void locateReplyReceived( byte[] reply,
                                     GIOPConnection connection )
    {
        
    }


    public void closeConnectionReceived( byte[] close_conn,
                                         GIOPConnection connection )
    {
        
    }

    
    public void fragmentReceived( byte[] fragment,
                                  GIOPConnection connection )
    {
        
    }

}// ClientReplyListener
