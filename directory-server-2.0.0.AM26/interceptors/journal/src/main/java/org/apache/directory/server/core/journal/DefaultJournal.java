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
package org.apache.directory.server.core.journal;


import java.io.IOException;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.journal.JournalStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The default journal implementation. It stores the operation and the
 * associated status (acked or nacked) in a file which will be used to
 * restore the server if it crashes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultJournal implements Journal
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultJournal.class );

    /** Tells if the service is activated or not */
    private boolean enabled;

    /** An instance of the Journal store */
    private JournalStore store;

    /** 
     * A parameter indicating the number of operations stored in a journal
     * before it is rotated. If set to 0, no rotation is done
     */
    private int rotation;


    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws LdapException
    {
        LOG.debug( "Stopping the journal" );

        // We have to release the file, otherwise Windows won't be able
        // to stop the server
        if ( store != null )
        {
            try
            {
                store.destroy();
            }
            catch ( IOException ioe )
            {
                throw new LdapException( ioe.getMessage(), ioe );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public JournalStore getJournalStore()
    {
        return store;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.debug( "Starting the journal" );

        if ( store == null )
        {
            store = new DefaultJournalStore();
        }

        try
        {
            store.init( directoryService );
        }
        catch ( IOException ioe )
        {
            throw new LdapException( ioe.getMessage(), ioe );
        }

        LOG.debug( "The Journal service has been initialized" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void log( LdapPrincipal principal, long revision, LdifEntry entry ) throws LdapException
    {
        store.log( principal, revision, entry );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void ack( long revision )
    {
        store.ack( revision );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void nack( long revision )
    {
        store.nack( revision );
    }


    /**
     * @return the rotation
     */
    @Override
    public int getRotation()
    {
        return rotation;
    }


    /**
     * @param rotation the rotation to set
     */
    @Override
    public void setRotation( int rotation )
    {
        this.rotation = rotation;
    }


    @Override
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    @Override
    public void setJournalStore( JournalStore store )
    {
        this.store = store;
    }
}
