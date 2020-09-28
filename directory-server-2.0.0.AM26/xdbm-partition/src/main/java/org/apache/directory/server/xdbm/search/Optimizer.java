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


import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.server.core.api.partition.PartitionTxn;


/**
 * An optimizer applies heuristics to determine best execution path to a search
 * filter based on scan counts within database indices.  It annotates the nodes
 * of an expression subtree by setting a "count" key in the node.  Its goal is
 * to annotate nodes with counts to indicate which nodes to iterate over thereby
 * minimizing the number cycles in a search.  The SearchEngine relies on these
 * count markers to determine the appropriate path.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Optimizer
{
    /**
     * Annotates the expression node tree for optimized traversal metrics.
     *
     * @param partitionTxn The transaction to use
     * @param node the root of the expression node tree
     * @return The computed value for this node
     * @throws LdapException if there are failures while optimizing
     */
    Long annotate( PartitionTxn partitionTxn, ExprNode node ) throws LdapException;
}
