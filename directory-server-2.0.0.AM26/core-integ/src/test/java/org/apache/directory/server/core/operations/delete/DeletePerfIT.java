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
package org.apache.directory.server.core.operations.delete;

import java.util.Locale;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the delete operation performances
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(
        name="DeletePerfDS",
        partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry( 
                    entryLdif =
                        "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes = 
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "sn" ),
                    @CreateIndex( attribute = "cn" )
                } )
                
        },
        enableChangeLog = false )
public class DeletePerfIT extends AbstractLdapTestUnit
{
    /**
     * Test an add operation performance
     */
    @Test
    public void testDeleteOne() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn1 = new Dn( "cn=test,dc=example,dc=com" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn1 );
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "sn", "TEST" );
        entry.add( "cn", "test" );
        
        connection.add( entry );
        
        Dn dn2 = new Dn( "cn=test1,cn=test,dc=example,dc=com" );
        Entry entry2 = new DefaultEntry( getService().getSchemaManager(), dn2 );
        entry2.add( "ObjectClass", "top", "person" );
        entry2.add( "sn", "TEST1" );
        entry2.add( "cn", "test1" );
        
        connection.add( entry2 );

        Dn dn3 = new Dn( "cn=test2,cn=test,dc=example,dc=com" );
        Entry entry3 = new DefaultEntry( getService().getSchemaManager(), dn3 );
        entry3.add( "ObjectClass", "top", "person" );
        entry3.add( "sn", "TEST2" );
        entry3.add( "cn", "test2" );
        
        connection.add( entry3 );

        // Deletion
        connection.delete( dn3 );
        connection.delete( dn2 );
        connection.delete( dn1 );

        connection.close();
    }

    
    /**
     * Test an add operation performance
     */
    @Test
    public void testDeletePerf() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( "cn=test,dc=example,dc=com" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn );
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "sn", "TEST" );
        entry.add( "cn", "test" );
        
        connection.add(entry );
        int nbIterations = 15000;

        long t0 = System.currentTimeMillis();
        long tt0 = System.currentTimeMillis();
        
        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i% 1000 == 0 )
            {
                long tt1 = System.currentTimeMillis();
                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }
            
            String name = "test" + i;
            dn = new Dn( "cn=" + name + ",dc=example,dc=com" );
            entry = new DefaultEntry( getService().getSchemaManager(), dn );
            entry.add( "ObjectClass", "top", "person" );
            entry.add( "sn", name.toUpperCase( Locale.ROOT ) );
            entry.add( "cn", name );
            
            connection.add(entry );
        }
        
        long t1 = System.currentTimeMillis();
        
        System.out.println( "Delta addition : " + ( t1 - t0 ) );
        
        // Deletion

        t0 = System.currentTimeMillis();
        long t00 = 0L;
        tt0 = System.currentTimeMillis();
        
        for ( int i = 0; i < nbIterations; i++ )
        {
            if ( i % 100 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 5000 )
            {
                t00 = System.currentTimeMillis();
            }
            
            String name = "test" + i;
            dn = new Dn( "cn=" + name + ",dc=example,dc=com" );
            
            connection.delete( dn );
        }
        
        t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta deletion: " + deltaWarmed + "( " + ( ( ( nbIterations - 5000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );

        connection.close();
    }
}
