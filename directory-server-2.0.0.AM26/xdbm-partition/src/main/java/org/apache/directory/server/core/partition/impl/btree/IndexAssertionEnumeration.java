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


import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.server.xdbm.IndexEntry;


/**
 * A prefetching NamingEnumeration over an underlying NamingEnumeration which 
 * determines if a element should be returned based on a Assertion.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexAssertionEnumeration implements NamingEnumeration<IndexEntry>
{
    /** The prefetched candidate */
    private final IndexEntry prefetched = new IndexEntry();
    /** The returned candidate */
    private final IndexEntry candidate = new IndexEntry();
    /** The iteration cursor */
    private final NamingEnumeration<IndexEntry> underlying;
    /** LUT used to avoid returning duplicates */
    private final Map<Object, Object> candidates;
    /** */
    private final IndexAssertion assertion;
    /** */
    private final boolean checkDups;
    /** */
    private boolean hasMore = true;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    public IndexAssertionEnumeration( NamingEnumeration<IndexEntry> underlying, IndexAssertion assertion )
        throws NamingException
    {
        this.underlying = underlying;
        candidates = null;
        this.assertion = assertion;
        checkDups = false;
        prefetch();
    }


    public IndexAssertionEnumeration( NamingEnumeration<IndexEntry> underlying, IndexAssertion assertion,
        boolean enableDupCheck ) throws NamingException
    {
        this.underlying = underlying;
        candidates = new HashMap<>();
        this.assertion = assertion;
        checkDups = enableDupCheck;
        prefetch();
    }


    // ------------------------------------------------------------------------
    // Enumeration Method Implementations
    // ------------------------------------------------------------------------

    /**
     * @see java.util.Enumeration#nextElement()
     */
    public IndexEntry nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException e )
        {
            throw new NoSuchElementException();
        }
    }


    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return hasMore;
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Method Implementations
    // ------------------------------------------------------------------------

    /**
     * @see javax.naming.NamingEnumeration#next()
     */
    public IndexEntry next() throws NamingException
    {
        candidate.copy( prefetched );
        prefetch();
        return candidate;
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return hasMore;
    }


    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close() throws NamingException
    {
        hasMore = false;
        underlying.close();
    }


    // ------------------------------------------------------------------------
    // Private and Protected Methods
    // ------------------------------------------------------------------------

    private void prefetch() throws NamingException
    {
        IndexEntry rec = null;

        /*
         * Scan underlying Cursor until we arrive at the next valid candidate
         * if the cursor is exhuasted we clean up after completing the loop
         */
        while ( underlying.hasMore() )
        {
            rec = underlying.next();

            // If value is valid then we set it as the next candidate to return
            try
            {
                if ( assertion.assertCandidate( rec ) )
                {
                    if ( checkDups )
                    {
                        boolean dup = candidates.containsKey( rec.getId() );

                        if ( dup )
                        {
                            /*
                             * Dup checking is on and candidate is a duplicate that
                             * has already been seen so we need to skip it.
                             */
                            continue;
                        }
                        else
                        {
                            /*
                             * Dup checking is on and the candidate is not in the
                             * dup LUT so we need to set it as the next to return
                             * and add it to the LUT in case we encounter it another
                             * time.
                             */
                            prefetched.copy( rec );
                            candidates.put( rec.getId(), rec.getId() );
                            return;
                        }
                    }

                    prefetched.copy( rec );
                    return;
                }
            }
            catch ( Exception e )
            {
                NamingException ne = new NamingException();
                ne.setRootCause( e );
                throw ne;
            }
        }

        // At this pt the underlying Cursor has been exhausted so we close up
        close();
    }
}
