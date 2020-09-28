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
package org.apache.directory.server.core.api.event;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.name.Dn;


/**
 * Tests if an entry is eligible for return by evaluating a filter expression on
 * the candidate.  The evaluation can proceed by applying the filter on the 
 * attributes of the entry itself or indices can be used for rapid evaluation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Evaluator
{
    /**
     * Evaluates a candidate to determine if a filter expression selects it.
     * 
     * @param refinement the filter expression to evaluate on the candidate
     * @param dn the normalized distinguished name of the entry being tested
     * @param entry the entry to evaluate
     * @return <tt>true</tt> if the filter selects the candidate false otherwise
     * @throws LdapException if there is a database fault during evaluation
     */
    boolean evaluate( ExprNode refinement, Dn dn, Entry entry ) throws LdapException;
}
