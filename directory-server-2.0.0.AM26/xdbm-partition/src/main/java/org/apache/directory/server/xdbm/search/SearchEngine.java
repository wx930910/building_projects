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
package org.apache.directory.server.xdbm.search;


import org.apache.directory.api.ldap.model.constants.JndiPropertyConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.PartitionTxn;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface SearchEngine
{
    /**
     * TODO put this in the right place
     * The alias dereferencing mode key for JNDI providers
     */
    String ALIASMODE_KEY = JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES;
    
    /**
     * TODO put this in the right place
     * The alias dereferencing mode value for JNDI providers
     */
    String ALWAYS = "always";
    
    /**
     * TODO put this in the right place
     * The alias dereferencing mode value for JNDI providers
     */
    String NEVER = "never";
    
    /**
     * TODO put this in the right place
     * The alias dereferencing mode value for JNDI providers
     */
    String FINDING = "finding";
    
    /**
     * TODO put this in the right place
     * The alias dereferencing mode value for JNDI providers
     */
    String SEARCHING = "searching";


    /**
     * Gets the optimizer for this DefaultSearchEngine.
     *
     * @return the optimizer
     */
    Optimizer getOptimizer();


    /**
     * Conducts a search on a database. It returns a set of UUID we found for the 
     * given filter.
     * 
     * @param partitionTxn The transaction to use
     * @param schemaManager The SchemaManager instance
     * @param searchContext the search context
     * @return A set of UUID representing the full result, up to he sizeLimit
     * @throws LdapException if the search fails
     */
    PartitionSearchResult computeResult( PartitionTxn partitionTxn, SchemaManager schemaManager, 
            SearchOperationContext searchContext ) throws LdapException;


    /**
     * Builds an Evaluator for a filter expression.
     * 
     * @param partitionTxn The transaction to use
     * @param filter the filter root AST node
     * @return true if the filter passes the entry, false otherwise
     * @throws LdapException if something goes wrong while accessing the db
     */
    Evaluator<? extends ExprNode> evaluator( PartitionTxn partitionTxn, ExprNode filter ) throws LdapException;
}