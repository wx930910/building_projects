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

package org.apache.directory.server.ldap.replication.provider;


import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;


/**
 * Interface of a replication request handler in a provider/master.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ReplicationRequestHandler
{
    /**
     * initializes the replication provider
     *
     * @param server the LdapServer instance
     */
    void start( LdapServer server );


    /**
     * stops the replication provider
     */
    void stop();


    /**
     * A method to be used by any RFC 4533 compatible provider implementation 
     *
     * @param session the LdapSession instance
     * @param req the SearchRequest with the SyncRequest control
     * @throws LdapException If the syncrepl request wasn't handled properly
     */
    void handleSyncRequest( LdapSession session, SearchRequest req ) throws LdapException;
}
