package org.jacorb.transaction;

import org.omg.PortableInterceptor.*;
import org.jacorb.orb.*;
import org.omg.IOP_N.*;
/**
 * This class registers the ServerContextTransferInterceptor
 * with the ORB. For that purpose, a slot is allocated
 * at the PICurrent. The slot id is afterwards available
 * via the static attribute
 *
 * @author Nicolas Noffke
 * @version $Id: ServerInitializer.java,v 1.3 2001-03-19 11:10:19 brose Exp $
 */

public class ServerInitializer 
    extends org.jacorb.orb.LocalityConstrainedObject 
    implements ORBInitializer
{

    //for acces by the server (and the PICurrent)
    public static int slot_id = -1;

    public ServerInitializer() {
    }

    // implementation of org.omg.PortableInterceptor.ORBInitializerOperations interface
    /**
     * Registers the Interceptor with a codec and a slot id.
     */
    public void post_init(ORBInitInfo info) {
        try{
            Encoding encoding = new Encoding(ENCODING_CDR_ENCAPS.value, 
                                             (byte) 1, (byte) 0);
            Codec codec = info.codec_factory().create_codec(encoding);

            slot_id = info.allocate_slot_id();

            info.add_server_request_interceptor
                (new ServerContextTransferInterceptor(codec, slot_id));
        }catch (Exception e){
            org.jacorb.util.Debug.output(2, e);
        }
    }

    /**
     *
     * @param info <description>
     */
    public void pre_init(ORBInitInfo info) {
    
    }

} // ServerInitializer






