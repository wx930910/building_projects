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
package org.apache.directory.server.kerberos.kdc;


import java.net.InetAddress;

import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;


/**
 * The context used to store the collected and computed data while processing a 
 * kerberos message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class KdcContext
{
    private static final long serialVersionUID = 6490030984626825108L;

    /** The KDC server configuration */
    private KerberosConfig config;
    private PrincipalStore store;

    /** The request being processed */
    private KdcReq request;

    /** The kerberos response */
    private KerberosMessage reply;

    /** The client IP address */
    private InetAddress clientAddress;
    private CipherTextHandler cipherTextHandler;

    /** The encryption type */
    private EncryptionType encryptionType;

    /** the replay cache */
    private ReplayCache replayCache;

    /**
     * @return Returns the config.
     */
    public KerberosConfig getConfig()
    {
        return config;
    }


    /**
     * @param config The config to set.
     */
    public void setConfig( KerberosConfig config )
    {
        this.config = config;
    }


    /**
     * @return Returns the store.
     */
    public PrincipalStore getStore()
    {
        return store;
    }


    /**
     * @param store The store to set.
     */
    public void setStore( PrincipalStore store )
    {
        this.store = store;
    }


    /**
     * @return Returns the request.
     */
    public KdcReq getRequest()
    {
        return request;
    }


    /**
     * @param request The request to set.
     */
    public void setRequest( KdcReq request )
    {
        this.request = request;
    }


    /**
     * @return Returns the reply.
     */
    public KerberosMessage getReply()
    {
        return reply;
    }


    /**
     * @param reply The reply to set.
     */
    public void setReply( KerberosMessage reply )
    {
        this.reply = reply;
    }


    /**
     * @return Returns the clientAddress.
     */
    public InetAddress getClientAddress()
    {
        return clientAddress;
    }


    /**
     * @param clientAddress The clientAddress to set.
     */
    public void setClientAddress( InetAddress clientAddress )
    {
        this.clientAddress = clientAddress;
    }


    /**
     * @return Returns the {@link CipherTextHandler}.
     */
    public CipherTextHandler getCipherTextHandler()
    {
        return cipherTextHandler;
    }


    /**
     * @param cipherTextHandler The {@link CipherTextHandler} to set.
     */
    public void setCipherTextHandler( CipherTextHandler cipherTextHandler )
    {
        this.cipherTextHandler = cipherTextHandler;
    }


    /**
     * Returns the encryption type to use for this session.
     *
     * @return The encryption type.
     */
    public EncryptionType getEncryptionType()
    {
        return encryptionType;
    }


    /**
     * Sets the encryption type to use for this session.
     *
     * @param encryptionType The encryption type to set.
     */
    public void setEncryptionType( EncryptionType encryptionType )
    {
        this.encryptionType = encryptionType;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Req : " ).append( request.toString( "    " ) );
        sb.append( "Client address : " ).append( clientAddress );

        if ( encryptionType != null )
        {
            sb.append( '\n' );
            sb.append( "EncryptionType : " ).append( encryptionType );
        }

        return sb.toString();
    }
    
    
    /**
     * sets the replay cache
     *
     * @param replayCache The Replay cache instance
     */
    public void setReplayCache( ReplayCache replayCache )
    {
        this.replayCache = replayCache;
    }


    /**
     * @return the replay cache
     */
    public ReplayCache getReplayCache()
    {
        return replayCache;
    }
}
