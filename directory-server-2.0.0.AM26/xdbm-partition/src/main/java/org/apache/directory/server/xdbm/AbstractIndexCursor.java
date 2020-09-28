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
package org.apache.directory.server.xdbm;


import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.partition.PartitionTxn;


/**
 * An abstract Cursor used by the index cursors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractIndexCursor<V> extends AbstractCursor<IndexEntry<V, String>>
{
    /** Tells if there are some element available in the cursor */
    private boolean available = false;
    
    /** A transaction associated with this cursor */
    protected PartitionTxn partitionTxn;

    /** The message used for unsupported operations */
    protected static final String UNSUPPORTED_MSG = "Unsupported operation";


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return available;
    }


    /**
     * Gets the message to return for operations that are not supported
     * 
     * @return The Unsupported message
     */
    protected abstract String getUnsupportedMessage();


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<V, String> element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<V, String> element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    /**
     * {@inheritDoc}
     */
    protected boolean setAvailable( boolean available )
    {
        this.available = available;
        return available;
    }
    


    @Override
    public boolean previous() throws LdapException, CursorException
    {
        return false;
    }


    @Override
    public boolean next() throws LdapException, CursorException
    {
        return false;
    }
}