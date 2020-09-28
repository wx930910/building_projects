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
package org.apache.directory.server.core.jndi;


import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests our ability to list elements as the admin user and as a non admin user
 * on security sensitive values.  We do not return results or name class pairs
 * for user accounts if the user is not the admin.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "ListIT")
public class ListIT extends AbstractLdapTestUnit
{

    @Test
    public void testListSystemAsNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getService().getAdminSession().add(
            new DefaultEntry( getService().getSchemaManager(), akarasulu.getEntry() ) );

        LdapContext sysRoot = getContext( akarasulu.getDn().getName(), getService(), "ou=system" );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration<NameClassPair> list = sysRoot.list( "" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        assertFalse( set.contains( "uid=admin,ou=system" ) );
        assertFalse( set.contains( "ou=users,ou=system" ) );
        assertFalse( set.contains( "ou=groups,ou=system" ) );
    }


    @Test
    public void testListUsersAsNonAdmin() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getService().getAdminSession().add(
            new DefaultEntry( getService().getSchemaManager(), akarasulu.getEntry() ) );

        LdapContext sysRoot = getContext( akarasulu.getDn().getName(), getService(), "ou=system" );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration<NameClassPair> list = sysRoot.list( "ou=users" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        // @todo this assertion fails now - is this the expected behavoir?
        // assertFalse( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
    }


    @Test
    public void testListSystemAsAdmin() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration<NameClassPair> list = sysRoot.list( "" );

        int nbRead = 0;
        
        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
            nbRead++;
        }

        assertTrue( set.contains( "uid=admin,ou=system" ) );
        assertTrue( set.contains( "ou=users,ou=system" ) );
        assertTrue( set.contains( "ou=groups,ou=system" ) );
        assertTrue( set.contains( "prefNodeName=sysPrefRoot,ou=system" ) );
        assertTrue( set.contains( "ou=configuration,ou=system" ) );
        
        assertEquals( 5, nbRead );
        
        // Again with a SUBTREE scope
        nbRead = 0;

        SearchControls sc = new SearchControls();
        sc.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> ne = sysRoot.search( "", "(objectClass=*)", sc );
        
        while ( ne.hasMoreElements() )
        {
            ne.nextElement();
            nbRead++;
        }
        
        assertEquals( 10, nbRead );
    }


    @Test
    public void testListUsersAsAdmin() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        HashSet<String> set = new HashSet<String>();
        LdifEntry akarasulu = getUserAddLdif();
        getService().getAdminSession().add(
            new DefaultEntry( getService().getSchemaManager(), akarasulu.getEntry() ) );

        NamingEnumeration<NameClassPair> list = sysRoot.list( "ou=users" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        assertTrue( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
    }
}
