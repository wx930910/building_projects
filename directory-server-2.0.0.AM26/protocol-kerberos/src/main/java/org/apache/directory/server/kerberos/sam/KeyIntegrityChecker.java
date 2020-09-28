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
package org.apache.directory.server.kerberos.sam;


import javax.security.auth.kerberos.KerberosKey;


/**
 * Checks the integrity of a kerberos key to decode-decrypt an encrypted
 * generalized timestamp representing the pre-auth data.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface KeyIntegrityChecker
{
    /**
     * Checks the integrity of a KerberosKey to decrypt-decode and compare an
     * encrypted encoded generalized timestamp representing the preauth data.
     *
     * @param preauthData the generalized timestamp encrypted with client hotp
     * generated KerberosKey
     * @param key the KerberosKey generated from server side hotp value
     * @return true if the key can decrypt-decode and make sense out of the
     * timestamp verifying that it is in skew, false otherwise
     */
    boolean checkKeyIntegrity( byte[] preauthData, KerberosKey key );
}
