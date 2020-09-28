/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.client.api;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.LookupLdapConnectionValidator;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.CreateLdapConnectionPoolRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A test class for the connection pool.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateLdapServer(
    transports = {
            @CreateTransport(protocol = "LDAP")
    })
@CreateDS(name = "classDS",
    enableChangeLog = true,
    partitions = {
            @CreatePartition(
                    name = "example",
                    suffix = "dc=example,dc=com",
                    contextEntry = @ContextEntry(
                            entryLdif =
                            "dn: dc=example,dc=com\n" +
                            "objectClass: domain\n" +
                            "objectClass: top\n" +
                            "dc: example\n\n"
                    ),
                    indexes = {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
            })
    })
@ApplyLdifs({
            "dn: cn=class,ou=system",
            "objectClass: person",
            "cn: class",
            "sn: sn_class"
    })
@CreateLdapConnectionPool(
    factoryClass = ValidatingPoolableLdapConnectionFactory.class,
    connectionFactoryClass = TrackingLdapConnectionFactory.class,
    validatorClass = LookupLdapConnectionValidator.class,
    maxActive = 1,
    testOnBorrow = true )
public class ValidatingLdapConnectionPoolTest
{
    private static Logger LOG = LoggerFactory.getLogger( ValidatingLdapConnectionPoolTest.class );

    @ClassRule
    public static CreateLdapConnectionPoolRule createLdapConnectionPoolRule = 
            new CreateLdapConnectionPoolRule();

    @Test
    public void testClassLdapConnectionPool()
    {
        LOG.debug( "testClassLdapConnectionPool" );

        LdapConnectionPool pool = createLdapConnectionPoolRule.getLdapConnectionPool();
        TrackingLdapConnectionFactory factory = 
            (TrackingLdapConnectionFactory)createLdapConnectionPoolRule.getLdapConnectionFactory();
        LdapConnection ldapConnection = null;
        try
        {
            ldapConnection = pool.getConnection();
            assertEquals( 1, factory.getBindCalled() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    pool.releaseConnection( ldapConnection );
                    assertEquals( 1, factory.getBindCalled() );
                }
                catch ( LdapException e )
                {
                    fail( "failed to release connection: " + e.getMessage() );
                }
            }
        }
        try
        {
            ldapConnection = pool.getConnection();
            assertEquals( 1, factory.getBindCalled() );
    
            ldapConnection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
            assertEquals( 1, factory.getBindCalled() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    pool.releaseConnection( ldapConnection );
                    assertEquals( 2, factory.getBindCalled() );
                }
                catch ( LdapException e )
                {
                    fail( "failed to release connection: " + e.getMessage() );
                }
            }
        }
    }
}
