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
package org.apache.directory.server.xdbm.search.impl;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.MockCoreSession;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Test class for AndCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndCursorTest extends AbstractCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AndCursorTest.class );

    File wkdir;


    @BeforeClass
    public static void setup() throws Exception
    {
        // setup the standard registries
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AndCursorTest.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        loaded = schemaManager.loadWithDeps( "collective" );

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }
    }


    @Before
    public void createStore() throws Exception
    {
        directoryService = new MockDirectoryService();

        // setup the working directory for the store
        wkdir = File.createTempFile( getClass().getSimpleName(), "db" );
        wkdir.delete();
        wkdir = new File( wkdir.getParentFile(), getClass().getSimpleName() );
        wkdir.mkdirs();

        StoreUtils.createdExtraAttributes( schemaManager );
        
        // initialize the store
        store = new AvlPartition( schemaManager, directoryService.getDnFactory() );
        ( ( Partition ) store ).setId( "example" );
        store.setCacheSize( 10 );
        store.setPartitionPath( wkdir.toURI() );
        store.setSyncOnWrite( false );

        store.addIndex( new AvlIndex<String>( SchemaConstants.OU_AT_OID ) );
        store.addIndex( new AvlIndex<String>( SchemaConstants.CN_AT_OID ) );
        ( ( Partition ) store ).setSuffixDn( new Dn( schemaManager, "o=Good Times Co." ) );
        ( ( Partition ) store ).initialize();

        StoreUtils.loadExampleData( store, schemaManager );

        evaluatorBuilder = new EvaluatorBuilder( store, schemaManager );
        cursorBuilder = new CursorBuilder( store, evaluatorBuilder );
        directoryService.setSchemaManager( schemaManager );
        session = new MockCoreSession( new LdapPrincipal(), directoryService );

        LOG.debug( "Created new store" );
    }


    @After
    public void destroyStore() throws Exception
    {
        if ( store != null )
        {
            ( ( Partition ) store ).destroy( null );
        }

        store = null;
        if ( wkdir != null )
        {
            FileUtils.deleteDirectory( wkdir );
        }

        wkdir = null;
    }


    @Test
    public void testAndCursorWithCursorBuilder() throws Exception
    {
        String filter = "(&(cn=J*)(sn=*))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();

        Set<String> expectedUuid = new HashSet<String>();
        expectedUuid.add( Strings.getUUID( 5 ) );
        expectedUuid.add( Strings.getUUID( 6 ) );
        expectedUuid.add( Strings.getUUID( 8 ) );

        Cursor<Entry> cursor = buildCursor( txn, exprNode );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        Entry entry = cursor.get();
        String uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        expectedUuid.remove( uuid );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test
    public void testAndCursorWithManualFilter() throws Exception
    {
        ExprNode exprNode = FilterParser.parse( schemaManager, "(&(cn=J*)(sn=*))" );
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();

        Set<String> expectedUuid = new HashSet<String>();
        expectedUuid.add( Strings.getUUID( 5 ) );
        expectedUuid.add( Strings.getUUID( 6 ) );
        expectedUuid.add( Strings.getUUID( 8 ) );

        Set<String> foundUuid = new HashSet<String>();

        Cursor<Entry> cursor = buildCursor( txn, exprNode );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        Entry entry = cursor.get();
        String uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        cursor.first();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.afterLast();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        cursor.last();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        try
        {
            cursor.get();
            fail( "should fail with InvalidCursorPositionException" );
        }
        catch ( InvalidCursorPositionException ice )
        {
        }

        cursor.close();
        assertTrue( cursor.isClosed() );
    }
}
