package org.jacorb.orb;
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

import java.util.*;
import org.jacorb.util.Debug;
import org.jacorb.util.Environment;
import org.omg.CORBA.NO_MEMORY;

/**
 * A BufferManager is used to share a pool of buffers and to implement
 * a buffer  allocation policy.  This  reduces the  number of  memory
 * allocations and deallocations and the overall memory footprint.
 * Buffers are generally created on demand.
 *
 * @author Gerald Brose, FU Berlin
 * @version $Id: BufferManager.java,v 1.15 2003-12-16 08:42:56 gerald Exp $
*/

public final class BufferManager
{
    /** the buffer pool */
    private Stack[] bufferPool;
    // The 'extra-large' buffer cache.
    private byte[] bufferMax = null;

    /** the maximal buffer size managed since the buffer
	pool is ordered by buffer size in log2 steps */

    private static int MAX;

    /** the buffer at pos n has size 2**(n+MIN_OFFSET)
	so the smallest available buffer is 2**MIN_OFFSET,
        the largest buffers managed are 2**(MIN_OFFSET+MAX-1)
    */

    private static final int MIN_OFFSET = 5;

    /** max number of buffers of the same size held in pool */

    private static final int THRESHOLD = 20;
    private static final int MEM_BUFSIZE = 256;
    private static final int MIN_PREFERRED_BUFS = 10;

    // Purge thread for QoS purging of the bufferMax cache.
    private Reaper reaper;
    /**
     * <code>time</code> denotes whether the maxCache will be active:
     * -1: Not active
     * 0 : Active, never flushed
     * >0: Active with reaper flush thread.
     */
    private static int time = 0;

    static
    {
        if (Environment.hasProperty ("jacorb.bufferManagerMaxFlush"))
        {
            time = Environment.getIntProperty ("jacorb.bufferManagerMaxFlush", 10);
        }
    }

    private static BufferManager singleton = new BufferManager();


    private BufferManager()
    {
        MAX = Environment.getMaxManagedBufSize();
	bufferPool = new Stack[ MAX ];

	for( int i = 0; i < MAX; i++)
        {
	    bufferPool[ i ] = new Stack();
        }

        /* create a number of buffers for the preferred memory buffer
           size */

        int m_pos = 0;
        int j = MEM_BUFSIZE;

        while( j > 1 )
        {
            j = j >> 1;
            m_pos++;
        }
        for( int min = 0; min < MIN_PREFERRED_BUFS; min++ )
        {
            bufferPool[ m_pos -MIN_OFFSET ].push( new byte[ MEM_BUFSIZE ]);
        }

        if (time > 0)
        {
            // create new reaper
            reaper = new Reaper (time);
            reaper.setName ("BufferManager MaxCache Reaper");
            reaper.setDaemon (true);
            reaper.start ();
        }
    }

    public static BufferManager getInstance()
    {
        return singleton;
    }

    /**
     * Log 2, rounded up
     */

    private static final int log2up(int n)
    {
	int l =0;
	int nn = n-1;
	while( (nn >>l) != 0 )
	    l++;

	return l;
    }


    /**
     * Log 2, rounded down
     */

    private static final int log2down(int n)
    {
	int l =0;
	int nn = n;
	while( (nn >>l) != 0 )
	    l++;

	return l-1;
    }


    public  byte[] getPreferredMemoryBuffer()
    {
        return getBuffer( MEM_BUFSIZE );
    }


    public synchronized byte[] getBuffer( int initial )
    {
        return getBuffer (initial, false);
    }


    /**
     * <code>getBuffer</code> returns a new buffer.
     *
     * @param initial an <code>int</code> value
     * @param cdrStr a <code>boolean</code> value to denote if CDROuputStream is caller
     *               (may use cache in this situation)
     * @return a <code>byte[]</code> value
     */

    public synchronized byte[] getBuffer( int initial, boolean cdrStr )
    {
        byte [] result;
        Stack s;

        int log = log2up(initial);

        if (log >= MAX)
        {
            try
            {
                if (cdrStr==false || time < 0)
                {
                    // Defaults to returning asked for size
                    result = new byte[initial];
                }
                else
                {
                    // Using cache so do below determination
                    if (bufferMax == null || bufferMax.length < initial)
                    {
                        // Autocache really large values for speed
                        bufferMax = new byte[initial*2];
                    }
                    // Else return the cached buffer
                    result = bufferMax;
                }
            }
            catch (OutOfMemoryError e)
            {
                org.jacorb.util.Debug.output
                    (2, "BufferManager failed to allocate sufficient memory for byte array");
                throw new NO_MEMORY ();
            }
        }
        else
        {
            s = bufferPool[log > MIN_OFFSET ? log-MIN_OFFSET : 0 ];

            if( ! s.isEmpty() )
            {
                result = (byte [])s.pop ();
            }
            else
            {
                result = new byte[log > MIN_OFFSET ? 1<<log : 1 << MIN_OFFSET ];
            }
        }
        return result;
    }

    public synchronized void returnBuffer(byte[] current)
    {
        returnBuffer (current, false);
    }


    /**
     * Describe <code>returnBuffer</code> method here.
     *
     * @param current a <code>byte[]</code> value
     * @param cdrStr a <code>boolean</code> value value to denote if CDROuputStream is
     *               caller (may use cache in this situation)
     */
    synchronized void returnBuffer(byte[] current, boolean cdrStr)
    {
        if (current != null)
        {
            int log_curr = log2down(current.length);

            /*
              org.jacorb.util.Debug.output( 4, "return buffer: " + current.length +
              " bytes, log2: " + log_curr );
            */

            if( log_curr >= MIN_OFFSET )
            {
                if( log_curr > MAX )
                {
                    // Only cache if CDROutputStream is called, cache is enabled &
                    // the new value is > than the cached value.
                    if (cdrStr==true &&
                        (time >= 0 &&
                         (bufferMax == null || bufferMax.length < current.length)))
                    {
                        bufferMax = current;
                    }
                    return;
                }

                Stack s = bufferPool[ log_curr-MIN_OFFSET ];
                if( s.size() < THRESHOLD )
                {
                    s.push( current );
                }
            }
        }
    }

    public void release()
    {
        // printStatistics();
	for( int i= MAX; i > 0; )
	{
	    i--;
	    bufferPool[i].removeAllElements();
	}
        if (reaper != null)
        {
            reaper.done = true;
            reaper.wake ();
        }
    }


    private class Reaper extends Thread
    {
        public boolean done = false;
        private int sleepInterval = 0;

        public Reaper (int sleepInterval)
        {
            // Convert from seconds to milliseconds
            this.sleepInterval = (sleepInterval * 1000);
        }

        public void run ()
        {
            long time;

            while (true)
            {
                // Sleep (note time check on wake to catch premature awakening bug)

                try
                {
                    time = sleepInterval + System.currentTimeMillis ();
                    do
                    {
                        sleep (sleepInterval);
                    }
                    while (System.currentTimeMillis () <= time);
                }
                catch (InterruptedException ex) {}

                // Check not shutting down

                if (done)
                {
                    break;
                }

                if ( Debug.isDebugEnabled() )
                {
                    org.jacorb.util.Debug.output
                    (
                        4,
                        "Reaper thread purging maxBufferCache. It had size: " +
                        (bufferMax == null ? 0 : bufferMax.length)
                    );
                }
                bufferMax = null;
            }
        }

        public synchronized void wake ()
        {
            // Only one thread waiting so safe to use notify rather than notifyAll.
            notify ();
        }
    }
}
