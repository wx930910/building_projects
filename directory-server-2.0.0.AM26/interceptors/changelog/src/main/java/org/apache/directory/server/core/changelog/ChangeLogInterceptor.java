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
package org.apache.directory.server.core.changelog;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.ChangeType;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifRevertor;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.entry.ServerEntryUtils;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.shared.SchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor which intercepts write operations to the directory and
 * logs them with the server's ChangeLog service.
 * Note: Adding/deleting a tag is not recorded as a change
 */
public class ChangeLogInterceptor extends BaseInterceptor
{
    /** for debugging */
    private static final Logger LOG = LoggerFactory.getLogger( ChangeLogInterceptor.class );

    /** used to ignore modify operations to tombstone entries */
    private AttributeType entryDeleted;

    /** the changelog service to log changes to */
    private ChangeLog changeLog;

    /** OID of the 'rev' attribute used in changeLogEvent and tag objectclasses */
    private static final String REV_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.47";


    /**
     * Creates a new instance of a ChangeLogInterceptor.
     */
    public ChangeLogInterceptor()
    {
        super( InterceptorEnum.CHANGE_LOG_INTERCEPTOR );
    }


    // -----------------------------------------------------------------------
    // Overridden init() and destroy() methods
    // -----------------------------------------------------------------------
    /**
     * The init method will initialize the local variables and load the
     * entryDeleted AttributeType.
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        changeLog = directoryService.getChangeLog();
        entryDeleted = directoryService.getSchemaManager()
            .getAttributeType( ApacheSchemaConstants.ENTRY_DELETED_AT_OID );
    }


    // -----------------------------------------------------------------------
    // Overridden (only change inducing) intercepted methods
    // -----------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        next( addContext );

        if ( !changeLog.isEnabled() )
        {
            return;
        }

        Entry addEntry = addContext.getEntry();

        // we don't want to record addition of a tag as a change
        if ( addEntry.get( REV_AT_OID ) != null )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.Add );
        forward.setDn( addContext.getDn() );

        for ( Attribute attribute : addEntry.getAttributes() )
        {
            AttributeType attributeType = attribute.getAttributeType();
            forward.addAttribute( addEntry.get( attributeType ).clone() );
        }

        LdifEntry reverse = LdifRevertor.reverseAdd( addContext.getDn() );
        addContext.setChangeLogEvent( changeLog.log( getPrincipal( addContext ), forward, reverse ) );
    }


    /**
     * The delete operation has to be stored with a way to restore the deleted element.
     * There is no way to do that but reading the entry and dump it into the LOG.
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        // @todo make sure we're not putting in operational attributes that cannot be user modified
        // must save the entry if change log is enabled
        Entry serverEntry = null;

        if ( changeLog.isEnabled() )
        {
            serverEntry = getAttributes( deleteContext );
        }

        next( deleteContext );

        if ( !changeLog.isEnabled() )
        {
            return;
        }

        // we don't want to record deleting a tag as a change
        if ( serverEntry.get( REV_AT_OID ) != null )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.Delete );
        forward.setDn( deleteContext.getDn() );

        Entry reverseEntry = new DefaultEntry( serverEntry.getDn() );

        boolean isCollectiveSubentry = serverEntry.hasObjectClass( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC );

        for ( Attribute attribute : serverEntry )
        {
            // filter collective attributes, they can't be added by the revert operation
            AttributeType at = schemaManager.lookupAttributeTypeRegistry( attribute.getId() );

            if ( !at.isCollective() || isCollectiveSubentry )
            {
                reverseEntry.add( attribute.clone() );
            }
        }

        LdifEntry reverse = LdifRevertor.reverseDel( deleteContext.getDn(), reverseEntry );
        deleteContext.setChangeLogEvent( changeLog.log( getPrincipal( deleteContext ), forward, reverse ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        Entry serverEntry = null;
        Modification modification = ServerEntryUtils.getModificationItem( modifyContext.getModItems(), entryDeleted );
        boolean isDelete = modification != null;

        if ( !isDelete && ( changeLog.isEnabled() ) )
        {
            // @todo make sure we're not putting in operational attributes that cannot be user modified
            serverEntry = getAttributes( modifyContext );
        }

        // Duplicate modifications so that the reverse does not contain the operational attributes
        List<Modification> clonedMods = new ArrayList<>();

        for ( Modification mod : modifyContext.getModItems() )
        {
            clonedMods.add( mod.clone() );
        }

        // Call the next interceptor
        next( modifyContext );

        // @TODO: needs big consideration!!!
        // NOTE: perhaps we need to log this as a system operation that cannot and should not be reapplied?
        if ( isDelete 
            || !changeLog.isEnabled()

            // if there are no modifications due to stripping out bogus non-
            // existing attributes then we will have no modification items and
            // should ignore not this without registering it with the changelog

            || modifyContext.getModItems().isEmpty() )
        {
            if ( isDelete )
            {
                LOG.debug( "Bypassing changelog on modify of entryDeleted attribute." );
            }

            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.Modify );
        forward.setDn( modifyContext.getDn() );

        List<Modification> mods = new ArrayList<>( clonedMods.size() );

        for ( Modification modItem : clonedMods )
        {
            // TODO: handle correctly http://issues.apache.org/jira/browse/DIRSERVER-1198
            mods.add( modItem );

            forward.addModification( modItem );
        }

        Entry clientEntry = new DefaultEntry( serverEntry.getDn() );

        for ( Attribute attribute : serverEntry )
        {
            clientEntry.add( attribute.clone() );
        }

        LdifEntry reverse = LdifRevertor.reverseModify(
            modifyContext.getDn(),
            mods,
            clientEntry );

        modifyContext.setChangeLogEvent( changeLog.log( getPrincipal( modifyContext ), forward, reverse ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        next( moveContext );

        if ( !changeLog.isEnabled() )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.ModDn );
        forward.setDn( moveContext.getDn() );
        forward.setNewSuperior( moveContext.getNewSuperior().getName() );

        LdifEntry reverse = LdifRevertor.reverseMove( moveContext.getNewSuperior(), moveContext.getDn() );
        moveContext.setChangeLogEvent( changeLog.log( getPrincipal( moveContext ), forward, reverse ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Entry serverEntry = null;

        if ( changeLog.isEnabled() )
        {
            // @todo make sure we're not putting in operational attributes that cannot be user modified
            serverEntry = moveAndRenameContext.getOriginalEntry();
        }

        next( moveAndRenameContext );

        if ( !changeLog.isEnabled() )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.ModDn );
        forward.setDn( moveAndRenameContext.getDn() );
        forward.setDeleteOldRdn( moveAndRenameContext.getDeleteOldRdn() );
        forward.setNewRdn( moveAndRenameContext.getNewRdn().getName() );
        forward.setNewSuperior( moveAndRenameContext.getNewSuperiorDn().getName() );

        List<LdifEntry> reverses = LdifRevertor.reverseMoveAndRename(
            serverEntry, moveAndRenameContext.getNewSuperiorDn(), moveAndRenameContext.getNewRdn(), false );

        if ( moveAndRenameContext.isReferralIgnored() )
        {
            forward.addControl( new ManageDsaITImpl() );
            LdifEntry reversedEntry = reverses.get( 0 );
            reversedEntry.addControl( new ManageDsaITImpl() );
        }

        moveAndRenameContext
            .setChangeLogEvent( changeLog.log( getPrincipal( moveAndRenameContext ), forward, reverses ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Entry serverEntry = null;

        if ( renameContext.getEntry() != null )
        {
            serverEntry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getOriginalEntry();
        }

        next( renameContext );

        // After this point, the entry has been modified. The cloned entry contains
        // the modified entry, the originalEntry has changed

        if ( !changeLog.isEnabled() )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.ModRdn );
        forward.setDn( renameContext.getDn() );
        forward.setNewRdn( renameContext.getNewRdn().getName() );
        forward.setDeleteOldRdn( renameContext.getDeleteOldRdn() );

        List<LdifEntry> reverses = LdifRevertor.reverseRename(
            serverEntry, renameContext.getNewRdn(), renameContext.getDeleteOldRdn() );

        renameContext.setChangeLogEvent( changeLog.log( getPrincipal( renameContext ), forward, reverses ) );
    }


    /**
     * Gets attributes required for modifications.
     *
     * @param dn the dn of the entry to get
     * @return the entry's attributes (may be immutable if the schema subentry)
     * @throws Exception on error accessing the entry's attributes
     */
    private Entry getAttributes( OperationContext opContext ) throws LdapException
    {
        Dn dn = opContext.getDn();
        Entry serverEntry;

        // @todo make sure we're not putting in operational attributes that cannot be user modified
        if ( dn.equals( ServerDNConstants.CN_SCHEMA_DN ) )
        {
            return SchemaService.getSubschemaEntryCloned( directoryService );
        }
        else
        {
            CoreSession session = opContext.getSession();
            LookupOperationContext lookupContext = new LookupOperationContext( session, dn, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            lookupContext.setPartition( opContext.getPartition() );
            lookupContext.setTransaction( opContext.getTransaction() );
            
            serverEntry = directoryService.getPartitionNexus().lookup( lookupContext );
        }

        return serverEntry;
    }
}
