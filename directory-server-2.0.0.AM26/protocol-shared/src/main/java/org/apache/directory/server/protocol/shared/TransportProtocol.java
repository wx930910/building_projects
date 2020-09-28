/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.protocol.shared;


/**
 * Type safe enumeration for the transport protocol.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum TransportProtocol
{
    TCP(0, "TCP"), UDP(1, "UDP");

    private final int intValue;
    private final String stringValue;


    TransportProtocol( int intValue, String stringValue )
    {
        this.intValue = intValue;
        this.stringValue = stringValue;
    }


    /**
     * Gets an integer value for switches.
     *
     * @return ordinal integer value
     */
    public int getIntValue()
    {
        return intValue;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return stringValue;
    }
}
