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

package org.apache.directory.server.dns.protocol;


import java.util.ArrayList;

import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.DnsMessageModifier;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.service.DnsContext;
import org.apache.directory.server.dns.service.DomainNameService;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnsProtocolHandler extends IoHandlerAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( DnsProtocolHandler.class );

    private DnsServer config;
    private RecordStore store;
    private String contextKey = "context";


    /**
     * Creates a new instance of DnsProtocolHandler.
     *
     * @param config
     * @param store
     */
    public DnsProtocolHandler( DnsServer config, RecordStore store )
    {
        this.config = config;
        this.store = store;
    }


    @Override
    public void sessionCreated( IoSession session ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} CREATED:  {}", session.getRemoteAddress(), session.getTransportMetadata() );
        }

        if ( session.getTransportMetadata().isConnectionless() )
        {
            session.getFilterChain().addFirst( "codec",
                new ProtocolCodecFilter( DnsProtocolUdpCodecFactory.getInstance() ) );
        }
        else
        {
            session.getFilterChain().addFirst( "codec",
                new ProtocolCodecFilter( DnsProtocolTcpCodecFactory.getInstance() ) );
        }
    }


    @Override
    public void sessionOpened( IoSession session )
    {
        LOG.debug( "{} OPENED", session.getRemoteAddress() );
    }


    @Override
    public void sessionClosed( IoSession session )
    {
        LOG.debug( "{} CLOSED", session.getRemoteAddress() );
    }


    @Override
    public void sessionIdle( IoSession session, IdleStatus status )
    {
        LOG.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
    }


    @Override
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        LOG.error( session.getRemoteAddress() + " EXCEPTION", cause );
        session.closeNow();
    }


    @Override
    public void messageReceived( IoSession session, Object message )
    {
        LOG.debug( "{} RCVD:  {}", session.getRemoteAddress(), message );

        try
        {
            DnsContext dnsContext = new DnsContext();
            dnsContext.setConfig( config );
            dnsContext.setStore( store );
            session.setAttribute( getContextKey(), dnsContext );

            DomainNameService.execute( dnsContext, ( DnsMessage ) message );

            DnsMessage response = dnsContext.getReply();

            session.write( response );
        }
        catch ( Exception e )
        {
            LOG.error( e.getLocalizedMessage(), e );

            DnsMessage request = ( DnsMessage ) message;
            DnsException de = ( DnsException ) e;

            DnsMessageModifier modifier = new DnsMessageModifier();

            modifier.setTransactionId( request.getTransactionId() );
            modifier.setMessageType( MessageType.RESPONSE );
            modifier.setOpCode( OpCode.QUERY );
            modifier.setAuthoritativeAnswer( false );
            modifier.setTruncated( false );
            modifier.setRecursionDesired( request.isRecursionDesired() );
            modifier.setRecursionAvailable( false );
            modifier.setReserved( false );
            modifier.setAcceptNonAuthenticatedData( false );
            modifier.setResponseCode( ResponseCode.convert( ( byte ) de.getResponseCode() ) );
            modifier.setQuestionRecords( request.getQuestionRecords() );
            modifier.setAnswerRecords( new ArrayList<ResourceRecord>() );
            modifier.setAuthorityRecords( new ArrayList<ResourceRecord>() );
            modifier.setAdditionalRecords( new ArrayList<ResourceRecord>() );

            session.write( modifier.getDnsMessage() );
        }
    }


    @Override
    public void messageSent( IoSession session, Object message )
    {
        LOG.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }

    
    @Override
    public void inputClosed( IoSession session )
    {
    }
}
