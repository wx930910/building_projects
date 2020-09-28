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

package org.apache.directory.shared.client.api.operations.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A class to test the search operation with a returningAttributes parameter
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs(
    { "dn: cn=user1,ou=users,ou=system", "objectClass: person", "objectClass: top", "sn: user1 sn", "cn: user1",

        // alias to the above entry
        "dn: cn=user1-alias,ou=users,ou=system",
        "objectClass: alias",
        "objectClass: top",
        "objectClass: extensibleObject",
        "aliasedObjectName: cn=user1,ou=users,ou=system",
        "cn: user1-alias" })
public class SearchRequestReturningAttributesTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = (LdapNetworkConnection)LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    /**
     * Test a search requesting all the attributes (* and +)
     *
     * @throws Exception
     */
    @Test
    public void testSearchAll() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "*", "+" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 14, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
        assertTrue( entry.containsAttribute( "entryDN" ) );
        assertTrue( entry.containsAttribute( "subschemaSubentry" ) );
        assertTrue( entry.containsAttribute( "nbChildren" ) );
        assertTrue( entry.containsAttribute( "nbSubordinates" ) );
        assertTrue( entry.containsAttribute( "hasSubordinates" ) );
        assertTrue( entry.containsAttribute( "structuralObjectclass" ) );
    }


    /**
     * Test a search requesting all the user attributes (*)
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllUsers() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "*" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
    }


    /**
     * Test a search requesting all the operational attributes (+)
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllOperationals() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "+" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 11, entry.size() );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
        assertTrue( entry.containsAttribute( "entryDN" ) );
        assertTrue( entry.containsAttribute( "entryParentId" ) );
        assertTrue( entry.containsAttribute( "subschemaSubentry" ) );
        assertTrue( entry.containsAttribute( "nbChildren" ) );
        assertTrue( entry.containsAttribute( "nbSubordinates" ) );
        assertTrue( entry.containsAttribute( "hasSubordinates" ) );
        assertTrue( entry.containsAttribute( "structuralObjectclass" ) );
    }


    /**
     * Test a search requesting all the user attributes plus a couple of operational
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllUsersAndSomeOperationals() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "*", "entryCSN", "entryUUID" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 5, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting all the operational attributes and a couple of users attributes
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllOperationalAndSomeUsers() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "+", "cn", "sn" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }

        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 13, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
        assertTrue( entry.containsAttribute( "entryDN" ) );
        assertTrue( entry.containsAttribute( "entryParentId" ) );
        assertTrue( entry.containsAttribute( "subschemaSubentry" ) );
        assertTrue( entry.containsAttribute( "nbChildren" ) );
        assertTrue( entry.containsAttribute( "nbSubordinates" ) );
        assertTrue( entry.containsAttribute( "hasSubordinates" ) );
        assertTrue( entry.containsAttribute( "structuralObjectclass" ) );
    }


    /**
     * Test a search requesting some user and Operational attributes
     *
     * @throws Exception
     */
    @Test
    public void testSearchSomeOpsAndUsers() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "entryUUID", "sn", "entryCSN" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some attributes which appear more than one
     *
     * @throws Exception
     */
    @Test
    public void testSearchWithDuplicatedAttrs() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "entryUUID", "cn", "sn", "entryCSN", "entryUUID" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some attributes using text and OID, and duplicated
     *
     * @throws Exception
     */
    @Test
    public void testSearchWithOIDAndtext() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "1.3.6.1.1.16.4", "surName", "entryCSN", "entryUUID" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some attributes which are not present
     *
     * @throws Exception
     */
    @Test
    public void testSearchWithMissingAttributes() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "1.3.6.1.1.16.4", "gn", "entryCSN", "entryUUID" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting no attributes (1.1)
     *
     * @throws Exception
     */
    @Test
    public void testSearchNoAttributes() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "1.1" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 0, entry.size() );
    }


    /**
     * Test a search requesting no attributes (1.1) and some attributes
     *
     * @throws Exception
     */
    @Test
    public void testSearchNoAttributesAndAttributes() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "1.1", "cn" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
    }


    /**
     * Test a search requesting no attributes (1.1) and all attributes (*, +)
     *
     * @throws Exception
     */
    @Test
    public void testSearchNoAttributesAllAttributes() throws Exception
    {
        EntryCursor cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "1.1", "*", "+" );
        int count = 0;
        Entry entry = null;

        while ( cursor.next() )
        {
            entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( entry );

        assertEquals( 14, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
        assertTrue( entry.containsAttribute( "entryDN" ) );
        assertTrue( entry.containsAttribute( "entryParentId" ) );
        assertTrue( entry.containsAttribute( "subschemaSubentry" ) );
        assertTrue( entry.containsAttribute( "nbChildren" ) );
        assertTrue( entry.containsAttribute( "nbSubordinates" ) );
        assertTrue( entry.containsAttribute( "hasSubordinates" ) );
        assertTrue( entry.containsAttribute( "structuralObjectclass" ) );
    }


    /**
     *  DIRSERVER-1600
     */
    @Test
    public void testSearchTypesOnly() throws Exception
    {
        SearchRequest sr = new SearchRequestImpl();
        sr.setBase( new Dn( "uid=admin,ou=system" ) );
        sr.setFilter( "(uid=admin)" );
        sr.setScope( SearchScope.OBJECT );
        sr.setTypesOnly( true );

        Cursor<Response> cursor = connection.search( sr );
        int count = 0;
        Entry response = null;

        while ( cursor.next() )
        {
            response = ( ( SearchResultEntry ) cursor.get() ).getEntry();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNull( response.get( SchemaConstants.UID_AT ).get() );
    }
}
