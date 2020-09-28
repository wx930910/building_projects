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


import org.apache.directory.server.kerberos.shared.messages.application.ApplicationReply;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordReplyModifier extends AbstractPasswordMessageModifier
{
    private ApplicationReply applicationReply;
    private PrivateMessage privateMessage;


    /**
     * Returns the {@link ChangePasswordReply}.
     *
     * @return The {@link ChangePasswordReply}.
     */
    public ChangePasswordReply getChangePasswordReply()
    {
        return new ChangePasswordReply( versionNumber, applicationReply, privateMessage );
    }


    /**
     * Sets the {@link ApplicationReply}.
     *
     * @param applicationReply
     */
    public void setApplicationReply( ApplicationReply applicationReply )
    {
        this.applicationReply = applicationReply;
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
