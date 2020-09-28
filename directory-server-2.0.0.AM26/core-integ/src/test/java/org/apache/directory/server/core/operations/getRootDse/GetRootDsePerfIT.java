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
package org.apache.directory.server.core.operations.getRootDse;

import static org.junit.Assert.assertNotNull;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the GetRootDSE operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
public class GetRootDsePerfIT extends AbstractLdapTestUnit
{
    /**
     * A GetRootDSE performance test
     */
    @Test
    public void testPerfGetRootDse() throws Exception
    {
        GetRootDseOperationContext getRootDseContext = new GetRootDseOperationContext( getService().getAdminSession() );
        Entry rootDse = getService().getOperationManager().getRootDse( getRootDseContext );

        assertNotNull( rootDse );
        int nbIterations = 1500000;

        long t0 = System.currentTimeMillis();
        long t00 = 0L;
        long tt0 = System.currentTimeMillis();
        
        for ( int i = 0; i < nbIterations; i++ )
        {
            getRootDseContext.setCurrentInterceptor( 0 );
            
            if ( i % 100000 == 0 )
            {
                long tt1 = System.currentTimeMillis();

                System.out.println( i + ", " + ( tt1 - tt0 ) );
                tt0 = tt1;
            }

            if ( i == 500000 )
            {
                t00 = System.currentTimeMillis();
            }

            rootDse = getService().getOperationManager().getRootDse( getRootDseContext );
        }
        
        long t1 = System.currentTimeMillis();

        Long deltaWarmed = ( t1 - t00 );
        System.out.println( "Delta getRootDSE: " + deltaWarmed + "( " + ( ( ( nbIterations - 500000 ) * 1000 ) / deltaWarmed ) + " per s ) /" + ( t1 - t0 ) );
    }
}
