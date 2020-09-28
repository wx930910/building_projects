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
package org.apache.directory.server.ldap.handlers.sasl.gssapi;


import javax.naming.Context;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.AuthorizeCallback;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.protocol.shared.kerberos.GetPrincipal;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.sasl.AbstractSaslCallbackHandler;
import org.apache.directory.server.ldap.handlers.sasl.SaslConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GssapiCallbackHandler extends AbstractSaslCallbackHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( GssapiCallbackHandler.class );


    /**
     * Creates a new instance of GssapiCallbackHandler.
     *
     * @param ldapSession the mina IO session
     * @param adminSession the admin session
     * @param bindRequest the bind message
     */
    public GssapiCallbackHandler( LdapSession ldapSession, CoreSession adminSession, BindRequest bindRequest )
    {
        super( adminSession.getDirectoryService(), bindRequest );
        this.ldapSession = ldapSession;
        this.adminSession = adminSession;
    }


    protected Attribute lookupPassword( String username, String password )
    {
        // do nothing, password not used by GSSAPI
        return null;
    }


    protected void authorize( AuthorizeCallback authorizeCB ) throws Exception
    {
        LOG.debug( "Processing conversion of principal name to Dn." );

        String username = authorizeCB.getAuthorizationID();

        // find the user's entry
        GetPrincipal getPrincipal = new GetPrincipal( new KerberosPrincipal( username ) );
        PrincipalStoreEntry entry = ( PrincipalStoreEntry ) getPrincipal.execute( adminSession, new Dn( ldapSession
            .getLdapServer().getSearchBaseDn() ) );
        String bindDn = entry.getDistinguishedName();

        LOG.debug( "Converted username {} to Dn {}.", username, bindDn );

        LdapPrincipal ldapPrincipal = new LdapPrincipal( adminSession.getDirectoryService().getSchemaManager(),
            new Dn( entry.getDistinguishedName() ),
            AuthenticationLevel.STRONG, Strings.EMPTY_BYTES );
        ldapSession.putSaslProperty( SaslConstants.SASL_AUTHENT_USER, ldapPrincipal );
        ldapSession.putSaslProperty( Context.SECURITY_PRINCIPAL, bindDn );

        authorizeCB.setAuthorizedID( bindDn );
        authorizeCB.setAuthorized( true );
    }
}
