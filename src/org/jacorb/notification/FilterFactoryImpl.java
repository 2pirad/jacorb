package org.jacorb.notification;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2003 Gerald Brose
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
 *
 */

import java.io.IOException;
import org.jacorb.notification.interfaces.Disposable;
import org.jacorb.notification.util.LogConfiguration;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNotifyFilter.Filter;
import org.omg.CosNotifyFilter.FilterFactory;
import org.omg.CosNotifyFilter.FilterFactoryPOA;
import org.omg.CosNotifyFilter.InvalidGrammar;
import org.omg.CosNotifyFilter.MappingFilter;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

/**
 * FilterFactoryImpl.java
 *
 *
 * Created: Sat Oct 12 17:25:43 2002
 *
 * @author Alphonse Bendt
 * @version $Id: FilterFactoryImpl.java,v 1.11 2003-08-25 21:00:46 alphonse.bendt Exp $
 */

public class FilterFactoryImpl extends FilterFactoryPOA implements Disposable
{

    public static String CONSTRAINT_GRAMMAR = "EXTENDED_TCL";

    protected ApplicationContext applicationContext_;
    protected boolean isApplicationContextCreatedHere_;

    private FilterFactory thisRef_;

    public FilterFactoryImpl() throws InvalidName, IOException, AdapterInactive
    {
        super();

        LogConfiguration.getInstance().configure();

        final ORB _orb = ORB.init( new String[ 0 ], null );
        POA _poa = POAHelper.narrow( _orb.resolve_initial_references( "RootPOA" ) );
        applicationContext_ = new ApplicationContext( _orb, _poa, true );
        isApplicationContextCreatedHere_ = true;

        getFilterFactory();

        _poa.the_POAManager().activate();

        Thread t = new Thread( new Runnable()
                               {
                                   public void run()
                                   {
                                       _orb.run();
                                   }
                               }

                             );

        t.setDaemon( true );
        t.start();
    }

    public FilterFactoryImpl( ApplicationContext applicationContext ) throws InvalidName
    {
        super();

        applicationContext_ = applicationContext;
        isApplicationContextCreatedHere_ = false;
    }

    public Filter create_filter( String grammar )
    throws InvalidGrammar
    {

        FilterImpl _servant = create_filter_servant( grammar );

        Filter _filter = _servant._this( applicationContext_.getOrb() );

        return _filter;
    }

    FilterImpl create_filter_servant( String grammar )
    throws InvalidGrammar
    {

        if ( CONSTRAINT_GRAMMAR.equals( grammar ) )
        {

            FilterImpl _filterServant = new FilterImpl( applicationContext_, CONSTRAINT_GRAMMAR );

            _filterServant.init();

            return _filterServant;
        }

        throw new InvalidGrammar( "Constraint Language '"
                                  + grammar
                                  + "' is not supported. Try one of the following: "
                                  + CONSTRAINT_GRAMMAR );
    }

    public MappingFilter create_mapping_filter( String grammar,
            Any any ) throws InvalidGrammar
    {

        FilterImpl _filterImpl = create_filter_servant( grammar );

        MappingFilterImpl _mappingFilterServant = new MappingFilterImpl( applicationContext_,
                _filterImpl,
                any );

        MappingFilter _filter = _mappingFilterServant._this( applicationContext_.getOrb() );

        return _filter;
    }

    public void dispose()
    {
        if ( isApplicationContextCreatedHere_ )
        {
            applicationContext_.getOrb().shutdown( true );
            applicationContext_.dispose();
        }
    }

    public FilterFactory getFilterFactory()
    {
        if ( thisRef_ == null )
        {
            synchronized ( this )
            {
                if ( thisRef_ == null )
                {
                    thisRef_ = _this( applicationContext_.getOrb() );
                }
            }
        }

        return thisRef_;
    }

    public POA _default_POA()
    {
        return applicationContext_.getPoa();
    }

}
