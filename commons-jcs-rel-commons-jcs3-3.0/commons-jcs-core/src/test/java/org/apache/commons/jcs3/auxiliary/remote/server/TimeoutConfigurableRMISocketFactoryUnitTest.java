package org.apache.commons.jcs3.auxiliary.remote.server;

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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/** Unit tests for the custom factory */
public class TimeoutConfigurableRMISocketFactoryUnitTest
    extends TestCase
{
    /**
     * Simple test to see that we can create a server socket and connect.
     * <p>
     * @throws IOException
     */
    public void testCreateAndConnect() throws IOException
    {
        // SETUP
        int port = 3455;
        String host = "localhost";
        TimeoutConfigurableRMISocketFactory factory = new TimeoutConfigurableRMISocketFactory();

        // DO WORK
        ServerSocket serverSocket = factory.createServerSocket( port );
        Socket socket = factory.createSocket( host, port );
        socket.close();
        serverSocket.close();

        // VERIFY
        // passive, no errors
    }
}
