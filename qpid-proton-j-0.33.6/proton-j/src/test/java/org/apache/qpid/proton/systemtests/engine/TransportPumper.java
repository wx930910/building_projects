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
 *
 */
package org.apache.qpid.proton.systemtests.engine;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.TransportResult;

public class TransportPumper
{
    private static final String SERVER_ROLE = "server";
    private static final String CLIENT_ROLE = "client";

    private final Transport _clientTransport;
    private final Transport _serverTransport;

    public TransportPumper(Transport clientTransport, Transport serverTransport)
    {
        _clientTransport = clientTransport;
        _serverTransport = serverTransport;
    }

    public void pumpAll()
    {
        boolean bytesToTransfer = true;
        while(bytesToTransfer)
        {
            int clientOutputLength = pumpOnceFromClientToServer();
            int serverOutputLength = pumpOnceFromServerToClient();
            bytesToTransfer = clientOutputLength > 0 || serverOutputLength > 0;
        }
    }

    public int pumpOnceFromClientToServer()
    {
        return pumpOnce(_clientTransport, CLIENT_ROLE, _serverTransport, SERVER_ROLE);
    }

    public int pumpOnceFromServerToClient()
    {
        return pumpOnce(_serverTransport, SERVER_ROLE, _clientTransport, CLIENT_ROLE);
    }

    private int pumpOnce(Transport transportFrom, String fromRole, Transport transportTo, String toRole)
    {
        int outputLength = transportFrom.pending();
        if (outputLength > 0)
        {
            ByteBuffer outputBuffer = transportFrom.head();

            int remaining = outputBuffer.remaining();
            assertTrue("Unexpected remaining in buffer: " + remaining + " vs " + outputLength, remaining >= outputLength);

            byte[] output = new byte[remaining];
            outputBuffer.get(output);

            transportFrom.pop(remaining);

            ByteBuffer inputBuffer = transportTo.getInputBuffer();
            inputBuffer.put(output, 0, output.length);

            TransportResult result = transportTo.processInput();
            result.checkIsOk();
        }

        return outputLength;
    }

}
