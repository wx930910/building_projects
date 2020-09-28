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


/**
 * A ChangeLogStore which allows tagging for tracking server state snapshots.
 * At most one tag per revision can be created.  There is no point to creating
 * more than one tag on a revision in our case for snapshotting server state.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TaggableChangeLogStore extends ChangeLogStore
{
    /**
     * Creates a tag for a snapshot of the server in a specific state at a revision.
     *
     * @param revision the revision to tag the snapshot
     * @return the Tag associated with the revision
     */
    Tag tag( long revision );


    /**
     * Creates a snapshot of the server at the current revision.
     *
     * @return the revision at which the tag is created
     */
    Tag tag();


    /**
     * Creates a snapshot of the server at the current revision with a description
     * of the snapshot tag.
     *
     * @param description a description of the state associate with the tag
     * @return the revision at which the tag is created
     */
    Tag tag( String description );


    /**
     * Gets the latest tag if one was at all taken.
     *
     * @return the last tag to have been created (youngest), or null if no
     * tags have been created
     */
    Tag getLatest();


    /**
     * Removes a Tag created for a given revision.
     *
     * @param revision the revision number that was tagged
     * @return the removed tag, null if there is no Tag present with the given revision
     */
    Tag removeTag( long revision );


    /**
     * Creates a tag with the given description for a snapshot of the server
     * in a specific state at a revision.
     *
     * @param revision the revision number that was tagged
     * @param descrition a description of the state associate with the tag
     * @return the Tag associated with the revision
     */
    Tag tag( long revision, String descrition );
}
