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
package org.apache.directory.server.xdbm.search.cursor;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over entries satisfying one level scope constraints with alias
 * dereferencing considerations when enabled during search.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChildrenCursor extends AbstractIndexCursor<String>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** Error message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_719 );

    /** A Cursor over the entries in the scope of the search base */
    private final Cursor<IndexEntry<ParentIdAndRdn, String>> cursor;

    /** The Parent ID */
    private String parentId;

    /** The prefetched element */
    private IndexEntry<String, String> prefetched;


    /**
     * Creates a Cursor over entries satisfying one level scope criteria.
     *
     * @param partitionTxn The transaction to use
     * @param db the entry store
     * @param parentId The parent ID
     * @param cursor The wrapped cursor
     */
    public ChildrenCursor( PartitionTxn partitionTxn, Store db, String parentId, 
            Cursor<IndexEntry<ParentIdAndRdn, String>> cursor )
    {
        this.parentId = parentId;
        this.cursor = cursor;
        this.partitionTxn = partitionTxn;

        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating ChildrenCursor {}", this );
        }
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
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
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed();

        boolean hasPrevious = cursor.previous();

        if ( hasPrevious )
        {
            IndexEntry entry = cursor.get();

            if ( ( ( ParentIdAndRdn ) entry.getTuple().getKey() ).getParentId().equals( parentId ) )
            {
                prefetched = entry;
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed();

        boolean hasNext = cursor.next();

        if ( hasNext )
        {
            IndexEntry cursorEntry = cursor.get();
            IndexEntry<String, String> entry = new IndexEntry();
            entry.setId( ( String ) cursorEntry.getId() );
            entry.setKey( ( ( ParentIdAndRdn ) cursorEntry.getTuple().getKey() ).getParentId() );

            if ( entry.getKey().equals( parentId ) )
            {
                prefetched = entry;
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<String, String> get() throws CursorException
    {
        checkNotClosed();

        return prefetched;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing ChildrenCursor {}", this );
        }

        cursor.close();

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing ChildrenCursor {}", this );
        }

        cursor.close( cause );

        super.close( cause );
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "ChildrenCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#parent<" ).append( parentId ).append( "> :\n" );

        sb.append( cursor.toString( tabs + "  " ) );

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