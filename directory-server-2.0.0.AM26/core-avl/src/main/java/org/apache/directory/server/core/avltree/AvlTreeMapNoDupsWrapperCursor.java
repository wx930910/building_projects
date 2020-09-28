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
package org.apache.directory.server.core.avltree;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cursor that converts SingletonOrOrderedSet objects in the value from a
 * AvlTreeMap into Tuples with just K and V presuming that all the keys have
 * no duplicates.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTreeMapNoDupsWrapperCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private final AvlSingletonOrOrderedSetCursor<K, V> wrapped;
    private final Tuple<K, V> returnedTuple = new Tuple<>();


    public AvlTreeMapNoDupsWrapperCursor( AvlSingletonOrOrderedSetCursor<K, V> wrapped )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating AvlTreeMapNoDupsWrapperCursor {}", this );
        }

        this.wrapped = wrapped;
    }


    public void afterKey( K key ) throws Exception
    {
        wrapped.afterKey( key );
    }


    public void afterValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( "This Cursor does not support duplicate keys." );
    }


    public void beforeKey( K key ) throws Exception
    {
        wrapped.beforeKey( key );
    }


    public void beforeValue( K key, V value ) throws Exception
    {
        throw new UnsupportedOperationException( "This Cursor does not support duplicate keys." );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K, V> element ) throws LdapException, CursorException
    {
        wrapped.afterKey( element.getKey() );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        wrapped.afterLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return wrapped.available();
    }


    /**
     * {@inheritDoc}
     */
    public void before( Tuple<K, V> element ) throws LdapException, CursorException
    {
        wrapped.beforeKey( element.getKey() );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        wrapped.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        return wrapped.first();
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K, V> get() throws CursorException
    {
        if ( wrapped.available() )
        {
            Tuple<K, SingletonOrOrderedSet<V>> tuple = wrapped.get();

            if ( tuple.getValue().isOrderedSet() )
            {
                tuple.getValue().getOrderedSet().printTree();
            }

            returnedTuple.setBoth( tuple.getKey(), tuple.getValue().getSingleton() );
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        return wrapped.last();
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        return wrapped.next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        return wrapped.previous();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AvlTreeMapNoDupsWrapperCursor {}", this );
        }

        wrapped.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception reason ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AvlTreeMapNoDupsWrapperCursor {}", this );
        }

        wrapped.close( reason );
    }
}
