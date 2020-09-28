/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.xdbm.impl.avl;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.avltree.AvlSingletonOrOrderedSetCursor;
import org.apache.directory.server.core.avltree.AvlTree;
import org.apache.directory.server.core.avltree.AvlTreeCursor;
import org.apache.directory.server.core.avltree.SingletonOrOrderedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor which walks and advance over AvlTables that may contain duplicate
 * keys with values stored in an AvlTree.  All duplicate keys are traversed
 * returning the key and the value in a Tuple.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTableDupsCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    private static final Logger LOG = LoggerFactory.getLogger( AvlTableDupsCursor.class );

    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** The AVL backed table this Cursor traverses over. */
    private final AvlTable<K, V> table;

    /**
     * The underlying wrapped cursor which returns Tuples whose values are
     * either V objects or AvlTree objects.
     */
    private final AvlSingletonOrOrderedSetCursor<K, V> wrappedCursor;

    /**
     * A Cursor over a set of value objects for the current key held in the
     * containerTuple.  A new Cursor will be set for each new key as we
     * traverse.  The Cursor traverses over either a AvlTree object full
     * of values in a multi-valued key or it traverses over a BTree which
     * contains the values in the key field of it's Tuples.
     */
    private Cursor<V> dupsCursor;

    /** The current Tuple returned from the wrapped cursor. */
    private final Tuple<K, SingletonOrOrderedSet<V>> wrappedTuple = new Tuple<>();

    /**
     * The Tuple that is used to return values via the get() method. This
     * same Tuple instance will be returned every time.  At different
     * positions it may return different values for the same key.
     */
    private final Tuple<K, V> returnedTuple = new Tuple<>();

    /** Whether or not a value is available when get() is called. */
    private boolean valueAvailable;


    /**
     * Creates a new instance of AvlTableDupsCursor.
     *
     * @param table the AvlTable to build a Cursor on.
     */
    public AvlTableDupsCursor( AvlTable<K, V> table )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating AvlTableDupsCursor {}", this );
        }

        this.table = table;
        this.wrappedCursor = new AvlSingletonOrOrderedSetCursor<>( table.getAvlTreeMap() );
        LOG.debug( "Created on table {}", table.getName() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return valueAvailable;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeKey( K key ) throws Exception
    {
        beforeValue( key, null );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( K key, V value ) throws LdapException, CursorException
    {
        checkNotClosed();
        wrappedCursor.beforeKey( key );

        if ( dupsCursor != null )
        {
            try
            {
                dupsCursor.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }

        if ( wrappedCursor.next() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );

            if ( wrappedTuple.getValue().isOrderedSet() )
            {
                AvlTree<V> avlTree = wrappedTuple.getValue().getOrderedSet();
                dupsCursor = new AvlTreeCursor<>( avlTree );
            }
            else
            {
                dupsCursor = new SingletonCursor<>(
                    wrappedTuple.getValue().getSingleton(), wrappedCursor.getValuComparator() );
            }

            if ( value == null )
            {
                clearValue();
                return;
            }

            /*
             * The cursor over the values is only advanced if we're on the
             * same key as the primary cursor.  This is because we want this
             * method to really position us within a set of duplicate key
             * entries at the proper value.
             */
            if ( table.getKeyComparator().compare( wrappedTuple.getKey(), key ) == 0 )
            {
                dupsCursor.before( value );
            }

            clearValue();
            return;
        }

        clearValue();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );
    }


    /**
     * {@inheritDoc}
     */
    public void afterKey( K key ) throws Exception
    {
        afterValue( key, null );
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( K key, V value ) throws LdapException, CursorException
    {
        checkNotClosed();

        if ( dupsCursor != null )
        {
            try
            {
                dupsCursor.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }

        /*
         * There is a subtle difference between after and before handling
         * with dupicate key values.  Say we have the following tuples:
         *
         * (0, 0)
         * (1, 1)
         * (1, 2)
         * (1, 3)
         * (2, 2)
         *
         * If we request an after cursor on (1, 2).  We must make sure that
         * the container cursor does not advance after the entry with key 1
         * since this would result in us skip returning (1. 3) on the call to
         * next which will incorrectly return (2, 2) instead.
         *
         * So if the value is null in the element then we don't care about
         * this obviously since we just want to advance past the duplicate key
         * values all together.  But when it is not null, then we want to
         * go right before this key instead of after it.
         */

        if ( value == null )
        {
            wrappedCursor.afterKey( key );
        }
        else
        {
            wrappedCursor.beforeKey( key );
        }

        if ( wrappedCursor.next() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

            if ( values.isOrderedSet() )
            {
                AvlTree<V> set = values.getOrderedSet();
                dupsCursor = new AvlTreeCursor<>( set );
            }
            else
            {
                dupsCursor = new SingletonCursor<>( values.getSingleton(), wrappedCursor.getValuComparator() );
            }

            if ( value == null )
            {
                clearValue();

                return;
            }

            // only advance the dupsCursor if we're on same key
            if ( table.getKeyComparator().compare( wrappedTuple.getKey(), key ) == 0 )
            {
                dupsCursor.after( value );
            }

            clearValue();
            return;
        }

        clearValue();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K, V> element ) throws LdapException, CursorException
    {
        afterValue( element.getKey(), element.getValue() );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        clearValue();
        wrappedCursor.afterLast();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );

        if ( dupsCursor != null )
        {
            try
            {            
                dupsCursor.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }

        dupsCursor = null;
    }


    /**
     * {@inheritDoc}
     */
    public void before( Tuple<K, V> element ) throws LdapException, CursorException
    {
        beforeValue( element.getKey(), element.getValue() );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        clearValue();
        wrappedCursor.beforeFirst();
        wrappedTuple.setKey( null );
        wrappedTuple.setValue( null );

        if ( dupsCursor != null )
        {
            try
            {
                dupsCursor.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }

        dupsCursor = null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        checkNotClosed();
        clearValue();

        if ( dupsCursor != null )
        {
            try
            {
                dupsCursor.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }

        dupsCursor = null;

        if ( wrappedCursor.first() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

            if ( values.isOrderedSet() )
            {
                dupsCursor = new AvlTreeCursor<>( values.getOrderedSet() );
            }
            else
            {
                dupsCursor = new SingletonCursor<>( values.getSingleton(), wrappedCursor.getValuComparator() );
            }

            /*
             * Since only tables with duplicate keys enabled use this
             * cursor, entries must have at least one value, and therefore
             * call to last() will always return true.
             */
            dupsCursor.first();
            valueAvailable = true;
            returnedTuple.setKey( wrappedTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );

            return true;
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K, V> get() throws CursorException
    {
        checkNotClosed();

        if ( !valueAvailable )
        {
            throw new InvalidCursorPositionException();
        }

        return returnedTuple;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        checkNotClosed();
        clearValue();

        if ( dupsCursor != null )
        {
            try
            {
                dupsCursor.close();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }

        if ( wrappedCursor.last() )
        {
            wrappedTuple.setBoth( wrappedCursor.get() );
            SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

            if ( values.isOrderedSet() )
            {
                dupsCursor = new AvlTreeCursor<>( values.getOrderedSet() );
            }
            else
            {
                dupsCursor = new SingletonCursor<>( values.getSingleton(), wrappedCursor.getValuComparator() );
            }

            /*
             * Since only tables with duplicate keys enabled use this
             * cursor, entries must have at least one value, and therefore
             * call to last() will always return true.
             */
            dupsCursor.last();
            valueAvailable = true;
            returnedTuple.setKey( wrappedTuple.getKey() );
            returnedTuple.setValue( dupsCursor.get() );

            return true;
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed();

        /*
         * If the cursor over the values of the current key is null or is
         * extinguished then we need to advance to the next key.
         */
        if ( null == dupsCursor || !dupsCursor.next() )
        {
            if ( dupsCursor != null )
            {
                try
                {
                    dupsCursor.close();
                }
                catch ( IOException ioe )
                {
                    throw new LdapException( ioe.getMessage(), ioe );
                }
            }

            /*
             * If the wrappedCursor cursor has more elements we get the next
             * key/AvlTree Tuple to work with and get a cursor over it.
             */
            if ( wrappedCursor.next() )
            {
                wrappedTuple.setBoth( wrappedCursor.get() );
                SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

                if ( values.isOrderedSet() )
                {
                    dupsCursor = new AvlTreeCursor<>( values.getOrderedSet() );
                }
                else
                {
                    dupsCursor = new SingletonCursor<>( values.getSingleton(), wrappedCursor.getValuComparator() );
                }

                /*
                 * Since only tables with duplicate keys enabled use this
                 * cursor, entries must have at least one value, and therefore
                 * call to next() after bringing the cursor to beforeFirst()
                 * will always return true.
                 */
                dupsCursor.beforeFirst();
                dupsCursor.next();
            }
            else
            {
                dupsCursor = null;

                return false;
            }
        }

        /*
         * If we get to this point then cursor has more elements and
         * wrappedTuple holds the Tuple containing the key and the
         * AvlTree of values for that key which the Cursor traverses.  All we
         * need to do is populate our tuple object with the key and the value
         * in the cursor.
         */
        returnedTuple.setKey( wrappedTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );

        valueAvailable = true;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed();
        /*
         * If the cursor over the values of the current key is null or is
         * extinguished then we need to advance to the previous key.
         */
        if ( ( null == dupsCursor ) || !dupsCursor.previous() )
        {
            if ( dupsCursor != null )
            {
                try
                {
                    dupsCursor.close();
                }
                catch ( IOException ioe )
                {
                    throw new LdapException( ioe.getMessage(), ioe );
                }
            }

            /*
             * If the wrappedCursor cursor has more elements we get the previous
             * key/AvlTree Tuple to work with and get a cursor over it's
             * values.
             */
            if ( wrappedCursor.previous() )
            {
                wrappedTuple.setBoth( wrappedCursor.get() );
                SingletonOrOrderedSet<V> values = wrappedTuple.getValue();

                if ( values.isOrderedSet() )
                {
                    dupsCursor = new AvlTreeCursor<>( values.getOrderedSet() );
                }
                else
                {
                    dupsCursor = new SingletonCursor<>( values.getSingleton(), wrappedCursor.getValuComparator() );
                }

                /*
                 * Since only tables with duplicate keys enabled use this
                 * cursor, entries must have at least one value, and therefore
                 * call to previous() after bringing the cursor to afterLast()
                 * will always return true.
                 */
                dupsCursor.afterLast();
                dupsCursor.previous();
            }
            else
            {
                dupsCursor = null;

                return false;
            }
        }

        returnedTuple.setKey( wrappedTuple.getKey() );
        returnedTuple.setValue( dupsCursor.get() );

        valueAvailable = true;
        return true;
    }


    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AvlTableDupsCursor {}", this );
        }

        if ( dupsCursor != null )
        {
            dupsCursor.close();
        }

        wrappedCursor.close();
    }


    @Override
    public void close( Exception reason ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AvlTableDupsCursor {}", this );
        }

        if ( dupsCursor != null )
        {
            dupsCursor.close();
        }

        wrappedCursor.close();
    }


    /**
     * Clears the returned Tuple and makes sure valueAvailable returns false.
     */
    private void clearValue()
    {
        returnedTuple.setKey( null );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }
}
