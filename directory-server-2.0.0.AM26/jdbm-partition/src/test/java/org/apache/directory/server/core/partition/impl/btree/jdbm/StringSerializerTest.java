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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class StringSerializerTest
{
    @Test
    public void testRandom() throws IOException
    {
        StringSerializer serializer = StringSerializer.INSTANCE;

        for ( int i = 0; i < 100; i++ )
        {
            String str = RandomStringUtils.random( i );
            byte[] serialized = serializer.serialize( str );
            String deserialized = ( String ) serializer.deserialize( serialized );
            assertEquals( str, deserialized );
        }
    }


    char getChar( byte[] bites )
    {
        int ch = bites[0] << 8 & 0x0000FF00;
        ch |= bites[1] & 0x000000FF;

        return ( char ) ch;
    }


    byte[] getBytes( char ch )
    {
        byte[] bites = new byte[2];
        bites[0] = ( byte ) ( ch >> 8 & 0x00FF );
        bites[1] = ( byte ) ( ch & 0x00FF );

        return bites;
    }


    @Test
    public void testConversion()
    {
        for ( char ch = 0; ch < 16383; ch++ )
        {
            byte[] bites = getBytes( ch );
            char deserialized = getChar( bites );
            assertEquals( ch, deserialized );
        }
    }
}
