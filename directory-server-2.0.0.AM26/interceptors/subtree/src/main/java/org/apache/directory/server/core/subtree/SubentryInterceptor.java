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
package org.apache.directory.server.core.subtree;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ObjectClassNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.Subentries;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.subtree.AdministrativeRole;
import org.apache.directory.api.ldap.model.subtree.Subentry;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecification;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecificationParser;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
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
import org.apache.directory.server.core.api.subtree.SubentryCache;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Subentry interceptor service which is responsible for filtering
 * out subentries on search operations and injecting operational attributes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubentryInterceptor extends BaseInterceptor
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SubentryInterceptor.class );

    /** the subentry control OID */
    private static final String SUBENTRY_CONTROL = Subentries.OID;

    private Value subentryOC;

    /** The SubTree specification parser instance */
    private SubtreeSpecificationParser ssParser;

    /** A reference to the nexus for direct backend operations */
    private PartitionNexus nexus;

    /** An enum used for the entries update */
    private enum OperationEnum
    {
        ADD,
        REMOVE,
        REPLACE
    }


    /**
     * Creates a new instance of SubentryInterceptor
     */
    public SubentryInterceptor()
    {
        super( InterceptorEnum.SUBENTRY_INTERCEPTOR );
    }

    //-------------------------------------------------------------------------------------------
    // Search filter methods
    //-------------------------------------------------------------------------------------------
    /**
     * SearchResultFilter used to filter out subentries based on objectClass values.
     */
    private class HideSubentriesFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept( SearchOperationContext searchContext, Entry entry ) throws LdapException
        {
            // See if the requested entry is a subentry
            if ( directoryService.getSubentryCache().hasSubentry( entry.getDn() ) )
            {
                return false;
            }

            // see if we can use objectclass if present
            return !entry.contains( directoryService.getAtProvider().getObjectClass(), subentryOC );
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( String tabs )
        {
            return tabs + "HideSubentriesFilter";
        }
    }

    /**
     * SearchResultFilter used to filter out normal entries but shows subentries based on
     * objectClass values.
     */
    private class HideEntriesFilter implements EntryFilter
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept( SearchOperationContext searchContext, Entry entry ) throws LdapException
        {
            // See if the requested entry is a subentry
            if ( directoryService.getSubentryCache().hasSubentry( entry.getDn() ) )
            {
                return true;
            }

            // see if we can use objectclass if present
            return entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.SUBENTRY_OC );
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString( String tabs )
        {
            return tabs + "HideEntriesFilter";
        }
    }


    //-------------------------------------------------------------------------------------------
    // Interceptor initialization
    //-------------------------------------------------------------------------------------------
    /**
     * Initialize the Subentry Interceptor
     *
     * @param directoryService The DirectoryService instance
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        nexus = directoryService.getPartitionNexus();

        ssParser = new SubtreeSpecificationParser( schemaManager );
        AttributeType ocAt = directoryService.getAtProvider().getObjectClass();

        // prepare to find all subentries in all namingContexts
        Set<String> suffixes = nexus.listSuffixes();
        ExprNode filter = new EqualityNode<String>( ocAt, new Value( ocAt, SchemaConstants.SUBENTRY_OC ) );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.SUBTREE_SPECIFICATION_AT, SchemaConstants.OBJECT_CLASS_AT } );

        subentryOC = new Value( ocAt, SchemaConstants.SUBENTRY_OC );

        // search each namingContext for subentries
        for ( String suffix : suffixes )
        {
            CoreSession adminSession = directoryService.getAdminSession();

            Dn suffixDn = dnFactory.create( suffix );
            Partition partition = nexus.getPartition( suffixDn );

            SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, suffixDn, filter,
                controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
            searchOperationContext.setPartition( partition );
            searchOperationContext.setTransaction( partition.beginReadTransaction() );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            // Loop on all the found Subentries, parse the SubtreeSpecification
            // and store the subentry in the subrentry cache
            try
            {
                while ( subentries.next() )
                {
                    Entry subentry = subentries.get();
                    Dn subentryDn = subentry.getDn();

                    String subtree = subentry.get( directoryService.getAtProvider().getSubtreeSpecification() )
                        .getString();
                    SubtreeSpecification ss;

                    try
                    {
                        ss = ssParser.parse( subtree );
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "Failed while parsing subtreeSpecification for {}", subentryDn );
                        continue;
                    }

                    Subentry newSubentry = new Subentry();

                    newSubentry.setAdministrativeRoles( getSubentryAdminRoles( subentry ) );
                    newSubentry.setSubtreeSpecification( ss );

                    directoryService.getSubentryCache().addSubentry( subentryDn, newSubentry );
                }
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }
            finally
            {
                try
                {
                    subentries.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }
    }


    //-------------------------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------------------------
    /**
     * Return the list of AdministrativeRole for a subentry
     */
    private Set<AdministrativeRole> getSubentryAdminRoles( Entry subentry ) throws LdapException
    {
        Set<AdministrativeRole> adminRoles = new HashSet<>();

        Attribute oc = subentry.get( directoryService.getAtProvider().getObjectClass() );

        if ( oc == null )
        {
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, I18n.err( I18n.ERR_305 ) );
        }

        if ( oc.contains( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.AccessControlInnerArea );
        }

        if ( oc.contains( SchemaConstants.SUBSCHEMA_OC ) )
        {
            adminRoles.add( AdministrativeRole.SubSchemaSpecificArea );
        }

        if ( oc.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.CollectiveAttributeSpecificArea );
        }

        if ( oc.contains( ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) )
        {
            adminRoles.add( AdministrativeRole.TriggerExecutionInnerArea );
        }

        return adminRoles;
    }


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param opContext the invocation object to use for determining subentry visibility
     * @return true if subentries should be visible, false otherwise
     */
    private boolean isSubentryVisible( OperationContext opContext )
    {
        if ( !opContext.hasRequestControls() )
        {
            return false;
        }

        // found the subentry request control so we return its value
        if ( opContext.hasRequestControl( SUBENTRY_CONTROL ) )
        {
            Subentries subentries = ( Subentries ) opContext.getRequestControl( SUBENTRY_CONTROL );
            return subentries.isVisible();
        }

        return false;
    }


    /**
     * Update all the entries under an AP adding the
     */
    private void updateEntries( OperationContext opContext, OperationEnum operation, 
        Dn apDn, SubtreeSpecification ss, Dn baseDn, List<Attribute> operationalAttributes ) throws LdapException
    {
        ExprNode filter = ObjectClassNode.OBJECT_CLASS_NODE; // (objectClass=*)
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

        SearchOperationContext searchOperationContext = new SearchOperationContext( opContext.getSession(),
            baseDn, filter, controls );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
        searchOperationContext.setPartition( opContext.getPartition() );
        searchOperationContext.setTransaction( opContext.getTransaction() );

        EntryFilteringCursor subentries = nexus.search( searchOperationContext );

        try
        {
            while ( subentries.next() )
            {
                Entry candidate = subentries.get();
                Dn candidateDn = candidate.getDn();

                if ( directoryService.getEvaluator().evaluate( ss, apDn, candidateDn, candidate ) )
                {
                    List<Modification> modifications = null;

                    switch ( operation )
                    {
                        case ADD:
                            modifications = getOperationalModsForAdd( candidate, operationalAttributes );
                            break;

                        case REMOVE:
                            modifications = getOperationalModsForRemove( opContext.getDn(), candidate );
                            break;

                        case REPLACE:
                            // TODO: why is that commented out???
                            //modifications = getOperationalModsForReplace( subentryDn, candidate );
                            break;

                        default:
                            throw new IllegalArgumentException( "Unexpected operation " + operation );
                    }

                    LOG.debug( "The entry {} has been evaluated to true for subentry {}", candidate.getDn(), opContext.getDn() );
                    ModifyOperationContext modifyContext = new ModifyOperationContext( opContext.getSession(), candidateDn, modifications );
                    modifyContext.setPartition( opContext.getPartition() );
                    modifyContext.setTransaction( opContext.getTransaction() );
                    
                    nexus.modify( modifyContext );
                }
            }

            subentries.close();
        }
        catch ( Exception e )
        {
            throw new LdapOtherException( e.getMessage(), e );
        }
        finally
        {
            try
            {
                subentries.close();
            }
            catch ( Exception e )
            {
                LOG.error( I18n.err( I18n.ERR_168 ), e );
            }
        }
    }


    /**
     * Checks if the given Dn is a namingContext
     */
    private boolean isNamingContext( Dn dn ) throws LdapException
    {
        Dn namingContext = nexus.getSuffixDn( dn );

        return dn.equals( namingContext );
    }


    /**
     * Get the administrativePoint role
     */
    private void checkAdministrativeRole( OperationContext opContext, Dn apDn ) throws LdapException
    {
        CoreSession session = opContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, apDn,
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        lookupContext.setPartition( opContext.getPartition() );
        lookupContext.setTransaction( opContext.getTransaction() );

        Entry administrationPoint = directoryService.getPartitionNexus().lookup( lookupContext );

        // The administrativeRole AT must exist and not be null
        Attribute administrativeRole = administrationPoint.get( directoryService.getAtProvider()
            .getAdministrativeRole() );

        // check that administrativeRole has something valid in it for us
        if ( ( administrativeRole == null ) || ( administrativeRole.size() <= 0 ) )
        {
            LOG.error( "The entry on {} is not an AdministrativePoint", apDn );
            throw new LdapNoSuchAttributeException( I18n.err( I18n.ERR_306, apDn ) );
        }
    }


    /**
     * Get the SubtreeSpecification, parse it and stores it into the subentry
     */
    private void setSubtreeSpecification( Subentry subentry, Entry entry ) throws LdapException
    {
        String subtree = entry.get( directoryService.getAtProvider().getSubtreeSpecification() ).getString();
        SubtreeSpecification ss;

        try
        {
            ss = ssParser.parse( subtree );
        }
        catch ( Exception e )
        {
            String msg = I18n.err( I18n.ERR_307, entry.getDn() );
            LOG.warn( msg );
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
        }

        subentry.setSubtreeSpecification( ss );
    }


    /**
     * Checks to see if an entry being renamed has a descendant that is an
     * administrative point.
     *
     * @param name the name of the entry which is used as the search base
     * @return true if name is an administrative point or one of its descendants
     * are, false otherwise
     * @throws Exception if there are errors while searching the directory
     */
    private boolean hasAdministrativeDescendant( OperationContext opContext, Dn name ) throws LdapException
    {
        ExprNode filter = new PresenceNode( directoryService.getAtProvider().getAdministrativeRole() );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        SearchOperationContext searchOperationContext = new SearchOperationContext( opContext.getSession(), name,
            filter, controls );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
        searchOperationContext.setTransaction( opContext.getTransaction() );
        searchOperationContext.setPartition( opContext.getPartition() );

        EntryFilteringCursor aps = nexus.search( searchOperationContext );

        try
        {
            if ( aps.next() )
            {
                return true;
            }
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage(), e );
        }
        finally
        {
            try
            {
                aps.close();
            }
            catch ( Exception e )
            {
                LOG.error( I18n.err( I18n.ERR_168 ), e );
            }
        }

        return false;
    }


    private List<Modification> getModsOnEntryRdnChange( Dn oldName, Dn newName, Entry entry ) throws LdapException
    {
        List<Modification> modifications = new ArrayList<>();

        /*
         * There are two different situations warranting action.  First if
         * an ss evalutating to true with the old name no longer evalutates
         * to true with the new name.  This would be caused by specific chop
         * exclusions that effect the new name but did not effect the old
         * name. In this case we must remove subentry operational attribute
         * values associated with the dn of that subentry.
         *
         * In the second case an ss selects the entry with the new name when
         * it did not previously with the old name. Again this situation
         * would be caused by chop exclusions. In this case we must add subentry
         * operational attribute values with the dn of this subentry.
         */
        SubentryCache subentryCache = directoryService.getSubentryCache();
        SubtreeEvaluator evaluator = directoryService.getEvaluator();

        for ( Dn subentryDn : subentryCache )
        {
            Dn apDn = subentryDn.getParent();
            SubtreeSpecification ss = subentryCache.getSubentry( subentryDn ).getSubtreeSpecification();
            boolean isOldNameSelected = evaluator.evaluate( ss, apDn, oldName, entry );
            boolean isNewNameSelected = evaluator.evaluate( ss, apDn, newName, entry );

            if ( isOldNameSelected == isNewNameSelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldNameSelected && !isNewNameSelected )
            {
                for ( AttributeType operationalAttribute : directoryService.getAtProvider()
                    .getSubentryOperationalAttributes() )
                {
                    ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
                    Attribute opAttr = entry.get( operationalAttribute );

                    if ( opAttr != null )
                    {
                        opAttr = opAttr.clone();
                        opAttr.remove( subentryDn.getName() );

                        if ( opAttr.size() < 1 )
                        {
                            op = ModificationOperation.REMOVE_ATTRIBUTE;
                        }

                        modifications.add( new DefaultModification( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewNameSelected && !isOldNameSelected )
            {
                for ( AttributeType operationalAttribute : directoryService.getAtProvider()
                    .getSubentryOperationalAttributes() )
                {
                    ModificationOperation op = ModificationOperation.ADD_ATTRIBUTE;
                    Attribute opAttr = new DefaultAttribute( operationalAttribute );
                    opAttr.add( subentryDn.getName() );
                    modifications.add( new DefaultModification( op, opAttr ) );
                }
            }
        }

        return modifications;
    }


    // -----------------------------------------------------------------------
    // Methods dealing with subentry modification
    // -----------------------------------------------------------------------

    private Set<AdministrativeRole> getSubentryTypes( Entry entry, List<Modification> mods ) throws LdapException
    {
        Attribute ocFinalState = entry.get( directoryService.getAtProvider().getObjectClass() ).clone();

        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT )
                || mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT_OID ) )
            {
                switch ( mod.getOperation() )
                {
                    case ADD_ATTRIBUTE:
                        for ( Value value : mod.getAttribute() )
                        {
                            ocFinalState.add( value.getString() );
                        }

                        break;

                    case REMOVE_ATTRIBUTE:
                        for ( Value value : mod.getAttribute() )
                        {
                            ocFinalState.remove( value.getString() );
                        }

                        break;

                    case REPLACE_ATTRIBUTE:
                        ocFinalState = mod.getAttribute();
                        break;

                    default:
                        throw new IllegalArgumentException( "Unexpected modify operatoin " + mod.getOperation() );
                }
            }
        }

        Entry attrs = new DefaultEntry( schemaManager, Dn.ROOT_DSE );
        attrs.put( ocFinalState );
        return getSubentryAdminRoles( attrs );
    }


    /**
     * Update the list of modifications with a modification associated with a specific
     * role, if it's requested.
     */
    private void getOperationalModForReplace( boolean hasRole, AttributeType attributeType, Entry entry, Dn oldDn,
        Dn newDn, List<Modification> modifications ) throws LdapInvalidAttributeValueException
    {
        String oldDnStr = oldDn.getName();
        String newDnStr = newDn.getName();

        if ( hasRole )
        {
            Attribute operational = entry.get( attributeType ).clone();

            if ( operational == null )
            {
                operational = new DefaultAttribute( attributeType, newDnStr );
            }
            else
            {
                operational.remove( oldDnStr );
                operational.add( newDnStr );
            }

            modifications.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, operational ) );
        }
    }


    /**
     * Get the list of modifications to be applied on an entry to inject the operational attributes
     * associated with the administrative roles.
     */
    private List<Modification> getOperationalModsForReplace( Dn oldDn, Dn newDn, Subentry subentry, Entry entry )
        throws LdapException
    {
        List<Modification> modifications = new ArrayList<>();

        getOperationalModForReplace( subentry.isAccessControlAdminRole(), directoryService.getAtProvider()
            .getAccessControlSubentries(), entry, oldDn, newDn, modifications );
        getOperationalModForReplace( subentry.isSchemaAdminRole(), directoryService.getAtProvider()
            .getSubschemaSubentry(), entry, oldDn, newDn, modifications );
        getOperationalModForReplace( subentry.isCollectiveAdminRole(), directoryService.getAtProvider()
            .getCollectiveAttributeSubentries(), entry, oldDn, newDn, modifications );
        getOperationalModForReplace( subentry.isTriggersAdminRole(), directoryService.getAtProvider()
            .getTriggerExecutionSubentries(), entry, oldDn, newDn, modifications );

        return modifications;
    }


    /**
     * Gets the subschema operational attributes to be added to or removed from
     * an entry selected by a subentry's subtreeSpecification.
     */
    private List<Attribute> getSubentryOperationalAttributes( Dn dn, Subentry subentry ) throws LdapException
    {
        List<Attribute> attributes = new ArrayList<>();

        if ( subentry.isAccessControlAdminRole() )
        {
            Attribute accessControlSubentries = new DefaultAttribute( directoryService.getAtProvider()
                .getAccessControlSubentries(), dn.getName() );
            attributes.add( accessControlSubentries );
        }

        if ( subentry.isSchemaAdminRole() )
        {
            Attribute subschemaSubentry = new DefaultAttribute(
                directoryService.getAtProvider().getSubschemaSubentry(), dn.getName() );
            attributes.add( subschemaSubentry );
        }

        if ( subentry.isCollectiveAdminRole() )
        {
            Attribute collectiveAttributeSubentries = new DefaultAttribute( directoryService.getAtProvider()
                .getCollectiveAttributeSubentries(), dn.getName() );
            attributes.add( collectiveAttributeSubentries );
        }

        if ( subentry.isTriggersAdminRole() )
        {
            Attribute tiggerExecutionSubentries = new DefaultAttribute( directoryService.getAtProvider()
                .getTriggerExecutionSubentries(), dn.getName() );
            attributes.add( tiggerExecutionSubentries );
        }

        return attributes;
    }


    /**
     * Calculates the subentry operational attributes to remove from a candidate
     * entry selected by a subtreeSpecification.  When we remove a subentry we
     * must remove the operational attributes in the entries that were once selected
     * by the subtree specification of that subentry.  To do so we must perform
     * a modify operation with the set of modifications to perform.  This method
     * calculates those modifications.
     *
     * @param subentryDn the distinguished name of the subentry
     * @param candidate the candidate entry to removed from the
     * @return the set of modifications required to remove an entry's reference to
     * a subentry
     */
    private List<Modification> getOperationalModsForRemove( Dn subentryDn, Entry candidate ) throws LdapException
    {
        List<Modification> modifications = new ArrayList<>();
        String dn = subentryDn.getName();

        for ( AttributeType operationalAttribute : directoryService.getAtProvider().getSubentryOperationalAttributes() )
        {
            Attribute opAttr = candidate.get( operationalAttribute );

            if ( ( opAttr != null ) && opAttr.contains( dn ) )
            {
                Attribute attr = new DefaultAttribute( operationalAttribute, dn );
                modifications.add( new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE, attr ) );
            }
        }

        return modifications;
    }


    /**
     * Calculates the subentry operational attributes to add or replace from
     * a candidate entry selected by a subtree specification.  When a subentry
     * is added or it's specification is modified some entries must have new
     * operational attributes added to it to point back to the associated
     * subentry.  To do so a modify operation must be performed on entries
     * selected by the subtree specification.  This method calculates the
     * modify operation to be performed on the entry.
     */
    private List<Modification> getOperationalModsForAdd( Entry entry, List<Attribute> operationalAttributes )
        throws LdapException
    {
        List<Modification> modifications = new ArrayList<>();

        for ( Attribute operationalAttribute : operationalAttributes )
        {
            Attribute opAttrInEntry = entry.get( operationalAttribute.getAttributeType() );

            if ( ( opAttrInEntry != null ) && ( opAttrInEntry.size() > 0 ) )
            {
                Attribute newOperationalAttribute = operationalAttribute.clone();

                for ( Value value : opAttrInEntry )
                {
                    newOperationalAttribute.add( value );
                }

                modifications.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    newOperationalAttribute ) );
            }
            else
            {
                modifications
                    .add( new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, operationalAttribute ) );
            }
        }

        return modifications;
    }


    /**
     * Get the list of modification to apply to all the entries
     */
    private List<Modification> getModsOnEntryModification( Dn name, Entry oldEntry, Entry newEntry )
        throws LdapException
    {
        List<Modification> modList = new ArrayList<>();

        for ( Dn subentryDn : directoryService.getSubentryCache() )
        {
            Dn apDn = subentryDn.getParent();
            SubtreeSpecification ss = directoryService.getSubentryCache().getSubentry( subentryDn )
                .getSubtreeSpecification();
            boolean isOldEntrySelected = directoryService.getEvaluator().evaluate( ss, apDn, name, oldEntry );
            boolean isNewEntrySelected = directoryService.getEvaluator().evaluate( ss, apDn, name, newEntry );

            if ( isOldEntrySelected == isNewEntrySelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldEntrySelected && !isNewEntrySelected )
            {
                for ( AttributeType operationalAttribute : directoryService.getAtProvider()
                    .getSubentryOperationalAttributes() )
                {
                    ModificationOperation op = ModificationOperation.REPLACE_ATTRIBUTE;
                    Attribute opAttr = oldEntry.get( operationalAttribute );

                    if ( opAttr != null )
                    {
                        opAttr = opAttr.clone();
                        opAttr.remove( subentryDn.getName() );

                        if ( opAttr.size() < 1 )
                        {
                            op = ModificationOperation.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new DefaultModification( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewEntrySelected && !isOldEntrySelected )
            {
                for ( AttributeType operationalAttribute : directoryService.getAtProvider()
                    .getSubentryOperationalAttributes() )
                {
                    ModificationOperation op = ModificationOperation.ADD_ATTRIBUTE;
                    Attribute opAttr = new DefaultAttribute( operationalAttribute );
                    opAttr.add( subentryDn.getName() );
                    modList.add( new DefaultModification( op, opAttr ) );
                }
            }
        }

        return modList;
    }


    /**
     * Update the Operational Attribute with the reference to the subentry
     */
    private void setOperationalAttribute( Entry entry, Dn subentryDn, AttributeType opAttr ) throws LdapException
    {
        Attribute operational = entry.get( opAttr );

        if ( operational == null )
        {
            operational = new DefaultAttribute( opAttr );
            entry.put( operational );
        }

        operational.add( subentryDn.getName() );
    }


    //-------------------------------------------------------------------------------------------
    // Interceptor API methods
    //-------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Dn dn = addContext.getDn();
        Entry entry = addContext.getEntry();

        // Check if the added entry is a subentry
        if ( entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.SUBENTRY_OC ) )
        {
            // get the name of the administrative point and its administrativeRole attributes
            // The AP must be the parent Dn, but we also have to check that the given Dn
            // is not the rootDSE or a NamingContext
            if ( dn.isRootDse() || isNamingContext( dn ) )
            {
                // Not allowed : we can't get a parent in those cases
                throw new LdapOtherException( "Cannot find an AdministrativePoint for " + dn );
            }

            // Get the administrativePoint role : we must have one immediately
            // upper
            Dn apDn = dn.getParent();
            checkAdministrativeRole( addContext, apDn );

            /* ----------------------------------------------------------------
             * Build the set of operational attributes to be injected into
             * entries that are contained within the subtree represented by this
             * new subentry.  In the process we make sure the proper roles are
             * supported by the administrative point to allow the addition of
             * this new subentry.
             * ----------------------------------------------------------------
             */
            Subentry subentry = new Subentry();
            subentry.setAdministrativeRoles( getSubentryAdminRoles( entry ) );
            List<Attribute> operationalAttributes = getSubentryOperationalAttributes( dn, subentry );

            /* ----------------------------------------------------------------
             * Parse the subtreeSpecification of the subentry and add it to the
             * SubtreeSpecification cache.  If the parse succeeds we continue
             * to add the entry to the DIT.  Thereafter we search out entries
             * to modify the subentry operational attributes of.
             * ----------------------------------------------------------------
             */
            setSubtreeSpecification( subentry, entry );
            directoryService.getSubentryCache().addSubentry( dn, subentry );

            // Now inject the subentry into the backend
            next( addContext );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * while testing each entry returned for inclusion within the
             * subtree of the subentry's subtreeSpecification.  All included
             * entries will have their operational attributes merged with the
             * operational attributes calculated above.
             * ----------------------------------------------------------------
             */
            Dn baseDn = apDn;
            baseDn = baseDn.add( subentry.getSubtreeSpecification().getBase() );

            updateEntries( addContext, OperationEnum.ADD, apDn, subentry.getSubtreeSpecification(),
                baseDn, operationalAttributes );

            // Store the newly modified entry into the context for later use in interceptor
            // just in case
            addContext.setEntry( entry );
        }
        else
        {
            // The added entry is not a Subentry.
            // Nevertheless, we have to check if the entry is added into an AdministrativePoint
            // and is associated with some SubtreeSpecification
            // We brutally check *all* the subentries, as we don't hold a hierarchy
            // of AP
            // TODO : add a hierarchy of subentries
            for ( Dn subentryDn : directoryService.getSubentryCache() )
            {
                Dn apDn = subentryDn.getParent();

                // No need to evaluate the entry if it's not below an AP.
                if ( dn.isDescendantOf( apDn ) )
                {
                    Subentry subentry = directoryService.getSubentryCache().getSubentry( subentryDn );
                    SubtreeSpecification ss = subentry.getSubtreeSpecification();

                    // Now, evaluate the entry wrt the subentry ss
                    // and inject a ref to the subentry if it evaluates to true
                    if ( directoryService.getEvaluator().evaluate( ss, apDn, dn, entry ) )
                    {

                        if ( subentry.isAccessControlAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, directoryService.getAtProvider()
                                .getAccessControlSubentries() );
                        }

                        if ( subentry.isSchemaAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, directoryService.getAtProvider()
                                .getSubschemaSubentry() );
                        }

                        if ( subentry.isCollectiveAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, directoryService.getAtProvider()
                                .getCollectiveAttributeSubentries() );
                        }

                        if ( subentry.isTriggersAdminRole() )
                        {
                            setOperationalAttribute( entry, subentryDn, directoryService.getAtProvider()
                                .getTriggerExecutionSubentries() );
                        }
                    }
                }
            }

            // Now that the entry has been updated with the operational attributes,
            // we can update it into the add context
            addContext.setEntry( entry );

            // Propagate the addition down to the backend.
            next( addContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();
        Entry entry = deleteContext.getEntry();

        // If the entry has a "subentry" Objectclass, we can process the entry.
        // We first remove the re
        if ( entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry removedSubentry = directoryService.getSubentryCache().getSubentry( dn );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * for all entries included by the subtreeSpecification.  Then we
             * check the entry for subentry operational attribute that contain
             * the Dn of the subentry.  These are the subentry operational
             * attributes we remove from the entry in a modify operation.
             * ----------------------------------------------------------------
             */
            Dn apDn = dn.getParent();
            Dn baseDn = apDn;
            baseDn = baseDn.add( removedSubentry.getSubtreeSpecification().getBase() );

            // Remove all the references to this removed subentry from all the selected entries
            updateEntries( deleteContext, OperationEnum.REMOVE, apDn,
                removedSubentry.getSubtreeSpecification(), baseDn, null );

            // Update the cache
            directoryService.getSubentryCache().removeSubentry( dn );

            // Now delete the subentry itself
            next( deleteContext );
        }
        else
        {
            // TODO : deal with AP removal.
            next( deleteContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        Dn dn = modifyContext.getDn();
        List<Modification> modifications = modifyContext.getModItems();

        Entry entry = modifyContext.getEntry();

        // We have three types of modifications :
        // 1) A modification applied on a normal entry
        // 2) A modification done on a subentry (the entry will have a 'subentry' ObjectClass)
        // 3) A modification on a normal entry on whch we add a 'subentry' ObjectClass
        // The third case is a transformation of a normal entry to a subentry. Not sure if it's
        // legal ...

        boolean isSubtreeSpecificationModification = false;
        Modification subtreeMod = null;

        // Find the subtreeSpecification
        for ( Modification mod : modifications )
        {
            if ( mod.getAttribute().getAttributeType()
                .equals( directoryService.getAtProvider().getSubtreeSpecification() ) )
            {
                isSubtreeSpecificationModification = true;
                subtreeMod = mod;
                break;
            }
        }

        boolean containsSubentryOC = entry.contains( directoryService.getAtProvider().getObjectClass(),
            SchemaConstants.SUBENTRY_OC );

        // Check if we have a modified subentry attribute in a Subentry entry
        if ( containsSubentryOC && isSubtreeSpecificationModification )
        {
            Subentry subentry = directoryService.getSubentryCache().removeSubentry( dn );
            SubtreeSpecification ssOld = subentry.getSubtreeSpecification();
            SubtreeSpecification ssNew;

            try
            {
                ssNew = ssParser.parse( subtreeMod.getAttribute().getString() );
            }
            catch ( Exception e )
            {
                String msg = I18n.err( I18n.ERR_71 );
                LOG.error( msg, e );
                throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
            }

            subentry.setSubtreeSpecification( ssNew );
            subentry.setAdministrativeRoles( getSubentryTypes( entry, modifications ) );
            directoryService.getSubentryCache().addSubentry( dn, subentry );

            next( modifyContext );

            // search for all entries selected by the old SS and remove references to subentry
            Dn apName = dn.getParent();
            Dn oldBaseDn = apName;
            oldBaseDn = oldBaseDn.add( ssOld.getBase() );

            ExprNode filter = new PresenceNode( directoryService.getAtProvider().getObjectClass() );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( modifyContext.getSession(),
                oldBaseDn, filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
            searchOperationContext.setPartition( modifyContext.getPartition() );
            searchOperationContext.setTransaction( modifyContext.getTransaction() );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    Dn candidateDn = candidate.getDn();

                    if ( directoryService.getEvaluator().evaluate( ssOld, apName, candidateDn, candidate ) )
                    {
                        ModifyOperationContext newModifyContext = new ModifyOperationContext( modifyContext.getSession(), candidateDn,
                            getOperationalModsForRemove( dn, candidate ) );
                        newModifyContext.setPartition( modifyContext.getPartition() );
                        newModifyContext.setTransaction( modifyContext.getTransaction() );
                        
                        nexus.modify( newModifyContext );
                    }
                }

                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationErrorException( e.getMessage(), e );
            }
            finally
            {
                try
                {
                    subentries.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }

            // search for all selected entries by the new SS and add references to subentry
            subentry = directoryService.getSubentryCache().getSubentry( dn );
            List<Attribute> operationalAttributes = getSubentryOperationalAttributes( dn, subentry );
            Dn newBaseDn = apName;
            newBaseDn = newBaseDn.add( ssNew.getBase() );

            searchOperationContext = new SearchOperationContext( modifyContext.getSession(), newBaseDn, filter,
                controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
            searchOperationContext.setPartition( modifyContext.getPartition() );
            searchOperationContext.setTransaction( modifyContext.getTransaction() );

            subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    Dn candidateDn = candidate.getDn();

                    if ( directoryService.getEvaluator().evaluate( ssNew, apName, candidateDn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( modifyContext.getSession(), candidateDn,
                            getOperationalModsForAdd( candidate, operationalAttributes ) ) );
                    }
                }
                subentries.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationErrorException( e.getMessage(), e );
            }
            finally
            {
                try
                {
                    subentries.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }
        else
        {
            next( modifyContext );

            if ( !containsSubentryOC )
            {
                Entry newEntry = modifyContext.getAlteredEntry();

                List<Modification> subentriesOpAttrMods = getModsOnEntryModification( dn, entry, newEntry );

                if ( !subentriesOpAttrMods.isEmpty() )
                {
                    ModifyOperationContext newModifyContext = new ModifyOperationContext( modifyContext.getSession(), dn, subentriesOpAttrMods );
                    newModifyContext.setPartition( modifyContext.getPartition() );
                    newModifyContext.setTransaction( modifyContext.getTransaction() );
                    nexus.modify( newModifyContext );
                }
            }
        }
    }


    /**
     * The Move operation for a Subentry will deal with different cases :
     * 1) we move a normal entry
     * 2) we move a subentry
     * 3) we move an administrationPoint
     * <p>
     * <u>Case 1 :</u><br>
     * A normal entry (ie, not a subentry or an AP) may be part of some administrative areas.
     * We have to remove the references to the associated areas if the entry gets out of them.<br>
     * This entry can also be moved to some other administrative area, and it should then be
     * updated to point to the associated subentries.
     * <br><br>
     * There is one preliminary condition : If the entry has a descendant which is an
     * Administrative Point, then the move cannot be done.
     * <br><br>
     * <u>Case 2 :</u><br>
     * The subentry has to be moved under a new AP, otherwise this is an error. Once moved,
     * we have to update all the entries selected by the old subtreeSpecification, removing
     * the references to the subentry from all the selected entry, and update the entries
     * selected by the new subtreeSpecification by adding a reference to the subentry into them.
     * <br><br>
     * <u>Case 3 :</u><br>
     *
     *
     * @param moveContext The context containing all the needed informations to proceed
     * @throws LdapException If the move failed
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Dn oldDn = moveContext.getDn();
        Dn newSuperiorDn = moveContext.getNewSuperior();

        Entry entry = moveContext.getOriginalEntry();

        if ( entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.SUBENTRY_OC ) )
        {
            // This is a subentry. Moving a subentry means we have to:
            // o Check that there is a new AP where we move the subentry
            // o Remove the op Attr from all the entry selected by the subentry
            // o Add the op Attr in all the selected entry by the subentry

            // If we move it, we have to check that
            // the new parent is an AP
            checkAdministrativeRole( moveContext, newSuperiorDn );

            Subentry subentry = directoryService.getSubentryCache().removeSubentry( oldDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            Dn apName = oldDn.getParent();
            Dn baseDn = apName;
            baseDn = baseDn.add( ss.getBase() );
            Dn newName = newSuperiorDn;
            newName = newName.add( oldDn.getRdn() );
            
            if ( !newName.isSchemaAware() )
            {
                newName = new Dn( schemaManager, newName );
            }

            directoryService.getSubentryCache().addSubentry( newName, subentry );

            next( moveContext );

            subentry = directoryService.getSubentryCache().getSubentry( newName );

            ExprNode filter = new PresenceNode( directoryService.getAtProvider().getObjectClass() );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( moveContext.getSession(),
                baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
            searchOperationContext.setPartition( moveContext.getPartition() );
            searchOperationContext.setTransaction( moveContext.getTransaction() );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                // Modify all the entries under this subentry
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    Dn dn = candidate.getDn();
                    
                    if ( !dn.isSchemaAware() )
                    {
                        dn = new Dn( schemaManager, dn );
                    }

                    if ( directoryService.getEvaluator().evaluate( ss, apName, dn, candidate ) )
                    {
                        ModifyOperationContext newModifyContext = new ModifyOperationContext( moveContext.getSession(), dn,
                            getOperationalModsForReplace( oldDn, newName, subentry, candidate ) );
                        newModifyContext.setPartition( moveContext.getPartition() );
                        newModifyContext.setTransaction( moveContext.getTransaction() );
                        nexus.modify( newModifyContext );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }
            finally
            {
                try
                {
                    subentries.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }
        else
        {
            // A normal entry. It may be part of a SubtreeSpecifciation. In this
            // case, we have to update the opAttrs (removing old ones and adding the
            // new ones)

            // First, an moved entry which has an AP in one of its descendant
            // can't be moved.
            if ( hasAdministrativeDescendant( moveContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            // Move the entry
            next( moveContext );

            // calculate the new Dn now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Dn newDn = moveContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newDn, entry );

            // Update the entry operational attributes
            if ( !mods.isEmpty() )
            {
                ModifyOperationContext newModifyContext = new ModifyOperationContext( moveContext.getSession(), newDn, mods );
                newModifyContext.setPartition( moveContext.getPartition() );
                newModifyContext.setTransaction( moveContext.getTransaction() );
                nexus.modify( newModifyContext );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Dn oldDn = moveAndRenameContext.getDn();
        Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();

        Entry entry = moveAndRenameContext.getOriginalEntry();

        if ( entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = directoryService.getSubentryCache().removeSubentry( oldDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            Dn apName = oldDn.getParent();
            Dn baseDn = apName;
            baseDn = baseDn.add( ss.getBase() );
            Dn newName = newSuperiorDn.getParent();

            newName = newName.add( moveAndRenameContext.getNewRdn() );
            
            if ( !newName.isSchemaAware() )
            {
                newName = new Dn( schemaManager, newName );
            }

            directoryService.getSubentryCache().addSubentry( newName, subentry );

            next( moveAndRenameContext );

            subentry = directoryService.getSubentryCache().getSubentry( newName );

            ExprNode filter = new PresenceNode( directoryService.getAtProvider().getObjectClass() );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext(
                moveAndRenameContext.getSession(), baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
            searchOperationContext.setPartition( moveAndRenameContext.getPartition() );
            searchOperationContext.setTransaction( moveAndRenameContext.getTransaction() );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    Dn dn = candidate.getDn();
                    
                    if ( !dn.isSchemaAware() )
                    {
                        dn = new Dn( schemaManager, dn );
                    }

                    if ( directoryService.getEvaluator().evaluate( ss, apName, dn, candidate ) )
                    {
                        ModifyOperationContext newModifyContext = new ModifyOperationContext( moveAndRenameContext.getSession(), dn,
                            getOperationalModsForReplace( oldDn, newName, subentry, candidate ) );
                        newModifyContext.setPartition( moveAndRenameContext.getPartition() );
                        newModifyContext.setTransaction( moveAndRenameContext.getTransaction() );
                        nexus.modify( newModifyContext );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }
            finally
            {
                try
                {
                    subentries.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( moveAndRenameContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            next( moveAndRenameContext );

            // calculate the new Dn now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Dn newDn = moveAndRenameContext.getNewDn();
            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newDn, entry );

            if ( !mods.isEmpty() )
            {
                nexus.modify( new ModifyOperationContext( moveAndRenameContext.getSession(), newDn, mods ) );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Dn oldDn = renameContext.getDn();

        Entry entry = ( ( ClonedServerEntry ) renameContext.getEntry() ).getClonedEntry();

        if ( entry.contains( directoryService.getAtProvider().getObjectClass(), SchemaConstants.SUBENTRY_OC ) )
        {
            // @Todo To be reviewed !!!
            Subentry subentry = directoryService.getSubentryCache().removeSubentry( oldDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            Dn apName = oldDn.getParent();
            Dn baseDn = apName;
            baseDn = baseDn.add( ss.getBase() );
            Dn newName = oldDn.getParent();

            newName = newName.add( renameContext.getNewRdn() );

            if ( !newName.isSchemaAware() )
            {
                newName = new Dn( schemaManager, newName );
            }

            directoryService.getSubentryCache().addSubentry( newName, subentry );
            next( renameContext );

            subentry = directoryService.getSubentryCache().getSubentry( newName );
            ExprNode filter = new PresenceNode( directoryService.getAtProvider().getObjectClass() );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            SearchOperationContext searchOperationContext = new SearchOperationContext( renameContext.getSession(),
                baseDn,
                filter, controls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
            searchOperationContext.setPartition( renameContext.getPartition() );
            searchOperationContext.setTransaction( renameContext.getTransaction() );

            EntryFilteringCursor subentries = nexus.search( searchOperationContext );

            try
            {
                while ( subentries.next() )
                {
                    Entry candidate = subentries.get();
                    Dn dn = candidate.getDn();

                    if ( !dn.isSchemaAware() )
                    {
                        dn = new Dn( schemaManager, dn );
                    }

                    if ( directoryService.getEvaluator().evaluate( ss, apName, dn, candidate ) )
                    {
                        nexus.modify( new ModifyOperationContext( renameContext.getSession(), dn,
                            getOperationalModsForReplace(
                                oldDn, newName, subentry, candidate ) ) );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }
            finally
            {
                try
                {
                    subentries.close();
                }
                catch ( Exception e )
                {
                    LOG.error( I18n.err( I18n.ERR_168 ), e );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( renameContext, oldDn ) )
            {
                String msg = I18n.err( I18n.ERR_308 );
                LOG.warn( msg );
                throw new LdapSchemaViolationException( ResultCodeEnum.NOT_ALLOWED_ON_RDN, msg );
            }

            next( renameContext );

            // calculate the new Dn now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            Dn newName = renameContext.getNewDn();

            List<Modification> mods = getModsOnEntryRdnChange( oldDn, newName, entry );

            if ( !mods.isEmpty() )
            {
                ModifyOperationContext newModifyContext = new ModifyOperationContext( renameContext.getSession(), newName, mods );
                newModifyContext.setPartition( renameContext.getPartition() );
                newModifyContext.setTransaction( renameContext.getTransaction() );
                nexus.modify( newModifyContext );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );

        // object scope searches by default return subentries
        if ( searchContext.getScope() == SearchScope.OBJECT )
        {
            return cursor;
        }

        // DO NOT hide subentries for replication operations
        if ( searchContext.isSyncreplSearch() )
        {
            return cursor;
        }

        // for subtree and one level scope we filter
        if ( !isSubentryVisible( searchContext ) )
        {
            cursor.addEntryFilter( new HideSubentriesFilter() );
        }
        else
        {
            cursor.addEntryFilter( new HideEntriesFilter() );
        }

        return cursor;
    }
}
