package org.apache.commons.jcs3.utils.struct;

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

import java.util.Map;

import org.apache.commons.jcs3.log.Log;
import org.apache.commons.jcs3.log.LogManager;
import junit.framework.TestCase;

/**
 * This ensures that the jcs version of the LRU map is as fast as the commons
 * version. It has been testing at .6 to .7 times the commons LRU.
 *
 */
public class JCSvsCommonsLRUMapPerformanceTest
    extends TestCase
{
    /** jcs / commons */
    float ratioPut = 0;

    /** jcs / commons */
    float ratioGet = 0;

    /** goal */
    float target = 1.0f;

    /** loops */
    int loops = 20;

    /** number to test with */
    int tries = 100000;

    /**
     * A unit test for JUnit
     *
     * @throws Exception
     *                Description of the Exception
     */
    public void testSimpleLoad()
        throws Exception
    {
        Log log = LogManager.getLog( LRUMap.class );
        if ( log.isDebugEnabled() )
        {
            System.out.println( "The log level must be at info or above for the a performance test." );
            return;
        }

        doWork();
        assertTrue( this.ratioPut < target );
        assertTrue( this.ratioGet < target );
    }

    /**
     *
     */
    public void doWork()
    {
        long start = 0;
        long end = 0;
        long time = 0;
        float tPer = 0;

        long putTotalJCS = 0;
        long getTotalJCS = 0;
        long putTotalHashtable = 0;
        long getTotalHashtable = 0;

        String name = "LRUMap";
        String cache2Name = "";

        try
        {
            Map<String, String> cache = new LRUMap<>( tries );

            for ( int j = 0; j < loops; j++ )
            {
                name = "JCS      ";
                start = System.currentTimeMillis();
                for ( int i = 0; i < tries; i++ )
                {
                    cache.put( "key:" + i, "data" + i );
                }
                end = System.currentTimeMillis();
                time = end - start;
                putTotalJCS += time;
                tPer = Float.intBitsToFloat( (int) time ) / Float.intBitsToFloat( tries );
                System.out.println( name + " put time for " + tries + " = " + time + "; millis per = " + tPer );

                start = System.currentTimeMillis();
                for ( int i = 0; i < tries; i++ )
                {
                    cache.get( "key:" + i );
                }
                end = System.currentTimeMillis();
                time = end - start;
                getTotalJCS += time;
                tPer = Float.intBitsToFloat( (int) time ) / Float.intBitsToFloat( tries );
                System.out.println( name + " get time for " + tries + " = " + time + "; millis per = " + tPer );

                // /////////////////////////////////////////////////////////////
                cache2Name = "Commons  ";
                // or LRUMapJCS
                Map<String, String> cache2 = new org.apache.commons.collections4.map.LRUMap<>( tries );
                // cache2Name = "Hashtable";
                // Hashtable cache2 = new Hashtable();
                start = System.currentTimeMillis();
                for ( int i = 0; i < tries; i++ )
                {
                    cache2.put( "key:" + i, "data" + i );
                }
                end = System.currentTimeMillis();
                time = end - start;
                putTotalHashtable += time;
                tPer = Float.intBitsToFloat( (int) time ) / Float.intBitsToFloat( tries );
                System.out.println( cache2Name + " put time for " + tries + " = " + time + "; millis per = " + tPer );

                start = System.currentTimeMillis();
                for ( int i = 0; i < tries; i++ )
                {
                    cache2.get( "key:" + i );
                }
                end = System.currentTimeMillis();
                time = end - start;
                getTotalHashtable += time;
                tPer = Float.intBitsToFloat( (int) time ) / Float.intBitsToFloat( tries );
                System.out.println( cache2Name + " get time for " + tries + " = " + time + "; millis per = " + tPer );

                System.out.println( "\n" );
            }

        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            System.out.println( e );
        }

        long putAvJCS = putTotalJCS / loops;
        long getAvJCS = getTotalJCS / loops;
        long putAvHashtable = putTotalHashtable / loops;
        long getAvHashtable = getTotalHashtable / loops;

        System.out.println( "Finished " + loops + " loops of " + tries + " gets and puts" );

        System.out.println( "\n" );
        System.out.println( "Put average for LRUMap       = " + putAvJCS );
        System.out.println( "Put average for " + cache2Name + " = " + putAvHashtable );
        ratioPut = Float.intBitsToFloat( (int) putAvJCS ) / Float.intBitsToFloat( (int) putAvHashtable );
        System.out.println( name + " puts took " + ratioPut + " times the " + cache2Name + ", the goal is <" + target
            + "x" );

        System.out.println( "\n" );
        System.out.println( "Get average for LRUMap       = " + getAvJCS );
        System.out.println( "Get average for " + cache2Name + " = " + getAvHashtable );
        ratioGet = Float.intBitsToFloat( (int) getAvJCS ) / Float.intBitsToFloat( (int) getAvHashtable );
        System.out.println( name + " gets took " + ratioGet + " times the " + cache2Name + ", the goal is <" + target
            + "x" );
    }
}
