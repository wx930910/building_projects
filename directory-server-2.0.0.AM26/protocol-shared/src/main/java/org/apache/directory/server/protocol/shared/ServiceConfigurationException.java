/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.protocol.shared;


/**
 * An exception for the Service configuration
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServiceConfigurationException extends RuntimeException
{
    private static final long serialVersionUID = 8298096524637031225L;


    /**
     * Creates a new instance of ServiceConfigurationException.
     */
    public ServiceConfigurationException()
    {
        super();
    }


    /**
     * Creates a new instance of ServiceConfigurationException.
     *
     * @param message The exception's message
     * @param cause The exception's cause
     */
    public ServiceConfigurationException( String message, Throwable cause )
    {
        super( message, cause );
    }


    /**
     * Creates a new instance of ServiceConfigurationException.
     *
     * @param message The exception's message
     */
    public ServiceConfigurationException( String message )
    {
        super( message );
    }


    /**
     * Creates a new instance of ServiceConfigurationException.
     *
     * @param cause The exception's cause
     */
    public ServiceConfigurationException( Throwable cause )
    {
        super( cause );
    }
}
