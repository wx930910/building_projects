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
package org.apache.directory.server.core.schema;


import org.apache.directory.api.ldap.model.constants.MetaSchemaConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributesFactory;
import org.apache.directory.api.ldap.model.schema.DitContentRule;
import org.apache.directory.api.ldap.model.schema.DitStructureRule;
import org.apache.directory.api.ldap.model.schema.LdapSyntax;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.MatchingRuleUse;
import org.apache.directory.api.ldap.model.schema.NameForm;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.SchemaObject;
import org.apache.directory.api.ldap.model.schema.parsers.LdapComparatorDescription;
import org.apache.directory.api.ldap.model.schema.parsers.NormalizerDescription;
import org.apache.directory.api.ldap.model.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.util.Base64;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;


/**
 * Responsible for translating modify operations on the subschemaSubentry into
 * operations against entries within the schema partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SchemaSubentryModifier
{
    private AttributesFactory factory = new AttributesFactory();

    /** The server schemaManager */
    private SchemaManager schemaManager;

    /** The Dn factory */
    private DnFactory dnFactory;


    /**
     * 
     * Creates a new instance of SchemaSubentryModifier.
     *
     * @param schemaManager The server schemaManager
     * @param dnFactory The Dn factory
     */
    public SchemaSubentryModifier( SchemaManager schemaManager, DnFactory dnFactory )
    {
        this.schemaManager = schemaManager;
        this.dnFactory = dnFactory;
    }


    private Dn getDn( SchemaObject obj ) throws LdapInvalidDnException
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "m-oid=" ).append( obj.getOid() ).append( ",ou=" );

        if ( obj instanceof LdapSyntax )
        {
            buf.append( SchemaConstants.SYNTAXES );
        }
        else if ( obj instanceof MatchingRule )
        {
            buf.append( SchemaConstants.MATCHING_RULES_AT );
        }
        else if ( obj instanceof AttributeType )
        {
            buf.append( SchemaConstants.ATTRIBUTE_TYPES_AT );
        }
        else if ( obj instanceof ObjectClass )
        {
            buf.append( SchemaConstants.OBJECT_CLASSES_AT );
        }
        else if ( obj instanceof MatchingRuleUse )
        {
            buf.append( SchemaConstants.MATCHING_RULE_USE_AT );
        }
        else if ( obj instanceof DitStructureRule )
        {
            buf.append( SchemaConstants.DIT_STRUCTURE_RULES_AT );
        }
        else if ( obj instanceof DitContentRule )
        {
            buf.append( SchemaConstants.DIT_CONTENT_RULES_AT );
        }
        else if ( obj instanceof NameForm )
        {
            buf.append( SchemaConstants.NAME_FORMS_AT );
        }

        buf.append( ",cn=" ).append( obj.getSchemaName() ).append( ",ou=schema" );
        return dnFactory.create( buf.toString() );
    }


    public void add( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        LdapComparatorDescription comparatorDescription ) throws LdapException
    {
        String schemaName = getSchema( comparatorDescription );
        Dn dn = dnFactory.create(
            "m-oid=" + comparatorDescription.getOid(),
            SchemaConstants.COMPARATORS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );

        Entry entry = getEntry( dn, comparatorDescription );
        
        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        AddOperationContext addContext = new AddOperationContext( modifyContext.getSession(), entry );
        addContext.setCurrentInterceptor( position );
        addContext.setPartition( partition );
        addContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.add( addContext );
    }


    public void add( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        NormalizerDescription normalizerDescription ) throws LdapException
    {
        String schemaName = getSchema( normalizerDescription );
        Dn dn = dnFactory.create(
            "m-oid=" + normalizerDescription.getOid(),
            SchemaConstants.NORMALIZERS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );

        Entry entry = getEntry( dn, normalizerDescription );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        AddOperationContext addContext = new AddOperationContext( modifyContext.getSession(), entry );
        addContext.setCurrentInterceptor( position );
        addContext.setPartition( partition );
        addContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.add( addContext );
    }


    public void add( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        SyntaxCheckerDescription syntaxCheckerDescription ) throws LdapException
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        Dn dn = dnFactory.create(
            "m-oid=" + syntaxCheckerDescription.getOid(),
            SchemaConstants.SYNTAX_CHECKERS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );

        Entry entry = getEntry( dn, syntaxCheckerDescription );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        AddOperationContext addContext = new AddOperationContext( modifyContext.getSession(), entry );
        addContext.setCurrentInterceptor( position );
        addContext.setPartition( partition );
        addContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.add( addContext );
    }


    public void addSchemaObject( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        SchemaObject obj ) throws LdapException
    {
        Schema schema = schemaManager.getLoadedSchema( obj.getSchemaName() );
        Dn dn = getDn( obj );
        Entry entry = factory.getAttributes( obj, schema, schemaManager );
        entry.setDn( dn );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        AddOperationContext addContext = new AddOperationContext( modifyContext.getSession(), entry );
        addContext.setCurrentInterceptor( position );
        addContext.setPartition( partition );
        addContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.add( addContext );
    }


    public void deleteSchemaObject( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        SchemaObject obj ) throws LdapException
    {
        Dn dn = getDn( obj );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        DeleteOperationContext deleteContext = new DeleteOperationContext( modifyContext.getSession(), dn );
        deleteContext.setEntry( modifyContext.getSession().lookup( dn ) );
        deleteContext.setCurrentInterceptor( position );
        deleteContext.setPartition( partition );
        deleteContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.delete( deleteContext );
    }


    public void delete( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        NormalizerDescription normalizerDescription ) throws LdapException
    {
        String schemaName = getSchema( normalizerDescription );
        Dn dn = dnFactory.create(
            "m-oid=" + normalizerDescription.getOid(),
            SchemaConstants.NORMALIZERS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        DeleteOperationContext deleteContext = new DeleteOperationContext( modifyContext.getSession(), dn );
        deleteContext.setEntry( modifyContext.getSession().lookup( dn ) );
        deleteContext.setCurrentInterceptor( position );
        deleteContext.setPartition( partition );
        deleteContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.delete( deleteContext );
    }


    public void delete( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        SyntaxCheckerDescription syntaxCheckerDescription ) throws LdapException
    {
        String schemaName = getSchema( syntaxCheckerDescription );
        Dn dn = dnFactory.create(
            "m-oid=" + syntaxCheckerDescription.getOid(),
            SchemaConstants.SYNTAX_CHECKERS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        DeleteOperationContext deleteContext = new DeleteOperationContext( modifyContext.getSession(), dn );
        deleteContext.setEntry( modifyContext.getSession().lookup( dn ) );
        deleteContext.setCurrentInterceptor( position );
        deleteContext.setPartition( partition );
        deleteContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.delete( deleteContext );
    }


    public void delete( Interceptor nextInterceptor, int position, ModifyOperationContext modifyContext,
        LdapComparatorDescription comparatorDescription ) throws LdapException
    {
        String schemaName = getSchema( comparatorDescription );
        Dn dn = dnFactory.create(
            "m-oid=" + comparatorDescription.getOid(),
            SchemaConstants.COMPARATORS_PATH,
            "cn=" + schemaName,
            SchemaConstants.OU_SCHEMA );

        Partition partition = modifyContext.getSession().getDirectoryService().getPartitionNexus().getPartition( dn );

        DeleteOperationContext deleteContext = new DeleteOperationContext( modifyContext.getSession(), dn );
        deleteContext.setEntry( modifyContext.getSession().lookup( dn ) );
        deleteContext.setCurrentInterceptor( position );
        deleteContext.setPartition( partition );
        deleteContext.setTransaction( modifyContext.getTransaction() );

        nextInterceptor.delete( deleteContext );
    }


    private Entry getEntry( Dn dn, LdapComparatorDescription comparatorDescription )
    {
        Entry entry = new DefaultEntry( schemaManager, dn );

        entry.put( SchemaConstants.OBJECT_CLASS_AT,
            SchemaConstants.TOP_OC,
            MetaSchemaConstants.META_TOP_OC,
            MetaSchemaConstants.META_COMPARATOR_OC );

        entry.put( MetaSchemaConstants.M_OID_AT, comparatorDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, comparatorDescription.getFqcn() );

        if ( comparatorDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT,
                Base64.decode( comparatorDescription.getBytecode().toCharArray() ) );
        }

        if ( comparatorDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, comparatorDescription.getDescription() );
        }

        return entry;
    }


    private Entry getEntry( Dn dn, NormalizerDescription normalizerDescription )
    {
        Entry entry = new DefaultEntry( schemaManager, dn );

        entry.put( SchemaConstants.OBJECT_CLASS_AT,
            SchemaConstants.TOP_OC,
            MetaSchemaConstants.META_TOP_OC,
            MetaSchemaConstants.META_NORMALIZER_OC );

        entry.put( MetaSchemaConstants.M_OID_AT, normalizerDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, normalizerDescription.getFqcn() );

        if ( normalizerDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT,
                Base64.decode( normalizerDescription.getBytecode().toCharArray() ) );
        }

        if ( normalizerDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, normalizerDescription.getDescription() );
        }

        return entry;
    }


    private String getSchema( SchemaObject desc )
    {
        if ( desc.getExtensions().containsKey( MetaSchemaConstants.X_SCHEMA_AT ) )
        {
            return desc.getExtensions().get( MetaSchemaConstants.X_SCHEMA_AT ).get( 0 );
        }

        return MetaSchemaConstants.SCHEMA_OTHER;
    }


    private Entry getEntry( Dn dn, SyntaxCheckerDescription syntaxCheckerDescription )
    {
        Entry entry = new DefaultEntry( schemaManager, dn );

        entry.put( SchemaConstants.OBJECT_CLASS_AT,
            SchemaConstants.TOP_OC,
            MetaSchemaConstants.META_TOP_OC,
            MetaSchemaConstants.META_SYNTAX_CHECKER_OC );

        entry.put( MetaSchemaConstants.M_OID_AT, syntaxCheckerDescription.getOid() );
        entry.put( MetaSchemaConstants.M_FQCN_AT, syntaxCheckerDescription.getFqcn() );

        if ( syntaxCheckerDescription.getBytecode() != null )
        {
            entry.put( MetaSchemaConstants.M_BYTECODE_AT,
                Base64.decode( syntaxCheckerDescription.getBytecode().toCharArray() ) );
        }

        if ( syntaxCheckerDescription.getDescription() != null )
        {
            entry.put( MetaSchemaConstants.M_DESCRIPTION_AT, syntaxCheckerDescription.getDescription() );
        }

        return entry;
    }
}
