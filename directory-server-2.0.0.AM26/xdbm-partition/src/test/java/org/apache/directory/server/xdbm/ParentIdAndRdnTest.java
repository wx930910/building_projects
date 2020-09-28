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
package org.apache.directory.server.xdbm;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.xdbm.impl.avl.AvlPartitionTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the {@link ParentIdAndRdn} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ParentIdAndRdnTest
{
    private static final Logger LOG = LoggerFactory.getLogger( ParentIdAndRdnTest.class );

    private static SchemaManager schemaManager = null;


    @BeforeClass
    public static void setup() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = AvlPartitionTest.class.getResource( "" ).getPath();
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
    }


    @Test
    public void testCompareEquals() throws LdapInvalidDnException
    {
        ParentIdAndRdn rdn1 = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( schemaManager, "cn=test" ) );
        ParentIdAndRdn rdn2 = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( schemaManager, "CN=test2" ) );
        ParentIdAndRdn rdn3 = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( schemaManager, "ou=test" ) );
        ParentIdAndRdn rdn4 = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( schemaManager, "2.5.4.11=test2" ) );
        ParentIdAndRdn rdn5 = new ParentIdAndRdn( Strings.getUUID( 1L ), new Rdn( schemaManager, "CommonName= Test " ) );
        ParentIdAndRdn rdn6 = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( schemaManager, "cn=test+sn=small" ) );
        ParentIdAndRdn rdn7 = new ParentIdAndRdn( Strings.getUUID( 2L ), new Rdn( schemaManager,
            "2.5.4.4= Small + 2.5.4.3 = TEST " ) );

        // First rdn
        assertEquals( 0, rdn1.compareTo( rdn1 ) );
        assertTrue( rdn1.compareTo( rdn2 ) < 0 );
        assertTrue( rdn1.compareTo( rdn3 ) > 0 );
        assertTrue( rdn1.compareTo( rdn4 ) > 0 );
        assertTrue( rdn1.compareTo( rdn5 ) > 0 );

        // Second rdn
        assertTrue( rdn2.compareTo( rdn1 ) > 0 );
        assertEquals( 0, rdn2.compareTo( rdn2 ) );
        assertTrue( rdn2.compareTo( rdn3 ) > 0 );
        assertTrue( rdn2.compareTo( rdn4 ) > 0 );
        assertTrue( rdn2.compareTo( rdn5 ) > 0 );

        // Third rdn
        assertTrue( rdn3.compareTo( rdn1 ) < 0 );
        assertTrue( rdn3.compareTo( rdn2 ) < 0 );
        assertEquals( 0, rdn3.compareTo( rdn3 ) );
        assertTrue( rdn3.compareTo( rdn4 ) < 0 );
        assertTrue( rdn3.compareTo( rdn5 ) > 0 );

        // Forth rdn
        assertTrue( rdn4.compareTo( rdn1 ) < 0 );
        assertTrue( rdn4.compareTo( rdn2 ) < 0 );
        assertTrue( rdn4.compareTo( rdn3 ) > 0 );
        assertEquals( 0, rdn4.compareTo( rdn4 ) );
        assertTrue( rdn4.compareTo( rdn5 ) > 0 );

        // Fifth rdn
        assertTrue( rdn5.compareTo( rdn1 ) < 0 );
        assertTrue( rdn5.compareTo( rdn2 ) < 0 );
        assertTrue( rdn5.compareTo( rdn3 ) < 0 );
        assertTrue( rdn5.compareTo( rdn4 ) < 0 );
        assertEquals( 0, rdn5.compareTo( rdn5 ) );

        // Sixth rdn
        //assertEquals( 0, rdn6.compareTo( rdn7 ) );
        //assertEquals( 0, rdn7.compareTo( rdn6 ) );
        assertTrue( rdn1.compareTo( rdn6 ) < 0 );
        assertTrue( rdn1.compareTo( rdn7 ) < 0 );
    }
}
