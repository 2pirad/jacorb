package org.jacorb.test.notification.mocks;

import junit.framework.Assert;
import org.jacorb.notification.interfaces.Message;



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

/**
 * @author Alphonse Bendt
 * @version $Id: MockTaskProcessor.java,v 1.2 2004-05-09 19:37:25 alphonse.bendt Exp $
 */
public class MockTaskProcessor extends NullTaskProcessor {

    int processMessageInvoked_;
    int processMessageExpected_;

    public void processMessage(Message message) {
        ++processMessageInvoked_;
    }

    public void expectProcessMessage(int x) {
        processMessageExpected_ = x;
    }

    public void verify() {
        Assert.assertEquals(processMessageExpected_, processMessageInvoked_);
    }
}
