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
import java.util.Enumeration;

import org.apache.directory.server.changepw.value.ChangePasswordData;
import org.apache.directory.server.changepw.value.ChangePasswordDataModifier;
import org.apache.directory.server.kerberos.shared.io.decoder.PrincipalNameDecoder;
import org.apache.directory.shared.asn1.der.ASN1InputStream;
import org.apache.directory.shared.asn1.der.DEREncodable;
import org.apache.directory.shared.asn1.der.DERGeneralString;
import org.apache.directory.shared.asn1.der.DEROctetString;
import org.apache.directory.shared.asn1.der.DERSequence;
import org.apache.directory.shared.asn1.der.DERTaggedObject;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordDataDecoder
{
    /**
     * Decodes bytes into a ChangePasswordData.
     *
     * @param encodedChangePasswdData
     * @return The {@link ChangePasswordData}.
     * @throws IOException
     */
    public ChangePasswordData decodeChangePasswordData( byte[] encodedChangePasswdData ) throws IOException
    {
        ASN1InputStream ais = new ASN1InputStream( encodedChangePasswdData );

        DERSequence sequence = ( DERSequence ) ais.readObject();

        return decodeChangePasswdData( sequence );
    }


    protected ChangePasswordData decodeChangePasswdData( DERSequence sequence )
    {
        ChangePasswordDataModifier modifier = new ChangePasswordDataModifier();

        for ( Enumeration e = sequence.getObjects(); e.hasMoreElements(); )
        {
            DERTaggedObject object = ( ( DERTaggedObject ) e.nextElement() );
            int tag = object.getTagNo();
            DEREncodable derObject = object.getObject();
            switch ( tag )
            {
                case 0:
                    DEROctetString tag0 = ( DEROctetString ) derObject;
                    modifier.setNewPassword( tag0.getOctets() );
                    break;
                case 1:
                    DERSequence tag1 = ( DERSequence ) derObject;
                    modifier.setTargetName( PrincipalNameDecoder.decode( tag1 ) );
                    break;
                case 2:
                    DERGeneralString tag2 = ( DERGeneralString ) derObject;
                    modifier.setTargetRealm( tag2.getString() );
                    break;
                default:
                    break;
            }
        }

        return modifier.getChangePasswdData();
    }
}
