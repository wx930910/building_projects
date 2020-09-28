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

package org.apache.commons.proxy2.impl;

import org.apache.commons.proxy2.Interceptor;
import org.apache.commons.proxy2.Invoker;
import org.apache.commons.proxy2.ObjectProvider;
import org.apache.commons.proxy2.ProxyFactory;

/**
 * Base abstract {@link ProxyFactory} implementation, primarily providing implementations of the interface methods that
 * are typically convenience constructs over the other methods.
 */
public abstract class AbstractProxyFactory implements ProxyFactory
{
    /**
     * Returns true if all <code>proxyClasses</code> are interfaces.
     * 
     * @param proxyClasses
     *            the proxy classes
     * @return true if all <code>proxyClasses</code> are interfaces
     */
    @Override
    public boolean canProxy(Class<?>... proxyClasses)
    {
        for (Class<?> proxyClass : proxyClasses)
        {
            if (!proxyClass.isInterface())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a proxy which delegates to the object provided by <code>delegateProvider</code>. The proxy will be
     * generated using the current thread's "context class loader."
     * 
     * @param delegateProvider
     *            the delegate provider
     * @param proxyClasses
     *            the interfaces that the proxy should implement
     * @return a proxy which delegates to the object provided by the target object provider
     */
    @Override
    public <T> T createDelegatorProxy(ObjectProvider<?> delegateProvider, Class<?>... proxyClasses)
    {
        return createDelegatorProxy(Thread.currentThread().getContextClassLoader(), delegateProvider, proxyClasses);
    }

    /**
     * Creates a proxy which passes through a {@link Interceptor interceptor} before eventually reaching the
     * <code>target</code> object. The proxy will be generated using the current thread's "context class loader."
     * 
     * @param target
     *            the target object
     * @param interceptor
     *            the method interceptor
     * @param proxyClasses
     *            the interfaces that the proxy should implement
     * @return a proxy which passes through a {@link Interceptor interceptor} before eventually reaching the
     *         <code>target</code> object.
     */
    @Override
    public <T> T createInterceptorProxy(Object target, Interceptor interceptor, Class<?>... proxyClasses)
    {
        return createInterceptorProxy(Thread.currentThread().getContextClassLoader(), target, interceptor,
                proxyClasses);
    }

    /**
     * Creates a proxy which uses the provided {@link Invoker} to handle all method invocations. The proxy will be
     * generated using the current thread's "context class loader."
     * 
     * @param invoker
     *            the invoker
     * @param proxyClasses
     *            the interfaces that the proxy should implement
     * @return a proxy which uses the provided {@link Invoker} to handle all method invocations
     */
    @Override
    public <T> T createInvokerProxy(Invoker invoker, Class<?>... proxyClasses)
    {
        return createInvokerProxy(Thread.currentThread().getContextClassLoader(), invoker, proxyClasses);
    }

}
