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
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.PrepareString;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.evaluator.SubstringEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor traversing candidates matching a Substring assertion expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringCursor extends AbstractIndexCursor<String>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_725 );
    private final boolean hasIndex;
    private final Cursor<IndexEntry<String, String>> wrapped;
    private final SubstringEvaluator evaluator;
    private final IndexEntry<String, String> indexEntry = new IndexEntry<>();


    /**
     * Creates a new instance of an SubstringCursor
     * 
     * @param partitionTxn The transaction to use
     * @param store The store
     * @param substringEvaluator The SubstringEvaluator
     * @throws LdapException If the creation failed
     * @throws IndexNotFoundException If the index was not found
     */
    @SuppressWarnings("unchecked")
    public SubstringCursor( PartitionTxn partitionTxn, Store store, final SubstringEvaluator substringEvaluator )
        throws LdapException, IndexNotFoundException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating SubstringCursor {}", this );
        }

        evaluator = substringEvaluator;
        this.partitionTxn = partitionTxn;
        hasIndex = store.hasIndexOn( evaluator.getExpression().getAttributeType() );

        if ( hasIndex )
        {
            wrapped = ( ( Index<String, String> ) store.getIndex( evaluator.getExpression().getAttributeType() ) )
                .forwardCursor( partitionTxn );
        }
        else
        {
            /*
             * There is no index on the attribute here.  We have no choice but
             * to perform a full table scan but need to leverage an index for the
             * wrapped Cursor.  We know that all entries are listed under
             * the ndn index and so this will enumerate over all entries.  The
             * substringEvaluator is used in an assertion to constrain the
             * result set to only those entries matching the pattern.  The
             * substringEvaluator handles all the details of normalization and
             * knows to use it, when it itself detects the lack of an index on
             * the node's attribute.
             */
            wrapped = new AllEntriesCursor( partitionTxn, store );
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
        
        if ( evaluator.getExpression().getInitial() != null && hasIndex )
        {
            IndexEntry<String, String> beforeFirstIndexEntry = new IndexEntry<>();
            String normalizedKey = evaluator.getExpression().getAttributeType().getEquality().getNormalizer().normalize( 
                evaluator.getExpression().getInitial(), PrepareString.AssertionType.SUBSTRING_INITIAL );
            beforeFirstIndexEntry.setKey( normalizedKey );
            wrapped.before( beforeFirstIndexEntry );
        }
        else
        {
            wrapped.beforeFirst();
        }

        clear();
    }


    private void clear()
    {
        setAvailable( false );
        indexEntry.setEntry( null );
        indexEntry.setId( null );
        indexEntry.setKey( null );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();

        // to keep the cursor always *after* the last matched tuple
        // This fixes an issue if the last matched tuple is also the last record present in the
        // index. In this case the wrapped cursor is positioning on the last tuple instead of positioning after that
        wrapped.afterLast();
        clear();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        beforeFirst();
        return next();
    }


    private boolean evaluateCandidate( PartitionTxn partitionTxn, IndexEntry<String, String> indexEntry ) throws LdapException
    {
        if ( hasIndex )
        {
            String key = indexEntry.getKey();
            return evaluator.getPattern().matcher( key ).matches();
        }
        else
        {
            return evaluator.evaluate( partitionTxn, indexEntry );
        }
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
    @Override
    public boolean previous() throws LdapException, CursorException
    {
        while ( wrapped.previous() )
        {
            checkNotClosed();
            IndexEntry<String, String> entry = wrapped.get();

            if ( evaluateCandidate( partitionTxn, entry ) )
            {
                setAvailable( true );
                this.indexEntry.setId( entry.getId() );
                this.indexEntry.setKey( entry.getKey() );
                this.indexEntry.setEntry( entry.getEntry() );
                return true;
            }
        }

        clear();
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() throws LdapException, CursorException
    {
        while ( wrapped.next() )
        {
            checkNotClosed();
            IndexEntry<String, String> entry = wrapped.get();

            if ( evaluateCandidate( partitionTxn, entry ) )
            {
                setAvailable( true );
                this.indexEntry.setId( entry.getId() );
                this.indexEntry.setKey( entry.getKey() );
                this.indexEntry.setEntry( entry.getEntry() );

                return true;
            }
        }

        clear();
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<String, String> get() throws CursorException
    {
        checkNotClosed();

        if ( available() )
        {
            return indexEntry;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing SubstringCursor {}", this );
        }

        super.close();
        wrapped.close();
        clear();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing SubstringCursor {}", this );
        }

        super.close( cause );
        wrapped.close( cause );
        clear();
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "SubstringCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#index<" ).append( hasIndex ).append( "> :\n" );

        sb.append( tabs + "  >>" ).append( evaluator ).append( '\n' );

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
