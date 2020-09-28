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
package org.apache.directory.server.core.authz;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NoPermissionException;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.shared.partition.DefaultPartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that controls access to {@link DefaultPartitionNexus}.
 * If a user tries to perform any operations that requires
 * permission he or she doesn't have, {@link NoPermissionException} will be
 * thrown and therefore the current invocation chain will terminate.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultAuthorizationInterceptor extends BaseInterceptor
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultAuthorizationInterceptor.class );

    /** the base distinguished {@link Name} for the admin system */
    private Dn adminSystemDn;

    /** the base distinguished {@link Name} for all groups */
    private Dn groupsBaseDn;

    /** the base distinguished {@link Name} for all users */
    private Dn usersBaseDn;

    /** the distinguished {@link Name} for the administrator group */
    private Dn adminGroupDn;

    private Set<String> administrators = new HashSet<>( 2 );

    private PartitionNexus nexus;

    /**
     * the search result filter to use for collective attribute injection
     */
    private class DefaultAuthorizationSearchFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        public boolean accept( SearchOperationContext operation, Entry entry ) throws LdapException
        {
            return DefaultAuthorizationInterceptor.this.isSearchable( operation, entry );
        }


        /**
         * {@inheritDoc}
         */
        public String toString( String tabs )
        {
            return tabs + "DefaultAuthorizationSearchFilter";
        }
    }


    /**
     * Creates a new instance of DefaultAuthorizationInterceptor.
     */
    public DefaultAuthorizationInterceptor()
    {
        super( InterceptorEnum.DEFAULT_AUTHORIZATION_INTERCEPTOR );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        nexus = directoryService.getPartitionNexus();

        adminSystemDn = dnFactory.create( ServerDNConstants.ADMIN_SYSTEM_DN );

        groupsBaseDn = dnFactory.create( ServerDNConstants.GROUPS_SYSTEM_DN );

        usersBaseDn = dnFactory.create( ServerDNConstants.USERS_SYSTEM_DN );

        adminGroupDn = dnFactory.create( ServerDNConstants.ADMINISTRATORS_GROUP_DN );

        loadAdministrators( directoryService );
    }


    private void loadAdministrators( DirectoryService directoryService ) throws LdapException
    {
        // read in the administrators and cache their normalized names
        Set<String> newAdministrators = new HashSet<>( 2 );
        CoreSession adminSession = directoryService.getAdminSession();
        Partition partition = nexus.getPartition( adminGroupDn );
        Entry adminGroup;
        
        LookupOperationContext lookupContext = new LookupOperationContext( adminSession, adminGroupDn );
        lookupContext.setPartition( partition );

        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        { 
            lookupContext.setTransaction( partitionTxn );
            adminGroup = nexus.lookup( lookupContext );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        if ( adminGroup == null )
        {
            return;
        }

        Attribute uniqueMember = adminGroup.get( directoryService.getAtProvider().getUniqueMember() );

        for ( Value value : uniqueMember )
        {
            Dn memberDn = dnFactory.create( value.getString() );
            newAdministrators.add( memberDn.getNormName() );
        }

        administrators = newAdministrators;
    }


    // Note:
    //    Lookup, search and list operations need to be handled using a filter
    // and so we need access to the filter service.
    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( deleteContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            next( deleteContext );
            return;
        }

        Dn dn = deleteContext.getDn();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_12 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.equals( adminGroupDn ) )
        {
            String msg = I18n.err( I18n.ERR_13 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        Dn principalDn = getPrincipal( deleteContext ).getDn();

        if ( dn.equals( adminSystemDn ) )
        {
            String msg = I18n.err( I18n.ERR_14, principalDn.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.size() > 2 && !isAnAdministrator( principalDn ) )
        {
            if ( dn.isDescendantOf( adminSystemDn ) )
            {
                String msg = I18n.err( I18n.ERR_15, principalDn.getName(), dn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.isDescendantOf( groupsBaseDn ) )
            {
                String msg = I18n.err( I18n.ERR_16, principalDn.getName(), dn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.isDescendantOf( usersBaseDn ) )
            {
                String msg = I18n.err( I18n.ERR_16, principalDn.getName(), dn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }
        }

        next( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        CoreSession session = lookupContext.getSession();
        Entry entry = next( lookupContext );

        if ( session.getDirectoryService().isAccessControlEnabled() )
        {
            return entry;
        }

        protectLookUp( session.getEffectivePrincipal().getDn(), lookupContext.getDn() );

        return entry;
    }


    // ------------------------------------------------------------------------
    // Entry Modification Operations
    // ------------------------------------------------------------------------
    /**
     * This policy needs to be really tight too because some attributes may take
     * part in giving the user permissions to protected resources.  We do not want
     * users to self access these resources.  As far as we're concerned no one but
     * the admin needs access.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( !modifyContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            Dn dn = modifyContext.getDn();

            protectModifyAlterations( modifyContext, dn );
            next( modifyContext );

            // update administrators if we change administrators group
            if ( dn.getNormName().equals( adminGroupDn.getNormName() ) )
            {
                loadAdministrators( modifyContext.getSession().getDirectoryService() );
            }
        }
        else
        {
            next( modifyContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( !moveContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            protectDnAlterations( moveContext, moveContext.getDn() );
        }

        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( !moveAndRenameContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            protectDnAlterations( moveAndRenameContext, moveAndRenameContext.getDn() );
        }

        next( moveAndRenameContext );
    }


    // ------------------------------------------------------------------------
    // Dn altering operations are a no no for any user entry.  Basically here
    // are the rules of conduct to follow:
    //
    //  o No user should have the ability to move or rename their entry
    //  o Only the administrator can move or rename non-admin user entries
    //  o The administrator entry cannot be moved or renamed by anyone
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        if ( !renameContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            protectDnAlterations( renameContext, renameContext.getDn() );
        }

        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );

        if ( searchContext.getSession().getDirectoryService().isAccessControlEnabled() )
        {
            return cursor;
        }

        cursor.addEntryFilter( new DefaultAuthorizationSearchFilter() );

        return cursor;
    }


    private boolean isTheAdministrator( Dn dn )
    {
        return dn.getNormName().equals( adminSystemDn.getNormName() );
    }


    private boolean isAnAdministrator( Dn dn )
    {
        return isTheAdministrator( dn ) || administrators.contains( dn.getNormName() );
    }


    private void protectModifyAlterations( OperationContext opCtx, Dn dn ) throws LdapException
    {
        Dn principalDn = getPrincipal( opCtx ).getDn();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_17 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( !isAnAdministrator( principalDn ) )
        {
            // allow self modifications
            if ( dn.equals( getPrincipal( opCtx ).getDn() ) )
            {
                return;
            }

            if ( dn.equals( adminSystemDn ) )
            {
                String msg = I18n.err( I18n.ERR_18, principalDn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.size() > 2 )
            {
                if ( dn.isDescendantOf( adminSystemDn ) )
                {
                    String msg = I18n.err( I18n.ERR_19, principalDn.getName(), dn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }

                if ( dn.isDescendantOf( groupsBaseDn ) )
                {
                    String msg = I18n.err( I18n.ERR_20, principalDn.getName(), dn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }

                if ( dn.isDescendantOf( usersBaseDn ) )
                {
                    String msg = I18n.err( I18n.ERR_20, principalDn.getName(), dn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
            }
        }
    }


    private void protectDnAlterations( OperationContext opCtx, Dn dn ) throws LdapException
    {
        Dn principalDn = getPrincipal( opCtx ).getDn();

        if ( dn.isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_234 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( dn.equals( adminGroupDn ) )
        {
            String msg = I18n.err( I18n.ERR_21 );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( isTheAdministrator( dn ) )
        {
            String msg = I18n.err( I18n.ERR_22, principalDn.getName(), dn.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }

        if ( ( dn.size() > 2 ) && !isAnAdministrator( principalDn ) )
        {
            if ( dn.isDescendantOf( adminSystemDn ) )
            {
                String msg = I18n.err( I18n.ERR_23, principalDn.getName(), dn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.isDescendantOf( groupsBaseDn ) )
            {
                String msg = I18n.err( I18n.ERR_24, principalDn.getName(), dn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }

            if ( dn.isDescendantOf( usersBaseDn ) )
            {
                String msg = I18n.err( I18n.ERR_24, principalDn.getName(), dn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }
        }
    }


    private void protectLookUp( Dn principalDn, Dn normalizedDn ) throws LdapException
    {
        if ( !isAnAdministrator( principalDn ) )
        {
            if ( normalizedDn.size() > 2 )
            {
                if ( normalizedDn.isDescendantOf( adminSystemDn ) )
                {
                    // allow for self reads
                    if ( normalizedDn.equals( principalDn ) )
                    {
                        return;
                    }

                    String msg = I18n.err( I18n.ERR_25, normalizedDn.getName(), principalDn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }

                if ( normalizedDn.isDescendantOf( groupsBaseDn ) || normalizedDn.isDescendantOf( usersBaseDn ) )
                {
                    // allow for self reads
                    if ( normalizedDn.equals( principalDn ) )
                    {
                        return;
                    }

                    String msg = I18n.err( I18n.ERR_26, normalizedDn.getName(), principalDn.getName() );
                    LOG.error( msg );
                    throw new LdapNoPermissionException( msg );
                }
            }

            if ( isTheAdministrator( normalizedDn ) )
            {
                // allow for self reads
                if ( normalizedDn.equals( principalDn ) )
                {
                    return;
                }

                String msg = I18n.err( I18n.ERR_27, principalDn.getName() );
                LOG.error( msg );
                throw new LdapNoPermissionException( msg );
            }
        }
    }


    // False positive, we want to keep the comment
    private boolean isSearchable( OperationContext opContext, Entry entry ) throws LdapException
    {
        Dn principalDn = opContext.getSession().getEffectivePrincipal().getDn();
        Dn dn = entry.getDn();
        
        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( schemaManager, dn );
        }

        // Admin users gets full access to all entries
        if ( isAnAdministrator( principalDn ) )
        {
            return true;
        }

        // Users reading their own entries should be allowed to see all
        boolean isSelfRead = dn.equals( principalDn );

        if ( isSelfRead )
        {
            return true;
        }

        // Block off reads to anything under ou=users and ou=groups if not a self read
        if ( dn.size() >= 2 )
        {
            // stuff this if in here instead of up in outer if to prevent
            // constant needless reexecution for all entries in other depths

            if ( dn.isDescendantOf( adminSystemDn ) || dn.isDescendantOf( groupsBaseDn )
                || dn.isDescendantOf( usersBaseDn ) )
            {
                return false;
            }
        }

        // Non-admin users cannot read the admin entry
        return !isTheAdministrator( dn );
    }
}
