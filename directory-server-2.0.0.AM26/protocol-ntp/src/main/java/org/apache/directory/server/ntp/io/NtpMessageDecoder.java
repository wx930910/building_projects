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

package org.apache.directory.server.ntp.io;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.directory.server.ntp.messages.LeapIndicatorType;
import org.apache.directory.server.ntp.messages.ModeType;
import org.apache.directory.server.ntp.messages.NtpMessage;
import org.apache.directory.server.ntp.messages.NtpMessageModifier;
import org.apache.directory.server.ntp.messages.NtpTimeStamp;
import org.apache.directory.server.ntp.messages.ReferenceIdentifier;
import org.apache.directory.server.ntp.messages.StratumType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtpMessageDecoder
{
    /**
     * Decodes the {@link ByteBuffer} into an {@link NtpMessage}.
     *
     * @param request
     * @return The {@link NtpMessage}.
     */
    public NtpMessage decode( ByteBuffer request )
    {
        NtpMessageModifier modifier = new NtpMessageModifier();

        byte header = request.get();
        modifier.setLeapIndicator( parseLeapIndicator( header ) );
        modifier.setVersionNumber( parseVersionNumber( header ) );
        modifier.setMode( parseMode( header ) );
        modifier.setStratum( parseStratum( request ) );
        modifier.setPollInterval( parsePollInterval( request ) );
        modifier.setPrecision( parsePrecision( request ) );
        modifier.setRootDelay( parseRootDelay( request ) );
        modifier.setRootDispersion( parseRootDispersion( request ) );
        modifier.setReferenceIdentifier( parseReferenceIdentifier( request ) );
        modifier.setReferenceTimestamp( new NtpTimeStamp( request ) );
        modifier.setOriginateTimestamp( new NtpTimeStamp( request ) );

        byte[] unneededBytes = new byte[8];
        request.get( unneededBytes );

        modifier.setReceiveTimestamp( new NtpTimeStamp() );
        modifier.setTransmitTimestamp( new NtpTimeStamp( request ) );

        return modifier.getNtpMessage();
    }


    private LeapIndicatorType parseLeapIndicator( byte header )
    {
        return LeapIndicatorType.getTypeByOrdinal( ( header & 0xc0 ) >>> 6 );
    }


    private int parseVersionNumber( byte header )
    {
        return ( header & 0x38 ) >>> 3;
    }


    private ModeType parseMode( byte header )
    {
        return ModeType.getTypeByOrdinal( header & 0x07 );
    }


    private StratumType parseStratum( ByteBuffer request )
    {
        return StratumType.getTypeByOrdinal( request.get() );
    }


    private byte parsePollInterval( ByteBuffer bytes )
    {
        return ( byte ) Math.round( Math.pow( 2, bytes.get() ) );
    }


    private byte parsePrecision( ByteBuffer bytes )
    {
        return ( byte ) ( 1000 * Math.pow( 2, bytes.get() ) );
    }


    private ReferenceIdentifier parseReferenceIdentifier( ByteBuffer request )
    {
        byte[] nextFourBytes = new byte[4];
        request.get( nextFourBytes );
        return ReferenceIdentifier.getTypeByName( new String( nextFourBytes, StandardCharsets.UTF_8 ) );
    }


    private int parseRootDelay( ByteBuffer bytes )
    {
        int temp = 256 * ( 256 * ( 256 * bytes.get() + bytes.get() ) + bytes.get() ) + bytes.get();
        return 1000 * ( temp / 0x10000 );
    }


    private int parseRootDispersion( ByteBuffer bytes )
    {
        int temp = 256 * ( 256 * ( 256 * bytes.get() + bytes.get() ) + bytes.get() ) + bytes.get();
        return 1000 * ( temp / 0x10000 );
    }
}
