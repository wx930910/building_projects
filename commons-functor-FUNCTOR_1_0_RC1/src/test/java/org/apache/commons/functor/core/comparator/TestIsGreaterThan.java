/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.functor.core.comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.functor.core.Constant;
import org.junit.Test;

/**
 * @version $Revision$ $Date$
 * @author Rodney Waldhoff
 */
public class TestIsGreaterThan extends BaseComparisonPredicateTest {

    // Functor Testing Framework
    // ------------------------------------------------------------------------

    protected Object makeFunctor() {
        return new IsGreaterThan<Comparable<?>>();
    }

    // Tests
    // ------------------------------------------------------------------------

    @Test
    public void testTest() throws Exception {
        IsGreaterThan<Integer> p = new IsGreaterThan<Integer>();
        assertFalse(p.test(new Integer(2),new Integer(4)));
        assertFalse(p.test(new Integer(3),new Integer(4)));
        assertFalse(p.test(new Integer(4),new Integer(4)));
        assertTrue(p.test(new Integer(5),new Integer(4)));
        assertTrue(p.test(new Integer(6),new Integer(4)));
    }

    @Test
    public void testInstance() {
        assertTrue(IsGreaterThan.instance(new Integer(7)).test(new Integer(8)));
        assertTrue(! IsGreaterThan.instance(new Integer(7)).test(new Integer(6)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEquals() throws Exception {
        IsGreaterThan<Comparable<?>> p = new IsGreaterThan<Comparable<?>>();
        assertEquals(p,p);

        assertObjectsAreEqual(p,new IsGreaterThan<Comparable<?>>());
        assertObjectsAreEqual(p,new IsGreaterThan<Comparable<?>>(new ComparableComparator()));
        assertObjectsAreEqual(p,IsGreaterThan.instance());
        assertObjectsAreNotEqual(p,Constant.FALSE);
    }

}