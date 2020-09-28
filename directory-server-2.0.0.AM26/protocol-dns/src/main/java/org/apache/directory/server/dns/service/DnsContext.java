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
package org.apache.directory.server.dns.service;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.RecordStore;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnsContext
{
    private static final long serialVersionUID = -5911142975867852436L;

    private DnsServer config;
    private RecordStore store;
    private DnsMessage reply;
    private List<ResourceRecord> records = new ArrayList<>();


    /**
     * @return Returns the recordEntry.
     */
    public List<ResourceRecord> getResourceRecords()
    {
        return records;
    }


    /**
     * @param resourceRecord The resourceRecord to add.
     */
    public void addResourceRecord( ResourceRecord resourceRecord )
    {
        this.records.add( resourceRecord );
    }


    /**
     * @param resourceRecords The resourceRecords to add.
     */
    public void addResourceRecords( Collection<ResourceRecord> resourceRecords )
    {
        this.records.addAll( resourceRecords );
    }


    /**
     * @return Returns the config.
     */
    public DnsServer getConfig()
    {
        return config;
    }


    /**
     * @param config The config to set.
     */
    public void setConfig( DnsServer config )
    {
        this.config = config;
    }


    /**
     * @return Returns the reply.
     */
    public DnsMessage getReply()
    {
        return reply;
    }


    /**
     * @param reply The reply to set.
     */
    public void setReply( DnsMessage reply )
    {
        this.reply = reply;
    }


    /**
     * @return Returns the store.
     */
    public RecordStore getStore()
    {
        return store;
    }


    /**
     * @param store The store to set.
     */
    public void setStore( RecordStore store )
    {
        this.store = store;
    }
}
