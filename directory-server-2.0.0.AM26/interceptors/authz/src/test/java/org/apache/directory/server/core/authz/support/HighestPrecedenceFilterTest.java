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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.api.ldap.aci.ACITuple;
import org.apache.directory.api.ldap.aci.MicroOperation;
import org.apache.directory.api.ldap.aci.ProtectedItem;
import org.apache.directory.api.ldap.aci.UserClass;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link HighestPrecedenceFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class HighestPrecedenceFilterTest
{
    private static final Collection<ProtectedItem> PI_EMPTY_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<ProtectedItem>() );
    private static final Collection<UserClass> UC_EMPTY_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Collection<ACITuple> AT_EMPTY_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Set<MicroOperation> MO_EMPTY_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );


    @Test
    public void testZeroTuple() throws Exception
    {
        HighestPrecedenceFilter filter = new HighestPrecedenceFilter();
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( AT_EMPTY_COLLECTION );

        assertEquals( 0, filter.filter( aciContext, null, null ).size() );
    }


    @Test
    public void testOneTuple() throws Exception
    {
        HighestPrecedenceFilter filter = new HighestPrecedenceFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();

        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PI_EMPTY_COLLECTION, MO_EMPTY_SET,
            true, 10 ) );
        tuples = Collections.unmodifiableCollection( tuples );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        assertEquals( tuples, filter.filter( aciContext, null, null ) );
    }


    @Test
    public void testMoreThanOneTuples() throws Exception
    {
        final int MAX_PRECEDENCE = 10;
        HighestPrecedenceFilter filter = new HighestPrecedenceFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();

        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PI_EMPTY_COLLECTION, MO_EMPTY_SET,
            true,
            MAX_PRECEDENCE ) );
        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PI_EMPTY_COLLECTION, MO_EMPTY_SET,
            true,
            MAX_PRECEDENCE / 2 ) );
        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PI_EMPTY_COLLECTION, MO_EMPTY_SET,
            true,
            MAX_PRECEDENCE ) );
        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PI_EMPTY_COLLECTION, MO_EMPTY_SET,
            true,
            MAX_PRECEDENCE / 3 ) );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = filter.filter( aciContext, null, null );

        for ( ACITuple tuple : tuples )
        {
            assertNotNull( tuple.getPrecedence() );
            assertEquals( MAX_PRECEDENCE, tuple.getPrecedence().intValue() );
        }
    }
}
