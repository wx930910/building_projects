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


import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;


/**
 * Interface for entry stores containing a part of the DIB (Directory
 * Information Base).  Partitions are associated with a specific suffix, and
 * all entries contained in the them have the same Dn suffix in common.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Partition
{
    /** root ID common to all partitions */
    String ROOT_ID = new UUID( 0L, 0L ).toString();

    /** Default id used for context entry if context entry doesn't exists */
    String DEFAULT_ID = new UUID( 0L, 1L ).toString();

    /**
     * Start a read transaction
     * 
     * @return a read transaction instance
     */
    PartitionReadTxn beginReadTransaction();

    
    /**
     * Start a write transaction
     * 
     * @return A write transaction instance
     */
    PartitionWriteTxn beginWriteTransaction();
    

    // -----------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------

    /**
     * Gets the unique identifier for this partition.
     *
     * @return the unique identifier for this partition
     */
    String getId();


    /**
     * Sets the unique identifier for this partition.
     *
     * @param id the unique identifier for this partition
     */
    void setId( String id );


    /**
     * Gets the schema manager assigned to this Partition.
     *
     * @return the schema manager
     */
    SchemaManager getSchemaManager();


    /**
     * Sets the schema manager assigned to this Partition.
     *
     * @param schemaManager The SchemaManager instance
     */
    void setSchemaManager( SchemaManager schemaManager );


    // -----------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------

    /**
     * Initializes this partition. <code>isInitialized()</code> will return <tt>true</tt> if
     * <code>doInit()</code> returns without any errors. <code>destroy()</code> is called automatically
     * as a clean-up process if <code>doInit()</code> throws an exception.
     *
     * @throws LdapException if initialization fails in any way
     */
    void initialize() throws LdapException;

    /**
     * Repair this partition. 
     *
     * @throws Exception if repair fails in any way
     */
    void repair() throws Exception;


    /**
     * Gets the normalized suffix as an Dn for this Partition after it has
     * been initialized.  Attempts to get this Dn before initialization
     * throw an IllegalStateException.
     *
     * @return the suffix for this Partition.
     * @throws IllegalStateException if the Partition has not been initialized
     */
    Dn getSuffixDn();


    /**
     * Sets the suffix Dn, must be normalized.
     * 
     * @param suffixDn the new suffix Dn
     * @throws LdapInvalidDnException If the Dn is invalid
     */
    void setSuffixDn( Dn suffixDn ) throws LdapInvalidDnException;


    /**
     * Instructs this Partition to synchronize with it's persistent store, and
     * destroy all held resources, in preparation for a shutdown event.
     * 
     * @param partitionTxn The transaction to use
     * @throws LdapException If we can't destroy the partition
     */
    void destroy( PartitionTxn partitionTxn ) throws LdapException;


    /**
     * Checks to see if this partition is initialized or not.
     * 
     * @return <tt>true</tt> if the partition is initialized, false otherwise
     */
    boolean isInitialized();


    /**
     * Flushes any changes made to this partition now.
     * 
     * @throws LdapException if buffers cannot be flushed to disk
     */
    void sync() throws LdapException;


    /**
     * Deletes a leaf entry from this ContextPartition: non-leaf entries cannot be
     * deleted until this operation has been applied to their children.
     *
     * @param deleteContext the context of the entry to
     * delete from this ContextPartition.
     * @return The delete Entry, if found
     * @throws LdapException if there are any problems
     */
    Entry delete( DeleteOperationContext deleteContext ) throws LdapException;


    /**
     * Adds an entry to this ContextPartition.
     *
     * @param addContext the context used  to add and entry to this ContextPartition
     * @throws LdapException if there are any problems
     */
    void add( AddOperationContext addContext ) throws LdapException;


    /**
     * Modifies an entry by adding, removing or replacing a set of attributes.
     *
     * @param modifyContext The context containing the modification operation
     * to perform on the entry which is one of constants specified by the
     * DirContext interface:
     * <code>ADD_ATTRIBUTE, REMOVE_ATTRIBUTE, REPLACE_ATTRIBUTE</code>.
     * @throws LdapException if there are any problems
     */
    void modify( ModifyOperationContext modifyContext ) throws LdapException;


    /**
     * Conducts a search against this ContextPartition.  Namespace specific
     * parameters for search are contained within the environment using
     * namespace specific keys into the hash.  For example in the LDAP namespace
     * a ContextPartition implementation may look for search Controls using a
     * namespace specific or implementation specific key for the set of LDAP
     * Controls.
     *
     * @param searchContext The context containing the information used by the operation
     * @return a NamingEnumeration containing objects of type
     * @throws LdapException if there are any problems
     */
    EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException;


    /**
     * Looks up an entry by distinguished/absolute name.  This is a simplified
     * version of the search operation used to point read an entry used for
     * convenience.
     * 
     * Depending on the context parameters, we my look for a simple entry,
     * or for a restricted set of attributes for this entry
     *
     * @param lookupContext The context containing the parameters
     * @return an Attributes object representing the entry
     * @throws LdapException if there are any problems
     */
    Entry lookup( LookupOperationContext lookupContext ) throws LdapException;


    /**
     * Fast operation to check and see if a particular entry exists.
     *
     * @param hasEntryContext The context used to pass informations
     * @return true if the entry exists, false if it does not
     * @throws LdapException if there are any problems
     */
    boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException;


    /**
     * Modifies an entry by changing its relative name. Optionally attributes
     * associated with the old relative name can be removed from the entry.
     * This makes sense only in certain namespaces like LDAP and will be ignored
     * if it is irrelevant.
     *
     * @param renameContext the modify Dn context
     * @throws LdapException if there are any problems
     */
    void rename( RenameOperationContext renameContext ) throws LdapException;


    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry.
     *
     * @param moveContext The context containing the DNs to move
     * @throws LdapException if there are any problems
     */
    void move( MoveOperationContext moveContext ) throws LdapException;


    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry and changes the RN of the child entry which can optionally
     * have its old RN attributes removed.  The removal of old RN attributes
     * may not make sense in all namespaces.  If the concept is undefined in a
     * namespace this parameters is ignored.  An example of a namespace where
     * this parameter is significant is the LDAP namespace.
     *
     * @param moveAndRenameContext The context contain all the information about
     * the modifyDN operation
     * @throws LdapException if there are any problems
     */
    void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException;


    /**
     * Represents an unbind operation issued by an authenticated client.  Partitions
     * need not support this operation.  This operation is here to enable those
     * interested in implementing virtual directories with ApacheDS.
     * 
     * @param unbindContext the context used to unbind
     * @throws LdapException if something goes wrong
     */
    void unbind( UnbindOperationContext unbindContext ) throws LdapException;


    /**
     * Dump the requested index to a given stream
     * 
     * @param partitionTxn The transaction to use
     * @param stream The Stream used to dump the index
     * @param name The index to dump to stdout
     * @throws IOException if we can't write the data
     */
    void dumpIndex( PartitionTxn partitionTxn, OutputStream stream, String name ) throws IOException;


    
    /**
     * Get the contextCSN
     * 
     * @param partitionTxn The transaction to use
     * @return the current highest committed CSN value
     */
    String getContextCsn( PartitionTxn partitionTxn );

    
    /**
     * Saves the context CSN value in the context entry of the partition
     * 
     * @param partitionTxn The transaction to use
     * @throws LdapException If the context can't be saved
     */
    void saveContextCsn( PartitionTxn partitionTxn ) throws LdapException;
    
    
    /**
     * Return the number of children and subordinates for a given entry
     *
     * @param partitionTxn The transaction to use
     * @param entry The entry
     * @return The Subordinate instance that contains the values.
     * @throws LdapException If we had an issue while processing the request
     */
    Subordinates getSubordinates( PartitionTxn partitionTxn, Entry entry ) throws LdapException;
}
