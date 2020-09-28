/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.scxml2.issues;

import java.net.URL;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.SCXMLTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for issue 64.
 * WONTFIX
 */
public class Issue64Test {

    private URL works, fails;
    private SCXMLExecutor exec;

    /**
     * Set up instance variables required by this test case.
     */
    @Before
    public void setUp() {
        works = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/issues/issue64-01.xml");
        fails = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/issues/issue64-02.xml");
    }

    /**
     * Tear down instance variables required by this test case.
     */
    @After
    public void tearDown() {
        works = fails = null;
        exec = null;
    }
    
    @Test
    public void test01issue64() throws Exception {
        exec = SCXMLTestHelper.getExecutor(SCXMLTestHelper.parse(works));
        SCXMLTestHelper.assertPostTriggerState(exec, "show.bug", "end");
    }
    
    @Test
    public void test02issue64() throws Exception {
        exec = SCXMLTestHelper.getExecutor(SCXMLTestHelper.parse(fails));
        SCXMLTestHelper.assertPostTriggerState(exec, "show.bug", "end");
    }
}

