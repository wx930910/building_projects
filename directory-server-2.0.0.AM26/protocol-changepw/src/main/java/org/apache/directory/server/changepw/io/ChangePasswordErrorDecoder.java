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
package org.apache.directory.server.changepw.io;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.directory.server.changepw.messages.ChangePasswordError;
import org.apache.directory.server.kerberos.protocol.KerberosDecoder;
import org.apache.directory.shared.kerberos.messages.KrbError;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordErrorDecoder
{
    private static final int HEADER_LENGTH = 6;


    /**
     * Decodes a {@link ByteBuffer} into a {@link ChangePasswordError}.
     *
     * @param buf
     * @return The {@link ChangePasswordError}.
     * @throws IOException
     */
    public ChangePasswordError decode( ByteBuffer buf ) throws IOException
    {
        ChangePasswordErrorModifier modifier = new ChangePasswordErrorModifier();

        short messageLength = buf.getShort();

        modifier.setProtocolVersionNumber( buf.getShort() );

        // AP_REQ length will be 0 for error messages
        buf.getShort(); // authHeader length

        int errorLength = messageLength - HEADER_LENGTH;

        byte[] errorBytes = new byte[errorLength];

        buf.get( errorBytes );
        ByteBuffer errorBuffer = ByteBuffer.wrap( errorBytes );

        KrbError errorMessage = KerberosDecoder.decodeKrbError( errorBuffer );

        modifier.setErrorMessage( errorMessage );

        return modifier.getChangePasswordError();
    }
}
