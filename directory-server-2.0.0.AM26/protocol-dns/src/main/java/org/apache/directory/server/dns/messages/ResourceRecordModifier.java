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
package org.apache.directory.server.dns.messages;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.util.Strings;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ResourceRecordModifier
{
    private String dnsName;
    private RecordType dnsType;
    private RecordClass dnsClass;
    private int dnsTtl;

    private Map<String, Object> attributes = new HashMap<>();


    /**
     * Returns the {@link ResourceRecord} built by this {@link ResourceRecordModifier}.
     *
     * @return The {@link ResourceRecord}.
     */
    public ResourceRecord getEntry()
    {
        return new ResourceRecordImpl( dnsName, dnsType, dnsClass, dnsTtl, attributes );
    }


    /**
     * @param dnsName The dnsName to set.
     */
    public void setDnsName( String dnsName )
    {
        this.dnsName = dnsName;
    }


    /**
     * @param dnsType The dnsType to set.
     */
    public void setDnsType( RecordType dnsType )
    {
        this.dnsType = dnsType;
    }


    /**
     * @param dnsClass The dnsClass to set.
     */
    public void setDnsClass( RecordClass dnsClass )
    {
        this.dnsClass = dnsClass;
    }


    /**
     * @param dnsTtl The dnsTtl to set.
     */
    public void setDnsTtl( int dnsTtl )
    {
        this.dnsTtl = dnsTtl;
    }


    /**
     * @param id The id to set
     * @param value The value to set 
     */
    public void put( String id, String value )
    {
        attributes.put( Strings.toLowerCaseAscii( id ), value );
    }
}
