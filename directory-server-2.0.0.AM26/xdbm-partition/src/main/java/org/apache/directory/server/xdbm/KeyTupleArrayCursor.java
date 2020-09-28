/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.xdbm;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.avltree.ArrayTree;
import org.apache.directory.server.core.avltree.ArrayTreeCursor;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Cursor over a set of values for the same key which are store in an in
 * memory ArrayTree.  This Cursor is limited to the same key and it's tuples
 * will always return the same key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeyTupleArrayCursor<K, V> extends AbstractCursor<Tuple<K, V>>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private final ArrayTreeCursor<V> wrapped;
    private final K key;

    private Tuple<K, V> returnedTuple = new Tuple<>();
    private boolean valueAvailable;


    /**
     * Creates a Cursor over the tuples of an ArrayTree.
     *
     * @param arrayTree the ArrayTree to build a Tuple returning Cursor over
     * @param key the constant key for which values are returned
     */
    public KeyTupleArrayCursor( ArrayTree<V> arrayTree, K key )
    {
        this.key = key;
        this.wrapped = new ArrayTreeCursor<>( arrayTree );

        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating KeyTupleArrayCursor {}", this );
        }
    }


    private void clearValue()
    {
        returnedTuple.setKey( key );
        returnedTuple.setValue( null );
        valueAvailable = false;
    }


    public boolean available()
    {
        return valueAvailable;
    }


    public void beforeKey( K key ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    public void afterKey( K key ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
    }


    public void beforeValue( K key, V value ) throws Exception
    {
        checkNotClosed();
        if ( key != null && !key.equals( this.key ) )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
        }

        wrapped.before( value );
        clearValue();
    }


    public void afterValue( K key, V value ) throws Exception
    {
        checkNotClosed();
        if ( key != null && !key.equals( this.key ) )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
        }

        wrapped.after( value );
        clearValue();
    }


    /**
     * Positions this Cursor over the same keys before the value of the
     * supplied element Tuple.  The supplied element Tuple's key is not
     * considered at all.
     *
     * @param element the valueTuple who's value is used to position this Cursor
     * @throws LdapException if there are failures to position the Cursor
     * @throws CursorException if there are failures to position the Cursor
     */
    public void before( Tuple<K, V> element ) throws LdapException, CursorException
    {
        checkNotClosed();
        wrapped.before( element.getValue() );
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    public void after( Tuple<K, V> element ) throws LdapException, CursorException
    {
        checkNotClosed();
        wrapped.after( element.getValue() );
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        wrapped.beforeFirst();
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        wrapped.afterLast();
        clearValue();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        afterLast();

        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed();
        if ( wrapped.previous() )
        {
            returnedTuple.setKey( key );
            returnedTuple.setValue( wrapped.get() );
            valueAvailable = true;
            return true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed();
        if ( wrapped.next() )
        {
            returnedTuple.setKey( key );
            returnedTuple.setValue( wrapped.get() );
            valueAvailable = true;
            return true;
        }
        else
        {
            clearValue();
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Tuple<K, V> get() throws CursorException
    {
        checkNotClosed();

        if ( valueAvailable )
        {
            return returnedTuple;
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing KeyTupleArrayCursor {}", this );
        }

        if ( wrapped != null )
        {
            wrapped.close();
        }

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception reason ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing KeyTupleArrayCursor {}", this );
        }

        if ( wrapped != null )
        {
            wrapped.close( reason );
        }

        super.close( reason );
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "KeyTupleArrayCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#" ).append( key );

        sb.append( " :\n" );

        sb.append( wrapped.toString( tabs + "    " ) );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
