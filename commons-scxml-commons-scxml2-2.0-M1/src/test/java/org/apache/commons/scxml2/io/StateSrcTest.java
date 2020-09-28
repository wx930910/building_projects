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
package org.apache.commons.scxml2.io;

import java.net.URL;
import java.util.Set;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.SCXMLTestHelper;
import org.apache.commons.scxml2.model.EnterableState;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
/**
 * Test white box nature of <state> element "src" attribute.
 */
public class StateSrcTest {

    // Test data
    private URL src01, src04, src05;
    private SCXML scxml;
    private SCXMLExecutor exec;

    /**
     * Set up instance variables required by this test case.
     */
    @Before
    public void setUp() {
        src01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/io/src-test-1.xml");
        src04 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/io/src-test-4.xml");
        src05 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/io/src-test-5.xml");
    }

    /**
     * Tear down instance variables required by this test case.
     */
    @After
    public void tearDown() {
        src01 = src04 = src05 = null;
        scxml = null;
        exec = null;
    }

    /**
     * Test the implementation
     */    
    @Test
    public void testRecursiveSrcInclude() throws Exception {
        scxml = SCXMLTestHelper.parse(src01);
        Assert.assertNotNull(scxml);
        exec = SCXMLTestHelper.getExecutor(scxml);
        Assert.assertNotNull(exec);
        Set<EnterableState> states = exec.getCurrentStatus().getStates();
        Assert.assertEquals(1, states.size());
        Assert.assertEquals("srctest3", states.iterator().next().getId());
        states = SCXMLTestHelper.fireEvent(exec, "src.test");
        Assert.assertEquals(1, states.size());
        Assert.assertEquals("srctest1end", states.iterator().next().getId());
        Assert.assertTrue(exec.getCurrentStatus().isFinal());
    }
    
    @Test
    public void testBadSrcInclude() throws Exception {
        try {
            scxml = SCXMLReader.read(src04);
            Assert.fail("Document with bad <state> src attribute shouldn't be parsed!");
        } catch (ModelException me) {
            Assert.assertTrue("Unexpected error message for bad <state> 'src' URI",
                me.getMessage() != null && me.getMessage().contains("Source attribute in <state src="));
        }
    }
    
    @Test
    public void testBadSrcFragmentInclude() throws Exception {
        try {
            scxml = SCXMLReader.read(src05);
            Assert.fail("Document with bad <state> src attribute shouldn't be parsed!");
        } catch (ModelException me) {
            Assert.assertTrue("Unexpected error message for bad <state> 'src' URI fragment",
                me.getMessage() != null && me.getMessage().contains("URI Fragment in <state src="));
        }
    }
}

