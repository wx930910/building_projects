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
package org.apache.directory.server.core.api;


import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.util.tree.DnNode;
import org.apache.directory.api.util.TimeProvider;
import org.apache.directory.server.core.api.administrative.AccessControlAdministrativePoint;
import org.apache.directory.server.core.api.administrative.CollectiveAttributeAdministrativePoint;
import org.apache.directory.server.core.api.administrative.SubschemaAdministrativePoint;
import org.apache.directory.server.core.api.administrative.TriggerExecutionAdministrativePoint;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.entry.ServerEntryFactory;
import org.apache.directory.server.core.api.event.EventService;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.api.subtree.SubentryCache;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;


/**
 * All the features a DirectroyService instance must implement. This gives a control
 * of the Directory based server. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface DirectoryService extends ServerEntryFactory
{
    String JNDI_KEY = DirectoryService.class.getName();


    /**
     * Reverts the server's state to an earlier revision.  Note that the revsion number
     * still increases to revert back even though the state reverted to is the same.
     * Note that implementations may lock the server from making changes or searching
     * the directory until this operation has completed.
     *
     * @param revision the revision number to revert to
     * @return the new revision reached by applying all changes needed to revert to the
     * original state
     * @throws LdapException if there are problems reverting back to the earlier state
     */
    long revert( long revision ) throws LdapException;


    /**
     * Reverts the server's state to the latest tagged snapshot if one was taken.  If
     * there is no tag a illegal state exception will result.  If the latest revision
     * is not earlier than the current revision (both are same), then no changes were
     * made to the directory to be reverted.  In this case we return the current
     * revision and do nothing logging the fact that we ignored the request to revert.
     *
     * @return the new revision reached by applying all changes needed to revert
     * to the new state or the same version before this call if no revert actually
     * took place
     * @throws LdapException if there are problems reverting back to the earlier state
     */
    long revert() throws LdapException;


    PartitionNexus getPartitionNexus();


    void addPartition( Partition partition ) throws LdapException;


    void removePartition( Partition partition ) throws LdapException;


    /**
     * @return The Directory Service SchemaManager
     */
    SchemaManager getSchemaManager();


    /**
     * @return The LDAP codec service.
     */
    LdapApiService getLdapCodecService();


    /**
     * @return The referral manager
     */
    ReferralManager getReferralManager();


    /**
     * Set the referralManager
     * 
     * @param referralManager The initialized referralManager
     */
    void setReferralManager( ReferralManager referralManager );


    /**
     * @return The schema partition
     */
    SchemaPartition getSchemaPartition();


    /**
     * Set the SchemaPartition
     * @param schemaPartition the SchemaPartition instance
     */
    void setSchemaPartition( SchemaPartition schemaPartition );


    EventService getEventService();


    /**
     * @param eventService The {@link EventService} instance
     */
    void setEventService( EventService eventService );


    /**
     * Starts up this service.
     * 
     * @throws LdapException if failed to start up
     */
    void startup() throws LdapException;


    /**
     * Shuts down this service.
     * 
     * @throws LdapException if failed to shut down
     */
    void shutdown() throws LdapException;


    /**
     * Calls {@link Partition#sync()} for all registered {@link Partition}s.
     * @throws LdapException if synchronization failed
     */
    void sync() throws LdapException;


    /**
     * Returns <tt>true</tt> if this service is started.
     * @return true if the service has started, false otherwise
     */
    boolean isStarted();


    /**
     * @return The Admin session
     */
    CoreSession getAdminSession();


    /**
     * @return Returns the hash mapping the Dn of a subentry to its SubtreeSpecification/types
     **/
    SubentryCache getSubentryCache();


    /**
     * @return Returns the subentry evaluator
     */
    SubtreeEvaluator getEvaluator();


    /**
     * Gets a logical session to perform operations on this DirectoryService
     * as the anonymous user.  This bypasses authentication without
     * propagating a bind operation into the core.
     *
     * @return a logical session as the anonymous user
     * @throws LdapException If we weren't able to get the session
     */
    CoreSession getSession() throws LdapException;


    /**
     * Gets a logical session to perform operations on this DirectoryService
     * as a specific user.  This bypasses authentication without propagating
     * a bind operation into the core.
     *
     * @param principal The Principal
     * @return a logical session as a specific user
     * @throws LdapException If we weren't able to get the session
     */
    CoreSession getSession( LdapPrincipal principal ) throws LdapException;


    /**
     * Gets a logical session to perform operations on this DirectoryService
     * as a specific user with a separate authorization principal.  This
     * bypasses authentication without propagating a bind operation into the
     * core.
     *
     * @param principalDn The principal Dn
     * @param credentials The principal credentials
     * @return a logical session as a specific user
     * @throws LdapException If we weren't able to get the session
     */
    CoreSession getSession( Dn principalDn, byte[] credentials ) throws LdapException;


    /**
     * Gets a logical session to perform operations on this DirectoryService
     * as a specific user with a separate authorization principal.  This
     * bypasses authentication without propagating a bind operation into the
     * core.
     *
     * @param principalDn The principal Dn
     * @param credentials The principal credentials
     * @param saslMechanism The SASL mechanisms
     * @param saslAuthId The SASL authorization ID
     * @return a logical session as a specific user
     * @throws LdapException If we weren't able to get the session
     */
    CoreSession getSession( Dn principalDn, byte[] credentials, String saslMechanism, String saslAuthId )
        throws LdapException;


    /**
     * Set the instance Identifier
     * 
     * @param instanceId The instance identifier
     */
    void setInstanceId( String instanceId );


    /**
     * @return The instance identifier
     */
    String getInstanceId();


    /**
     * Gets the {@link Partition}s used by this DirectoryService.
     *
     * @return the set of partitions used
     */
    Set<? extends Partition> getPartitions();


    /**
     * Sets {@link Partition}s used by this DirectoryService.
     *
     * @param partitions the partitions to used
     */
    void setPartitions( Set<? extends Partition> partitions );


    /**
     * Returns <tt>true</tt> if access control checks are enabled.
     *
     * @return true if access control checks are enabled, false otherwise
     */
    boolean isAccessControlEnabled();


    /**
     * Sets whether to enable basic access control checks or not.
     *
     * @param accessControlEnabled true to enable access control checks, false otherwise
     */
    void setAccessControlEnabled( boolean accessControlEnabled );


    /**
     * Returns <tt>true</tt> if anonymous access is allowed on entries besides the RootDSE.
     * If the access control subsystem is enabled then access to some entries may not be
     * allowed even when full anonymous access is enabled.
     *
     * @return true if anonymous access is allowed on entries besides the RootDSE, false
     * if anonymous access is allowed to all entries.
     */
    boolean isAllowAnonymousAccess();


    /**
     * Returns <tt>true</tt> if the service requires the userPassword attribute
     * to be masked. It's an option in the server.xml file.
     *
     * @return true if the service requires that the userPassword is to be hidden
     */
    boolean isPasswordHidden();


    /**
     * Sets whether the userPassword attribute is readable, or hidden.
     *
     * @param passwordHidden true to enable hide the userPassword attribute, false otherwise
     */
    void setPasswordHidden( boolean passwordHidden );


    /**
     * Sets whether to allow anonymous access to entries other than the RootDSE.  If the
     * access control subsystem is enabled then access to some entries may not be allowed
     * even when full anonymous access is enabled.
     *
     * @param enableAnonymousAccess true to enable anonymous access, false to disable it
     */
    void setAllowAnonymousAccess( boolean enableAnonymousAccess );


    /**
     * Returns interceptors in the server.
     *
     * @return the interceptors in the server.
     */
    List<Interceptor> getInterceptors();


    /**
     * Returns interceptors in the server.
     *
     * @param operation The operation that the interceptors must implement
     * @return the interceptors in the server.
     */
    List<String> getInterceptors( OperationEnum operation );


    /**
     * Sets the interceptors in the server.
     *
     * @param interceptors the interceptors to be used in the server.
     */
    void setInterceptors( List<Interceptor> interceptors );


    /**
     * Add an interceptor in the first position in the interceptor list.
     * 
     * @param interceptor The added interceptor
     * @throws LdapException If the interceptor can't be added
     */
    void addFirst( Interceptor interceptor ) throws LdapException;


    /**
     * Add an interceptor in the last position in the interceptor list.
     * 
     * @param interceptor The added interceptor
     * @throws LdapException If the interceptor can't be added
     */
    void addLast( Interceptor interceptor ) throws LdapException;


    /**
     * Add an interceptor after a given interceptor in the interceptor list.
     * 
     * @param interceptorName The interceptor name to find
     * @param interceptor The added interceptor
     */
    void addAfter( String interceptorName, Interceptor interceptor );


    /**
     * Remove an interceptor from the list of interceptors
     * @param interceptorName The interceptor to remove
     */
    void remove( String interceptorName );


    /**
     * Sets the journal in the server.
     *
     * @param journal the journal to be used in the server.
     */
    void setJournal( Journal journal );


    /**
     * Returns test directory entries({@link org.apache.directory.api.ldap.model.ldif.LdifEntry}) to be loaded while
     * bootstrapping.
     *
     * @return test entries to load during bootstrapping
     */
    List<LdifEntry> getTestEntries();


    /**
     * Sets test directory entries to be loaded while bootstrapping.
     *
     * @param testEntries the test entries to load while bootstrapping
     */
    void setTestEntries( List<? extends LdifEntry> testEntries );


    /**
     * Returns the instance layout which contains the path for various directories
     *
     * @return the InstanceLayout for this directory service.
     */
    InstanceLayout getInstanceLayout();


    /**
     * Sets the InstanceLayout used by the DirectoryService to store the files
     * @param instanceLayout The InstanceLayout to set
     * @throws IOException If the layout could not be created
     */
    void setInstanceLayout( InstanceLayout instanceLayout ) throws IOException;


    /**
     * Sets the shutdown hook flag which controls whether or not this DirectoryService
     * registers a JVM shutdown hook to flush caches and synchronize to disk safely.  This is
     * enabled by default.
     *
     * @param shutdownHookEnabled true to enable the shutdown hook, false to disable
     */
    void setShutdownHookEnabled( boolean shutdownHookEnabled );


    /**
     * Checks to see if this DirectoryService has registered a JVM shutdown hook
     * to flush caches and synchronize to disk safely.  This is enabled by default.
     *
     * @return true if a shutdown hook is registered, false if it is not
     */
    boolean isShutdownHookEnabled();


    void setExitVmOnShutdown( boolean exitVmOnShutdown );


    boolean isExitVmOnShutdown();


    void setSystemPartition( Partition systemPartition );


    Partition getSystemPartition();


    boolean isDenormalizeOpAttrsEnabled();


    void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled );


    /**
     * Gets the ChangeLog service for this DirectoryService used for tracking
     * changes (revisions) to the server and using them to revert the server
     * to earlier revisions.
     *
     * @return the change log service
     */
    ChangeLog getChangeLog();


    /**
     * Gets the Journal service for this DirectoryService used for tracking
     * changes to the server.
     *
     * @return the journal service
     */
    Journal getJournal();


    /**
     * Sets the ChangeLog service for this DirectoryService used for tracking
     * changes (revisions) to the server and using them to revert the server
     * to earlier revisions.
     *
     * @param changeLog the change log service to set
     */
    void setChangeLog( ChangeLog changeLog );


    /**
     * Create a new Entry.
     * 
     * @param ldif the String representing the attributes, in LDIF format
     * @param dn the Dn for this new entry
     * @return The new Entry instance
     */
    Entry newEntry( String ldif, String dn );


    /**
     * Gets the operation manager.
     * 
     * @return the OperationManager instance
     */
    OperationManager getOperationManager();


    /**
     * @return The maximum allowed size for an incoming PDU
     */
    int getMaxPDUSize();


    /**
     * Set the maximum allowed size for an incoming PDU
     * @param maxPDUSize A positive number of bytes for the PDU. A negative or
     * null value will be transformed to {@link Integer#MAX_VALUE}
     */
    void setMaxPDUSize( int maxPDUSize );


    /**
     * Get an Interceptor instance from its name
     * @param interceptorName The interceptor's name for which we want the instance
     * @return the interceptor for the given name
     */
    Interceptor getInterceptor( String interceptorName );


    /**
     * Get a new CSN
     * @return The CSN generated for this directory service
     */
    Csn getCSN();


    /**
     * @return the replicaId
     */
    int getReplicaId();


    /**
     * @param replicaId the replicaId to set
     */
    void setReplicaId( int replicaId );


    /**
     * Associates a SchemaManager to the service
     * 
     * @param schemaManager The SchemaManager to associate
     */
    void setSchemaManager( SchemaManager schemaManager );


    /**
     * the time interval at which the DirectoryService's data is flushed to disk
     * 
     * @param syncPeriodMillis the syncPeriodMillis to set
     */
    void setSyncPeriodMillis( long syncPeriodMillis );


    /**
     * @return the syncPeriodMillis
     */
    long getSyncPeriodMillis();


    /**
     * @return The AccessControl AdministrativePoint cache
     */
    DnNode<AccessControlAdministrativePoint> getAccessControlAPCache();


    /**
     * @return The CollectiveAttribute AdministrativePoint cache
     */
    DnNode<CollectiveAttributeAdministrativePoint> getCollectiveAttributeAPCache();


    /**
     * @return The Subschema AdministrativePoint cache
     */
    DnNode<SubschemaAdministrativePoint> getSubschemaAPCache();


    /**
     * @return The TriggerExecution AdministrativePoint cache
     */
    DnNode<TriggerExecutionAdministrativePoint> getTriggerExecutionAPCache();


    /**
     * @return true if the password policy is enabled, false otherwise
     */
    boolean isPwdPolicyEnabled();


    /**
     * Gets the Dn factory.
     *
     * @return the Dn factory
     */
    DnFactory getDnFactory();


    /**
     * Sets the Dn factory.
     * 
     * @param dnFactory The Dn factory to use
     */
    void setDnFactory( DnFactory dnFactory );


    /**
     * Gets the {@link AttributeTypeProvider}.
     * 
     * @return the {@link AttributeTypeProvider}
     */
    AttributeTypeProvider getAtProvider();


    /**
     * Gets the {@link ObjectClassProvider}.
     * 
     * @return the {@link ObjectClassProvider}
     */
    ObjectClassProvider getOcProvider();


    /**
     * Gets the time provider.
     * 
     * @return the time provider
     */
    TimeProvider getTimeProvider();


    /**
     * Sets the time provider.
     * 
     * @param timeProvider the time provider
     */
    void setTimeProvider( TimeProvider timeProvider );
}
