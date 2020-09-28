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
package org.apache.commons.scxml2.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.SCXMLTestHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CustomActionTest {

    private URL hello01, custom01, external01, override01, payload01;
    private SCXMLExecutor exec;

    /**
     * Set up instance variables required by this test case.
     */
    @Before
    public void setUp() {
        hello01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/hello-world.xml");
        custom01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/custom-hello-world-01.xml");
        external01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/external-hello-world.xml");
        override01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/custom-hello-world-03.xml");
        payload01 = this.getClass().getClassLoader().
            getResource("org/apache/commons/scxml2/custom-hello-world-04-jexl.xml");

        Hello.callbacks = 0;
    }

    /**
     * Tear down instance variables required by this test case.
     */
    @After
    public void tearDown() {
        hello01 = custom01 = external01 = payload01 = null;
        exec = null;
    }

    @Test
    public void testAddGoodCustomAction01() throws Exception {
        new CustomAction("http://my.actions.domain/CUSTOM", "hello",
            Hello.class);
    }

    @Test
    public void testAddBadCustomAction01() {
        try {
            new CustomAction(null, "hello", Hello.class);
            Assert.fail("Added custom action with illegal namespace");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testAddBadCustomAction02() {
        try {
            new CustomAction("  ", "hello", Hello.class);
            Assert.fail("Added custom action with illegal namespace");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testAddBadCustomAction03() {
        try {
            new CustomAction("http://my.actions.domain/CUSTOM", "",
                Hello.class);
            Assert.fail("Added custom action with illegal local name");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testAddBadCustomAction04() {
        try {
            new CustomAction("http://my.actions.domain/CUSTOM", "  ",
                Hello.class);
            Assert.fail("Added custom action with illegal local name");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    @Test
    public void testAddBadCustomAction05() {
        try {            
            new CustomAction("http://www.w3.org/2005/07/scxml", "foo",
                Hello.class);
            Assert.fail("Added custom action in the SCXML namespace");
        } catch (IllegalArgumentException iae) {
            // Expected
        }
    }

    // Hello World example using the SCXML <log> action
    @Test
    public void testHelloWorld() throws Exception {
        // (1) Get a SCXMLExecutor
        exec = SCXMLTestHelper.getExecutor(hello01);
        // (2) Single, final state
        Assert.assertEquals("hello", (exec.getCurrentStatus().getStates().
                iterator().next()).getId());
        Assert.assertTrue(exec.getCurrentStatus().isFinal());
    }

    // Hello World example using a custom <hello> action
    @Test
    public void testCustomActionHelloWorld() throws Exception {
        // (1) Form a list of custom actions defined in the SCXML
        //     document (and any included documents via "src" attributes)
        CustomAction ca1 =
            new CustomAction("http://my.custom-actions.domain/CUSTOM1",
                             "hello", Hello.class);
        // Register the same action under a different name, just to test
        // multiple custom actions
        CustomAction ca2 =
            new CustomAction("http://my.custom-actions.domain/CUSTOM2",
                             "bar", Hello.class);
        List<CustomAction> customActions = new ArrayList<CustomAction>();
        customActions.add(ca1);
        customActions.add(ca2);
        // (2) Parse the document
        SCXML scxml = SCXMLTestHelper.parse(custom01, customActions);
        // (3) Get a SCXMLExecutor
        exec = SCXMLTestHelper.getExecutor(scxml);
        // (4) Single, final state
        Assert.assertEquals("custom", (exec.getCurrentStatus().getStates().
                iterator().next()).getId());
        Assert.assertTrue(exec.getCurrentStatus().isFinal());

        // The custom action defined by Hello.class should be called
        // to execute() exactly twice at this point (one by <my:hello/> and the other by <foo:bar/>).
        Assert.assertEquals(2, Hello.callbacks);
    }

    // Hello World example using custom <my:hello> action
    // as part of an external state source (src attribute)
    @Test
    public void testCustomActionExternalSrcHelloWorld() throws Exception {
        // (1) Form a list of custom actions defined in the SCXML
        //     document (and any included documents via "src" attributes)
        CustomAction ca =
            new CustomAction("http://my.custom-actions.domain/CUSTOM",
                             "hello", Hello.class);
        List<CustomAction> customActions = new ArrayList<CustomAction>();
        customActions.add(ca);
        // (2) Parse the document
        SCXML scxml = SCXMLTestHelper.parse(external01, customActions);
        // (3) Get a SCXMLExecutor
        exec = SCXMLTestHelper.getExecutor(scxml);
        // (4) Single, final state
        Assert.assertEquals("custom", (exec.getCurrentStatus().getStates().
            iterator().next()).getId());

        // The custom action defined by Hello.class should be called
        // to execute() exactly twice at this point (one by <my:hello/> and the other by <my:hello/> in external).
        Assert.assertEquals(2, Hello.callbacks);
    }

    // Hello World example using custom <my:send> action
    // (overriding SCXML local name "send")
    @Test
    public void testCustomActionOverrideLocalName() throws Exception {
        // (1) List of custom actions, use same local name as SCXML action
        CustomAction ca =
            new CustomAction("http://my.custom-actions.domain/CUSTOM",
                             "send", Hello.class);
        List<CustomAction> customActions = new ArrayList<CustomAction>();
        customActions.add(ca);
        // (2) Parse the document
        SCXML scxml = SCXMLTestHelper.parse(override01, customActions);
        // (3) Get a SCXMLExecutor
        exec = SCXMLTestHelper.getExecutor(scxml);
        // (4) Single, final state
        Assert.assertEquals("custom", (exec.getCurrentStatus().getStates().
            iterator().next()).getId());

        // The custom action defined by Hello.class should be called
        // to execute() exactly once at this point (by <my:send/>).
        Assert.assertEquals(1, Hello.callbacks);
    }

    // Hello World example using custom <my:hello> action that generates an
    // event which has the payload examined with JEXL expressions
    @Test
    public void testCustomActionEventPayloadHelloWorldJexl() throws Exception {
        // (1) Form a list of custom actions defined in the SCXML
        //     document (and any included documents via "src" attributes)
        CustomAction ca =
            new CustomAction("http://my.custom-actions.domain/CUSTOM",
                             "hello", Hello.class);
        List<CustomAction> customActions = new ArrayList<CustomAction>();
        customActions.add(ca);
        // (2) Parse the document
        SCXML scxml = SCXMLTestHelper.parse(payload01, customActions);
        // (3) Get a SCXMLExecutor
        exec = SCXMLTestHelper.getExecutor(scxml);
        // (4) Single, final state
        Assert.assertEquals("Invalid intermediate state",
                     "custom1", (exec.getCurrentStatus().getStates().
                                iterator().next()).getId());
        // (5) Verify datamodel variable is correct
        Assert.assertEquals("Missing helloName1 in root context", "custom04a",
                     exec.getRootContext().get("helloName1"));

        // The custom action defined by Hello.class should be called
        // to execute() exactly once at this point (by onentry in init state).
        Assert.assertEquals(1, Hello.callbacks);

        // (6) Check use of payload in non-initial state
        SCXMLTestHelper.fireEvent(exec, "custom.next");
        // (7) Verify correct end state
        Assert.assertEquals("Missing helloName1 in root context", "custom04b",
                exec.getRootContext().get("helloName1"));
        Assert.assertEquals("Invalid final state",
                "end", (exec.getCurrentStatus().getStates().
                iterator().next()).getId());
        Assert.assertTrue(exec.getCurrentStatus().isFinal());

        // The custom action defined by Hello.class should be called
        // to execute() exactly two times at this point (by onentry in custom2 state).
        Assert.assertEquals(2, Hello.callbacks);
    }
}

