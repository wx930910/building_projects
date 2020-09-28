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
package org.apache.directory.server.core.api.interceptor.context;


import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;


/**
 * A context for tracking lookup operations. Lookup operations will return a
 * cloned server entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LookupOperationContext extends FilteringOperationContext
{
    /** flag to indicate if this search is done for replication */
    private boolean syncreplLookup;

    /**
     * Creates a new instance of LookupOperationContext.
     *
     * @param session The session to use
     */
    public LookupOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * Creates a new instance of LookupOperationContext.
     *
     * @param session The session to use
     * @param dn The Entry's Dn we are looking for
     */
    public LookupOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * Creates a new instance of LookupOperationContext.
     *
     * @param session The session to use
     * @param returningAttributes The attributes to return
     */
    public LookupOperationContext( CoreSession session, String... returningAttributes )
    {
        super( session, returningAttributes );
        
        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * Creates a new instance of LookupOperationContext.
     *
     * @param session The session to use
     * @param dn The Entry's Dn we are looking for
     * @param returningAttributes The attributes to return
     */
    public LookupOperationContext( CoreSession session, Dn dn, String... returningAttributes )
    {
        super( session, dn, returningAttributes );
        
        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return "Lookup";
    }

    
    /**
     * @return true if this is a syncrepl specific search
     */
    public boolean isSyncreplLookup()
    {
        return syncreplLookup;
    }


    /**
     * sets the flag to indicate if this is a synrepl specific search or not
     * 
     * @param syncreplLookup <tt>true</tt> if it's a syncrepl search
     */
    public void setSyncreplLookup( boolean syncreplLookup )
    {
        this.syncreplLookup = syncreplLookup;
    }
}
