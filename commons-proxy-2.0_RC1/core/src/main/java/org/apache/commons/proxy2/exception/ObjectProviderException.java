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

package org.apache.commons.proxy2.exception;

/**
 * {@link org.apache.commons.proxy2.ObjectProvider} implementations should throw this exception type to indicate that
 * there was a problem creating/finding the object.
 * 
 * @since 1.0
 */
public class ObjectProviderException extends RuntimeException
{
    /** Serialization version */
    private static final long serialVersionUID = -1L;

    //******************************************************************************************************************
    // Constructors
    //******************************************************************************************************************

    /**
     * Create a new ObjectProviderException instance.
     */
    public ObjectProviderException()
    {
    }

    /**
     * Create a new ObjectProviderException instance.
     * 
     * @param message
     */
    public ObjectProviderException(String message)
    {
        super(message);
    }

    /**
     * Create a new ObjectProviderException instance using {@link String#format(String, Object...)} for the message.
     * 
     * @param message
     * @param arguments
     */
    public ObjectProviderException(String message, Object... arguments)
    {
        super(String.format(message, arguments));
    }

    /**
     * Create a new ObjectProviderException instance.
     * 
     * @param cause
     */
    public ObjectProviderException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Create a new ObjectProviderException instance.
     * 
     * @param message
     * @param cause
     */
    public ObjectProviderException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Create a new ObjectProviderException instance using {@link String#format(String, Object...)} for the message.
     * 
     * @param cause
     * @param message
     * @param arguments
     */
    public ObjectProviderException(Throwable cause, String message, Object... arguments)
    {
        super(String.format(message, arguments), cause);
    }
}
