/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * 
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.io.IOException;
import java.util.Iterator;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorIterator;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor which adapts an underlying Tuple based Cursor to one which returns
 * IndexEntry objects rather than tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexCursorAdaptor<K> extends AbstractIndexCursor<K>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    @SuppressWarnings("unchecked")
    final Cursor<Tuple> wrappedCursor;
    final IndexEntry<K, String> forwardEntry;


    /**
     * Creates an IndexCursorAdaptor which wraps and adapts a Cursor from a table to
     * one which returns an IndexEntry.
     *
     * @param partitionTxn The transaction to use
     * @param wrappedCursor the Cursor being adapted
     * @param forwardIndex true for a cursor over a forward index, false for
     * one over a reverse index
     */
    @SuppressWarnings("unchecked")
    public IndexCursorAdaptor( PartitionTxn partitionTxn, Cursor<Tuple> wrappedCursor, boolean forwardIndex )
    {
        this.wrappedCursor = wrappedCursor;

        forwardEntry = new IndexEntry<>();
        this.partitionTxn = partitionTxn;

        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating IndexCursorAdaptor {}", this );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean available()
    {
        return wrappedCursor.available();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void before( IndexEntry<K, String> element ) throws LdapException, CursorException
    {
        wrappedCursor.before( element.getTuple() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void after( IndexEntry<K, String> element ) throws LdapException, CursorException
    {
        wrappedCursor.after( element.getTuple() );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        wrappedCursor.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        wrappedCursor.afterLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        return wrappedCursor.first();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        return wrappedCursor.last();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed()
    {
        return wrappedCursor.isClosed();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean previous() throws LdapException, CursorException
    {
        return wrappedCursor.previous();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() throws LdapException, CursorException
    {
        return wrappedCursor.next();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexEntry<K, String> get() throws CursorException
    {
        Tuple<K, String> tuple = wrappedCursor.get();
        forwardEntry.setTuple( tuple );

        return forwardEntry;
    }


    @Override
    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        wrappedCursor.setClosureMonitor( monitor );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing IndexCursorAdaptor {}", this );
        }

        wrappedCursor.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception reason ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing IndexCursorAdaptor {}", this );
        }

        wrappedCursor.close( reason );
    }


    @Override
    public Iterator<IndexEntry<K, String>> iterator()
    {
        return new CursorIterator<>( this );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAfterLast()
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_13102_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isAfterLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBeforeFirst()
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_13102_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isBeforeFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFirst()
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_13102_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLast()
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_13102_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "IndexCursorAdaptor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( " :\n" );

        sb.append( wrappedCursor.toString( tabs + "    " ) );

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
