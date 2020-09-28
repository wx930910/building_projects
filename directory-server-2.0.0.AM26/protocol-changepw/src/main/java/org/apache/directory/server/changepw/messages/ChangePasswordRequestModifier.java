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
package org.apache.directory.server.changepw.messages;


import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;
import org.apache.directory.shared.kerberos.messages.ApReq;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordRequestModifier extends AbstractPasswordMessageModifier
{
    private ApReq authHeader;
    private PrivateMessage privateMessage;


    /**
     * Returns the {@link ChangePasswordRequest}.
     *
     * @return The {@link ChangePasswordRequest}.
     */
    public ChangePasswordRequest getChangePasswordMessage()
    {
        return new ChangePasswordRequest( versionNumber, authHeader, privateMessage );
    }


    /**
     * Sets the AuthHeader.
     *
     * @param authHeader
     */
    public void setAuthHeader( ApReq authHeader )
    {
        this.authHeader = authHeader;
    }


    /**
     * Sets the {@link PrivateMessage}.
     *
     * @param privateMessage
     */
    public void setPrivateMessage( PrivateMessage privateMessage )
    {
        this.privateMessage = privateMessage;
    }
}
