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

import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.sampling.RandomAssert;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for the {@link ChengBetaSampler}. The tests hit edge cases for the sampler.
 */
public class ChengBetaSamplerTest {
    /**
     * Test the constructor with a bad alpha.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroAlpha() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double alpha = 0;
        final double beta = 1;
        ChengBetaSampler.of(rng, alpha, beta);
    }

    /**
     * Test the constructor with a bad beta.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWithZeroBeta() {
        final RestorableUniformRandomProvider rng =
            RandomSource.create(RandomSource.SPLIT_MIX_64);
        final double alpha = 1;
        final double beta = 0;
        ChengBetaSampler.of(rng, alpha, beta);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithAlphaAndBetaAbove1AndAlphaBelowBeta() {
        testSharedStateSampler(1.23, 4.56);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithAlphaAndBetaAbove1AndAlphaAboveBeta() {
        testSharedStateSampler(4.56, 1.23);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithAlphaOrBetaBelow1AndAlphaBelowBeta() {
        testSharedStateSampler(0.23, 4.56);
    }

    /**
     * Test the SharedStateSampler implementation.
     */
    @Test
    public void testSharedStateSamplerWithAlphaOrBetaBelow1AndAlphaAboveBeta() {
        testSharedStateSampler(4.56, 0.23);
    }

    /**
     * Test the SharedStateSampler implementation.
     *
     * @param alpha Alpha.
     * @param beta Beta.
     */
    private static void testSharedStateSampler(double alpha, double beta) {
        final UniformRandomProvider rng1 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        final UniformRandomProvider rng2 = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        // Use instance constructor not factory constructor to exercise 1.X public API
        final SharedStateContinuousSampler sampler1 =
            new ChengBetaSampler(rng1, alpha, beta);
        final SharedStateContinuousSampler sampler2 = sampler1.withUniformRandomProvider(rng2);
        RandomAssert.assertProduceSameSequence(sampler1, sampler2);
    }

    /**
     * Test the toString method. This is added to ensure coverage as the factory constructor
     * used in other tests does not create an instance of the wrapper class.
     */
    @Test
    public void testToString() {
        final UniformRandomProvider rng = RandomSource.create(RandomSource.SPLIT_MIX_64, 0L);
        Assert.assertTrue(new ChengBetaSampler(rng, 1.0, 2.0).toString()
                .toLowerCase().contains("beta"));
    }
}
