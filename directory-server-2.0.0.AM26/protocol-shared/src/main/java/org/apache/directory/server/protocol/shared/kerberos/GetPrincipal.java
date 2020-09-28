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
package org.apache.directory.server.protocol.shared.kerberos;


import java.text.ParseException;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;
import org.apache.directory.shared.kerberos.KerberosAttribute;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.codec.types.SamType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;


/**
 * Encapsulates the action of looking up a principal in an embedded ApacheDS DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GetPrincipal
{
    /** The name of the principal to get. */
    private final KerberosPrincipal principal;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param principal The principal to search for in the directory.
     */
    public GetPrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
    }


    /**
     * Note that the base is a relative path from the existing context.
     * It is not a Dn.
     * 
     * @param session The Session
     * @param base The principal's Dn
     * @return The found entry
     * @throws Exception If the execution failed
     */
    public Object execute( CoreSession session, Dn base ) throws Exception
    {
        if ( principal == null )
        {
            return null;
        }

        Entry entry = StoreUtils.findPrincipalEntry( session, base, principal.getName() );
        
        if ( entry == null )
        {
            return null;
        }
        else
        {
            return getEntry( entry );
        }
    }


    /**
     * Marshals an a PrincipalStoreEntry from an Attributes object.
     *
     * @param dn the distinguished name of the Kerberos principal
     * @param attrs the attributes of the Kerberos principal
     * @return the entry for the principal
     * @throws LdapException if there are any access problems
     */
    private PrincipalStoreEntry getEntry( Entry entry ) throws LdapException
    {
        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();

        modifier.setDistinguishedName( entry.getDn().getName() );

        String principalname = entry.get( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ).getString();
        modifier.setPrincipal( new KerberosPrincipal( principalname, PrincipalNameType.KRB_NT_PRINCIPAL.getValue() ) );

        String keyVersionNumber = entry.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).getString();
        modifier.setKeyVersionNumber( Integer.parseInt( keyVersionNumber ) );

        if ( entry.get( KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT ) != null )
        {
            String val = entry.get( KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT ).getString();
            modifier.setDisabled( "true".equalsIgnoreCase( val ) );
        }

        if ( entry.get( KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT ) != null )
        {
            String val = entry.get( KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT ).getString();
            modifier.setLockedOut( "true".equalsIgnoreCase( val ) );
        }

        if ( entry.get( KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT ) != null )
        {
            String val = entry.get( KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT ).getString();
            try
            {
                modifier.setExpiration( KerberosTime.getTime( val ) );
            }
            catch ( ParseException e )
            {
                throw new LdapInvalidAttributeValueException( ResultCodeEnum.OPERATIONS_ERROR,
                    "Account expiration attribute "
                    + KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT 
                    + " contained an invalid value for generalizedTime: "
                    + val );
            }
        }

        if ( entry.get( KerberosAttribute.APACHE_SAM_TYPE_AT ) != null )
        {
            String samType = entry.get( KerberosAttribute.APACHE_SAM_TYPE_AT ).getString();
            modifier.setSamType( SamType.getTypeByOrdinal( Integer.parseInt( samType ) ) );
        }

        if ( entry.get( KerberosAttribute.KRB5_KEY_AT ) != null )
        {
            Attribute krb5key = entry.get( KerberosAttribute.KRB5_KEY_AT );
            
            try
            {
                Map<EncryptionType, EncryptionKey> keyMap = modifier.reconstituteKeyMap( krb5key );
                modifier.setKeyMap( keyMap );
            }
            catch ( KerberosException ioe )
            {
                throw new LdapInvalidAttributeValueException( ResultCodeEnum.OPERATIONS_ERROR, 
                    I18n.err( I18n.ERR_623, KerberosAttribute.KRB5_KEY_AT ), ioe );
            }
        }

        return modifier.getEntry();
    }
}
