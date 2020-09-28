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
package org.apache.directory.server.xdbm.search.evaluator;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.impl.ScanCountComparator;


/**
 * An Evaluator for logical disjunction (OR) expressions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrEvaluator implements Evaluator<OrNode>
{
    /** The list of evaluators associated with each of the children */
    private final List<Evaluator<? extends ExprNode>> evaluators;

    /** The OrNode */
    private final OrNode node;


    /**
     * Creates a new OrEvaluator
     * 
     * @param node The OrNode
     * @param evaluators The inner evaluators
     */
    public OrEvaluator( OrNode node, List<Evaluator<? extends ExprNode>> evaluators )
    {
        this.node = node;
        this.evaluators = optimize( evaluators );
    }


    /**
     * Takes a set of Evaluators and copies then sorts them in a new list with
     * decreasing scan counts on their expression nodes.  This is done to have
     * the Evaluators with the greatest scan counts which have the highest
     * probability of accepting a candidate first.  That will increase the
     * chance of shorting the checks on evaluators early so extra lookups and
     * comparisons are avoided.
     *
     * @param unoptimized the unoptimized list of Evaluators
     * @return optimized Evaluator list with decreasing scan count ordering
     */
    private List<Evaluator<? extends ExprNode>> optimize(
        List<Evaluator<? extends ExprNode>> unoptimized )
    {
        List<Evaluator<? extends ExprNode>> optimized = new ArrayList<>(
            unoptimized.size() );
        optimized.addAll( unoptimized );

        Collections.sort( optimized, new ScanCountComparator() );

        return optimized;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( PartitionTxn partitionTxn, IndexEntry<?, String> indexEntry ) throws LdapException
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( evaluator.evaluate( partitionTxn, indexEntry ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( Entry entry ) throws LdapException
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( evaluator.evaluate( entry ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public OrNode getExpression()
    {
        return node;
    }


    /**
     * Dumps the evaluators
     */
    private String dumpEvaluators( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        for ( Evaluator<? extends ExprNode> evaluator : evaluators )
        {
            sb.append( evaluator.toString( tabs + "  " ) );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "OrEvaluator : " ).append( node ).append( "\n" );

        if ( ( evaluators != null ) && !evaluators.isEmpty() )
        {
            sb.append( dumpEvaluators( tabs ) );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}