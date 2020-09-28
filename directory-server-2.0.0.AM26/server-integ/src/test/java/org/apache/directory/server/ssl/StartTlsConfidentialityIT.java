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
package org.apache.directory.server.ssl;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.directory.api.util.Network;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.integ.ServerIntegrationUtils;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test case to verify proper operation of confidentiality requirements as 
 * specified in https://issues.apache.org/jira/browse/DIRSERVER-1189.  
 * 
 * Starts up the server binds via SUN JNDI provider to perform various 
 * operations on entries which should be rejected when a TLS secured 
 * connection is not established.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "StartTlsConfidentialityIT-class")
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP"),
            @CreateTransport(protocol = "LDAPS")
    },
    extendedOpHandlers =
        { StartTlsHandler.class })
public class StartTlsConfidentialityIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( StartTlsConfidentialityIT.class );

    boolean oldConfidentialityRequiredValue;

    /**
     * Sets up the key store and installs the self signed certificate for the 
     * server (created on first startup) which is to be used by the StartTLS 
     * JDNDI client that will connect.  The key store is created from scratch
     * programmatically and whipped on each run.  The certificate is acquired 
     * by pulling down the bytes for administrator's userCertificate from 
     * uid=admin,ou=system.  We use sysRoot direct context instead of one over
     * the wire since the server is configured to prevent connections without
     * TLS secured connections.
     */
    @Before
    public void installKeyStoreWithCertificate() throws Exception
    {
        oldConfidentialityRequiredValue = getLdapServer().isConfidentialityRequired();

        System.setProperty( "javax.net.ssl.trustStore", ldapServer.getKeystoreFile() );
        System.setProperty( "javax.net.ssl.trustStorePassword", "secret" );
        System.setProperty( "javax.net.ssl.keyStore", ldapServer.getKeystoreFile() );
        System.setProperty( "javax.net.ssl.keyStorePassword", "secret" );

    }


    /**
     * Just deletes the generated key store file.
     */
    @After
    public void deleteKeyStore() throws Exception
    {
        getLdapServer().setConfidentialityRequired( oldConfidentialityRequiredValue );

        System.clearProperty( "javax.net.ssl.trustStore" );
        System.clearProperty( "javax.net.ssl.trustStorePassword" );
        System.clearProperty( "javax.net.ssl.keyStore" );
        System.clearProperty( "javax.net.ssl.keyStorePassword" );
    }


    private LdapContext getSecuredContext() throws Exception
    {
        LOG.debug( "testStartTls() test starting ... " );

        // Set up environment for creating initial context
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );

        // Must use the name of the server that is found in its certificate?
        env.put( Context.PROVIDER_URL, Network.ldapLoopbackUrl( getLdapServer().getPort() ) );

        // Create initial context
        LOG.debug( "About to get initial context" );
        LdapContext ctx = new InitialLdapContext( env, null );

        // Start TLS
        LOG.debug( "About send startTls extended operation" );
        StartTlsResponse tls = ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
        LOG.debug( "Extended operation issued" );
        tls.setHostnameVerifier( new HostnameVerifier()
        {
            public boolean verify( String hostname, SSLSession session )
            {
                return true;
            }
        } );
        LOG.debug( "TLS negotion about to begin" );
        tls.negotiate( ReloadableSSLSocketFactory.getDefault() );
        return ctx;
    }


    /**
     * Checks to make sure insecure binds fail while secure binds succeed.
     */
    @Test
    public void testConfidentiality() throws Exception
    {
        getLdapServer().setConfidentialityRequired( true );

        // -------------------------------------------------------------------
        // Unsecured bind should fail
        // -------------------------------------------------------------------

        try
        {
            ServerIntegrationUtils.getWiredContext( getLdapServer() );
            fail( "Should not get here due to violation of confidentiality requirements" );
        }
        catch ( AuthenticationNotSupportedException e )
        {
        }

        // -------------------------------------------------------------------
        // get anonymous connection with StartTLS (no bind request sent)
        // -------------------------------------------------------------------

        LdapContext ctx = getSecuredContext();
        assertNotNull( ctx );

        // -------------------------------------------------------------------
        // upgrade connection via bind request (same physical connection - TLS)
        // -------------------------------------------------------------------

        ctx.addToEnvironment( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        ctx.addToEnvironment( Context.SECURITY_CREDENTIALS, "secret" );
        ctx.addToEnvironment( Context.SECURITY_AUTHENTICATION, "simple" );

        /*
         * Since Java 9 behavior of LdapContext.reconnect() changed. It no longer uses the existing
         * physical connection but always creates a new one, hence previous StartTLS is not established.
         * 
         * However calling reconnect explicitly is not mandatory, addToEnvironment causes bind
         * with the next operation.
         * 
         * Issue: https://bugs.openjdk.java.net/browse/JDK-8059009
         * Commit: http://hg.openjdk.java.net/jdk9/jdk9/jdk/rev/021b47694183
         */
        //ctx.reconnect( null );

        // -------------------------------------------------------------------
        // do a search and confirm
        // -------------------------------------------------------------------

        NamingEnumeration<SearchResult> results = ctx.search( "ou=system", "(objectClass=*)", new SearchControls() );
        Set<String> names = new HashSet<String>();
        while ( results.hasMore() )
        {
            names.add( results.next().getName() );
        }
        results.close();
        assertTrue( names.contains( "prefNodeName=sysPrefRoot" ) );
        assertTrue( names.contains( "ou=users" ) );
        assertTrue( names.contains( "ou=configuration" ) );
        assertTrue( names.contains( "uid=admin" ) );
        assertTrue( names.contains( "ou=groups" ) );

        // -------------------------------------------------------------------
        // do add and confirm
        // -------------------------------------------------------------------

        Attributes attrs = new BasicAttributes( "objectClass", "person", true );
        attrs.put( "sn", "foo" );
        attrs.put( "cn", "foo bar" );
        ctx.createSubcontext( "cn=foo bar,ou=system", attrs );
        assertNotNull( ctx.lookup( "cn=foo bar,ou=system" ) );

        // -------------------------------------------------------------------
        // do modify and confirm
        // -------------------------------------------------------------------

        ModificationItem[] mods = new ModificationItem[]
            {
                new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute( "cn", "fbar" ) )
        };
        ctx.modifyAttributes( "cn=foo bar,ou=system", mods );
        Attributes reread = ctx.getAttributes( "cn=foo bar,ou=system" );
        assertTrue( reread.get( "cn" ).contains( "fbar" ) );

        // -------------------------------------------------------------------
        // do rename and confirm 
        // -------------------------------------------------------------------

        ctx.rename( "cn=foo bar,ou=system", "cn=fbar,ou=system" );
        try
        {
            ctx.getAttributes( "cn=foo bar,ou=system" );
            fail( "old name of renamed entry should not be found" );
        }
        catch ( NameNotFoundException e )
        {
        }
        reread = ctx.getAttributes( "cn=fbar,ou=system" );
        assertTrue( reread.get( "cn" ).contains( "fbar" ) );

        // -------------------------------------------------------------------
        // do delete and confirm
        // -------------------------------------------------------------------

        ctx.destroySubcontext( "cn=fbar,ou=system" );
        try
        {
            ctx.getAttributes( "cn=fbar,ou=system" );
            fail( "deleted entry should not be found" );
        }
        catch ( NameNotFoundException e )
        {
        }

        ctx.close();
    }
}
