package org.jacorb.notification.util;

/*
 *        JacORB - a free Java ORB
 *
 *   Copyright (C) 1999-2004 Gerald Brose
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * Abstract Base Class for Simple Pooling Mechanism. Subclasses must at least
 * implement the method newInstance. To use a Object call lendObject. After use
 * the Object must be returned with returnObject(Object). An Object must not be
 * used after it has been returned to its pool!
 * 
 * @author Alphonse Bendt
 * @version $Id: AbstractObjectPool.java,v 1.12 2004/07/12 11:20:15
 *          alphonse.bendt Exp $
 */

public abstract class AbstractObjectPool implements Runnable, Configurable
{
    public static final boolean DEBUG = false;

    /**
     * time the cleaner thread sleeps between two cleanups
     */
    public static final long SLEEP = 5000L;

    public static final int LOWER_WATERMARK_DEFAULT = 30;

    public static final int SIZE_INCREASE_DEFAULT = 30;

    public static final int INITIAL_SIZE_DEFAULT = 100;

    public static final int MAXIMUM_WATERMARK_DEFAULT = 1000;

    /**
     * non synchronized as accessing methods are synchronized.
     */
    private static final List sPoolsToLookAfter = new ArrayList();

    private static AbstractObjectPool[] asArray;

    private static boolean modified = true;

    private final static AbstractObjectPool[] ARRAY_TEMPLATE = new AbstractObjectPool[0];

    private static Thread sCleanerThread;

    private static ListCleaner sListCleaner;

    private static AbstractObjectPool[] getAllPools()
    {
        synchronized (sPoolsToLookAfter)
        {
            if (modified)
            {
                asArray = (AbstractObjectPool[]) sPoolsToLookAfter.toArray(ARRAY_TEMPLATE);
                modified = false;
            }
        }
        return asArray;
    }

    private static void registerPool(AbstractObjectPool pool)
    {
        synchronized (sPoolsToLookAfter)
        {
            sPoolsToLookAfter.add(pool);
            modified = true;
            startListCleaner();
        }
    }

    private static void deregisterPool(AbstractObjectPool pool)
    {
        synchronized (sPoolsToLookAfter)
        {
            sPoolsToLookAfter.remove(pool);
            modified = true;
            if (sPoolsToLookAfter.isEmpty())
            {
                stopListCleaner();
            }
        }
    }

    private static class ListCleaner extends Thread
    {
        private SynchronizedBoolean active_ = new SynchronizedBoolean(true);

        public void setInactive()
        {
            active_.set(false);

            interrupt();
        }

        private void ensureIsActive() throws InterruptedException
        {
            if (!active_.get())
            {
                throw new InterruptedException();
            }
        }

        public void run()
        {
            try
            {
                while (active_.get())
                {
                    try
                    {
                        runLoop();
                    } catch (Exception e)
                    {
                    }
                }
            } finally
            {
                synchronized (AbstractObjectPool.class)
                {
                    sCleanerThread = null;
                }
            }
        }

        private void runLoop() throws InterruptedException
        {
            while (true)
            {
                try
                {
                    sleep(SLEEP);
                } catch (InterruptedException ie)
                {
                }

                ensureIsActive();

                Runnable[] poolsToCheck = getAllPools();

                for (int x = 0; x < poolsToCheck.length; ++x)
                {

                    try
                    {
                        poolsToCheck[x].run();
                    } catch (Throwable t)
                    {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    private static ListCleaner getListCleaner()
    {
        synchronized (AbstractObjectPool.class)
        {
            if (sListCleaner == null)
            {
                sListCleaner = new ListCleaner();
            }
            return sListCleaner;
        }
    }

    private static void stopListCleaner()
    {
        synchronized (AbstractObjectPool.class)
        {
            if (sCleanerThread != null)
            {
                sListCleaner.setInactive();
            }
        }
    }

    private static void startListCleaner()
    {
        synchronized (AbstractObjectPool.class)
        {
            if (sCleanerThread == null)
            {
                sCleanerThread = new Thread(getListCleaner());

                sCleanerThread.setName("ObjectPoolCleaner");
                sCleanerThread.setPriority(Thread.MIN_PRIORITY + 1);
                sCleanerThread.setDaemon(true);
                sCleanerThread.start();
            }
        }
    }

    private String name_;

    private LinkedList pool_;

    /**
     * @todo check synchronization
     * @see news://news.gmane.org:119/200406041629.48096.Farrell_John_W@cat.com
     */
    private Set active_ = Collections.synchronizedSet(new HashSet());

    /**
     * lower watermark. if pool size is below that value, create sizeIncrease_
     * new elements.
     */
    private int lowerWatermark_;

    /**
     * how many instances should the pool maximal keep. instances that are
     * returned to a pool which size is greater than maxWatermark_ are discarded
     * and left for the Garbage Collector.
     */
    private int maxWatermark_;

    /**
     * how many instances should be created if pool size falls below
     * lowerWatermark_.
     */
    private int sizeIncrease_;

    /**
     * how many instances should be created at startup of the pool.
     */
    private int initialSize_;

    protected Logger logger_;

    protected Configuration config_;

    public void configure(Configuration conf)
    {
        config_ = conf;
        logger_ = ((org.jacorb.config.Configuration) conf).getNamedLogger(getClass().getName());
        init();
        registerPool(this);
    }

    protected AbstractObjectPool(String name)
    {
        this(name, LOWER_WATERMARK_DEFAULT, SIZE_INCREASE_DEFAULT, INITIAL_SIZE_DEFAULT,
                MAXIMUM_WATERMARK_DEFAULT);
    }

    protected AbstractObjectPool(String name, int threshold, int sizeincrease, int initialsize,
            int maxsize)
    {
        name_ = name;
        pool_ = new LinkedList();
        lowerWatermark_ = threshold;
        sizeIncrease_ = sizeincrease;
        initialSize_ = initialsize;
        maxWatermark_ = maxsize;
    }

    public void run()
    {
        synchronized (pool_)
        {
            if (pool_.size() > lowerWatermark_)
            {
                return;
            }
        }

        List os = new ArrayList(sizeIncrease_);

        for (int x = 0; x < sizeIncrease_; ++x)
        {
            Object _i = createInstance();

            os.add(_i);
        }

        synchronized (pool_)
        {
            pool_.addAll(os);
        }
    }

    private Object createInstance()
    {
        Object _i = newInstance();

        return _i;
    }

    /**
     * Initialize this Pool. An initial Number of Objects is created. Cleanup
     * Thread is started.
     */
    public void init()
    {
        synchronized (pool_)
        {
            for (int x = 0; x < initialSize_; ++x)
            {
                Object _i = createInstance();

                pool_.add(_i);
            }
        }
    }

    /**
     * Release this Pool.
     */
    public void dispose()
    {
        deregisterPool(this);
        pool_.clear();
        active_.clear();
    }

    /**
     * lend an object from the pool.
     */
    public Object lendObject()
    {
        Object _ret = null;

        synchronized (pool_)
        {
            if (!pool_.isEmpty())
            {
                _ret = pool_.removeFirst();
            }
        }

        if (_ret == null)
        {
            _ret = createInstance();
        }

        try
        {
            ((Configurable) _ret).configure(this.config_);
        } catch (ClassCastException cce)
        {
            // no worries, just don't configure
        } catch (ConfigurationException ce)
        {
        }

        activateObject(_ret);
        active_.add(_ret);

        //        logger_.debug("lendObject " + _ret);

        return _ret;
    }

    /**
     * return an Object to the pool.
     */
    public void returnObject(Object o)
    {
        //logger_.debug("returnObject " + o);

        if (active_.remove(o))
        {
            passivateObject(o);

            if (pool_.size() < maxWatermark_)
            {
                synchronized (pool_)
                {
                    pool_.add(o);
                    pool_.notifyAll();
                }
            }
            else
            {
                destroyObject(o);
            }
        }
        else
        {

            // ignore
            //                 logger_.warn( "Object " + o + " was not in pool " + name_ +".
            // multiple release?" );
            //                throw new RuntimeException();

        }
    }

    /**
     * This method is called by the Pool to create a new Instance. Subclasses
     * must override appropiately .
     */
    public abstract Object newInstance();

    /**
     * Is called after Object is returned to pool. No Op.
     */
    public void passivateObject(Object o)
    {
    }

    /**
     * Is called before Object is returned to Client (lendObject). No Op
     */
    public void activateObject(Object o)
    {
    }

    /**
     * Is called if Pool is full and returned Object is discarded. No Op.
     */
    public void destroyObject(Object o)
    {
    }
}