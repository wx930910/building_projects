package org.apache.commons.jcs3;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.apache.commons.jcs3.access.CacheAccess;

import junit.framework.TestCase;

/**
 * Simple test for the JCS class.
 */
public class JCSUnitTest
    extends TestCase
{
    /** A random for key generation. */
    Random random = new Random();

    /**
     * @throws Exception
     */
    public void testJCS()
        throws Exception
    {
        CacheAccess<String, LinkedList<HashMap<String, String>>> jcs = JCS.getInstance( "testCache1" );

        LinkedList<HashMap<String, String>> list = buildList();

        jcs.put( "some:key", list );

        assertEquals( list, jcs.get( "some:key" ) );
    }

    /**
     * @return builds a list
     */
    private LinkedList<HashMap<String, String>> buildList()
    {
        LinkedList<HashMap<String, String>> list = new LinkedList<>();

        for ( int i = 0; i < 100; i++ )
        {
            list.add( buildMap() );
        }

        return list;
    }

    /**
     * @return a map
     */
    private HashMap<String, String> buildMap()
    {
        HashMap<String, String> map = new HashMap<>();

        byte[] keyBytes = new byte[32];
        byte[] valBytes = new byte[128];

        for ( int i = 0; i < 10; i++ )
        {
            random.nextBytes( keyBytes );
            random.nextBytes( valBytes );

            map.put( new String( keyBytes ), new String( valBytes ) );
        }

        return map;
    }
}
