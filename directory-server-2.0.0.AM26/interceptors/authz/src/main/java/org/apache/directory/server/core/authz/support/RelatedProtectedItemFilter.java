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
package org.apache.directory.server.core.authz.support;


import java.util.Collection;
import java.util.Iterator;

import org.apache.directory.api.ldap.aci.ACITuple;
import org.apache.directory.api.ldap.aci.ProtectedItem;
import org.apache.directory.api.ldap.aci.protectedItem.AllAttributeValuesItem;
import org.apache.directory.api.ldap.aci.protectedItem.AttributeTypeItem;
import org.apache.directory.api.ldap.aci.protectedItem.AttributeValueItem;
import org.apache.directory.api.ldap.aci.protectedItem.ClassesItem;
import org.apache.directory.api.ldap.aci.protectedItem.MaxImmSubItem;
import org.apache.directory.api.ldap.aci.protectedItem.MaxValueCountElem;
import org.apache.directory.api.ldap.aci.protectedItem.MaxValueCountItem;
import org.apache.directory.api.ldap.aci.protectedItem.RangeOfValuesItem;
import org.apache.directory.api.ldap.aci.protectedItem.RestrictedByElem;
import org.apache.directory.api.ldap.aci.protectedItem.RestrictedByItem;
import org.apache.directory.api.ldap.aci.protectedItem.SelfValueItem;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.event.Evaluator;
import org.apache.directory.server.core.api.subtree.RefinementEvaluator;
import org.apache.directory.server.i18n.I18n;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link ProtectedItem}s
 * are not related with the operation. (18.8.3.2, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RelatedProtectedItemFilter implements ACITupleFilter
{
    private final RefinementEvaluator refinementEvaluator;
    private final Evaluator entryEvaluator;
    private final SchemaManager schemaManager;


    public RelatedProtectedItemFilter( RefinementEvaluator refinementEvaluator, Evaluator entryEvaluator,
        SchemaManager schemaManager )
    {
        this.refinementEvaluator = refinementEvaluator;
        this.entryEvaluator = entryEvaluator;
        this.schemaManager = schemaManager;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry )
        throws LdapException
    {
        if ( aciContext.getAciTuples().isEmpty() )
        {
            return aciContext.getAciTuples();
        }

        for ( Iterator<ACITuple> i = aciContext.getAciTuples().iterator(); i.hasNext(); )
        {
            ACITuple tuple = i.next();

            if ( !isRelated( tuple, scope, aciContext.getUserDn(), aciContext.getEntryDn(),
                aciContext.getAttributeType(), aciContext.getAttrValue(), aciContext.getEntry() ) )
            {
                i.remove();
            }
        }

        return aciContext.getAciTuples();
    }


    private boolean isRelated( ACITuple tuple, OperationScope scope, Dn userName, Dn entryName,
        AttributeType attributeType, Value attrValue, Entry entry ) throws LdapException, InternalError
    {
        String oid = null;

        if ( attributeType != null )
        {
            oid = attributeType.getOid();
        }

        for ( ProtectedItem item : tuple.getProtectedItems() )
        {
            if ( item == ProtectedItem.ENTRY )
            {
                if ( scope != OperationScope.ENTRY )
                {
                    continue;
                }

                return true;
            }
            else if ( ( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES ) 
                    || ( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES ) )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE && scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                return true;
            }
            else if ( item instanceof AllAttributeValuesItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                AllAttributeValuesItem aav = ( AllAttributeValuesItem ) item;

                for ( Iterator<AttributeType> iterator = aav.iterator(); iterator.hasNext(); )
                {
                    AttributeType attr = iterator.next();

                    if ( oid.equals( attr.getOid() ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof AttributeTypeItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                AttributeTypeItem at = ( AttributeTypeItem ) item;

                for ( Iterator<AttributeType> iterator = at.iterator(); iterator.hasNext(); )
                {
                    AttributeType attr = iterator.next();

                    if ( oid.equals( attr.getOid() ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof AttributeValueItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                AttributeValueItem av = ( AttributeValueItem ) item;

                for ( Iterator<Attribute> j = av.iterator(); j.hasNext(); )
                {
                    Attribute entryAttribute = j.next();

                    AttributeType attr = entryAttribute.getAttributeType();
                    String attrOid;

                    if ( attr != null )
                    {
                        attrOid = entryAttribute.getAttributeType().getOid();
                    }
                    else
                    {
                        attr = schemaManager.lookupAttributeTypeRegistry( entryAttribute.getId() );
                        attrOid = attr.getOid();
                        entryAttribute.apply( attr );
                    }

                    if ( oid.equals( attrOid ) && entryAttribute.contains( attrValue ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ClassesItem )
            {
                ClassesItem refinement = ( ClassesItem ) item;

                if ( refinementEvaluator
                    .evaluate( refinement.getClasses(), entry.get( SchemaConstants.OBJECT_CLASS_AT ) ) )
                {
                    return true;
                }
            }
            else if ( item instanceof MaxImmSubItem )
            {
                return true;
            }
            else if ( item instanceof MaxValueCountItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                MaxValueCountItem mvc = ( MaxValueCountItem ) item;

                for ( Iterator<MaxValueCountElem> j = mvc.iterator(); j.hasNext(); )
                {
                    MaxValueCountElem mvcItem = j.next();

                    if ( oid.equals( mvcItem.getAttributeType().getOid() ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof RangeOfValuesItem )
            {
                RangeOfValuesItem rov = ( RangeOfValuesItem ) item;

                if ( entryEvaluator.evaluate( rov.getRefinement(), entryName, entry ) )
                {
                    return true;
                }
            }
            else if ( item instanceof RestrictedByItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                RestrictedByItem rb = ( RestrictedByItem ) item;

                for ( Iterator<RestrictedByElem> j = rb.iterator(); j.hasNext(); )
                {
                    RestrictedByElem rbItem = j.next();

                    if ( oid.equals( rbItem.getAttributeType().getOid() ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof SelfValueItem )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE && scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                SelfValueItem sv = ( SelfValueItem ) item;

                for ( Iterator<AttributeType> iterator = sv.iterator(); iterator.hasNext(); )
                {
                    AttributeType attr = iterator.next();

                    if ( oid.equals( attr.getOid() ) )
                    {
                        Attribute entryAttribute = entry.get( oid );

                        if ( ( entryAttribute != null ) && entryAttribute.contains( userName.getNormName() ) )
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                throw new InternalError( I18n.err( I18n.ERR_232, item.getClass().getName() ) );
            }
        }

        return false;
    }
}
