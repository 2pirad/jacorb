package org.jacorb.notification.evaluate;

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

import org.apache.log.Hierarchy;
import org.apache.log.Logger;
import org.jacorb.notification.node.EvaluationResult;
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.DynamicAny.DynAnyFactory;
import org.omg.DynamicAny.DynAnyFactoryPackage.InconsistentTypeCode;
import org.omg.DynamicAny.DynAnyPackage.InvalidValue;
import org.omg.DynamicAny.DynAnyPackage.TypeMismatch;

/**
 * ResultExtractor.java
 *
 *
 * @author Alphonse Bendt
 * @version $Id: ResultExtractor.java,v 1.8 2003-08-25 21:00:46 alphonse.bendt Exp $
 */

public class ResultExtractor
{

    private DynAnyFactory dynAnyFactory_;

    private Logger logger_ =
        Hierarchy.getDefaultHierarchy().getLoggerFor( getClass().getName() );

    public ResultExtractor( DynAnyFactory factory )
    {
        dynAnyFactory_ = factory;
    }

    public EvaluationResult extractFromAny( Any any )
    {
        logger_.debug( "extractFromAny(Any)" );

        if (any == null) {
            return null;
        }

        EvaluationResult _ret = new EvaluationResult();

        switch ( any.type().kind().value() )
        {

        case TCKind._tk_boolean:
            logger_.debug( "bool" );
            _ret.setBool( any.extract_boolean() );
            break;

        case TCKind._tk_string:
            logger_.debug( "string" );
            _ret.setString( any.extract_string() );
            break;

        case TCKind._tk_long:
            logger_.debug( "long" );
            _ret.setLong( any.extract_long() );
            break;

        case TCKind._tk_short:
            logger_.debug( "int" );
            _ret.setLong( any.extract_short() );
            break;

        case TCKind._tk_ulonglong:
            logger_.debug("long long");

            _ret.setLongLong( any.extract_ulonglong() );
            break;

        case TCKind._tk_any:
            logger_.debug( "nested" );
            return extractFromAny( any.extract_any() );

        default:
            _ret.addAny( any );
            break;
        }

        return _ret;
    }

} // ResultExtractor
