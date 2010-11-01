package org.jacorb.orb;
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

import java.util.ArrayList;
import java.util.List;
import org.jacorb.config.*;
import org.omg.CORBA.NO_MEMORY;

/**
 * A BufferManager is used to share a pool of buffers and to implement
 * a buffer  allocation policy.  This  reduces the  number of  memory
 * allocations and deallocations and the overall memory footprint.
 * Buffers are generally created on demand.
 *
 * The BufferManager uses a singleton pattern, so will only be a single
 * shared BuffferManager across all ORBs in a process.
 *
 * @author Gerald Brose, FU Berlin
 * @version $Id: BufferManager.java,v 1.33 2010-11-01 16:49:09 alexander.bykov Exp $
*/

public final class BufferManager extends AbstractBufferManager
{
    /** the buffer pool */
    private final List[] bufferPool;
    // The 'extra-large' buffer cache.
    private byte[] bufferMax = null;

    /**
     * the maximal buffer size managed since the buffer
     * pool is ordered by buffer size in log2 steps
     */

    private final int MAX;

    /**
     * the buffer at pos n has size 2**(n+MIN_OFFSET)
     * so the smallest available buffer is 2**MIN_OFFSET,
     * the largest buffers managed are 2**(MIN_OFFSET+MAX-1)
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
    private final int time;

    /**
     * used to create the singleton ORB buffermanager
     * @param configuration
     */
    public BufferManager(Configuration configuration)
    {
        this(
            configuration.getAttributeAsInteger("jacorb.bufferManagerMaxFlush", 0),
            configuration.getAttributeAsInteger("jacorb.maxManagedBufSize", 18));
    }

    /**
     * Configures the singleton.  It is important that this method is
     * synchronized over the singleton instance, to avoid race conditions
     * with getBuffer().  The static synchronization of the calling
     * method is not enough.
     */

    private BufferManager(int time, int max)
    {
        this.time = time;
        this.MAX = max;

        bufferPool = new List[ MAX ];

        for( int i = 0; i < MAX; i++)
        {
            bufferPool[ i ] = new ArrayList();
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
            bufferPool[ m_pos -MIN_OFFSET ].add(new byte[ MEM_BUFSIZE ]);
        }

        if (time > 0)
        {
            if (reaper != null)
            {
                // this is the case when
                // the BufferManager is re-configured
                reaper.dispose();
            }

            // create new reaper
            reaper = new Reaper(time);
            reaper.setName ("BufferManager MaxCache Reaper");
            reaper.setDaemon (true);
            reaper.start();
        }
    }

    /**
     * Log 2, rounded up
     */

    private static final int log2up(int n)
    {
        int l =0;
        int nn = n-1;
        while( (nn >>l) != 0 )
        {
            l++;
        }

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
        {
            l++;
        }

        return l-1;
    }


    public byte[] getPreferredMemoryBuffer()
    {
        return getBuffer( MEM_BUFSIZE );
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
        final byte [] result;

        int log = log2up(initial);

        if (log >= MAX)
        {
            try
            {
                if (!cdrStr || time < 0)
                {
                    // Defaults to returning asked for size
                    result = new byte[initial];
                }
                else
                {
                    synchronized(this)
                    {
                        // Using cache so do below determination
                        if (bufferMax == null || bufferMax.length < initial)
                        {
                            // Autocache really large values for speed
                            bufferMax = new byte[initial*2];
                        }
                        // Else return the cached buffer
                        result = bufferMax;
                        bufferMax = null;
                    }
                }
            }
            catch (OutOfMemoryError e)
            {
                throw new NO_MEMORY(e.toString());
            }
        }
        else
        {
            final List s;
            synchronized(this)
            {
                s = bufferPool[log > MIN_OFFSET ? log - MIN_OFFSET : 0 ];
            }

            if(!s.isEmpty())
            {
                // pop least recently added buffer from the list
                result = (byte[])s.remove(s.size()-1);
            }
            else
            {
                result = new byte[log > MIN_OFFSET ? 1<<log : 1 << MIN_OFFSET ];
            }
        }
        return result;
    }

    /**
     * Describe <code>returnBuffer</code> method here.
     *
     * @param current a <code>byte[]</code> value
     * @param cdrStr a <code>boolean</code> value value to denote if CDROuputStream is
     *               caller (may use cache in this situation)
     */
    public void returnBuffer(byte[] current, boolean cdrStr)
    {
        if (current != null)
        {
            int log_curr = log2down(current.length);

            if( log_curr >= MIN_OFFSET )
            {
                if( log_curr > MAX )
                {
                    synchronized(this)
                    {
                        // Only cache if CDROutputStream is called, cache is enabled &
                        // the new value is > than the cached value.
                        if (cdrStr &&
                                (time >= 0 &&
                                        (bufferMax == null || bufferMax.length < current.length)))
                        {
                            bufferMax = current;
                        }
                        return;
                    }
                }

                synchronized(this)
                {
                    List s = bufferPool[ log_curr - MIN_OFFSET ];
                    if( s.size() < THRESHOLD )
                    {
                        s.add( current );
                    }
                }
            }
        }
    }

    public synchronized void release()
    {
        // printStatistics();
        for( int i= MAX; i > 0; )
        {
            i--;
            bufferPool[i].clear();
        }
        if (reaper != null)
        {
            reaper.dispose();
            reaper = null;
        }
    }


    private class Reaper extends Thread
    {
        private boolean done = false;
        private int sleepInterval = 0;

        public Reaper (int sleepInterval)
        {
            super("BufferManagerReaper");
            // Convert from seconds to milliseconds
            this.sleepInterval = (sleepInterval * 1000);
        }

        public void run()
        {
            long time;

            while (true)
            {
                // Sleep (note time check on wake to catch premature awakening bug)

                try
                {
                    time = sleepInterval + System.currentTimeMillis();
                    synchronized(this)
                    {
                        while(!done && System.currentTimeMillis() <= time)
                        {
                            wait(sleepInterval);
                        }
                    }
                }
                catch (InterruptedException ex) {
                    // ignored
                }

                // Check not shutting down

                synchronized(this)
                {
                    if(done)
                    {
                        break;
                    }
                }

                synchronized(BufferManager.this)
                {
                    bufferMax = null;
                }
            }
        }

        public synchronized void dispose()
        {
            done = true;

            interrupt();

            // Only one thread waiting so safe to use notify rather than notifyAll.
            notify();
        }
    }
}
