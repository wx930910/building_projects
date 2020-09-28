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

package org.apache.directory.server.dns.store.jndi.operations;


import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordModifier;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.directory.server.dns.store.jndi.DnsOperation;
import org.apache.directory.server.i18n.I18n;


/**
 * A JNDI context operation for looking up Resource Records from an embedded JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GetRecords implements DnsOperation
{
    /** The name of the question to get. */
    private final QuestionRecord question;


    /**
     * Creates the action to be used against the embedded JNDI provider.
     * 
     * @param question 
     */
    public GetRecords( QuestionRecord question )
    {
        this.question = question;
    }

    /**
     * Mappings of type to objectClass.
     */
    private static final Map<RecordType, String> TYPE_TO_OBJECTCLASS;

    static
    {
        EnumMap<RecordType, String> typeToObjectClass = new EnumMap<>( RecordType.class );
        typeToObjectClass.put( RecordType.SOA, "apacheDnsStartOfAuthorityRecord" );
        typeToObjectClass.put( RecordType.A, "apacheDnsAddressRecord" );
        typeToObjectClass.put( RecordType.NS, "apacheDnsNameServerRecord" );
        typeToObjectClass.put( RecordType.CNAME, "apacheDnsCanonicalNameRecord" );
        typeToObjectClass.put( RecordType.PTR, "apacheDnsPointerRecord" );
        typeToObjectClass.put( RecordType.MX, "apacheDnsMailExchangeRecord" );
        typeToObjectClass.put( RecordType.SRV, "apacheDnsServiceRecord" );
        typeToObjectClass.put( RecordType.TXT, "apacheDnsTextRecord" );

        TYPE_TO_OBJECTCLASS = Collections.unmodifiableMap( typeToObjectClass );
    }

    /**
     * Mappings of type to objectClass.
     */
    private static final Map<String, RecordType> OBJECTCLASS_TO_TYPE;

    static
    {
        Map<String, RecordType> objectClassToType = new HashMap<>();
        objectClassToType.put( "apacheDnsStartOfAuthorityRecord", RecordType.SOA );
        objectClassToType.put( "apacheDnsAddressRecord", RecordType.A );
        objectClassToType.put( "apacheDnsNameServerRecord", RecordType.NS );
        objectClassToType.put( "apacheDnsCanonicalNameRecord", RecordType.CNAME );
        objectClassToType.put( "apacheDnsPointerRecord", RecordType.PTR );
        objectClassToType.put( "apacheDnsMailExchangeRecord", RecordType.MX );
        objectClassToType.put( "apacheDnsServiceRecord", RecordType.SRV );
        objectClassToType.put( "apacheDnsTextRecord", RecordType.TXT );
        objectClassToType.put( "apacheDnsReferralNameServer", RecordType.NS );
        objectClassToType.put( "apacheDnsReferralAddress", RecordType.A );

        OBJECTCLASS_TO_TYPE = Collections.unmodifiableMap( objectClassToType );
    }


    /**
     * Note that the base is a relative path from the exiting context.
     * It is not a Dn.
     */
    public Set<ResourceRecord> execute( DirContext ctx, Name base ) throws Exception
    {
        if ( question == null )
        {
            return null;
        }

        String name = question.getDomainName();
        RecordType type = question.getRecordType();

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        String filter = "(objectClass=" + TYPE_TO_OBJECTCLASS.get( type ) + ")";

        NamingEnumeration<SearchResult> list = ctx.search( transformDomainName( name ), filter, controls );

        Set<ResourceRecord> set = new HashSet<>();

        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            Name relative = getRelativeName( ctx.getNameInNamespace(), result.getName() );

            set.add( getRecord( result.getAttributes(), relative ) );
        }

        return set;
    }


    /**
     * Marshals a RecordStoreEntry from an Attributes object.
     *
     * @param attrs the attributes of the DNS question
     * @return the entry for the question
     * @throws NamingException if there are any access problems
     */
    private ResourceRecord getRecord( Attributes attrs, Name relative ) throws NamingException
    {
        String soaMinimum = "86400";
        String soaClass = "IN";

        ResourceRecordModifier modifier = new ResourceRecordModifier();

        Attribute attr;

        // if no name, transform rdn
        attr = attrs.get( DnsAttribute.NAME );

        if ( attr != null )
        {
            modifier.setDnsName( ( String ) attr.get() );
        }
        else
        {
            relative = getDomainComponents( relative );

            String dnsName;
            dnsName = transformDistinguishedName( relative.toString() );
            modifier.setDnsName( dnsName );
        }

        // type is implicit in objectclass
        attr = attrs.get( DnsAttribute.TYPE );

        if ( attr != null )
        {
            modifier.setDnsType( RecordType.valueOf( ( String ) attr.get() ) );
        }
        else
        {
            modifier.setDnsType( getType( attrs.get( SchemaConstants.OBJECT_CLASS_AT ) ) );
        }

        // class defaults to SOA CLASS
        attr = attrs.get( DnsAttribute.CLASS );
        String dnsClass = attr != null ? ( String ) attr.get() : soaClass;
        modifier.setDnsClass( RecordClass.valueOf( dnsClass ) );

        // ttl defaults to SOA MINIMUM
        attr = attrs.get( DnsAttribute.TTL );
        String dnsTtl = attr != null ? ( String ) attr.get() : soaMinimum;
        modifier.setDnsTtl( Integer.parseInt( dnsTtl ) );

        NamingEnumeration<String> ids = attrs.getIDs();

        while ( ids.hasMore() )
        {
            String id = ids.next();
            modifier.put( id, ( String ) attrs.get( id ).get() );
        }

        return modifier.getEntry();
    }


    /**
     * Uses the algorithm in <a href="http://www.faqs.org/rfcs/rfc2247.html">RFC 2247</a>
     * to transform any Internet domain name into a distinguished name.
     *
     * @param domainName the domain name
     * @return the distinguished name
     */
    String transformDomainName( String domainName )
    {
        if ( domainName == null || domainName.length() == 0 )
        {
            return "";
        }

        StringBuilder buf = new StringBuilder( domainName.length() + 16 );

        buf.append( "dc=" );
        buf.append( domainName.replaceAll( "\\.", ",dc=" ) );

        return buf.toString();
    }


    /**
     * Uses the algorithm in <a href="http://www.faqs.org/rfcs/rfc2247.html">RFC 2247</a>
     * to transform a distinguished name into an Internet domain name.
     *
     * @param distinguishedName the distinguished name
     * @return the domain name
     */
    String transformDistinguishedName( String distinguishedName )
    {
        if ( distinguishedName == null || distinguishedName.length() == 0 )
        {
            return "";
        }

        String domainName = distinguishedName.replaceFirst( "dc=", "" );
        domainName = domainName.replaceAll( ",dc=", "." );

        return domainName;
    }


    private RecordType getType( Attribute objectClass ) throws NamingException
    {
        NamingEnumeration<?> list = objectClass.getAll();

        while ( list.hasMore() )
        {
            String value = ( String ) list.next();

            if ( !value.equals( "apacheDnsAbstractRecord" ) )
            {
                RecordType type = OBJECTCLASS_TO_TYPE.get( value );

                if ( type == null )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_646 ) );
                }

                return type;
            }
        }

        throw new NamingException( I18n.err( I18n.ERR_647 ) );
    }


    private Name getRelativeName( String nameInNamespace, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn = null;

        Name ctxRoot = new CompoundName( nameInNamespace, props );
        searchBaseDn = new CompoundName( baseDn, props );

        if ( !searchBaseDn.startsWith( ctxRoot ) )
        {
            throw new NamingException( I18n.err( I18n.ERR_648, baseDn ) );
        }

        for ( int ii = 0; ii < ctxRoot.size(); ii++ )
        {
            searchBaseDn.remove( 0 );
        }

        return searchBaseDn;
    }


    private Name getDomainComponents( Name name ) throws NamingException
    {
        for ( int ii = 0; ii < name.size(); ii++ )
        {
            if ( !name.get( ii ).startsWith( "dc=" ) )
            {
                name.remove( ii );
            }
        }

        return name;
    }
}
