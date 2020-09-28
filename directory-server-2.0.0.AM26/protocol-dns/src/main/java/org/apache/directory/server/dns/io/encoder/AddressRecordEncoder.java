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

package org.apache.directory.server.dns.io.encoder;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.core.buffer.IoBuffer;


/**
 * 3.4.1. A RDATA format
 * 
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                    ADDRESS                    |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * ADDRESS         A 32 bit Internet address.
 * 
 * Hosts that have multiple Internet addresses will have multiple A
 * records.
 * 
 * A records cause no additional section processing.  The RDATA section of
 * an A line in a master file is an Internet address expressed as four
 * decimal numbers separated by dots without any imbedded spaces (e.g.,
 * "10.2.0.52" or "192.0.5.6").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddressRecordEncoder extends ResourceRecordEncoder
{
    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    protected void putResourceRecordData( IoBuffer byteBuffer, ResourceRecord record )
    {
        String ipAddress = record.get( DnsAttribute.IP_ADDRESS );

        try
        {
            byteBuffer.put( InetAddress.getByName( ipAddress ).getAddress() );
        }
        catch ( UnknownHostException uhe )
        {
        }
    }
}
