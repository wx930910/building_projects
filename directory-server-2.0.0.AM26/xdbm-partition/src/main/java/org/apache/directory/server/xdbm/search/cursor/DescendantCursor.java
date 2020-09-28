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
import java.util.ArrayDeque;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Rdn;
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
 * We use the Rdn index to fetch all the descendants of a given entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DescendantCursor extends AbstractIndexCursor<String>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** Error message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_719 );

    /** The entry database/store */
    private final Store db;

    /** The prefetched element */
    private IndexEntry prefetched;

    /** The current Cursor over the entries in the scope of the search base */
    private Cursor<IndexEntry<ParentIdAndRdn, String>> currentCursor;

    /** The current Parent ID */
    private String currentParentId;

    /** The stack of cursors used to process the depth-first traversal */
    private ArrayDeque<Cursor> cursorStack;

    /** The stack of parentIds used to process the depth-first traversal */
    private ArrayDeque<String> parentIdStack;

    /** The initial entry ID we are looking descendants for */
    private String baseId;

    /** A flag to tell that we are in the top level cursor or not */
    private boolean topLevel;

    protected static final boolean TOP_LEVEL = true;
    protected static final boolean INNER = false;


    /**
     * Creates a Cursor over entries satisfying one level scope criteria.
     *
     * @param partitionTxn The transaction to use
     * @param db the entry store
     * @param baseId The base ID
     * @param parentId The parent ID
     * @param cursor The wrapped cursor
     */
    public DescendantCursor( PartitionTxn partitionTxn, Store db, String baseId, String parentId,
            Cursor<IndexEntry<ParentIdAndRdn, String>> cursor )
    {
        this( partitionTxn, db, baseId, parentId, cursor, TOP_LEVEL );
    }


    /**
     * Creates a Cursor over entries satisfying one level scope criteria.
     *
     * @param partitionTxn The transaction to use
     * @param db the entry store
     * @param baseId The base ID
     * @param parentId The parent ID
     * @param cursor The wrapped cursor
     * @param topLevel If we are at the top level
     */
    public DescendantCursor( PartitionTxn partitionTxn, Store db, String baseId, String parentId,
            Cursor<IndexEntry<ParentIdAndRdn, String>> cursor, boolean topLevel )
    {
        this.db = db;
        currentParentId = parentId;
        currentCursor = cursor;
        cursorStack = new ArrayDeque();
        parentIdStack = new ArrayDeque();
        this.baseId = baseId;
        this.topLevel = topLevel;
        this.partitionTxn = partitionTxn;

        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating ChildrenCursor {}", this );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void afterLast() throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean first() throws LdapException, CursorException
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    @Override
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

        boolean hasPrevious = currentCursor.previous();

        if ( hasPrevious )
        {
            IndexEntry entry = currentCursor.get();

            if ( ( ( ParentIdAndRdn ) entry.getTuple().getKey() ).getParentId().equals( currentParentId ) )
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
        boolean finished = false;

        while ( !finished )
        {
            boolean hasNext = currentCursor.next();

            // We will use a depth first approach. The alternative (Breadth-first) would be
            // too memory consuming.
            // The idea is to use a ChildrenCursor each time we have an entry with chidren,
            // and process recursively.
            if ( hasNext )
            {
                IndexEntry cursorEntry = currentCursor.get();
                ParentIdAndRdn parentIdAndRdn = ( ( ParentIdAndRdn ) ( cursorEntry.getKey() ) );

                // Check that we aren't out of the cursor's limit
                if ( !parentIdAndRdn.getParentId().equals( currentParentId ) )
                {
                    // Ok, we went too far. Unstack the cursor and return
                    finished = cursorStack.isEmpty();

                    if ( !finished )
                    {
                        try
                        {
                            currentCursor.close();
                        }
                        catch ( IOException ioe )
                        {
                            throw new LdapException( ioe.getMessage(), ioe );
                        }

                        currentCursor = cursorStack.pop();
                        currentParentId = parentIdStack.pop();
                    }

                    // And continue...
                }
                else
                {
                    // We have a candidate, it will be returned.
                    if ( topLevel )
                    {
                        prefetched = new IndexEntry();
                        prefetched.setId( cursorEntry.getId() );
                        prefetched.setKey( baseId );
                    }
                    else
                    {
                        prefetched = cursorEntry;
                    }

                    // Check if the current entry has children or not.
                    if ( parentIdAndRdn.getNbDescendants() > 0 )
                    {
                        String newParentId = ( String ) cursorEntry.getId();

                        // Yes, then create a new cursor and go down one level
                        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = db.getRdnIndex().forwardCursor( partitionTxn );

                        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<>();
                        startingPos.setKey( new ParentIdAndRdn( newParentId, ( Rdn[] ) null ) );
                        cursor.before( startingPos );

                        cursorStack.push( currentCursor );
                        parentIdStack.push( currentParentId );

                        currentCursor = cursor;
                        currentParentId = newParentId;
                    }

                    return true;
                }
            }
            else
            {
                // The current cursor has been exhausted. Get back to the parent's cursor.
                finished = cursorStack.isEmpty();

                if ( !finished )
                {
                    try
                    {
                        currentCursor.close();
                    }
                    catch ( IOException ioe )
                    {
                        throw new LdapException( ioe.getMessage(), ioe );
                    }

                    currentCursor = cursorStack.pop();
                    currentParentId = parentIdStack.pop();
                }
                // and continue...
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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

        // Close the cursors stored in the stack, if we have some
        for ( Object cursor : cursorStack )
        {
            ( ( Cursor<IndexEntry<?, ?>> ) cursor ).close();
        }

        // And finally, close the current cursor
        currentCursor.close();

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

        // Close the cursors stored in the stack, if we have some
        for ( Object cursor : cursorStack )
        {
            ( ( Cursor<IndexEntry<?, ?>> ) cursor ).close( cause );
        }

        // And finally, close the current cursor
        currentCursor.close( cause );

        super.close( cause );
    }


    /**
     * Dumps the cursors
     */
    private String dumpCursors( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        for ( Object cursor : cursorStack )
        {
            sb.append( ( ( Cursor<IndexEntry<ParentIdAndRdn, String>> ) cursor ).toString( tabs + "  " ) );
            sb.append( "\n" );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "DescendantCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#baseId<" ).append( baseId );
        sb.append( ", " ).append( db ).append( "> :\n" );

        sb.append( dumpCursors( tabs + "  " ) );

        if ( currentCursor != null )
        {
            sb.append( tabs + "  <current>\n" );
            sb.append( currentCursor.toString( tabs + "    " ) );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}