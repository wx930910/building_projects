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
package org.apache.directory.server.kerberos.kdc.ticketgrant;


import org.apache.directory.server.kerberos.kdc.KdcContext;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketGrantingContext extends KdcContext
{
    private static final long serialVersionUID = 2130665703752837491L;

    private ApReq authHeader;
    private Ticket tgt;
    private Ticket newTicket;
    private Authenticator authenticator;

    private PrincipalStoreEntry ticketPrincipalEntry;
    private PrincipalStoreEntry requestPrincipalEntry;


    /**
     * @return Returns the requestPrincipalEntry.
     */
    public PrincipalStoreEntry getRequestPrincipalEntry()
    {
        return requestPrincipalEntry;
    }


    /**
     * @param requestPrincipalEntry The requestPrincipalEntry to set.
     */
    public void setRequestPrincipalEntry( PrincipalStoreEntry requestPrincipalEntry )
    {
        this.requestPrincipalEntry = requestPrincipalEntry;
    }


    /**
     * @return Returns the ticketPrincipalEntry.
     */
    public PrincipalStoreEntry getTicketPrincipalEntry()
    {
        return ticketPrincipalEntry;
    }


    /**
     * @param ticketPrincipalEntry The ticketPrincipalEntry to set.
     */
    public void setTicketPrincipalEntry( PrincipalStoreEntry ticketPrincipalEntry )
    {
        this.ticketPrincipalEntry = ticketPrincipalEntry;
    }


    /**
     * @return Returns the authenticator.
     */
    public Authenticator getAuthenticator()
    {
        return authenticator;
    }


    /**
     * @param authenticator The authenticator to set.
     */
    public void setAuthenticator( Authenticator authenticator )
    {
        this.authenticator = authenticator;
    }


    /**
     * @return Returns the newTicket.
     */
    public Ticket getNewTicket()
    {
        return newTicket;
    }


    /**
     * @param newTicket The newTicket to set.
     */
    public void setNewTicket( Ticket newTicket )
    {
        this.newTicket = newTicket;
    }


    /**
     * @return Returns the tgt.
     */
    public Ticket getTgt()
    {
        return tgt;
    }


    /**
     * @param tgt The tgt to set.
     */
    public void setTgt( Ticket tgt )
    {
        this.tgt = tgt;
    }


    /**
     * @return Returns the authHeader.
     */
    public ApReq getAuthHeader()
    {
        return authHeader;
    }


    /**
     * @param authHeader The authHeader to set.
     */
    public void setAuthHeader( ApReq authHeader )
    {
        this.authHeader = authHeader;
    }
}
