package org.jacorb.notification.engine;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1997-2003  Gerald Brose.
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

import org.omg.CosEventComm.Disconnected;
import org.omg.CosNotifyComm.SequencePushConsumer;
import org.omg.CosNotification.StructuredEvent;

/**
 * @author Alphonse Bendt
 * @version $Id: PushSequenceOperation.java,v 1.1.2.1 2004-05-09 17:38:44 alphonse.bendt Exp $
 */
public class PushSequenceOperation implements PushOperation {

    private SequencePushConsumer sequencePushConsumer_;

    private StructuredEvent[] structuredEvents_;

    public PushSequenceOperation(SequencePushConsumer pushConsumer,
                                 StructuredEvent[] structuredEvents) {
        sequencePushConsumer_ = pushConsumer;
        structuredEvents_ = structuredEvents;
    }

    public void invokePush() throws Disconnected {
        sequencePushConsumer_.push_structured_events(structuredEvents_);
    }

    public void dispose() {
        // nothing to do
    }
}
