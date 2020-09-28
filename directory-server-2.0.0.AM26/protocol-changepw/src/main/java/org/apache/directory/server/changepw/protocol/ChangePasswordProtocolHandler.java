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

package org.apache.directory.server.changepw.protocol;


import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.changepw.exceptions.ChangePasswordException;
import org.apache.directory.server.changepw.exceptions.ErrorType;
import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.service.ChangePasswordContext;
import org.apache.directory.server.changepw.service.ChangePasswordService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordProtocolHandler implements IoHandler
{
    private static final Logger log = LoggerFactory.getLogger( ChangePasswordProtocolHandler.class );

    private ChangePasswordServer config;
    private PrincipalStore store;
    private String contextKey = "context";


    /**
     * Creates a new instance of ChangePasswordProtocolHandler.
     *
     * @param config
     * @param store
     */
    public ChangePasswordProtocolHandler( ChangePasswordServer config, PrincipalStore store )
    {
        this.config = config;
        this.store = store;
    }


    public void sessionCreated( IoSession session ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} CREATED:  {}", session.getRemoteAddress(), session.getTransportMetadata() );
        }

        if ( session.getTransportMetadata().isConnectionless() )
        {
            session.getFilterChain().addFirst( "codec",
                new ProtocolCodecFilter( ChangePasswordUdpProtocolCodecFactory.getInstance() ) );
        }
        else
        {
            session.getFilterChain().addFirst( "codec",
                new ProtocolCodecFilter( ChangePasswordTcpProtocolCodecFactory.getInstance() ) );
        }
    }


    public void sessionOpened( IoSession session )
    {
        log.debug( "{} OPENED", session.getRemoteAddress() );
    }


    public void sessionClosed( IoSession session )
    {
        log.debug( "{} CLOSED", session.getRemoteAddress() );
    }


    public void sessionIdle( IoSession session, IdleStatus status )
    {
        log.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
    }


    public void exceptionCaught( IoSession session, Throwable cause )
    {
        log.debug( session.getRemoteAddress() + " EXCEPTION", cause );
        session.close( true );
    }


    public void messageReceived( IoSession session, Object message )
    {
        log.debug( "{} RCVD:  {}", session.getRemoteAddress(), message );

        InetAddress clientAddress = ( ( InetSocketAddress ) session.getRemoteAddress() ).getAddress();
        ChangePasswordRequest request = ( ChangePasswordRequest ) message;

        try
        {
            ChangePasswordContext changepwContext = new ChangePasswordContext();
            changepwContext.setConfig( config );
            changepwContext.setStore( store );
            changepwContext.setClientAddress( clientAddress );
            changepwContext.setRequest( request );
            session.setAttribute( getContextKey(), changepwContext );

            ChangePasswordService.execute( session, changepwContext );

            session.write( changepwContext.getReply() );
        }
        catch ( KerberosException ke )
        {
            if ( log.isDebugEnabled() )
            {
                log.warn( ke.getLocalizedMessage(), ke );
            }
            else
            {
                log.warn( ke.getLocalizedMessage() );
            }

            KrbError errorMessage = getErrorMessage( config.getServicePrincipal(), ke );

            ChangePasswordErrorModifier modifier = new ChangePasswordErrorModifier();
            modifier.setErrorMessage( errorMessage );

            session.write( modifier.getChangePasswordError() );
        }
        catch ( Exception e )
        {
            log.error( I18n.err( I18n.ERR_152, e.getLocalizedMessage() ), e );

            session.write( getErrorMessage( config.getServicePrincipal(), new ChangePasswordException(
                ErrorType.KRB5_KPASSWD_UNKNOWN_ERROR ) ) );
        }
    }


    public void messageSent( IoSession session, Object message )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
        }
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }


    private KrbError getErrorMessage( KerberosPrincipal principal, KerberosException exception )
    {
        KrbError krbError = new KrbError();

        KerberosTime now = new KerberosTime();

        krbError.setErrorCode( ErrorType.getTypeByOrdinal( exception.getErrorCode() ) );
        krbError.setEText( exception.getLocalizedMessage() );
        krbError.setSName( principal );
        krbError.setSTime( now );
        krbError.setSusec( 0 );
        krbError.setEData( buildExplanatoryData( exception ) );

        return krbError;
    }


    private byte[] buildExplanatoryData( KerberosException exception )
    {
        short resultCode = ( short ) exception.getErrorCode();

        byte[] resultString =
            { ( byte ) 0x00 };

        if ( exception.getExplanatoryData() == null || exception.getExplanatoryData().length == 0 )
        {
            try
            {
                resultString = exception.getLocalizedMessage().getBytes( "UTF-8" );
            }
            catch ( UnsupportedEncodingException uee )
            {
                log.error( uee.getLocalizedMessage() );
            }
        }
        else
        {
            resultString = exception.getExplanatoryData();
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate( 256 );
        byteBuffer.putShort( resultCode );
        byteBuffer.put( resultString );

        byteBuffer.flip();
        byte[] explanatoryData = new byte[byteBuffer.remaining()];
        byteBuffer.get( explanatoryData, 0, explanatoryData.length );

        return explanatoryData;
    }
}
