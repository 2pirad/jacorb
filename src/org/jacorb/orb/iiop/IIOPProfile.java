/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2004 Gerald Brose.
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Library General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this library; if not, write to the Free
 *   Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jacorb.orb.iiop;

import java.util.*;

import org.apache.avalon.framework.configuration.*;
import org.apache.avalon.framework.logger.Logger;

import org.jacorb.orb.CDRInputStream;
import org.jacorb.orb.CDROutputStream;
import org.jacorb.orb.IIOPAddress;
import org.jacorb.orb.TaggedComponentList;

import org.omg.ETF.*;
import org.omg.IOP.*;
import org.omg.SSLIOP.*;
import org.omg.CSIIOP.*;

/**
 * @author Andre Spiegel
 * @version $Id: IIOPProfile.java,v 1.12 2004-08-26 08:51:23 simon.mcqueen Exp $
 */
public class IIOPProfile
    extends org.jacorb.orb.etf.ProfileBase
{
    private IIOPAddress          primaryAddress = null;

    private boolean dnsEnabled = false;
    private Logger logger;
    
    public IIOPProfile()
    {
        super();
    }
    
    public IIOPProfile(byte[] data)
    {
        initFromProfileData(data);
    }

    public IIOPProfile(IIOPAddress address, byte[] objectKey)
    {
        this.version        = new org.omg.GIOP.Version((byte)1,(byte)2);
        this.primaryAddress = address;
        this.objectKey      = objectKey;
        this.components     = new TaggedComponentList();
    }

    public IIOPProfile(IIOPAddress address, byte[] objectKey, int minor)
    {
        this.version        = new org.omg.GIOP.Version((byte)1,(byte)minor);
        this.primaryAddress = address;
        this.objectKey      = objectKey;
        this.components     = new TaggedComponentList();
    }

    /**
     * Constructs an IIOPProfile from a corbaloc URL.  Only to be used
     * from the corbaloc parser.
     */

    public IIOPProfile(String corbaloc)
    {
        this.version = null;
        this.primaryAddress = null;
        this.objectKey = null;
        this.components = null;
        this.corbalocStr = corbaloc;
    }

    public void configure(Configuration configuration)
        throws ConfigurationException
    {
        this.configuration = (org.jacorb.config.Configuration)configuration;
        logger = this.configuration.getNamedLogger("jacorb.iiop.profile");
        dnsEnabled = configuration.getAttribute("jacorb.dns.enable","off").equals("on");
        if (this.primaryAddress != null)
            this.primaryAddress.configure(configuration);

        if (this.corbalocStr != null)
        {
            try
            {
                this.decode_corbaloc(this.corbalocStr);
            }
            catch(Exception e)
            {
                e.printStackTrace(); // debug
            }
        }
    }



    private void decode_corbaloc(String addr)
    {
        String host = "127.0.0.1"; //default to localhost
        short port = 2809; // default IIOP port

        int major = 1;
        int minor = 2; // should this be 0? should it be configurable?

        String errorstr =
            "Illegal IIOP protocol format in object address format: " + addr;
        int sep = addr.indexOf(':');
        String protocol_identifier = "";
        if( sep != 0)
            protocol_identifier = addr.substring( 0,sep);
        if( sep + 1 == addr.length())
            throw new IllegalArgumentException(errorstr);
        addr = addr.substring(sep + 1);

        // decode optional version number
        sep = addr.indexOf( '@' );
        if( sep > -1)
        {
            String ver_str =  addr.substring(0,sep);
            addr = addr.substring(sep+1);
            sep = ver_str.indexOf('.');
            if( sep != -1 )
            {
                try
                {
                    major = Integer.parseInt(ver_str.substring(0,sep));
                    minor = Integer.parseInt(ver_str.substring(sep+1));
                }
                catch( NumberFormatException nfe )
                {
                    throw new IllegalArgumentException(errorstr);
                }
            }
        }
        version = new org.omg.GIOP.Version((byte)major,(byte)minor);

        sep = addr.indexOf(':');
        if( sep != -1 )
        {
            try
            {
                port =(short)Integer.parseInt(addr.substring(sep+1));
                host = addr.substring(0, sep);
            }
            catch( NumberFormatException ill )
            {
                throw new IllegalArgumentException(errorstr);
            }
        }
        primaryAddress = new IIOPAddress(host,port);
        try
        {
            primaryAddress.configure(configuration);
        }
        catch( ConfigurationException ce)
        {
            if (logger.isWarnEnabled())
                logger.warn("ConfigurationException", ce );
        }
        decode_extensions(protocol_identifier.toLowerCase());
    }

    private void decode_extensions(String ident)
    {
        this.components = new TaggedComponentList();
        if (ident.equals("ssliop"))
        {
            SSL ssl = new SSL();
            ssl.port = (short)primaryAddress.getPort();
            String propname =
                "jacorb.security.ssl.corbaloc_ssliop.supported_options";
            ssl.target_supports = get_ssl_options(propname);
            propname =
                "jacorb.security.ssl.corbaloc_ssliop.required_options";
            ssl.target_requires = get_ssl_options(propname);

            //create the tagged component containing the ssl struct
            CDROutputStream out = new CDROutputStream();
            out.beginEncapsulatedArray();
            SSLHelper.write( out, ssl );

            components.addComponent
                (new TaggedComponent( TAG_SSL_SEC_TRANS.value,
                                      out.getBufferCopy() )
                 );
        }
    }

    private short get_ssl_options(String propname)
    {
        //For the time being, we only use EstablishTrustInTarget,
        //because we don't handle any of the other options anyway.
        // So this makes a reasonable default.

        short value =
            (short)configuration.getAttributeAsInteger(propname,EstablishTrustInTarget.value);
        return value;
    }

    /**
     * This function marshals the appropriate information for this
     * transport into the tagged profile.  ORBs will typically need
     * to call the IOR interception points before calling marshal().
     */
/*    public void marshal(TaggedProfileHolder tagged_profile,
                         TaggedComponentSeqHolder components)
    {
        TaggedComponent[] allComponents = null;
        CDROutputStream profileDataStream = null;

        if (components == null)
        {
            components = new TaggedComponentSeqHolder(new TaggedComponent[0]);
        }

        switch( version.minor )
        {
            case 2 :
            {
                //same as IIOP 1.1
            }
            case 1:
            {
                // create IIOP 1.1 profile

                // concatenate the two component lists

                allComponents = new TaggedComponent[   this.components.size()
                                                     + components.value.length ];
                System.arraycopy( this.components.asArray(), 0,
                                  allComponents, 0, this.components.size() );
                System.arraycopy( components.value, 0,
                                  allComponents, this.components.size(),
                                  components.value.length );

                ProfileBody_1_1 pb1 = new ProfileBody_1_1
                (
                    new org.omg.IIOP.Version( version.major, version.minor ),
                    dnsEnabled ? primaryAddress.getHostname() : primaryAddress.getIP(),
                    (short)primaryAddress.getPort(),
                    objectKey,
                    allComponents
                );

                // serialize the profile id 1, leave idx 0 for v.1.0 profile
                profileDataStream = new CDROutputStream();
                profileDataStream.beginEncapsulatedArray();
                ProfileBody_1_1Helper.write( profileDataStream, pb1 );

                tagged_profile.value = new TaggedProfile
                (
                    TAG_INTERNET_IOP.value,
                    profileDataStream.getBufferCopy()
                );
                break;
            }
            case 0:
            {
                // create IIOP 1.0 profile
                ProfileBody_1_0 pb0 = new ProfileBody_1_0
                (
                    new org.omg.IIOP.Version( version.major, version.minor ),
                    dnsEnabled ? primaryAddress.getHostname() : primaryAddress.getIP(),
                    (short)primaryAddress.getPort(),
                    objectKey
                );

                profileDataStream = new CDROutputStream();
                profileDataStream.beginEncapsulatedArray();
                ProfileBody_1_0Helper.write( profileDataStream, pb0 );

                tagged_profile.value = new TaggedProfile
                (
                    TAG_INTERNET_IOP.value,
                    profileDataStream.getBufferCopy()
                );
            }
        }
    }*/
    
    /**
    * Writes the bytes that would make up the ETF::AddressProfile bytes (new spec)
    * to a stream.
    * <p>
    * Writes GIOP version, host string, and port.
    */
    public void writeAddressProfile(CDROutputStream addressProfileStream)
    {
        org.omg.GIOP.VersionHelper.write( addressProfileStream, version);
        addressProfileStream.write_string(dnsEnabled
                                          ? primaryAddress.getHostname()
                                          : primaryAddress.getIP());
        addressProfileStream.write_ushort( (short) primaryAddress.getPort());
    }
    
    /**
    * Reads the bytes that make up the ETF::AddressProfile bytes (new spec)
    * from a stream.
    * <p>
    * Writes GIOP version, host string, and port.
    */
    public void readAddressProfile(CDRInputStream addressProfileStream)
    {
        this.version = org.omg.GIOP.VersionHelper.read(addressProfileStream);
        this.primaryAddress = IIOPAddress.read(addressProfileStream);
        if (configuration != null)
        {
            try
            {
                primaryAddress.configure(configuration);
            }
            catch( ConfigurationException ce)
            {
                if (logger.isWarnEnabled())
                    logger.warn("ConfigurationException", ce );
            }
        }
    }

    /**
     * To improve the management of a large set of profile instances,
     * the author may provide a hash function using the data in a Profile
     * instance. The Profile shall always implement this function and either
     * return a hash number, or 0 (zero) if no hashing is supported.
     */
    public int hash()
    {
        return hashCode();
    }

    public Object clone() throws CloneNotSupportedException
    {
        IIOPProfile result = (IIOPProfile)super.clone();  // bitwise copy

        result.version = new org.omg.GIOP.Version(this.version.major,
                                                   this.version.minor);

        // No need to make a deep copy of the primaryAddress, because
        // the address can safely be shared between this IIOPProfile
        // and the clone.  This way, both will profit from any subsequent
        // DNS resolution of that address.

        if (this.objectKey != null)
        {
            result.objectKey = new byte [this.objectKey.length];
            System.arraycopy(this.objectKey, 0, result.objectKey, 0,
                              this.objectKey.length);
        }

        if (this.components != null)
        {
            result.components = (TaggedComponentList)this.components.clone();
        }

        return result;
    }

    /**
     * This function shall determine if the passed profile, prof, is a match
     * to this profile.  The specifics of the match are left to the details
     * of the underlying transport, however profiles shall be considered a
     * match, if they would create connections that share the same attributes
     * relevant to the transport setup.  Among others, this could include
     * address information (eg. host address) and transport layer
     * characteristics (eg. encryption levels). If a match is found, it
     * shall return true, or false otherwise.
     */
    public boolean is_match(Profile prof)
    {
        if (prof instanceof IIOPProfile)
        {
            IIOPProfile other = (IIOPProfile)prof;
            return this.primaryAddress.equals(other.primaryAddress)
               &&  this.getAlternateAddresses().equals(other.getAlternateAddresses()
               &&  this.getSSLPort() == other.getSSLPort());
        }
        else
            return false;
    }

    public int tag()
    {
        return TAG_INTERNET_IOP.value;
    }

    public IIOPAddress getAddress()
    {
        return primaryAddress;
    }

    /**
     * Replaces the host in this profile's primary address with newHost
     * (if it is not null), and the port with newPort (if it is not -1).
     */
    public void patchPrimaryAddress(String newHost, int newPort)
    {
        if (newHost != null)
        {
            primaryAddress = new IIOPAddress
                (
                 newHost,
                 (newPort != -1) ? newPort
                 : primaryAddress.getPort()
                 );

        }
        else if(newPort != -1)
        {
            primaryAddress = new IIOPAddress(primaryAddress.getIP(),
                                             newPort);
        }
        try
        {
            primaryAddress.configure(configuration);
        }
        catch( ConfigurationException ce)
        {
            if (logger.isWarnEnabled())
                logger.warn("ConfigurationException", ce );
        }
    }

    public List getAlternateAddresses()
    {
        return components.getComponents(TAG_ALTERNATE_IIOP_ADDRESS.value,
                                        IIOPAddress.class);
    }

    public SSL getSSL()
    {
        return (SSL)components.getComponent( TAG_SSL_SEC_TRANS.value,
                                             SSLHelper.class );
    }

    /**
     * Returns the port on which SSL is available according to this profile,
     * or -1 if SSL is not supported.
     */
    public int getSSLPort()
    {
        SSL ssl = getSSL();
        if (ssl == null)
            return -1;
        else
        {
            int port = ssl.port;
            if (port < 0) port += 65536;
            return port;
        }
    }

    /**
     * Returns a copy of this profile that is compatible with GIOP 1.0.
     */
    public IIOPProfile to_GIOP_1_0()
    {
        IIOPProfile result = new IIOPProfile(this.primaryAddress,
                                              this.objectKey);
        result.version.minor = 0;
        return result;
    }

    public boolean equals(Object other)
    {
        if (other instanceof org.omg.ETF.Profile)
            return this.is_match((org.omg.ETF.Profile)other);
        else
            return false;
    }

    public int hashCode()
    {
        return primaryAddress.hashCode();
    }

    public String toString()
    {
        return primaryAddress.toString();
    }

}
