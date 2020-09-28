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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the partition schema loader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionSchemaLoaderIT
{
    private static SchemaManager schemaManager;
    private static DirectoryService directoryService;
    private static InstanceLayout instanceLayout;


    @BeforeClass
    public static void setUp() throws Exception
    {
        // setup working directory
        directoryService = new DefaultDirectoryService();
        String tmpDirPath = System.getProperty( "workingDirectory", System.getProperty( "java.io.tmpdir" ) );
        File workingDirectory = new File( tmpDirPath + "/server-work-"
            + PartitionSchemaLoaderIT.class.getSimpleName() );
        instanceLayout = new InstanceLayout( workingDirectory );
        directoryService.setInstanceLayout( instanceLayout );

        if ( !workingDirectory.exists() )
        {
            workingDirectory.mkdirs();
        }

        directoryService.getInstanceLayout().setPartitionsDir( workingDirectory );

        // --------------------------------------------------------------------
        // Load the bootstrap schemas to start up the schema partition
        // --------------------------------------------------------------------

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( workingDirectory );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors( schemaManager.getErrors() ) );
        }

        directoryService.setSchemaManager( schemaManager );
    }


    @AfterClass
    public static void cleanup() throws Exception
    {
        FileUtils.deleteDirectory( instanceLayout.getInstanceDirectory() );
    }


    @Test
    public void testGetSchemas() throws Exception
    {
        SchemaManager schemaManager = directoryService.getSchemaManager();

        Schema schema = schemaManager.getLoadedSchema( "mozilla" );
        assertNotNull( schema );
        assertEquals( "mozilla", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "core" );
        assertNotNull( schema );
        assertEquals( "core", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "apachedns" );
        assertNotNull( schema );
        assertEquals( "apachedns", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "autofs" );
        assertNotNull( schema );
        assertEquals( "autofs", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "apache" );
        assertNotNull( schema );
        assertEquals( "apache", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "cosine" );
        assertNotNull( schema );
        assertEquals( "cosine", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "krb5kdc" );
        assertNotNull( schema );
        assertEquals( "krb5kdc", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "samba" );
        assertNotNull( schema );
        assertEquals( "samba", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "collective" );
        assertNotNull( schema );
        assertEquals( "collective", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "java" );
        assertNotNull( schema );
        assertEquals( "java", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "dhcp" );
        assertNotNull( schema );
        assertEquals( "dhcp", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "corba" );
        assertNotNull( schema );
        assertEquals( "corba", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "nis" );
        assertNotNull( schema );
        assertEquals( "nis", schema.getSchemaName() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "inetorgperson" );
        assertNotNull( schema );
        assertEquals( "inetorgperson", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "system" );
        assertNotNull( schema );
        assertEquals( "system", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;

        schema = schemaManager.getLoadedSchema( "apachemeta" );
        assertNotNull( schema );
        assertEquals( "apachemeta", schema.getSchemaName() );
        assertFalse( schema.isDisabled() );
        assertEquals( "uid=admin,ou=system", schema.getOwner() );
        schema = null;
    }
}
