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

package org.apache.directory.server.dhcp;


import java.nio.ByteBuffer;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.server.dhcp.io.DhcpMessageDecoder;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class DhcpMessageDecoderTest extends AbstractDhcpTestCase
{
    private ByteBuffer requestByteBuffer;


    public DhcpMessageDecoderTest()
    {
        super( DhcpMessageDecoderTest.class );
    }


    @Test
    public void testParseDiscover() throws Exception
    {
        requestByteBuffer = getByteBufferFromFile( "DHCPDISCOVER.pdu" );

        DhcpMessageDecoder decoder = new DhcpMessageDecoder();
        DhcpMessage dhcpRequest = decoder.decode( requestByteBuffer );

        print( dhcpRequest );
    }


    @Test
    public void testParseOffer() throws Exception
    {
        requestByteBuffer = getByteBufferFromFile( "DHCPOFFER.pdu" );

        DhcpMessageDecoder decoder = new DhcpMessageDecoder();
        DhcpMessage dhcpRequest = decoder.decode( requestByteBuffer );

        print( dhcpRequest );
    }
}
