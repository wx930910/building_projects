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
package org.apache.directory.server.core.authz.support;


import java.util.Collection;
import java.util.Iterator;

import org.apache.directory.api.ldap.aci.ACITuple;
import org.apache.directory.api.ldap.aci.MicroOperation;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;


/**
 * An {@link ACITupleFilter} that discard tuples which doesn't contain any
 * related {@link MicroOperation}s. (18.8.3.4, X.501) 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
public class MicroOperationFilter implements ACITupleFilter
{
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry )
        throws LdapException
    {
        if ( aciContext.getAciTuples().isEmpty() )
        {
            return aciContext.getAciTuples();
        }

        for ( Iterator<ACITuple> i = aciContext.getAciTuples().iterator(); i.hasNext(); )
        {
            ACITuple tuple = i.next();

            /*
             * The ACITuple must contain all the MicroOperations specified within the
             * microOperations argument.  Just matching a single microOperation is not
             * enough.  All must be matched to retain the ACITuple.
             */

            boolean retain = true;

            for ( MicroOperation microOp : aciContext.getMicroOperations() )
            {
                if ( !tuple.getMicroOperations().contains( microOp ) )
                {
                    retain = false;
                    break;
                }
            }

            if ( !retain )
            {
                i.remove();
            }
        }

        return aciContext.getAciTuples();
    }

}
