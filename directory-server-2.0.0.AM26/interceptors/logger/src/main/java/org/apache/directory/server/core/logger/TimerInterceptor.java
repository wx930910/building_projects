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
package org.apache.directory.server.core.logger;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor used to log times to process each operation.
 * 
 * The way it works is that it gathers the time to process an operation
 * into a global counter, which is logged every 1000 operations (when
 * using the OPERATION_STATS logger). It's also possible to get the time for
 * each single operation if activating the OPERATION_TIME logger.
 * 
 * Thos two loggers must be set to DEBUG.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TimerInterceptor extends BaseInterceptor
{
    /** A aggregating logger */
    private static final Logger OPERATION_STATS = LoggerFactory.getLogger( Loggers.OPERATION_STAT.getName() );

    /** An operation logger */
    private static final Logger OPERATION_TIME = LoggerFactory.getLogger( Loggers.OPERATION_TIME.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG_STATS = OPERATION_STATS.isDebugEnabled();
    private static final boolean IS_DEBUG_TIME = OPERATION_TIME.isDebugEnabled();

    /** Stats for the add operation */
    private static AtomicLong totalAdd = new AtomicLong( 0 );
    private static AtomicInteger nbAddCalls = new AtomicInteger( 0 );

    /** Stats for the bind operation */
    private static AtomicLong totalBind = new AtomicLong( 0 );
    private static AtomicInteger nbBindCalls = new AtomicInteger( 0 );

    /** Stats for the compare operation */
    private static AtomicLong totalCompare = new AtomicLong( 0 );
    private static AtomicInteger nbCompareCalls = new AtomicInteger( 0 );

    /** Stats for the delete operation */
    private static AtomicLong totalDelete = new AtomicLong( 0 );
    private static AtomicInteger nbDeleteCalls = new AtomicInteger( 0 );

    /** Stats for the GetRootDse operation */
    private static AtomicLong totalGetRootDse = new AtomicLong( 0 );
    private static AtomicInteger nbGetRootDseCalls = new AtomicInteger( 0 );

    /** Stats for the HasEntry operation */
    private static AtomicLong totalHasEntry = new AtomicLong( 0 );
    private static AtomicInteger nbHasEntryCalls = new AtomicInteger( 0 );

    /** Stats for the lookup operation */
    private static AtomicLong totalLookup = new AtomicLong( 0 );
    private static AtomicInteger nbLookupCalls = new AtomicInteger( 0 );

    /** Stats for the modify operation */
    private static AtomicLong totalModify = new AtomicLong( 0 );
    private static AtomicInteger nbModifyCalls = new AtomicInteger( 0 );

    /** Stats for the move operation */
    private static AtomicLong totalMove = new AtomicLong( 0 );
    private static AtomicInteger nbMoveCalls = new AtomicInteger( 0 );

    /** Stats for the moveAndRename operation */
    private static AtomicLong totalMoveAndRename = new AtomicLong( 0 );
    private static AtomicInteger nbMoveAndRenameCalls = new AtomicInteger( 0 );

    /** Stats for the rename operation */
    private static AtomicLong totalRename = new AtomicLong( 0 );
    private static AtomicInteger nbRenameCalls = new AtomicInteger( 0 );

    /** Stats for the search operation */
    private static AtomicLong totalSearch = new AtomicLong( 0 );
    private static AtomicInteger nbSearchCalls = new AtomicInteger( 0 );

    /** Stats for the unbind operation */
    private static AtomicLong totalUnbind = new AtomicLong( 0 );
    private static AtomicInteger nbUnbindCalls = new AtomicInteger( 0 );


    /**
     * 
     * Creates a new instance of TimerInterceptor.
     *
     * @param name This interceptor's getName()
     */
    public TimerInterceptor( String name )
    {
        super( name );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( addContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbAddCalls.incrementAndGet();
            totalAdd.getAndAdd( delta );

            if ( nbAddCalls.get() % 1000 == 0 )
            {
                long average = totalAdd.get() / ( nbAddCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average add = {} microseconds, nb adds = {}", getName(), average,
                    nbAddCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta add = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( bindContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbBindCalls.incrementAndGet();
            totalBind.getAndAdd( delta );

            if ( nbBindCalls.get() % 1000 == 0 )
            {
                long average = totalBind.get() / ( nbBindCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average bind = {} microseconds, nb binds = {}", getName(), average,
                    nbBindCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta bind = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        boolean compare = next( compareContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbCompareCalls.incrementAndGet();
            totalCompare.getAndAdd( delta );

            if ( nbCompareCalls.get() % 1000 == 0 )
            {
                long average = totalCompare.get() / ( nbCompareCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average compare = {} microseconds, nb compares = {}", getName(), average,
                    nbCompareCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta compare = {}", getName(), delta );
        }

        return compare;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( deleteContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbDeleteCalls.incrementAndGet();
            totalDelete.getAndAdd( delta );

            if ( nbDeleteCalls.get() % 1000 == 0 )
            {
                long average = totalDelete.get() / ( nbDeleteCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average delete = {} microseconds, nb deletes = {}", getName(), average,
                    nbDeleteCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta delete = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        Entry rootDse = next( getRootDseContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbGetRootDseCalls.incrementAndGet();
            totalGetRootDse.getAndAdd( delta );

            if ( nbGetRootDseCalls.get() % 1000 == 0 )
            {
                long average = totalGetRootDse.get() / ( nbGetRootDseCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average getRootDSE = {} microseconds, nb getRootDSEs = {}", getName(),
                    average, nbGetRootDseCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta getRootDSE = {}", getName(), delta );
        }

        return rootDse;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        boolean hasEntry = next( hasEntryContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbHasEntryCalls.incrementAndGet();
            totalHasEntry.getAndAdd( delta );

            if ( nbHasEntryCalls.get() % 1000 == 0 )
            {
                long average = totalHasEntry.get() / ( nbHasEntryCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average hasEntry = {} microseconds, nb hasEntrys = {}", getName(), average,
                    nbHasEntryCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta hasEntry = {}", getName(), delta );
        }

        return hasEntry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        Entry entry = next( lookupContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbLookupCalls.incrementAndGet();
            totalLookup.getAndAdd( delta );

            if ( nbLookupCalls.get() % 1000 == 0 )
            {
                long average = totalLookup.get() / ( nbLookupCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average lookup = {} microseconds, nb lookups = {}", getName(), average,
                    nbLookupCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta lookup = {}", getName(), delta );
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( modifyContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbModifyCalls.incrementAndGet();
            totalModify.getAndAdd( delta );

            if ( nbModifyCalls.get() % 1000 == 0 )
            {
                long average = totalModify.get() / ( nbModifyCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average modify = {} microseconds, nb modifys = {}", getName(), average,
                    nbModifyCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta modify = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( moveContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbMoveCalls.incrementAndGet();
            totalMove.getAndAdd( delta );

            if ( nbMoveCalls.get() % 1000 == 0 )
            {
                long average = totalMove.get() / ( nbMoveCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average move = {} microseconds, nb moves = {}", getName(), average,
                    nbMoveCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta move = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( moveAndRenameContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbMoveAndRenameCalls.incrementAndGet();
            totalMoveAndRename.getAndAdd( delta );

            if ( nbMoveAndRenameCalls.get() % 1000 == 0 )
            {
                long average = totalMoveAndRename.get() / ( nbMoveAndRenameCalls.get() * 1000 );
                OPERATION_STATS.debug(
                    "{} : Average moveAndRename = {} microseconds, nb moveAndRenames = {}", getName(), average,
                    nbMoveAndRenameCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta moveAndRename = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( renameContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbRenameCalls.incrementAndGet();
            totalRename.getAndAdd( delta );

            if ( nbRenameCalls.get() % 1000 == 0 )
            {
                long average = totalRename.get() / ( nbRenameCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average rename = {} microseconds, nb renames = {}", getName(), average,
                    nbRenameCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta rename = {}", getName(), delta );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        EntryFilteringCursor cursor = next( searchContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbSearchCalls.incrementAndGet();
            totalSearch.getAndAdd( delta );

            if ( nbSearchCalls.get() % 1000 == 0 )
            {
                long average = totalSearch.get() / ( nbSearchCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average search = {} microseconds, nb searches = {}", getName(), average, nbSearchCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta search = {}", getName(), delta );
        }

        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        long t0 = System.nanoTime();
        next( unbindContext );
        long delta = System.nanoTime() - t0;

        if ( IS_DEBUG_STATS )
        {
            nbUnbindCalls.incrementAndGet();
            totalUnbind.getAndAdd( delta );

            if ( nbUnbindCalls.get() % 1000 == 0 )
            {
                long average = totalUnbind.get() / ( nbUnbindCalls.get() * 1000 );
                OPERATION_STATS.debug( "{} : Average unbind = {} microseconds, nb unbinds = {}", getName(), average,
                    nbUnbindCalls.get() );
            }
        }

        if ( IS_DEBUG_TIME )
        {
            OPERATION_TIME.debug( "{} : Delta unbind = {}", getName(), delta );
        }
    }
}
