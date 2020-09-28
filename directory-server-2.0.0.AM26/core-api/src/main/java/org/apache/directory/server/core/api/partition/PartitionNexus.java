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
package org.apache.directory.server.core.api.partition;


import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;


/**
 * A root {@link Partition} that contains all other partitions, and
 * routes all operations to the child partition that matches to its base suffixes.
 * It also provides some extended operations such as accessing rootDSE and
 * listing base suffixes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface PartitionNexus extends Partition
{
    /** the admin super user uid */
    String ADMIN_UID = "admin";

    /** the initial admin passwd set on startup */
    String ADMIN_PASSWORD_STRING = "secret";

    byte[] ADMIN_PASSWORD_BYTES = Strings.getBytesUtf8( ADMIN_PASSWORD_STRING );
    

    /**
     * Get's the RootDSE entry for the DSA.
     *
     * @param getRootDseContext The operation context
     * @return the attributes of the RootDSE
     */
    Entry getRootDse( GetRootDseOperationContext getRootDseContext );


    /**
     * Get's the RootDSE value associated with an AttributeType
     * 
     * @param attributeType The attribute type for which we want a value
     * @return the values associated with the given attributeType
     */
    Value getRootDseValue( AttributeType attributeType );


    /**
     * Add a partition to the server.
     * 
     * @param partition The Partition to add
     * @throws LdapException If the addition can't be done
     */
    void addContextPartition( Partition partition ) throws LdapException;


    /**
     * Remove a partition from the server.
     * 
     * @param partitionDn the partition Dn
     * @throws LdapException If the removal can't be done
     */
    void removeContextPartition( String partitionDn ) throws LdapException;


    /**
     * Get's the partition corresponding to a distinguished name.  This
     * name need not be the name of the partition suffix.  When used in
     * conjunction with get suffix this can properly find the partition
     * associated with the Dn.  Make sure to use the normalized Dn.
     * 
     * @param dn the normalized distinguished name to get a partition for
     * @return the partition containing the entry represented by the dn
     * @throws LdapException if there is no partition for the dn
     */
    Partition getPartition( Dn dn ) throws LdapException;


    /**
     * Finds the distinguished name of the suffix that would hold an entry with
     * the supplied distinguished name parameter.  If the Dn argument does not
     * fall under a partition suffix then the empty string Dn is returned.
     *
     * @param dn The Dn we want to find the suffix from
     * @return the suffix portion of dn, or the valid empty string Dn if no
     * naming context was found for dn.
     * @throws LdapException If we can't get the suffix Dn
     */
    Dn getSuffixDn( Dn dn ) throws LdapException;


    /**
     * Gets an iteration over the Name suffixes of the partitions managed by this
     * DefaultPartitionNexus.
     *
     * @return Iteration over ContextPartition suffix names as Names.
     * @throws LdapException if there are any problems
     */
    Set<String> listSuffixes() throws LdapException;


    /**
     * Adds a set of supportedExtension (OID Strings) to the RootDSE.
     * 
     * @param extensionOids a set of OID strings to add to the supportedExtension
     * attribute in the RootDSE
     * @throws LdapException If we cannot register the extensions
     */
    void registerSupportedExtensions( Set<String> extensionOids ) throws LdapException;


    /**
     * Adds a set of supportedSaslMechanisms (OID Strings) to the RootDSE.
     * 
     * @param supportedSaslMechanisms a set of OID strings to add to the supportedSaslMechanisms
     * attribute in the RootDSE
     * @throws LdapException If we cannot fetch the supported SASL mechanism
     */
    void registerSupportedSaslMechanisms( Set<String> supportedSaslMechanisms ) throws LdapException;


    /**
     * The Compare operation
     *
     * @param compareContext The context for the compare operation
     * @return true if the compare operation was successful, false otherwise
     * @throws LdapException If we had an issue during the operation
     */
    boolean compare( CompareOperationContext compareContext ) throws LdapException;
}
