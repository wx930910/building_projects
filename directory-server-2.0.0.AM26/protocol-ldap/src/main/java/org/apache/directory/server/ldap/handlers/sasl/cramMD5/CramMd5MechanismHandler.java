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
package org.apache.directory.server.ldap.handlers.sasl.cramMD5;


import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.sasl.AbstractMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.SaslConstants;


/**
 * The CRAM-MD Sasl mechanism handler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CramMd5MechanismHandler extends AbstractMechanismHandler
{
    public SaslServer handleMechanism( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        SaslServer ss = ( SaslServer ) ldapSession.getSaslProperty( SaslConstants.SASL_SERVER );

        // TODO - don't use session properties anymore
        if ( ss == null )
        {
            String saslHost = ldapSession.getLdapServer().getSaslHost();
            String userBaseDn = ldapSession.getLdapServer().getSearchBaseDn();
            ldapSession.putSaslProperty( SaslConstants.SASL_HOST, saslHost );
            ldapSession.putSaslProperty( SaslConstants.SASL_USER_BASE_DN, userBaseDn );
            Map<String, String> saslProps = new HashMap<>();

            CoreSession adminSession = ldapSession.getLdapServer().getDirectoryService().getAdminSession();

            CallbackHandler callbackHandler = new CramMd5CallbackHandler( ldapSession, adminSession, bindRequest );

            ss = Sasl.createSaslServer( SupportedSaslMechanisms.CRAM_MD5, SaslConstants.LDAP_PROTOCOL, saslHost,
                saslProps, callbackHandler );
            ldapSession.putSaslProperty( SaslConstants.SASL_SERVER, ss );
        }

        return ss;
    }


    /**
     * {@inheritDoc}
     */
    public void init( LdapSession ldapSession )
    {
        // Store the host in the ldap session
        String saslHost = ldapSession.getLdapServer().getSaslHost();
        ldapSession.putSaslProperty( SaslConstants.SASL_HOST, saslHost );
    }


    /**
     * Remove the SaslServer and Mechanism property.
     * 
     * @param ldapSession the Ldapsession instance
     */
    public void cleanup( LdapSession ldapSession )
    {
        ldapSession.clearSaslProperties();
    }

}
