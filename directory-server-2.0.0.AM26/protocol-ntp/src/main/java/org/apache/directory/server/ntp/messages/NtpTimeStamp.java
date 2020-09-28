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

package org.apache.directory.server.ntp.messages;


import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/**
 * NTP timestamps are represented as a 64-bit unsigned fixed-point number,
 * in seconds relative to 0h on 1 January 1900. The integer part is in the
 * first 32 bits and the fraction part in the last 32 bits. In the fraction
 * part, the non-significant low order can be set to 0.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtpTimeStamp
{
    /**
     * The number of milliseconds difference between the Java epoch and
     * the NTP epoch ( January 1, 1900, 00:00:00 GMT ).
     */
    private static final long NTP_EPOCH_DIFFERENCE = -2208988800000L;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS z", Locale.ROOT );

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );
    
    static
    {
        DATE_FORMAT.setTimeZone( UTC_TIME_ZONE );
    }

    private long seconds = 0;
    private long fraction = 0;


    /**
     * Creates a new instance of NtpTimeStamp that represents the time "right now."
     */
    public NtpTimeStamp()
    {
        this( new Date() );
    }


    /**
     * Creates a new instance of NtpTimeStamp that represents the given {@link Date}.
     *
     * @param date
     */
    public NtpTimeStamp( Date date )
    {
        long msSinceStartOfNtpEpoch = date.getTime() - NTP_EPOCH_DIFFERENCE;

        seconds = msSinceStartOfNtpEpoch / 1000;
        fraction = ( ( msSinceStartOfNtpEpoch % 1000 ) * 0x100000000L ) / 1000;
    }


    /**
     * Creates a new instance of NtpTimeStamp from encoded data in a {@link ByteBuffer}.
     *
     * @param data
     */
    public NtpTimeStamp( ByteBuffer data )
    {
        for ( int ii = 0; ii < 4; ii++ )
        {
            seconds = 256 * seconds + makePositive( data.get() );
        }

        for ( int ii = 4; ii < 8; ii++ )
        {
            fraction = 256 * fraction + makePositive( data.get() );
        }
    }


    /**
     * Writes this {@link NtpTimeStamp} to the given {@link ByteBuffer}.
     *
     * @param buffer
     */
    public void writeTo( ByteBuffer buffer )
    {
        byte[] bytes = new byte[8];

        long temp = seconds;
        for ( int ii = 3; ii >= 0; ii-- )
        {
            bytes[ii] = ( byte ) ( temp % 256 );
            temp = temp / 256;
        }

        temp = fraction;
        for ( int ii = 7; ii >= 4; ii-- )
        {
            bytes[ii] = ( byte ) ( temp % 256 );
            temp = temp / 256;
        }

        buffer.put( bytes );
    }


    public String toString()
    {
        long msSinceStartOfNtpEpoch = seconds * 1000 + ( fraction * 1000 ) / 0x100000000L;
        Date date = new Date( msSinceStartOfNtpEpoch + NTP_EPOCH_DIFFERENCE );

        synchronized ( DATE_FORMAT )
        {
            return "org.apache.ntp.message.NtpTimeStamp[ date = " + DATE_FORMAT.format( date ) + " ]";
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 37;
        hash = hash * 17 + Long.valueOf( seconds ).hashCode();
        hash = hash * 17 + Long.valueOf( fraction ).hashCode();

        return hash;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof NtpTimeStamp ) )
        {
            return false;
        }

        NtpTimeStamp that = ( NtpTimeStamp ) o;
        return ( this.seconds == that.seconds ) && ( this.fraction == that.fraction );
    }


    private int makePositive( byte b )
    {
        int byteAsInt = b;
        return ( byteAsInt < 0 ) ? 256 + byteAsInt : byteAsInt;
    }
}
