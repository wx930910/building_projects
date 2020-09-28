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
package org.apache.directory.server.core.api.interceptor;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
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
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.PartitionNexus;


/**
 * A easy-to-use implementation of {@link Interceptor}.  All methods are
 * implemented to pass the flow of control to next interceptor by defaults.
 * Please override the methods you have concern in.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class BaseInterceptor implements Interceptor
{
    /** The interceptor's name. Default to the class name */
    private String name;

    /** A reference to the DirectoryService instance */
    protected DirectoryService directoryService;

    /** A reference to the SchemaManager instance */
    protected SchemaManager schemaManager;

    /** The DN factory */
    protected DnFactory dnFactory;

    /** set of operational attribute types used for representing the password policy state of a user entry */
    protected static final Set<AttributeType> PWD_POLICY_STATE_ATTRIBUTE_TYPES = new HashSet<>();

    /**
     * The final interceptor which acts as a proxy in charge to dialog with the nexus partition.
     */
    private final Interceptor finalInterceptor = new Interceptor()
    {
        private PartitionNexus nexus;


        public String getName()
        {
            return "FINAL";
        }


        public void init( DirectoryService directoryService )
        {
            this.nexus = directoryService.getPartitionNexus();
        }


        public void destroy()
        {
            // unused
        }


        /**
         * {@inheritDoc}
         */
        public void add( AddOperationContext addContext ) throws LdapException
        {
            nexus.add( addContext );
        }


        /**
         * {@inheritDoc}
         */
        public void bind( BindOperationContext bindContext ) throws LdapException
        {
            // Do nothing here : there is no support for the Bind operation in Partition
        }


        /**
         * {@inheritDoc}
         */
        public boolean compare( CompareOperationContext compareContext ) throws LdapException
        {
            return nexus.compare( compareContext );
        }


        /**
         * {@inheritDoc}
         */
        public void delete( DeleteOperationContext deleteContext ) throws LdapException
        {
            nexus.delete( deleteContext );
        }


        /**
         * {@inheritDoc}
         */
        public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
        {
            return nexus.getRootDse( getRootDseContext );
        }


        /**
         * {@inheritDoc}
         */
        public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
        {
            return nexus.hasEntry( hasEntryContext );
        }


        /**
         * {@inheritDoc}
         */
        public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
        {
            return nexus.lookup( lookupContext );
        }


        /**
         * {@inheritDoc}
         */
        public void modify( ModifyOperationContext modifyContext ) throws LdapException
        {
            nexus.modify( modifyContext );
        }


        /**
         * {@inheritDoc}
         */
        public void move( MoveOperationContext moveContext ) throws LdapException
        {
            nexus.move( moveContext );
        }


        /**
         * {@inheritDoc}
         */
        public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
        {
            nexus.moveAndRename( moveAndRenameContext );
        }


        /**
         * {@inheritDoc}
         */
        public void rename( RenameOperationContext renameContext ) throws LdapException
        {
            nexus.rename( renameContext );
        }


        /**
         * {@inheritDoc}
         */
        public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
        {
            return nexus.search( searchContext );
        }


        /**
         * {@inheritDoc}
         */
        public void unbind( UnbindOperationContext unbindContext ) throws LdapException
        {
            nexus.unbind( unbindContext );
        }
    };


    /**
     * default interceptor name is its class, preventing accidental duplication of interceptors by naming
     * instances differently
     * @return (default, class name) interceptor name
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns {@link LdapPrincipal} of current context.
     * 
     * @param opContext TODO
     * @return the authenticated principal
     */
    public static LdapPrincipal getPrincipal( OperationContext opContext )
    {
        return opContext.getSession().getEffectivePrincipal();
    }


    /**
     * Creates a new instance with a default name : the class name itself.
     */
    protected BaseInterceptor()
    {
        name = getClass().getSimpleName();
    }


    /**
     * Creates a new instance with a given name.
     * 
     * @param name the Interceptor name
     */
    protected BaseInterceptor( String name )
    {
        this.name = name;
    }


    /**
     * Creates a new instance with a given name.
     * 
     * @param interceptor the Interceptor type
     */
    protected BaseInterceptor( InterceptorEnum interceptor )
    {
        this.name = interceptor.getName();
    }


    /**
     * This method does nothing by default.
     * 
     * @param directoryService The DirectoryService instance
     * @throws LdapException If the initialization failed
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        // Initialize the fields that will be used by all the interceptors
        this.directoryService = directoryService;
        schemaManager = directoryService.getSchemaManager();
        dnFactory = directoryService.getDnFactory();

        finalInterceptor.init( directoryService );
    }


    /**
     * This method does nothing by default.
     */
    public void destroy()
    {
    }


    /**
     * Computes the next interceptor to call for a given operation. If we find none,
     * we return the proxy to the nexus.
     * 
     * @param operationContext The operation context
     * @return The next interceptor in the list for this operation
     */
    protected Interceptor getNextInterceptor( OperationContext operationContext )
    {
        String currentInterceptor = operationContext.getNextInterceptor();

        if ( currentInterceptor.equals( "FINAL" ) )
        {
            return finalInterceptor;
        }

        return directoryService.getInterceptor( currentInterceptor );
    }


    // ------------------------------------------------------------------------
    // Interceptor's Invoke Method
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        // Do nothing
    }


    /**
     * Calls the next interceptor for the add operation.
     * 
     * @param addContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( AddOperationContext addContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( addContext );

        interceptor.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        // Do nothing
    }


    /**
     * Calls the next interceptor for the bind operation.
     * 
     * @param bindContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( BindOperationContext bindContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( bindContext );

        interceptor.bind( bindContext );
    }


    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        // Return false in any case
        return false;
    }


    /**
     * Calls the next interceptor for the compare operation.
     * 
     * @param compareContext The context in which we are executing this operation
     * @return a boolean indicating if the comparison is successfull
     * @throws LdapException If something went wrong
     */
    protected final boolean next( CompareOperationContext compareContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( compareContext );

        return interceptor.compare( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // Do nothing
    }


    /**
     * Calls the next interceptor for the delete operation.
     * 
     * @param deleteContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( DeleteOperationContext deleteContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( deleteContext );

        interceptor.delete( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        // Nothing to do
        return null;
    }


    /**
     * Calls the next interceptor for the getRootDse operation.
     * 
     * @param getRootDseContext The context in which we are executing this operation
     * @return the rootDSE
     * @throws LdapException If something went wrong
     */
    protected final Entry next( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( getRootDseContext );

        return interceptor.getRootDse( getRootDseContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        // Return false in any case
        return false;
    }


    /**
     * Calls the next interceptor for the hasEntry operation.
     * 
     * @param hasEntryContext The context in which we are executing this operation
     * @return a boolean indicating if the entry exists on the server
     * @throws LdapException If something went wrong
     */
    protected final boolean next( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( hasEntryContext );

        return interceptor.hasEntry( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        return next( lookupContext );
    }


    /**
     * Calls the next interceptor for the lookup operation.
     * 
     * @param lookupContext The context in which we are executing this operation
     * @return the Entry containing the found entry
     * @throws LdapException If something went wrong
     */
    protected final Entry next( LookupOperationContext lookupContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( lookupContext );

        return interceptor.lookup( lookupContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // Nothing to do
    }


    /**
     * Calls the next interceptor for the modify operation.
     * 
     * @param modifyContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( ModifyOperationContext modifyContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( modifyContext );

        interceptor.modify( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        // Do nothing
    }


    /**
     * Calls the next interceptor for the move operation.
     * 
     * @param moveContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( MoveOperationContext moveContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( moveContext );

        interceptor.move( moveContext );
    }


    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        // Do nothing
    }


    /**
     * Calls the next interceptor for the moveAndRename operation.
     * 
     * @param moveAndRenameContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( moveAndRenameContext );

        interceptor.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        // Nothing to do
    }


    /**
     * Calls the next interceptor for the rename operation.
     * 
     * @param renameContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( RenameOperationContext renameContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( renameContext );

        interceptor.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        return null;
    }


    /**
     * Calls the next interceptor for the search operation.
     * 
     * @param searchContext The context in which we are executing this operation
     * @return the cursor containing the found entries
     * @throws LdapException If something went wrong
     */
    protected final EntryFilteringCursor next( SearchOperationContext searchContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( searchContext );

        return interceptor.search( searchContext );
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        // Nothing to do
    }


    /**
     * Compute the next interceptor for the unbind operation.
     * 
     * @param unbindContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( UnbindOperationContext unbindContext ) throws LdapException
    {
        Interceptor interceptor = getNextInterceptor( unbindContext );

        interceptor.unbind( unbindContext );
    }
}
