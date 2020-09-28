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
package org.apache.directory.server.ldap;


import java.util.Set;

import org.apache.directory.api.ldap.model.message.ExtendedRequest;
import org.apache.directory.api.ldap.model.message.ExtendedResponse;


/**
 * An extension (hook) point that enables an implementor to provide his or her
 * own LDAP 'Extended' operation.  
 **
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public interface ExtendedOperationHandler<R extends ExtendedRequest, P extends ExtendedResponse>
{
    /**
     * @return the EXTENSION_OID of the extended request this handler can handle.
     */
    String getOid();


    /**
     * The OIDs of the extensions supported by this handler.  This includes the 
     * request as well as any responses associated with the request.  These OIDs 
     * will be registered with the server to publish them as supportedExtensions.
     * 
     * @return the OIDs supported by this handler.
     */
    Set<String> getExtensionOids();


    /**
     * Handles the specified extended operation.
     * 
     * @param session the session object related with current connection
     * @param req the LDAP Extended operation request
     * 
     * @throws Exception if failed to handle the operation
     */
    void handleExtendedOperation( LdapSession session, R req ) throws Exception;


    /**
     * Sets the LDAP server for this extendedOperation handler.
     * 
     * @param ldapServer the ldap protocol server
     */
    void setLdapServer( LdapServer ldapServer );
}
