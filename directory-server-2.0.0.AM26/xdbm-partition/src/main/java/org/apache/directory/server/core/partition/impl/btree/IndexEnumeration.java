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


import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.xdbm.IndexEntry;


/**
 * A NamingEnumeration over an Index which returns IndexRecords.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexEnumeration<T> implements NamingEnumeration<IndexEntry>
{
    /** */
    private final Pattern re;
    /** */
    private final IndexEntry tmp = new IndexEntry();
    /** */
    private final IndexEntry returned = new IndexEntry();
    /** */
    private final IndexEntry prefetched = new IndexEntry();
    /** */
    private final boolean swapKeyVal;
    /** */
    private final NamingEnumeration<Tuple> underlying;

    /** */
    private boolean hasMore = true;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    public IndexEnumeration( NamingEnumeration<Tuple> list ) throws NamingException
    {
        this( list, false, null );
    }


    public IndexEnumeration( NamingEnumeration<Tuple> list, boolean swapKeyVal ) throws NamingException
    {
        this( list, swapKeyVal, null );
    }


    public IndexEnumeration( NamingEnumeration<Tuple> list, boolean swapKeyVal, Pattern regex ) throws NamingException
    {
        re = regex;
        underlying = list;
        this.swapKeyVal = swapKeyVal;

        if ( !underlying.hasMore() )
        {
            hasMore = false;
            return;
        }

        prefetch();
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Interface Methods 
    // ------------------------------------------------------------------------

    /**
     * @see javax.naming.NamingEnumeration#next()
     */
    public IndexEntry next() throws NamingException
    {
        returned.copy( prefetched );
        prefetch();
        return returned;
    }


    /**
     * @see java.util.Enumeration#nextElement()
     */
    public IndexEntry nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException ne )
        {
            throw new NoSuchElementException();
        }
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore()
    {
        return hasMore;
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
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
    // Private Methods 
    // ------------------------------------------------------------------------

    private void prefetch() throws NamingException
    {
        while ( underlying.hasMore() )
        {
            Tuple tuple = underlying.next();

            if ( swapKeyVal )
            {
                throw new NotImplementedException();
                // tmp.setSwapped( tuple, null );
            }
            else
            {
                tmp.setTuple( tuple );
            }

            // If regex is null just transfer into prefetched from tmp record
            // but if it is not then use it to match.  Successful match shorts
            // while loop.
            if ( null == re || re.matcher( ( String ) tmp.getKey() ).matches() )
            {
                prefetched.copy( tmp );
                return;
            }
        }

        // If we got down here then cursor has been consumed without a match!
        hasMore = false;
    }
}
