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
package org.apache.directory.server.core.trigger;


import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.api.ldap.trigger.StoredProcedureParameter.Generic_LDAP_CONTEXT;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;


public abstract class AbstractStoredProcedureParameterInjector implements StoredProcedureParameterInjector
{
    private OperationContext opContext;
    private Map<Class<?>, MicroInjector> injectors;


    MicroInjector operationPrincipalInjector = new MicroInjector()
    {
        @Override
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            return getOperationPrincipal();
        }
    };

    MicroInjector ldapContextInjector = new MicroInjector()
    {
        @Override
        public Object inject( OperationContext opContext, StoredProcedureParameter param ) throws LdapException
        {
            Generic_LDAP_CONTEXT ldapCtxParam = ( Generic_LDAP_CONTEXT ) param;
            Dn ldapCtxName = ldapCtxParam.getCtxName();

            CoreSession session = opContext.getSession();
            LookupOperationContext lookupContext = 
                new LookupOperationContext( session, ldapCtxName, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            lookupContext.setPartition( opContext.getPartition() );
            lookupContext.setTransaction( opContext.getTransaction() );

            return session.getDirectoryService().getPartitionNexus().lookup( lookupContext );
        }
    };


    public AbstractStoredProcedureParameterInjector( OperationContext opContext )
    {
        this.opContext = opContext;
        injectors = new HashMap<>();
        injectors.put( StoredProcedureParameter.Generic_OPERATION_PRINCIPAL.class, operationPrincipalInjector );
        injectors.put( StoredProcedureParameter.Generic_LDAP_CONTEXT.class, ldapContextInjector );
    }


    protected Dn getOperationPrincipal() throws LdapInvalidDnException
    {
        Principal principal = opContext.getSession().getEffectivePrincipal();
        return opContext.getSession().getDirectoryService().getDnFactory().create( principal.getName() );
    }


    protected Map<Class<?>, MicroInjector> getInjectors()
    {
        return injectors;
    }


    public OperationContext getOperationContext()
    {
        return opContext;
    }


    public void setOperationContext( OperationContext invocation )
    {
        this.opContext = invocation;
    }


    @Override
    public final List<Object> getArgumentsToInject( OperationContext opContext,
        List<StoredProcedureParameter> parameterList ) throws LdapException
    {
        List<Object> arguments = new ArrayList<>();

        Iterator<StoredProcedureParameter> it = parameterList.iterator();

        while ( it.hasNext() )
        {
            StoredProcedureParameter spParameter = it.next();
            MicroInjector injector = injectors.get( spParameter.getClass() );
            arguments.add( injector.inject( opContext, spParameter ) );
        }

        return arguments;
    }
}
