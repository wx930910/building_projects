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
package org.apache.commons.functor.adapter;

import java.io.Serializable;

import org.apache.commons.functor.BinaryPredicate;
import org.apache.commons.functor.UnaryPredicate;

/**
 * Adapts a
 * {@link BinaryPredicate BinaryPredicate}
 * to the
 * {@link UnaryPredicate UnaryPredicate} interface
 * using a constant left-side argument.
 * <p/>
 * Note that although this class implements
 * {@link Serializable}, a given instance will
 * only be truly <code>Serializable</code> if the
 * underlying objects are.  Attempts to serialize
 * an instance whose delegates are not
 * <code>Serializable</code> will result in an exception.
 *
 * @param <A> the argument type.
 * @version $Revision$ $Date$
 * @author Rodney Waldhoff
 */
public final class RightBoundPredicate<A> implements UnaryPredicate<A>, Serializable {
    /**
     * serialVersionUID declaration.
     */
    private static final long serialVersionUID = -1768544281714574302L;
    /** The {@link BinaryPredicate BinaryPredicate} I'm wrapping. */
    private final BinaryPredicate<? super A, Object> predicate;
    /** The parameter to pass to {@code predicate}. */
    private final Object param;

    /**
     * Create a new RightBoundPredicate.
     * @param <R> bound arg type
     * @param predicate the predicate to adapt
     * @param arg the constant argument to use
     */
    @SuppressWarnings("unchecked")
    public <R> RightBoundPredicate(BinaryPredicate<? super A, ? super R> predicate, R arg) {
        if (predicate == null) {
            throw new IllegalArgumentException("BinaryPredicate argument was null");
        }
        this.predicate = (BinaryPredicate<? super A, Object>) predicate;
        this.param = arg;
    }

    /**
     * {@inheritDoc}
     */
    public boolean test(A obj) {
        return predicate.test(obj, param);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object that) {
        return that == this || (that instanceof RightBoundPredicate<?> && equals((RightBoundPredicate<?>) that));
    }

    /**
     * Learn whether another RightBoundPredicate is equal to this.
     * @param that RightBoundPredicate to test
     * @return boolean
     */
    public boolean equals(RightBoundPredicate<?> that) {
        return null != that
                && (null == predicate ? null == that.predicate : predicate.equals(that.predicate))
                && (null == param ? null == that.param : param.equals(that.param));
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int hash = "RightBoundPredicate".hashCode();
        if (null != predicate) {
            hash <<= 2;
            hash ^= predicate.hashCode();
        }
        if (null != param) {
            hash <<= 2;
            hash ^= param.hashCode();
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "RightBoundPredicate<" + predicate + "(?," + param + ")>";
    }

    /**
     * Adapt a BinaryPredicate as a UnaryPredicate.
     * @param <L> the left argument type.
     * @param <R> the right argument type.
     * @param predicate to adapt
     * @param arg right side
     * @return RightBoundPredicate
     */
    public static <L, R> RightBoundPredicate<L> bind(BinaryPredicate<? super L, ? super R> predicate, R arg) {
        return null == predicate ? null : new RightBoundPredicate<L>(predicate, arg);
    }

}
