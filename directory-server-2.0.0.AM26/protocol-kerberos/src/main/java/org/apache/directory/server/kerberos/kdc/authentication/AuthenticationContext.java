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
package org.apache.directory.server.kerberos.kdc.authentication;


import org.apache.directory.server.kerberos.kdc.KdcContext;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * A context used to store and manage Authentication elements
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationContext extends KdcContext
{
    private static final long serialVersionUID = -2249170923251265359L;

    /** The Kerberos Ticket associated to this context */
    private Ticket ticket;

    /** The client key */
    private EncryptionKey clientKey;

    /** The client entry */
    private PrincipalStoreEntry clientEntry;

    /** The server entry */
    private PrincipalStoreEntry serverEntry;

    /** Tell if we have had a pre-authentication */
    private boolean isPreAuthenticated;


    /**
     * @return Returns the serverEntry.
     */
    public PrincipalStoreEntry getServerEntry()
    {
        return serverEntry;
    }


    /**
     * @param serverEntry The serverEntry to set.
     */
    public void setServerEntry( PrincipalStoreEntry serverEntry )
    {
        this.serverEntry = serverEntry;
    }


    /**
     * @return Returns the clientEntry.
     */
    public PrincipalStoreEntry getClientEntry()
    {
        return clientEntry;
    }


    /**
     * @param clientEntry The clientEntry to set.
     */
    public void setClientEntry( PrincipalStoreEntry clientEntry )
    {
        this.clientEntry = clientEntry;
    }


    /**
     * @return Returns the checksumEngines.
     *
    public Map getChecksumEngines()
    {
        return checksumEngines;
    }
    */

    /**
     * @param checksumEngines The checksumEngines to set.
     *
    public void setChecksumEngines( Map checksumEngines )
    {
        this.checksumEngines = checksumEngines;
    }
    */

    /**
     * @return Returns the clientKey.
     */
    public EncryptionKey getClientKey()
    {
        return clientKey;
    }


    /**
     * @param clientKey The clientKey to set.
     */
    public void setClientKey( EncryptionKey clientKey )
    {
        this.clientKey = clientKey;
    }


    /**
     * @return Returns the ticket.
     */
    public Ticket getTicket()
    {
        return ticket;
    }


    /**
     * @param ticket The ticket to set.
     */
    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }


    /**
     * @return true if the client used pre-authentication.
     */
    public boolean isPreAuthenticated()
    {
        return isPreAuthenticated;
    }


    /**
     * @param isPreAuthenticated Whether the client used pre-authentication.
     */
    public void setPreAuthenticated( boolean isPreAuthenticated )
    {
        this.isPreAuthenticated = isPreAuthenticated;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "AuthenticationContext \n" );
        sb.append( super.toString() ).append( '\n' );
        sb.append( "PreAuth : " ).append( isPreAuthenticated ).append( "\n" );
        sb.append( "Client Entry : " ).append( clientEntry ).append( "\n" );

        return sb.toString();
    }
}
