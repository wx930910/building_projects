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
package org.apache.directory.server.core.api.subtree;


import java.util.Iterator;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.SimpleNode;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.i18n.I18n;


/**
 * A refinement leaf node evaluator.  This evaluator checks to see if the
 * objectClass attribute of a candidate entry is matched by a leaf node in
 * a refinement filter expression tree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RefinementLeafEvaluator
{
    /** A SchemaManager instance */
    private final SchemaManager schemaManager;

    /** A storage for the ObjectClass attributeType */
    private AttributeType objectClassAT;


    /**
     * Creates a refinement filter's leaf node evaluator.
     *
     * @param schemaManager The server schemaManager
     */
    public RefinementLeafEvaluator( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
        objectClassAT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }


    /**
     * Evaluates whether or not a simple leaf node of a refinement filter selects an
     * entry based on the entry's objectClass attribute values.
     *
     * @param node the leaf node of the refinement filter
     * @param objectClasses the objectClass attribute's values
     * @return true if the leaf node selects the entry based on objectClass values, false
     * if it rejects the entry
     * @throws LdapException If the evaluation failed
     */
    public boolean evaluate( SimpleNode node, Attribute objectClasses ) throws LdapException
    {
        if ( node == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_295 ) );
        }

        if ( !( node instanceof EqualityNode ) )
        {
            throw new LdapException( I18n.err( I18n.ERR_301, node ) );
        }

        if ( node.isSchemaAware() )
        {
            if ( !node.getAttributeType().equals( objectClassAT ) )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_302, node.getAttribute() ) );
            }
        }
        else if ( !node.getAttribute().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT )
            && !node.getAttribute().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_302, node.getAttribute() ) );
        }

        if ( null == objectClasses )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_303 ) );
        }

        if ( !( objectClasses.isInstanceOf( objectClassAT ) ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_304 ) );
        }

        // check if Ava value exists in attribute
        // If the filter value for the objectClass is an OID we need to resolve a name
        String value = node.getValue().getString();

        if ( objectClasses.contains( value ) )
        {
            return true;
        }

        if ( Character.isDigit( value.charAt( 0 ) ) )
        {
            Iterator<String> list = schemaManager.getGlobalOidRegistry().getNameSet( value ).iterator();

            while ( list.hasNext() )
            {
                String objectClass = list.next();

                if ( objectClasses.contains( objectClass ) )
                {
                    return true;
                }
            }
        }

        // no match so return false
        return false;
    }
}
