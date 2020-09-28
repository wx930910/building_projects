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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link OperationScope}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class OperationScopeTest
{
    @Test
    public void testGetName() throws Exception
    {
        assertEquals( "Entry", OperationScope.ENTRY.getName() );
        assertEquals( "Attribute Type", OperationScope.ATTRIBUTE_TYPE.getName() );
        assertEquals( "Attribute Type & Value", OperationScope.ATTRIBUTE_TYPE_AND_VALUE.getName() );
    }


    @Test
    public void testGetNameAndToStringEquality()
    {
        assertEquals( OperationScope.ENTRY.getName(), OperationScope.ENTRY.toString() );
        assertEquals( OperationScope.ATTRIBUTE_TYPE.getName(), OperationScope.ATTRIBUTE_TYPE.toString() );
        assertEquals( OperationScope.ATTRIBUTE_TYPE_AND_VALUE.getName(), OperationScope.ATTRIBUTE_TYPE_AND_VALUE
            .toString() );
    }
}
