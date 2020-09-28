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


import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordModifier;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.directory.server.dns.store.jndi.DnsOperation;


/**
 * A JNDI context operation for looking up a Resource Record with flat attributes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GetFlatRecord implements DnsOperation
{
    /** The name of the question to get. */
    private final QuestionRecord question;


    /**
     * Creates the action to be used against the embedded JNDI provider.
     * 
     * @param question 
     */
    public GetFlatRecord( QuestionRecord question )
    {
        this.question = question;
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

        Attributes matchAttrs = new BasicAttributes( true );

        matchAttrs.put( new BasicAttribute( DnsAttribute.NAME, question.getDomainName() ) );
        matchAttrs.put( new BasicAttribute( DnsAttribute.TYPE, question.getRecordType().name() ) );
        matchAttrs.put( new BasicAttribute( DnsAttribute.CLASS, question.getRecordClass().name() ) );

        Set<ResourceRecord> record = new HashSet<>();

        NamingEnumeration<SearchResult> answer = ctx.search( base, matchAttrs );

        if ( answer.hasMore() )
        {
            SearchResult result = answer.next();

            Attributes attrs = result.getAttributes();

            if ( attrs == null )
            {
                return null;
            }

            record.add( getRecord( attrs ) );
        }

        return record;
    }


    /**
     * Marshals a RecordStoreEntry from an Attributes object.
     *
     * @param attrs the attributes of the DNS question
     * @return the entry for the question
     * @throws NamingException if there are any access problems
     */
    private ResourceRecord getRecord( Attributes attrs ) throws NamingException
    {
        ResourceRecordModifier modifier = new ResourceRecordModifier();

        String dnsName = getAttrOrNull( attrs, DnsAttribute.NAME );
        String dnsType = getAttrOrNull( attrs, DnsAttribute.TYPE );
        String dnsClass = getAttrOrNull( attrs, DnsAttribute.CLASS );
        String dnsTtl = getAttrOrNull( attrs, DnsAttribute.TTL );

        modifier.setDnsName( dnsName );
        modifier.setDnsType( RecordType.valueOf( dnsType ) );
        modifier.setDnsClass( RecordClass.valueOf( dnsClass ) );
        modifier.setDnsTtl( Integer.parseInt( dnsTtl ) );

        NamingEnumeration<String> ids = attrs.getIDs();

        while ( ids.hasMore() )
        {
            String id = ids.next();
            modifier.put( id, ( String ) attrs.get( id ).get() );
        }

        return modifier.getEntry();
    }


    private String getAttrOrNull( Attributes attrs, String name ) throws NamingException
    {
        Attribute attr = attrs.get( name );
        return attr != null ? ( String ) attr.get() : null;
    }
}
