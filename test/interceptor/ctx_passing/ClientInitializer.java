package test.interceptor.ctx_passing;

import org.omg.PortableInterceptor.*;
import org.omg.IOP_N.*;

/**
 * ClientInitializer.java
 *
 *
 * Created: Fri Oct 26 10:58:29 2001
 *
 * @author Nicolas Noffke
 * @version $Id: ClientInitializer.java,v 1.1 2001-10-26 22:02:18 jacorb Exp $
 */

public class ClientInitializer 
    extends org.jacorb.orb.LocalityConstrainedObject 
    implements ORBInitializer  
{
    public static int slot_id = -1;

    public ClientInitializer()
    {        
    }

    // implementation of org.omg.PortableInterceptor.ORBInitializerOperations interface

    /**
     *
     * @param param1 <description>
     */
    public void pre_init(ORBInitInfo info) 
    {
    }

    /**
     *
     * @param param1 <description>
     */
    public void post_init(ORBInitInfo info) 
    {
        try
        {
            slot_id = info.allocate_slot_id();
        
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, 
                                             (byte) 1, (byte) 0);
            Codec codec = info.codec_factory().create_codec(encoding);
        
            info.add_client_request_interceptor( new ClientInterceptor( slot_id, codec ));
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}// ClientInitializer

