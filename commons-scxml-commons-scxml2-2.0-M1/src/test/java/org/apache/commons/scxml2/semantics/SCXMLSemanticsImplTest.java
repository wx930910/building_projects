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
package org.apache.commons.scxml2.semantics;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.scxml2.env.MockErrorReporter;
import org.apache.commons.scxml2.env.SimpleErrorReporter;
import org.apache.commons.scxml2.model.EnterableState;
import org.apache.commons.scxml2.model.Parallel;
import org.apache.commons.scxml2.model.State;
import org.junit.Assert;
import org.junit.Test;

public class SCXMLSemanticsImplTest {

    @Test
    public void testIsLegalConfigNoStates() {
        Set<EnterableState> states = new HashSet<EnterableState>();

        Assert.assertTrue(new SCXMLSemanticsImpl().isLegalConfig(states, new SimpleErrorReporter()));
    }

    @Test
    public void testIsLegalConfigInvalidParallel() {
        Set<EnterableState> states = new HashSet<EnterableState>();
        Parallel parallel = new Parallel();

        Parallel parent = new Parallel();
        parent.setId("4");

        State state1 = new State();
        state1.setId("1");
        State state2 = new State();
        state2.setId("2");

        parent.addChild(state1);
        parent.addChild(state2);

        parallel.setParent(parent);

        states.add(parallel);

        MockErrorReporter errorReporter = new MockErrorReporter();

        Assert.assertFalse(new SCXMLSemanticsImpl().isLegalConfig(states, errorReporter));
        Assert.assertEquals(ErrorConstants.ILLEGAL_CONFIG, errorReporter.getErrCode());
        Assert.assertEquals("Not all AND states active for parallel 4", errorReporter.getErrDetail());
    }

    @Test
    public void testIsLegalConfigMultipleTopLevel() {
        Set<EnterableState> states = new HashSet<EnterableState>();

        State state1 = new State();
        state1.setId("1");
        State state2 = new State();
        state2.setId("2");

        states.add(state1);
        states.add(state2);

        MockErrorReporter errorReporter = new MockErrorReporter();

        Assert.assertTrue(new SCXMLSemanticsImpl().isLegalConfig(states, errorReporter));
        Assert.assertEquals(ErrorConstants.ILLEGAL_CONFIG, errorReporter.getErrCode());
        Assert.assertEquals("Multiple top-level OR states active!", errorReporter.getErrDetail());
    }

    @Test
    public void testIsLegalConfigMultipleStatesActive() {
        Set<EnterableState> states = new HashSet<EnterableState>();

        State state1 = new State();
        state1.setId("1");

        State state2 = new State();
        state2.setId("2");

        State parent = new State();
        parent.setId("parentid");

        state2.setParent(parent);
        state1.setParent(parent);

        states.add(state1);
        states.add(state2);

        MockErrorReporter errorReporter = new MockErrorReporter();

        Assert.assertFalse(new SCXMLSemanticsImpl().isLegalConfig(states, errorReporter));
        Assert.assertEquals(ErrorConstants.ILLEGAL_CONFIG, errorReporter.getErrCode());
        Assert.assertEquals("Multiple OR states active for state parentid", errorReporter.getErrDetail());
    }
}
