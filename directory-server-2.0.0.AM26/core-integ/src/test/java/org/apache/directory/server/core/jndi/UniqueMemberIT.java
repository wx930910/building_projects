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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.constants.JndiPropertyConstants;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test DIRSERVER-757 : a UniqueMember attribute should only contain a Dn completed with an
 * optional UID. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "UniqueMemeberIT")
public class UniqueMemberIT extends AbstractLdapTestUnit
{

    /**
     * Test a valid entry
     *
     * @throws Exception on error
     */
    @Test
    public void testValidUniqueMember() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey, dc=example, dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        Attributes returned = sysRoot.getAttributes( "cn=kevin Spacey" );

        NamingEnumeration<? extends Attribute> attrList = returned.getAll();

        while ( attrList.hasMore() )
        {
            Attribute attr = attrList.next();

            if ( attr.getID().equalsIgnoreCase( "cn" ) )
            {
                assertEquals( "kevin Spacey", attr.get() );
                continue;
            }

            if ( attr.getID().equalsIgnoreCase( "objectClass" ) )
            {
                NamingEnumeration<?> values = attr.getAll();
                Set<String> expectedValues = new HashSet<String>();

                expectedValues.add( "top" );
                expectedValues.add( "groupofuniquenames" );

                while ( values.hasMoreElements() )
                {
                    String value = Strings.toLowerCaseAscii( ( ( String ) values.nextElement() ) );
                    assertTrue( expectedValues.contains( value ) );
                    expectedValues.remove( value );
                }

                assertEquals( 0, expectedValues.size() );
                continue;
            }

            if ( attr.getID().equalsIgnoreCase( "uniqueMember" ) )
            {
                assertEquals( "cn=kevin spacey, dc=example, dc=org", attr.get() );
            }
        }
    }


    /**
     * Test a valid entry, with an optional UID
     *
     * @throws Exception on error
     */
    @Test
    public void testValidUniqueMemberWithOptionnalUID() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey 2" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey 2, dc=example, dc=org#'010101'B" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey 2";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        Attributes returned = sysRoot.getAttributes( "cn=kevin Spacey 2" );

        NamingEnumeration<? extends Attribute> attrList = returned.getAll();

        while ( attrList.hasMore() )
        {
            Attribute attr = attrList.next();

            if ( attr.getID().equalsIgnoreCase( "cn" ) )
            {
                assertEquals( "kevin Spacey 2", attr.get() );
                continue;
            }

            if ( attr.getID().equalsIgnoreCase( "objectClass" ) )
            {
                NamingEnumeration<?> values = attr.getAll();
                Set<String> expectedValues = new HashSet<String>();

                expectedValues.add( "top" );
                expectedValues.add( "groupofuniquenames" );

                while ( values.hasMoreElements() )
                {
                    String value = Strings.toLowerCaseAscii( ( ( String ) values.nextElement() ) );
                    assertTrue( expectedValues.contains( value ) );
                    expectedValues.remove( value );
                }

                assertEquals( 0, expectedValues.size() );
                continue;
            }

            if ( attr.getID().equalsIgnoreCase( "uniqueMember" ) )
            {
                assertEquals( "cn=kevin spacey 2, dc=example, dc=org#'010101'B", attr.get() );
            }
        }
    }


    /**
     * Test a valid entry, with an optional UID
     *
     * @throws Exception on error
     */
    @Test
    public void testInvalidUniqueMemberBadDN() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey bad" );
        Attribute dc = new BasicAttribute( "uniqueMember", "kevin spacey bad, dc=example, dc=org#'010101'B" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey bad";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Test a valid entry, with an optional UID
     *
     * @throws Exception on error
     */
    @Test
    public void testInvalidUniqueMemberBadUID() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey bad 2" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey bad 2, dc=example, dc=org#'010101'" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey bad 2";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    @Test
    public void testSearchUniqueMemberFilter() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey, dc=example, dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES,
            AliasDerefMode.NEVER_DEREF_ALIASES.getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "",
            "(uniqueMember=cn = kevin spacey, dc=example, dc=org)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( Strings.toLowerCaseAscii( result.getName() ), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        attrs = map.get( "cn=kevin spacey,ou=system" );

        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "uniqueMember" ) );
    }


    @Test
    public void testSearchUniqueMemberFilterWithSpaces() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey,dc=example,dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES,
            AliasDerefMode.NEVER_DEREF_ALIASES.getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "",
            "(uniqueMember=cn = Kevin  Spacey , dc = example , dc = ORG)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( Strings.toLowerCaseAscii( result.getName() ), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        attrs = map.get( "cn=kevin spacey,ou=system" );

        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "uniqueMember" ) );
    }


    @Test
    public void testSearchUniqueMemberFilterWithBadDN() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey,dc=example,dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES,
            AliasDerefMode.NEVER_DEREF_ALIASES.getJndiValue() );

        NamingEnumeration<SearchResult> list = sysRoot.search( "", "(uniqueMember=cn=cevin spacey,dc=example,dc=org)",
            controls );

        assertFalse( list.hasMore() );
    }


    @Test
    public void testSearchUniqueMemberFilterWithUID() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute oc = new BasicAttribute( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new BasicAttribute( "cn", "kevin Spacey" );
        Attribute dc = new BasicAttribute( "uniqueMember", "cn=kevin spacey,dc=example,dc=org#'010101'B" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc );

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES,
            AliasDerefMode.NEVER_DEREF_ALIASES.getJndiValue() );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration<SearchResult> list = sysRoot.search( "",
            "(uniqueMember=cn= Kevin Spacey, dc=example, dc=org #'010101'B)", controls );

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            map.put( Strings.toLowerCaseAscii( result.getName() ), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );

        attrs = map.get( "cn=kevin spacey,ou=system" );

        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "uniqueMember" ) );
    }
}
