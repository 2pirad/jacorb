package org.jacorb.notification.util;

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

import org.jacorb.notification.util.WildcardMap;

/**
 * CachingWildcardMap.java
 *
 *
 *
 * @author Alphonse Bendt
 * @version $Id: CachingWildcardMap.java,v 1.1 2003-04-12 21:04:53 alphonse.bendt Exp $
 */

public class CachingWildcardMap extends WildcardMap {

    private Object[] cachedKeys_;
    private Object[] cachedValues_;
    private int cacheSize_;

    public CachingWildcardMap(int cacheSize) {

	super();

	cachedValues_ = new Object[cacheSize];
	cachedKeys_ = new Object[cacheSize];
	cacheSize_ = cacheSize;

    } // CachingWildcardMap constructor

    private int calcPosition(String key) {
	return key.charAt(0) % cacheSize_;
    }

    private void invalidateCache() {
	for (int x=0; x<cacheSize_; ++x) {
	    cachedKeys_[x] = null;
	    cachedValues_[x] = null;
	}
    }
    
    public Object remove(Object key) {

	invalidateCache();

	return super.remove(key);
    }

    public Object put( Object key, Object value ) {

	invalidateCache();

	return super.put(key, value);
    }

    public Object[] getWithExpansion( Object key ) {

	String _key = key.toString();
	int _pos = calcPosition(_key);
	Object[] _ret;

	if (_key.equals(cachedKeys_[_pos])) {
	    _ret = (Object[])cachedValues_[_pos];
	} else {
	    _ret = super.getWithExpansion( key );
	    cachedKeys_[_pos] = _key;
	    cachedValues_[_pos] = _ret;
	}
	return _ret;
    }

} // CachingWildcardMap
