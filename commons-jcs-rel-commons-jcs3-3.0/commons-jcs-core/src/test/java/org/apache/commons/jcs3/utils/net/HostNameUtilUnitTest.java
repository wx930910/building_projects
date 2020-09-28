package org.apache.commons.jcs3.utils.net;

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

import junit.framework.TestCase;

import java.net.UnknownHostException;

/** Tests for the host name util. */
public class HostNameUtilUnitTest
    extends TestCase
{
    /**
     * It's nearly impossible to unit test the getLocalHostLANAddress method.
     * <p>
     * @throws UnknownHostException
     */
    public void testGetLocalHostAddress_Simple() throws UnknownHostException
    {
        // DO WORK
        String result = HostNameUtil.getLocalHostAddress();

        // VERIFY
        //System.out.print( result );
        assertNotNull( "Should have a host address.", result );
    }
}
