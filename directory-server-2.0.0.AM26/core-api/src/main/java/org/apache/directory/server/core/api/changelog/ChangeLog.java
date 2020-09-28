/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.changelog;


import java.util.List;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;


/**
 * A facade for the change log subsystem.  It exposes the functionality
 * needed to interact with the facility to query for changes, take snapshots,
 * and revert the server to an earlier revision.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ChangeLog
{
    /**
     * Checks whether or not the change log has been enabled to track changes.
     *
     * @return true if the change log is tracking changes, false otherwise
     */
    boolean isEnabled();


    /**
     * Enable or disable the ChangeLog service
     * @param enabled true to enable the service, flase to disable it
     */
    void setEnabled( boolean enabled );


    /**
     * @return The underlying storage
     */
    ChangeLogStore getChangeLogStore();


    /**
     * Set the underlying storage
     * @param store The storage
     */
    void setChangeLogStore( ChangeLogStore store );


    /**
     * Gets the current revision for the server.
     *
     * @return the current revision
     * @throws LdapException if there is a problem accessing this information
     */
    long getCurrentRevision() throws LdapException;


    /**
     * Records a change as a forward LDIF, a reverse change to revert the change and
     * the authorized principal triggering the revertable change event.
     *
     * @param principal the authorized LDAP principal triggering the change
     * @param forward LDIF of the change going to the next state
     * @param reverse LDIF (anti-operation): the change required to revert this change
     * @return the new revision reached after having applied the forward LDIF
     * @throws LdapException if there are problems logging the change
     */
    ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, LdifEntry reverse ) throws LdapException;


    /**
     * Records a change as a forward LDIF, some reverse changes to revert the change and
     * the authorized principal triggering the revertable change event.
     *
     * @param principal the authorized LDAP principal triggering the change
     * @param forward LDIF of the change going to the next state
     * @param reverses LDIF (anti-operation): the changes required to revert this change
     * @return the new revision reached after having applied the forward LDIF
     * @throws LdapException if there are problems logging the change
     */
    ChangeLogEvent log( LdapPrincipal principal, LdifEntry forward, List<LdifEntry> reverses ) throws LdapException;


    /**
     * Returns whether or not this ChangeLogService supports searching for changes.
     *
     * @return true if log searching is supported, false otherwise
     */
    boolean isLogSearchSupported();


    /**
     * Returns whether or not this ChangeLogService supports searching for snapshot tags.
     *
     * @return true if snapshot tag searching is supported, false otherwise
     */
    boolean isTagSearchSupported();


    /**
     * Returns whether or not this ChangeLogService stores snapshot tags.
     *
     * @return true if this ChangeLogService supports tag storage, false otherwise
     */
    boolean isTagStorageSupported();


    /**
     * Gets the change log query engine which would be used to ask questions
     * about what changed, when, how and by whom.  It may not be supported by
     * all implementations.  Some implementations may simply log changes without
     * direct access to those changes from within the server.
     *
     * @return the change log query engine
     * @throws UnsupportedOperationException if the change log is not searchable
     */
    ChangeLogSearchEngine getChangeLogSearchEngine();


    /**
     * Gets the tag search engine used to query the snapshots taken.  If this ChangeLogService
     * does not support a taggable and searchable store then an UnsupportedOperationException
     * will result.
     *
     * @return the snapshot query engine for this store
     * @throws UnsupportedOperationException if the tag searching is not supported
     */
    TagSearchEngine getTagSearchEngine();


    /**
     * Creates a tag for a snapshot of the server in a specific state at a revision.
     * If the ChangeLog has a TaggableChangeLogStore then the tag is stored.  If it
     * does not then it's up to callers to track this tag since it will not be stored
     * by this service.
     *
     * @param revision the revision to tag the snapshot
     * @return the Tag associated with the revision
     * @throws Exception if there is a problem taking a tag
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    Tag tag( long revision ) throws Exception;


    /**
     * Creates a tag for a snapshot of the server in a specific state at a revision.
     * If the ChangeLog has a TaggableChangeLogStore then the tag is stored.  If it
     * does not then it's up to callers to track this tag since it will not be stored
     * by this service.
     *
     * @param revision the revision to tag the snapshot
     * @param description some information about what the snapshot tag represents
     * @return the Tag associated with the revision
     * @throws Exception if there is a problem taking a tag
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    Tag tag( long revision, String description ) throws Exception;


    /**
     * Creates a snapshot of the server at the current revision.
     *
     * @param description some information about what the snapshot tag represents
     * @return the revision at which the tag is created
     * @throws Exception if there is a problem taking a tag
     */
    Tag tag( String description ) throws Exception;


    /**
     * Creates a snapshot of the server at the current revision.
     *
     * @return the revision at which the tag is created
     * @throws Exception if there is a problem taking a tag
     */
    Tag tag() throws Exception;


    /**
     * @return The latest tag
     * @throws LdapException if there is a problem taking the latest tag
     */
    Tag getLatest() throws LdapException;


    /**
     * Initialize the ChangeLog system.
     * 
     * @param service The associated DirectoryService
     * @throws LdapException If the initialization failed
     */
    void init( DirectoryService service ) throws LdapException;


    /**
     * Flush the changes to disk
     * @throws LdapException If the flush failed
     */
    void sync() throws LdapException;


    /**
     * Destroy the changeLog
     * 
     * @throws LdapException If the destroy failed 
     */
    void destroy() throws LdapException;


    /**
     * Exposes the contents of ChangeLog to clients if set to true. Default setting is false.
     *
     * @param exposed true to expose the contents, false to not expose.
     */
    void setExposed( boolean exposed );


    /**
     * @return true if the changeLog system is visible by clients
     */
    boolean isExposed();


    /**
     * The prefix of the partition. Default value is <i>ou=changelog</i>.
     *
     * @param suffix suffix value to be set for the changelog partition
     */
    void setPartitionSuffix( String suffix );


    /**
     * The name of the revisions container under the partition. Default value is ou=revisions 
     *
     * @param revContainerName the name of the revisions container
     */
    void setRevisionsContainerName( String revContainerName );


    /**
     * The name of the tags container under the partition. Default value is ou=tags 
     *
     * @param tagContainerName the name of the revisions container
     */
    void setTagsContainerName( String tagContainerName );
}
