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


import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.api.ldap.model.filter.ApproximateNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ExtensibleNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.ScopeNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.i18n.I18n;


/**
 * Evaluates LeafNode assertions on candidates using a database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LeafEvaluator implements Evaluator
{
    /** equality matching type constant */
    private static final int EQUALITY_MATCH = 0;

    /** ordering matching type constant */
    private static final int ORDERING_MATCH = 1;

    /** substring matching type constant */
    private static final int SUBSTRING_MATCH = 3;

    /** Substring node evaluator we depend on */
    private SubstringEvaluator substringEvaluator;

    /** ScopeNode evaluator we depend on */
    private ScopeEvaluator scopeEvaluator;

    /** Constants used for comparisons */
    private static final boolean COMPARE_GREATER = true;
    private static final boolean COMPARE_LESSER = false;


    /**
     * Creates a leaf expression node evaluator.
     *
     * @param substringEvaluator The evaluator to use
     */
    public LeafEvaluator( SubstringEvaluator substringEvaluator )
    {
        this.scopeEvaluator = new ScopeEvaluator();
        this.substringEvaluator = substringEvaluator;
    }


    public ScopeEvaluator getScopeEvaluator()
    {
        return scopeEvaluator;
    }


    public SubstringEvaluator getSubstringEvaluator()
    {
        return substringEvaluator;
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( ExprNode node, Dn dn, Entry entry ) throws LdapException
    {
        if ( node instanceof ScopeNode )
        {
            return scopeEvaluator.evaluate( node, dn, entry );
        }

        if ( node instanceof PresenceNode )
        {
            return evalPresence( ( ( PresenceNode ) node ).getAttributeType(), entry );
        }
        else if ( ( node instanceof EqualityNode ) || ( node instanceof ApproximateNode ) )
        {
            return evalEquality( ( EqualityNode<?> ) node, entry );
        }
        else if ( node instanceof GreaterEqNode )
        {
            return evalGreaterOrLesser( ( GreaterEqNode<?> ) node, entry, COMPARE_GREATER );
        }
        else if ( node instanceof LessEqNode )
        {
            return evalGreaterOrLesser( ( LessEqNode<?> ) node, entry, COMPARE_LESSER );
        }
        else if ( node instanceof SubstringNode )
        {
            return substringEvaluator.evaluate( node, dn, entry );
        }
        else if ( node instanceof ExtensibleNode )
        {
            throw new NotImplementedException();
        }
        else
        {
            throw new LdapInvalidSearchFilterException( I18n.err( I18n.ERR_245, node ) );
        }
    }


    /**
     * Evaluates a simple greater than or less than attribute value assertion on
     * a perspective candidate.
     * 
     * @param node the greater than or less than node to evaluate
     * @param entry the perspective candidate
     * @param isGreater true if it is a greater than or equal to comparison,
     *      false if it is a less than or equal to comparison.
     * @return the ava evaluation on the perspective candidate
     * @throws LdapException if there is a database access failure
     */
    private boolean evalGreaterOrLesser( SimpleNode<?> node, Entry entry, boolean isGreaterOrLesser )
        throws LdapException
    {
        // get the attribute associated with the node
        Attribute attr = entry.get( node.getAttribute() );

        // If we do not have the attribute just return false
        if ( null == attr )
        {
            return false;
        }

        /*
         * We need to iterate through all values and for each value we normalize
         * and use the comparator to determine if a match exists.
         */
        Value filterValue = node.getValue();

        /*
         * Cheaper to not check isGreater in one loop - better to separate
         * out into two loops which you choose to execute based on isGreater
         */
        if ( isGreaterOrLesser == COMPARE_GREATER )
        {
            for ( Value value : attr )
            {
                // Found a value that is greater than or equal to the ava value
                if ( value.compareTo( filterValue ) >= 0 )
                {
                    return true;
                }
            }
        }
        else
        {
            for ( Value value : attr )
            {
                // Found a value that is less than or equal to the ava value
                if ( value.compareTo( filterValue ) <= 0 )
                {
                    return true;
                }
            }
        }

        // no match so return false
        return false;
    }


    /**
     * Evaluates a simple presence attribute value assertion on a perspective
     * candidate.
     * 
     * @param attrId the name of the attribute tested for presence 
     * @param entry the perspective candidate
     * @return the ava evaluation on the perspective candidate
     */
    private boolean evalPresence( AttributeType attributeType, Entry entry )
    {
        if ( entry == null )
        {
            return false;
        }

        return null != entry.get( attributeType );
    }


    /**
     * Evaluates a simple equality attribute value assertion on a perspective
     * candidate.
     *
     * @param node the equality node to evaluate
     * @param entry the perspective candidate
     * @return the ava evaluation on the perspective candidate
     * @throws org.apache.directory.api.ldap.model.exception.LdapException if there is a database access failure
     */
    private boolean evalEquality( EqualityNode<?> node, Entry entry ) throws LdapException
    {
        // get the attribute associated with the node
        Attribute attr = entry.get( node.getAttribute() );

        // If we do not have the attribute just return false
        if ( null == attr )
        {
            return false;
        }

        // check if Ava value exists in attribute
        Value value = node.getValue();

        // check if the normalized value is present
        if ( attr.contains( value ) )
        {
            return true;
        }

        /*
         * We need to now iterate through all values because we could not get
         * a lookup to work.  For each value we normalize and use the comparator
         * to determine if a match exists.
         */
        for ( Value val : attr )
        {
            if ( 0 == val.compareTo( value ) )
            {
                return true;
            }
        }

        // no match so return false
        return false;
    }


    /**
     * Gets the matching rule for an attributeType.
     *
     * @param attributeType the attributeType
     * @return the matching rule
     * @throws LdapException if there is a failure
     */
    private MatchingRule getMatchingRule( AttributeType attributeType, int matchType ) throws LdapException
    {
        MatchingRule mrule = null;

        switch ( matchType )
        {
            case ( EQUALITY_MATCH ):
                mrule = attributeType.getEquality();
                break;

            case ( SUBSTRING_MATCH ):
                mrule = attributeType.getSubstring();
                break;

            case ( ORDERING_MATCH ):
                mrule = attributeType.getOrdering();
                break;

            default:
                throw new LdapException( I18n.err( I18n.ERR_246, matchType ) );
        }

        if ( ( mrule == null ) && ( matchType != EQUALITY_MATCH ) )
        {
            mrule = attributeType.getEquality();
        }

        return mrule;
    }
}
