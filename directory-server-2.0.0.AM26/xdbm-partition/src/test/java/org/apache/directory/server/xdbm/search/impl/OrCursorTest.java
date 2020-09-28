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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
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
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.MockPartitionReadTxn;
import org.apache.directory.server.xdbm.StoreUtils;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.cursor.OrCursor;
import org.apache.directory.server.xdbm.search.cursor.SubstringCursor;
import org.apache.directory.server.xdbm.search.evaluator.SubstringEvaluator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Test class for OrCursor.
 * 
 * Note: The results of OrCursor need not be symmetric, in the sense that the results obtained<br>
 * in a particular order (say first to last ) need not be same as the results obtained in the<br> 
 * order last to first, cause each underlying cursor of OrCursor will have elements in their own order.<br>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrCursorTest extends AbstractCursorTest
{
    private static final Logger LOG = LoggerFactory.getLogger( OrCursorTest.class );

    File wkdir;
    static SchemaManager schemaManager = null;


    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = OrCursorTest.class.getResource( "" ).getPath();
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

        loaded = schemaManager.loadWithDeps( loader.getSchema( "collective" ) );

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
    public void testOrCursorUsingCursorBuilder() throws Exception
    {
        String filter = "(|(cn=J*)(sn=W*))";

        ExprNode exprNode = FilterParser.parse( schemaManager, filter );
        PartitionTxn txn = new MockPartitionReadTxn();

        Set<String> expectedUuid = new HashSet<String>();
        expectedUuid.add( Strings.getUUID( 5 ) );
        expectedUuid.add( Strings.getUUID( 6 ) );
        expectedUuid.add( Strings.getUUID( 8 ) );
        expectedUuid.add( Strings.getUUID( 9 ) );
        expectedUuid.add( Strings.getUUID( 10 ) );
        expectedUuid.add( Strings.getUUID( 11 ) );

        Set<String> foundUuid = new HashSet<String>();

        Cursor<Entry> cursor = buildCursor( txn, exprNode );

        cursor.afterLast();

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        Entry entry = cursor.get();
        String uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        cursor.beforeFirst();

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        entry = cursor.get();
        uuid = entry.get( "entryUUID" ).getString();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        cursor.close();
        assertTrue( cursor.isClosed() );
    }


    @Test(expected = InvalidCursorPositionException.class)
    @SuppressWarnings("unchecked")
    public void testOrCursor() throws Exception
    {
        PartitionTxn txn = ( ( Partition ) store ).beginReadTransaction();
        List<Evaluator<? extends ExprNode>> evaluators = new ArrayList<Evaluator<? extends ExprNode>>();
        List<Cursor<IndexEntry<?, String>>> cursors = new ArrayList<Cursor<IndexEntry<?, String>>>();
        Evaluator<? extends ExprNode> eval;
        Cursor<IndexEntry<?, String>> cursor;

        OrNode orNode = new OrNode();

        ExprNode exprNode = new SubstringNode( schemaManager.getAttributeType( "cn" ), "J", null );
        eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, schemaManager );
        Cursor subStrCursor1 = new SubstringCursor( txn, store, ( SubstringEvaluator ) eval );
        cursors.add( subStrCursor1 );
        evaluators.add( eval );
        orNode.addNode( exprNode );

        //        try
        //        {
        //            new OrCursor( cursors, evaluators );
        //            fail( "should throw IllegalArgumentException" );
        //        }
        //        catch( IllegalArgumentException ie ){ }

        exprNode = new SubstringNode( schemaManager.getAttributeType( "sn" ), "W", null );
        eval = new SubstringEvaluator( ( SubstringNode ) exprNode, store, schemaManager );
        evaluators.add( eval );
        Cursor subStrCursor2 = new SubstringCursor( txn, store, ( SubstringEvaluator ) eval );
        cursors.add( subStrCursor2 );

        orNode.addNode( exprNode );

        Set<String> expectedUuid = new HashSet<String>();
        expectedUuid.add( Strings.getUUID( 5 ) );
        expectedUuid.add( Strings.getUUID( 6 ) );
        expectedUuid.add( Strings.getUUID( 8 ) );
        expectedUuid.add( Strings.getUUID( 9 ) );
        expectedUuid.add( Strings.getUUID( 10 ) );
        expectedUuid.add( Strings.getUUID( 11 ) );

        Set<String> foundUuid = new HashSet<String>();

        cursor = new OrCursor( txn, cursors, evaluators );

        cursor.beforeFirst();
        assertFalse( cursor.available() );

        // from first
        assertTrue( cursor.first() );
        assertTrue( cursor.available() );
        String uuid = cursor.get().getId();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertTrue( cursor.next() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( expectedUuid.contains( uuid ) );
        foundUuid.add( uuid );
        expectedUuid.remove( uuid );

        assertFalse( cursor.next() );
        assertFalse( cursor.available() );

        // from last        
        cursor.afterLast();
        assertFalse( cursor.available() );

        assertTrue( cursor.last() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertTrue( cursor.previous() );
        assertTrue( cursor.available() );
        uuid = cursor.get().getId();
        assertTrue( foundUuid.contains( uuid ) );
        foundUuid.remove( uuid );

        assertFalse( cursor.previous() );
        assertFalse( cursor.available() );

        try
        {
            cursor.after( new IndexEntry<String, String>() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        try
        {
            cursor.before( new IndexEntry<String, String>() );
            fail( "should fail with UnsupportedOperationException " );
        }
        catch ( UnsupportedOperationException uoe )
        {
        }

        try
        {
            cursor.get();
        }
        finally
        {
            cursor.close();
        }
    }
}
