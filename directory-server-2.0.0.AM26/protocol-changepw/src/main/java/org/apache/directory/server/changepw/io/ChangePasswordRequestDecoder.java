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

import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.messages.ChangePasswordRequestModifier;
import org.apache.directory.server.kerberos.protocol.KerberosDecoder;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.KrbPriv;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordRequestDecoder
{
    /**
     * Decodes a {@link ByteBuffer} into a {@link ChangePasswordRequest}.
     *
     * @param buf
     * @return The {@link ChangePasswordRequest}.
     * @throws IOException
     */
    public ChangePasswordRequest decode( ByteBuffer buf ) throws IOException
    {
        ChangePasswordRequestModifier modifier = new ChangePasswordRequestModifier();

        buf.getShort(); // message length

        modifier.setProtocolVersionNumber( buf.getShort() );

        short authHeaderLength = buf.getShort();

        byte[] undecodedAuthHeader = new byte[authHeaderLength];
        buf.get( undecodedAuthHeader, 0, authHeaderLength );

        ApReq authHeader = KerberosDecoder.decodeApReq( undecodedAuthHeader );

        modifier.setAuthHeader( authHeader );

        byte[] encodedPrivate = new byte[buf.remaining()];
        buf.get( encodedPrivate, 0, buf.remaining() );

        KrbPriv privMessage = KerberosDecoder.decodeKrbPriv( encodedPrivate );

        modifier.setPrivateMessage( privMessage );

        return modifier.getChangePasswordMessage();
    }
}
