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
package org.apache.directory.server.protocol.shared.store;


import java.io.File;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;


/**
 * A filter interface for the LDIF loader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface LdifLoadFilter
{
    /**
     * Filters entries loaded from LDIF files by a LdifFileLoader.
     *
     * @param file the file being loaded
     * @param dn the distinguished name of the entry being loaded
     * @param entry the entry attributes within the LDIF file
     * @param coreSession session to be used for loading the entry into the DIT
     * @return true if the entry will be created in the DIT, false if it is to be skipped
     * @throws LdapException If the filtering failed
     */
    boolean filter( File file, Dn dn, Entry entry, CoreSession coreSession ) throws LdapException;
}
