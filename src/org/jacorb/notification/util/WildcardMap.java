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

import java.util.List;
import java.util.ArrayList;

/**
 * An Object that maps String Keys to Values. A WildcardMap cannot
 * contain duplicate
 * keys. Each Key has exactly one Entry associated. A Key can contain
 * the Wildcard Character '*' which matches zero or more characters of
 * the key. The WildcardMap supports two semantics of accessing the
 * entries. The first way is to ignore the special meaning of the
 * Wildcard character and to
 * just return the entries as they were inserted.<br>
 * This way you could put some entries in a WildcardMap and fetch them
 * again using the Operation {@link #getNoExpansion(Object) getNoExpansion()}:
 * <pre>
 * WildcardMap wc = new WildcardMap();
 * wc.put("abc", new Integer(1));
 * wc.put("a*", new Integer(2));
 * wc.getNoExpansion("abc") => 1
 * wc.getNoExpansion("a*") => 2
 * wc.getNoExpansion("xyz") => null
 * </pre>
 * This behaviour is similiar to a {@link java.util.Map Map}.<br>
 * The other way using the WildcardMap is to call the Operation {@link
 * #getWithExpansion(Object) getWithExpansion()}. This Operations
 * matches the requested Key to all contained Keys. If the Key of an Entry
 * contains the Wildcard Character '*' it is matched
 * as expected by the semantic of '*'. The Operations returns an array
 * of all matching entries:
 * <pre>
 * wc.getWithExpansion("abc") => [1,2]
 * wc.getWithExpansion("a") => [2]
 * wc.getWithExpansion("abcd") => [2]
 * wc.getWithExpansion("xyz") => []
 * </pre>
 *
 * @author Alphonse Bendt
 * @version $Id: WildcardMap.java,v 1.13 2004-05-09 19:01:42 alphonse.bendt Exp $
 */

public class WildcardMap
{
    public static final int DEFAULT_TOPLEVEL_SIZE = 4;

    private EntryList topLevel_;

    ////////////////////////////////////////

    public WildcardMap( int topLevelSize )
    {
        super();

        topLevel_ = new EntryList( topLevelSize );
    }

    public WildcardMap()
    {
        this( DEFAULT_TOPLEVEL_SIZE );
    }

    ////////////////////////////////////////

    /**
     * clear this map
     */
    public void clear()
    {
        topLevel_.clear();
    }

    /**
     * remove the specified key from this Map.
     */
    public Object remove( Object key )
    {
        char[] _key = key.toString().toCharArray();

        return topLevel_.remove( _key, 0, _key.length );
    }

    /**
     * The operation <code>put</code> associates the specified value
     * with the specified key in this map. The String representation
     * of the Key {@link java.lang.Object#toString() toString()} is
     * used. If the map previously
     * contained a mapping for this key, the old value is replaced by
     * the specified value.
     *
     * @param key key with which String representation the specified value
     * is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or null
     * if there was no mapping for key.
     */
    public Object put( Object key, Object value )
    {
        char[] _key = key.toString().toCharArray();
        WCEntry _entry = new WCEntry( _key, 0, _key.length, value );
        Object _ret = topLevel_.put( _entry );

        return _ret;
    }

    /**
     * Returns the value to which this map maps the specified
     * key. Returns null if the map contains no mapping for this key.
     * @param key key whose associated value is to be returned
     * @return the value to which this map maps the specified key, or
     * null if the map contains no mapping for this key.
     */
    public Object getNoExpansion( Object key )
    {
        char[] _key = key.toString().toCharArray();

        return topLevel_.getSingle( _key, 0, _key.length );
    }

    /**
     * Returns the value to which this map maps the specified
     * key. Additionaly return all Values which keys contain a
     * Wildcard and match the requested key. Returns null if the map
     * contains no mapping for this key.
     * @param key key whose associated value is to be returned
     * @return an Array of all Matching entries or null if no matching
     * entry could be found.
     */
    public Object[] getWithExpansion( Object key )
    {
        char[] _key = key.toString().toCharArray();

        return topLevel_.getMultiple( _key, 0, _key.length );
    }

    /**
     * @return a String representation of this WildcardMap
     */
    public String toString()
    {
        return topLevel_.toString();
    }

    static int countStarsInKey( char[] key, int start, int end )
    {
        int _starCount = 0;
        int x = start;

        while ( x < end )
        {
            if ( key[ x ] == '*' )
            {
                ++_starCount;
            }

            ++x;
        }
        return _starCount;
    }
}

/**
 * the idea for this implementation is based on extensible hashing and
 * trie's.
 * an EntryList maps Strings to values. common prefixes of Strings are
 * only stored once.<br>
 * See section 4.1.10 and section 4.2.5 in my masters thesis available
 * at http://www.jacorb.org/docs/DAbendt-web.pdf (in german) for a
 * broader description of what has been implemented here.
 */
class EntryList
{
    private static int DEFAULT_INITIAL_SIZE = 2;

    private PatternWrapper myPattern_;

    private char[] key_;

    private int start_;

    private int end_;

    private int depth_;

    private int splitted = 0;

    private WCEntry myEntry_;

    private EntryList[] entries_;

    ////////////////////////////////////////
    // Constructors

    EntryList( int size )
    {
        this( null, 0, 0, 0, null, size );
    }


    private EntryList( char[] key, int start, int end, int depth, WCEntry value )
    {
        this( key, start, end, depth, value, DEFAULT_INITIAL_SIZE );
    }

    private EntryList( char[] key, int start, int end, int depth, WCEntry entry, int size )
    {
        myEntry_ = entry;
        key_ = key;
        end_ = end;
        start_ = start;
        depth_ = depth;
        entries_ = new EntryList[ size ];
        initPattern( key_, start_, end_ );
    }

    ////////////////////////////////////////

    /**
     * check if this EntryList has an Entry associated
     */
    private boolean hasEntry()
    {
        return myEntry_ != null;
    }

    void clear()
    {
        entries_ = new EntryList[ DEFAULT_INITIAL_SIZE ];
    }

    /**
     * add an Entry to this List.
     */
    Object put( WCEntry entry )
    {
        char _first = entry.key_[ 0 ];

        ensureIndexIsAvailable( _first );

        int _idx = computeHashIndex( _first );

        if ( entries_[ _idx ] == null )
        {
            entries_[ _idx ] = new EntryList( entry.key_, 0, entry.key_.length, 0, entry );
            return null;
        }
        else
        {
            return entries_[ _idx ].put( entry.key_, 0, entry.key_.length, 0, entry, false );
        }
    }

    Object put( char[] key, int start, int stop, int depth, WCEntry value, boolean addLeadingStar )
    {
        int _insertKeyLength = stop - start;
        int _myKeyLength = end_ - start_;

        int _prefixLength = findCommonPrefix( key, start, stop );

        if ( _prefixLength == _insertKeyLength )
        {
            if ( endsWithStar() )
            {
                splitEntryList( this, _prefixLength );
            }

            Object _old = null;
            // overwrite

            if ( myEntry_ != null )
            {
                _old = myEntry_.getValue();
            }

            myEntry_ = value;

            return _old;
        }
        else if ( _prefixLength < _myKeyLength )
        {
            splitEntryList( this, _prefixLength );

            boolean _addStar = false;

            if ( endsWithStar() )
            {
                _addStar = true;
            }

            put( key, start, stop, depth + _prefixLength, value, _addStar );

        }
        else
        {
            char _firstRemainingChar = key[ start + _prefixLength ];
            ensureIndexIsAvailable( _firstRemainingChar );

            int idx = computeHashIndex( _firstRemainingChar );

            if ( entries_[ idx ] == null )
            {
                entries_[ idx ] = new EntryList( key,
                                                 start + _prefixLength,
                                                 stop,
                                                 depth_ + _prefixLength,
                                                 value );

                if ( addLeadingStar )
                {
                    entries_[ idx ].addLeadingStar();
                }
            }
            else
            {
                entries_[ idx ].put( key,
                                     start + _prefixLength,
                                     stop,
                                     depth + _prefixLength,
                                     value,
                                     false );
            }
        }

        return null;
    }

    Object getSingle( char[] key, int start, int stop )
    {
        //        Object _result = null;
        EntryList _entryList = lookup( key[ start ] );
        int _position = start;

        while ( _entryList != null )
        {
            int _remainingKeyLength = stop - _position;


            int _devoured = _entryList.compare( key,
                                                start + _entryList.depth_,
                                                start + _entryList.depth_ + _remainingKeyLength,
                                                false );

            if ( _devoured == _remainingKeyLength )
            {
                return ( _entryList.myEntry_.getValue() );
            }
            else if ( _devoured > 0 )
            {
                char _firstRemainingChar = key[ start + _entryList.depth_ + _devoured ];
                int _oldDepth = _entryList.depth_;

                _entryList = _entryList.lookup( _firstRemainingChar );

                if ( _entryList != null )
                {
                    _position += _entryList.depth_ - _oldDepth;
                }
            }
        }

        return null;
    }

    /**
     * check if the Key for this List ends with a star.
     */
    private boolean endsWithStar()
    {
        return key_[ end_ -1 ] == '*';
    }

    /**
     * lookup a key in this list. thereby perform Wildcard expansion.
     */
    Object[] getMultiple( char[] key, int start, int stop )
    {
        List _toBeProcessed = new ArrayList();

        List _resultList = new ArrayList();

        Cursor _startCursor;

        // first try exact match

        EntryList _list = lookup( key[ start ] );

        if ( _list != null )
        {
            // add EntryList to nodes to be processed

            _toBeProcessed.add( new Cursor(start, _list) );
        }

        // next try '*'

        if ( ( _list = lookup( '*' ) ) != null )
        {
            // add EntryList to nodes to be processed

            _startCursor = new Cursor(start, _list);

            _toBeProcessed.add( _startCursor );
        }

        // process all found nodes

        while ( !_toBeProcessed.isEmpty() )
        {
            Cursor _currentCursor = ( Cursor ) _toBeProcessed.get(0);

            int _remainingKeyLength = stop - _currentCursor.cursor_;

            // try to match the search key to the part of key which is
            // associated with the current node
            int _devoured =
                _currentCursor.list_.compare( key,
                                             start + _currentCursor.list_.depth_,
                                             start + _currentCursor.list_.depth_ + _remainingKeyLength,
                                             true );

            if ( _devoured >= _remainingKeyLength )
            {
                // the whole key could be matched

                if ( _currentCursor.list_.hasEntry() )
                {
                    // if the current node has a result add it to the
                    // result set.
                    _resultList.add( _currentCursor.list_.myEntry_.getValue() );
                }

                if ( ( _remainingKeyLength > 0 ) && _currentCursor.list_.endsWithStar() )
                {
                    // current key ends with *
                    // this means the last compare matched everything
                    // nontheless there still might be outgoing edges
                    // which must be checked if we have some more chars in
                    // the key left.

                    for ( int x = 0; x < _currentCursor.list_.entries_.length; ++x )
                    {
                        if ( _currentCursor.list_.entries_[ x ] != null )
                            {
                                _toBeProcessed.add( new Cursor(_currentCursor.list_.depth_ + 1,
                                                               _currentCursor.list_.entries_[ x ]) );
                            }
                    }
                }

                if ( _currentCursor.list_.lookup( '*' ) != null )
                {
                    // if there is a outgoing '*' visit it
                    // because it might match the end of a key

                    _currentCursor.list_ = _currentCursor.list_.lookup( '*' );
                    _currentCursor.cursor_ += _devoured;
                }
                else
                {
                    _toBeProcessed.remove( 0 );
                }
            }
            else if ( _devoured > 0 )
            {
                // a part could be matched
                char _firstRemainingChar = key[ start + _currentCursor.list_.depth_ + _devoured ];

                int _oldDepth = _currentCursor.list_.depth_;

                // * always matches

                if ( _currentCursor.list_.lookup( '*' ) != null )
                {
                    EntryList _entryList = _currentCursor.list_.lookup( '*' );

                    _toBeProcessed.add(new Cursor(_currentCursor.cursor_ + _entryList.depth_ - _oldDepth,
                                                  _entryList) );
                }

                if ( ( _currentCursor.list_ = _currentCursor.list_.lookup( _firstRemainingChar ) ) != null )
                {
                    // instead of removing the old and adding a new
                    // cursor we reuse the old cursor
                    _currentCursor.cursor_ += _currentCursor.list_.depth_ - _oldDepth;
                }
                else
                {
                    _toBeProcessed.remove( 0 );
                }
            }
            else
            {
                // no part of the search key could be matched
                _toBeProcessed.remove( 0 );
            }
        }

        return _resultList.toArray();
    }


    Object remove( char[] key, int start, int stop )
    {
        return remove( this, key, start, stop );
    }


    private static Object remove( EntryList l, char[] key, int start, int stop )
    {
        int _cursor = start;
        EntryList _current = l;

        while ( true )
        {
            int _devoured =
                findCommonPrefix( key,
                                  _cursor,
                                  stop,
                                  _current.key_,
                                  _current.start_,
                                  _current.end_ );

            _cursor += _devoured;

            if ( _cursor == stop )
            {
                Object _old = null;

                if ( _current.myEntry_ != null )
                {
                    _old = _current.myEntry_.getValue();
                    _current.myEntry_ = null;
                }

                return _old;
            }

            char _firstNext = key[ start + _devoured ];
            _current = _current.lookup( _firstNext );

            if ( _current == null )
            {
                return null;
            }
        }
    }

    ////////////////////////////////////////
    // private methods

    private static class Cursor
    {
        int cursor_;
        EntryList list_;

        Cursor(int cursor, EntryList list) {
            cursor_ = cursor;
            list_ = list;
        }

        public String toString()
        {
            String _rest = new String( list_.key_, cursor_, list_.end_ - cursor_ );

            return "Cursor: " + _rest;
        }
    }

    private void addLeadingStar()
    {
        int _newLength = end_ - start_ + 1;

        char[] _newKey = new char[ _newLength ];
        System.arraycopy( key_, start_, _newKey, 1, end_ - start_ );
        _newKey[ 0 ] = '*';

        initPattern( _newKey, 0, _newLength );
    }

    private void initPattern()
    {
        initPattern( key_, start_, end_ );
    }

    private void initPattern( char[] key, int start, int stop )
    {
        myPattern_ = null;

        int _starCount = WildcardMap.countStarsInKey( key, start, stop );

        if ( _starCount > 0 )
        {
            char[] _pattern = new char[ stop - start + _starCount + 1 ];
            _pattern[ 0 ] = '^'; // regexp to match begin of line
            int x = 0;
            int _offset = 1;

            while ( x < ( stop - start ) )
            {
                char _x = key[ start + x ];
                _pattern[ x + _offset ] = _x;

                // replace '*' with '.*'
                if ( _pattern[ x + _offset ] == '*' )
                {
                    _pattern[ x + _offset ] = '.';
                    _pattern[ x + _offset + 1 ] = '*';
                    ++_offset;
                }

                ++x;
            }

            String _patternString = new String( _pattern, 0 , stop - start + _starCount + 1 );
            myPattern_ = PatternWrapper.init( _patternString );
        }
    }


    private char key()
    {
        return key_[ start_ ];
    }


    private int keyLength()
    {
        return end_ - start_;
    }


    private EntryList lookup( char key )
    {
        int idx = computeHashIndex( key );

        if ( entries_[ idx ] != null && entries_[ idx ].key() == key )
        {
            return entries_[ idx ];
        }
        else
        {
            return null;
        }
    }

    /**
     * ensure that the index returned by computeHashIndex for a
     * specified key is available.
     * That means <ol>
     * <li> The Index is empty
     * <li> The Index contains an EntryList with the same Key as the
     * specified one
     * </ol>
     */
    private void ensureIndexIsAvailable( char key )
    {
        int idx = computeHashIndex( key );

        while ( true )
        {
            // assert (idx < entries_.length);

            if ( entries_[ idx ] == null || entries_[ idx ].key() == key )
            {
                return ;
            }

            doubleCapacity();

            idx = computeHashIndex( key );
        }
    }


    /**
     * double the capacity for our entries. copy entries from old list
     * into the new one.
     */
    private void doubleCapacity()
    {
        int _newSize = entries_.length * 2;

        EntryList[] _newList = new EntryList[ _newSize ];

        for ( int x = 0; x < entries_.length; ++x )
        {
            if ( entries_[ x ] != null )
            {
                int _arrayPos = computeHashIndex(entries_[ x ].key(), _newSize);
                _newList[ _arrayPos ] = entries_[ x ];
            }
        }

        entries_ = _newList;
    }


    private int compare( char[] a, int start, int stop, boolean wildcard )
    {
        if ( wildcard && myPattern_ != null )
        {
            return compareKeyToPattern( a, start, stop, myPattern_ );
        }
        else
        {
            return compareKeyToKey( a, start, stop, key_, start_, end_ );
        }
    }


    private int findCommonPrefix( char[] key, int start, int stop )
    {
        return findCommonPrefix( key, start, stop, key_, start_, end_ );
    }


    private void printToStringBuffer( StringBuffer sb, String offset )
    {
        if ( key_ != null )
        {
            sb.append( " --" );
            sb.append( key() );
            sb.append( "-->\n" );
            sb.append( offset );
            sb.append( "depth: " );
            sb.append( depth_ );
            sb.append( "\n" );
            sb.append( offset );
            sb.append( "key: " );
            sb.append( new String( key_, start_, end_ - start_ ) );
            sb.append( "\n" );
        }

        if ( myEntry_ != null )
        {
            sb.append( offset + myEntry_ );
            sb.append( "\n" );
        }

        for ( int x = 0; x < entries_.length; x++ )
        {
            sb.append( offset + x );
            sb.append( ":" );

            if ( entries_[ x ] == null )
            {
                sb.append( "empty" );
            }
            else
            {
                entries_[ x ].printToStringBuffer( sb, offset + "   " );
            }

            sb.append( "\n" );
        }
    }

    public String toString()
    {
        StringBuffer _b = new StringBuffer();
        printToStringBuffer( _b, "" );
        return _b.toString();
    }

    ////////////////////////////////////////
    // static methods

    private static void splitEntryList( EntryList list, int offset )
    {

        EntryList _ret = new EntryList( list.key_,
                                        list.start_ + offset,
                                        list.end_ ,
                                        list.depth_ + offset,
                                        list.myEntry_ ,
                                        list.entries_.length );

        System.arraycopy( list.entries_, 0, _ret.entries_, 0, list.entries_.length );

        list.entries_ = new EntryList[ DEFAULT_INITIAL_SIZE ];

        char _key = list.key_[ list.start_ + offset ];

        int _idx = computeHashIndex( _key, list.entries_.length );

        list.entries_[ _idx ] = _ret;
        list.myEntry_ = null;
        list.splitted++;
        list.end_ = list.start_ + offset;

        if ( list.endsWithStar() )
        {
            _ret.addLeadingStar();
        }

        list.initPattern();
    }


    private static int computeHashIndex( char c, int size )
    {
        return c % size;
    }


    private int computeHashIndex( char c )
    {
        return computeHashIndex( c, entries_.length );
    }


    static int compareKeyToKey( char[] firstKeyArray,
                                int start1,
                                int stop1,
                                char[] secondKeyArray,
                                int start2,
                                int stop2 )
    {
        int length1 = stop1 - start1;
        int length2 = stop2 - start2;
        int _guard = ( length1 > length2 ) ? length2 : length1;

        int _ret = 0;

        while ( _ret < _guard )
        {
            if ( firstKeyArray[ start1 + _ret ] != secondKeyArray[ start2 + _ret ] )
            {
                return _ret;
            }

            ++_ret;
        }

        return _ret;
    }

    private static int compareKeyToPattern( char[] string1,
                                    int start1,
                                    int stop1,
                                    PatternWrapper p )
    {
        String _other = new String( string1, start1, stop1 - start1 );

        return p.match( _other );
    }

    private static int findCommonPrefix( char[] key1,
                                         int start1,
                                         int stop1,
                                         char[] key2,
                                         int start2,
                                         int stop2 )
    {
        int _x = 0;
        int _length1 = stop1 - start1;
        int _length2 = stop2 - start2;

        int _guard = ( _length1 >= _length2 ) ? _length2 : _length1;

        while ( ( _x < _guard ) && ( key1[ start1 ] == key2[ start2 ] ) )
        {
            ++start1;
            ++start2;
            ++_x;
        }

        return _x;
    }
}

/**
 * This Class represents a Entry within a WildcardMap.
 * Each Entry is identified by a key and has a value associated.
 */
class WCEntry
{
    /**
     * start index of key within key_ array
     */
    private int start_;

    /**
     * stop index of key within key_ array
     */
    private int stop_;

    /**
     * this array contains the key. start and stop index of the key are denoted by start_ and stop_
     */
    char[] key_;

    /**
     * value associated to this Entry
     */
    private Object value_;

    ////////////////////////////////////////

    /**
     * Creates a new <code>WCEntry</code> instance.
     *
     * @param key a <code>char[]</code> value
     * @param start an <code>int</code> value
     * @param stop an <code>int</code> value
     * @param value an <code>Object</code> value
     */
    WCEntry( char[] key, int start, int stop, Object value )
    {
        key_ = key;
        start_ = start;
        stop_ = stop;
        value_ = value;
    }

    ////////////////////////////////////////

    /**
     * access the Value associated to this Entry
     */
    public Object getValue()
    {
        return value_;
    }


    public int hashCode() {
        return key_[start_];

    }


    public boolean equals( Object o )
    {
        try
        {
            WCEntry _other = ( WCEntry ) o;

            return ( EntryList.compareKeyToKey( key_,
                                                start_,
                                                stop_,
                                                _other.key_,
                                                _other.start_,
                                                _other.stop_ ) > 0 );
        }
        catch ( ClassCastException e )
        {
            return super.equals( o );
        }
        catch ( NullPointerException e )
        {
            return false;
        }
    }


    public String toString()
    {
        StringBuffer _b = new StringBuffer();

        _b.append( "['" );
        _b.append( new String( key_, start_ , stop_ - start_ ) );
        _b.append( "' => " );
        _b.append( value_ );
        _b.append( "]" );

        return _b.toString();
    }
}
