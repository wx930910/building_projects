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
package org.apache.directory.server.ldap.handlers.sasl;


import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.apache.directory.api.ldap.model.constants.SaslQoP;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link IoFilterAdapter} that handles integrity and confidentiality protection
 * for a SASL bound session.  The SaslFilter must be constructed with a SASL
 * context that has completed SASL negotiation.  Some SASL mechanisms, such as
 * CRAM-MD5, only support authentication and thus do not need this filter.  DIGEST-MD5
 * and GSSAPI do support message integrity and confidentiality and, therefore,
 * do need this filter.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SaslFilter extends IoFilterAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger( SaslFilter.class );

    /**
     * A session attribute key that makes next one write request bypass
     * this filter (not adding a security layer).  This is a marker attribute,
     * which means that you can put whatever as its value. ({@link Boolean#TRUE}
     * is preferred.)  The attribute is automatically removed from the session
     * attribute map as soon as {@link IoSession#write(Object)} is invoked,
     * and therefore should be put again if you want to make more messages
     * bypass this filter.
     */
    public static final String DISABLE_SECURITY_LAYER_ONCE = SaslFilter.class.getName() + ".DisableSecurityLayerOnce";

    private SaslServer saslServer;


    /**
     * Creates a new instance of SaslFilter.  The SaslFilter must be constructed
     * with a SASL context that has completed SASL negotiation.  The SASL context
     * will be used to provide message integrity and, optionally, message
     * confidentiality.
     *
     * @param saslServer The initialized SASL context.
     */
    public SaslFilter( SaslServer saslServer )
    {
        if ( saslServer == null )
        {
            throw new IllegalStateException();
        }

        this.saslServer = saslServer;
    }


    @Override
    public void messageReceived( NextFilter nextFilter, IoSession session, Object message ) throws SaslException
    {
        LOG.debug( "Message received:  {}", message );

        /*
         * Unwrap the data for mechanisms that support QoP (DIGEST-MD5, GSSAPI).
         */
        String qop = ( String ) saslServer.getNegotiatedProperty( Sasl.QOP );
        boolean hasSecurityLayer = ( qop != null && ( qop.equals( SaslQoP.AUTH_INT.getValue() ) || qop
            .equals( SaslQoP.AUTH_CONF.getValue() ) ) );

        if ( hasSecurityLayer )
        {
            /*
             * Get the buffer as bytes.  First 4 bytes are length as int.
             */
            IoBuffer buf = ( IoBuffer ) message;
            int bufferLength = buf.getInt();
            byte[] bufferBytes = new byte[bufferLength];
            buf.get( bufferBytes );

            LOG.debug( "Will use SASL to unwrap received message of length:  {}", bufferLength );
            byte[] token = saslServer.unwrap( bufferBytes, 0, bufferBytes.length );
            nextFilter.messageReceived( session, IoBuffer.wrap( token ) );
        }
        else
        {
            LOG.debug( "Will not use SASL on received message." );
            nextFilter.messageReceived( session, message );
        }
    }


    @Override
    public void filterWrite( NextFilter nextFilter, IoSession session, WriteRequest writeRequest ) throws SaslException
    {
        LOG.debug( "Filtering write request:  {}", writeRequest );

        /*
         * Check if security layer processing should be disabled once.
         */
        if ( session.containsAttribute( DISABLE_SECURITY_LAYER_ONCE ) )
        {
            // Remove the marker attribute because it is temporary.
            LOG.debug( "Disabling SaslFilter once; will not use SASL on write request." );
            session.removeAttribute( DISABLE_SECURITY_LAYER_ONCE );
            nextFilter.filterWrite( session, writeRequest );
            return;
        }

        /*
         * Wrap the data for mechanisms that support QoP (DIGEST-MD5, GSSAPI).
         */
        String qop = ( String ) saslServer.getNegotiatedProperty( Sasl.QOP );
        boolean hasSecurityLayer = ( qop != null && ( qop.equals( SaslQoP.AUTH_INT.getValue() ) || qop
            .equals( SaslQoP.AUTH_CONF.getValue() ) ) );

        IoBuffer saslLayerBuffer = null;

        if ( hasSecurityLayer )
        {
            /*
             * Get the buffer as bytes.
             */
            IoBuffer buf = ( IoBuffer ) writeRequest.getMessage();
            int bufferLength = buf.remaining();
            byte[] bufferBytes = new byte[bufferLength];
            buf.get( bufferBytes );

            LOG.debug( "Will use SASL to wrap message of length:  {}", bufferLength );

            byte[] saslLayer = saslServer.wrap( bufferBytes, 0, bufferBytes.length );

            /*
             * Prepend 4 byte length.
             */
            saslLayerBuffer = IoBuffer.allocate( 4 + saslLayer.length );
            saslLayerBuffer.putInt( saslLayer.length );
            saslLayerBuffer.put( saslLayer );
            saslLayerBuffer.position( 0 );
            saslLayerBuffer.limit( 4 + saslLayer.length );

            LOG.debug( "Sending encrypted token of length {}.", saslLayerBuffer.limit() );
            nextFilter.filterWrite( session, new DefaultWriteRequest( saslLayerBuffer, writeRequest.getFuture() ) );
        }
        else
        {
            LOG.debug( "Will not use SASL on write request." );
            nextFilter.filterWrite( session, writeRequest );
        }
    }
}
