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
package org.apache.commons.rng.simple.internal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link NativeSeedType} factory seed conversions.
 */
public class NativeSeedTypeTest {
    /**
     * Test the conversion throws for an unsupported type. All supported types are
     * tested in the {@link NativeSeedTypeParametricTest}.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testConvertSeedToBytesUsingNullThrows() {
        NativeSeedType.convertSeedToBytes(null);
    }

    /**
     * Test the conversion passes through a byte[]. This hits the edge case of a seed
     * that can be converted that is not a native type.
     */
    @Test
    public void testConvertSeedToBytesUsingByteArray() {
        final byte[] seed = {42, 78, 99};
        Assert.assertSame(seed, NativeSeedType.convertSeedToBytes(seed));
    }
}
