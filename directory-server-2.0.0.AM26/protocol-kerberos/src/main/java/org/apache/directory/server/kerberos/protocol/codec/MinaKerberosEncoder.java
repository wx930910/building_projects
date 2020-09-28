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
package org.apache.directory.server.kerberos.protocol.codec;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.shared.kerberos.codec.KerberosEncoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MinaKerberosEncoder extends ProtocolEncoderAdapter
{

    @Override
    public void encode( IoSession session, Object message, ProtocolEncoderOutput out ) throws Exception
    {
        Asn1Object asn1Obj = ( Asn1Object ) message;
        boolean isTcp = !session.getTransportMetadata().isConnectionless();

        ByteBuffer encodedByteBuf = KerberosEncoder.encode( asn1Obj, isTcp );
        IoBuffer buf = IoBuffer.allocate( encodedByteBuf.remaining() );
        buf.put( encodedByteBuf.array() );
        buf.flip();
        out.write( buf );
    }
}
