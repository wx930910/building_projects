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
package org.apache.directory.server.ldap.handlers.extended;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.extras.extended.ads_impl.storedProcedure.StoredProcedureFactory;
import org.apache.directory.api.ldap.extras.extended.storedProcedure.StoredProcedureRequest;
import org.apache.directory.api.ldap.extras.extended.storedProcedure.StoredProcedureResponse;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.sp.LdapContextParameter;
import org.apache.directory.server.core.api.sp.StoredProcEngine;
import org.apache.directory.server.core.api.sp.StoredProcEngineConfig;
import org.apache.directory.server.core.api.sp.StoredProcExecutionManager;
import org.apache.directory.server.core.api.sp.java.JavaStoredProcEngineConfig;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;


/**
 * A Handler for the StoredProcedure extended operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureExtendedOperationHandler implements
    ExtendedOperationHandler<StoredProcedureRequest, StoredProcedureResponse>
{
    private StoredProcExecutionManager manager;
    private static final Object[] EMPTY_CLASS_ARRAY = new Object[0];


    public StoredProcedureExtendedOperationHandler()
    {
        super();
        //StoredProcEngineConfig javaxScriptSPEngineConfig = new JavaxStoredProcEngineConfig();
        StoredProcEngineConfig javaSPEngineConfig = new JavaStoredProcEngineConfig();
        List<StoredProcEngineConfig> spEngineConfigs = new ArrayList<>();
        //spEngineConfigs.add( javaxScriptSPEngineConfig );
        spEngineConfigs.add( javaSPEngineConfig );
        String spContainer = "ou=Stored Procedures,ou=system";
        this.manager = new StoredProcExecutionManager( spContainer, spEngineConfigs );
    }


    @Override
    public void handleExtendedOperation( LdapSession session, StoredProcedureRequest req ) throws Exception
    {
        String procedure = req.getProcedureSpecification();
        Entry spUnit = manager.findStoredProcUnit( session.getCoreSession(), procedure );
        StoredProcEngine engine = manager.getStoredProcEngineInstance( spUnit );

        List<Object> valueList = new ArrayList<>( req.size() );

        for ( int ii = 0; ii < req.size(); ii++ )
        {
            byte[] serializedValue = ( byte[] ) req.getParameterValue( ii );
            Object value = SerializationUtils.deserialize( serializedValue );

            if ( value.getClass().equals( LdapContextParameter.class ) )
            {
                String paramCtx = ( ( LdapContextParameter ) value ).getValue();
                value = session.getCoreSession().lookup( new Dn( paramCtx ) );
            }

            valueList.add( value );
        }

        Object[] values = valueList.toArray( EMPTY_CLASS_ARRAY );
        Object response = engine.invokeProcedure( session.getCoreSession(), procedure, values );
        byte[] serializedResponse = SerializationUtils.serialize( ( Serializable ) response );
        LdapApiService codec = session.getLdapServer().getDirectoryService().getLdapCodecService();
        StoredProcedureFactory factory = ( StoredProcedureFactory ) codec.getExtendedResponseFactories().get( req.getRequestName() );
        StoredProcedureResponse resp = ( StoredProcedureResponse ) factory.newResponse( serializedResponse );
        resp.setMessageId( req.getMessageId() );
        
        session.getIoSession().write( resp );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getOid()
    {
        return StoredProcedureRequest.EXTENSION_OID;
    }

    private static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> s = new HashSet<>();
        s.add( StoredProcedureRequest.EXTENSION_OID );
        s.add( StoredProcedureResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( s );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
