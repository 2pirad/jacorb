package org.jacorb.orb.portableInterceptor;

import org.omg.IOP_N.*;
import org.omg.IOP_N.CodecFactoryPackage.*;
import org.omg.CORBA.ORB;

/**
 * This class represents a CodecFactory. The factory
 * has currently only one Codec, for ENCODING_CDR_ENCAPS 1.0. <br>
 * If users like to add their own codec, they have to modify
 * create_codec().
 *
 * See PI Spec p.10-80
 *
 * @author Nicolas Noffke
 * @version $Id: CodecFactoryImpl.java,v 1.3 2001-03-19 11:08:54 brose Exp $
 */

public class CodecFactoryImpl extends org.jacorb.orb.LocalityConstrainedObject 
  implements CodecFactory {

  private ORB orb = null;
  
  public CodecFactoryImpl(ORB orb) {
    this.orb = orb;
  }

  // implementation of org.omg.IOP_N.CodecFactoryOperations interface
  public Codec create_codec(Encoding enc) throws UnknownEncoding {
    if (enc.format == ENCODING_CDR_ENCAPS.value)
      if (enc.major_version == 1)
	if (enc.minor_version == 0)
	  return new Codec_CDR_1_0_Impl(orb);
	else
	  throw new UnknownEncoding();
      else
	throw new UnknownEncoding();
    else
      throw new UnknownEncoding();
  }  
} // CodecFactoryImpl






