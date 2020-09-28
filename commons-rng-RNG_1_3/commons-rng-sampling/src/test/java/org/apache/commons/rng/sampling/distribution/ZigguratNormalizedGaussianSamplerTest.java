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
package org.apache.commons.rng.sampling.distribution;

import org.junit.Test;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;

/**
 * Test for {@link ZigguratNormalizedGaussianSampler}.
 */
public class ZigguratNormalizedGaussianSamplerTest {
    // Cf. RNG-56
    @Test(expected = StackOverflowError.class)
    public void testInfiniteLoop() {
        // A bad implementation whose only purpose is to force access
        // to the rarest branch.
        final UniformRandomProvider bad = new UniformRandomProvider() {
                // CHECKSTYLE: stop all
                public long nextLong(long n) { return 0; }
                public long nextLong() { return Long.MAX_VALUE; }
                public int nextInt(int n) { return 0; }
                public int nextInt() { return Integer.MAX_VALUE; }
                public float nextFloat() { return 1; }
                public double nextDouble() { return 1;}
                public void nextBytes(byte[] bytes, int start, int len) {}
                public void nextBytes(byte[] bytes) {}
                public boolean nextBoolean() { return false; }
                // CHECKSTYLE: resume all
            };

        // Infinite loop (in v1.1).
        new ZigguratNormalizedGaussianSampler(bad).sample();
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSampler() {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final SharedStateContinuousSampler sampler1 =
            ZigguratNormalizedGaussianSampler.<ZigguratNormalizedGaussianSampler>of(rng1);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }
}
