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
package org.apache.directory.server.core.api;


/**
 * The list of Operation we can process on the Interceptors.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum OperationEnum
{
    ADD("add"),
    BIND("bind"),
    COMPARE("compare"),
    DELETE("delete"),
    GET_ROOT_DSE("getRootDse"),
    HAS_ENTRY("hasEntry"),
    LIST("list"),
    LOOKUP("lookup"),
    MODIFY("modify"),
    MOVE("move"),
    MOVE_AND_RENAME("moveAndRename"),
    RENAME("rename"),
    SEARCH("search"),
    UNBIND("unbind");

    /** The associated method name */
    private String methodName;

    /** A list of all the operations */
    private static OperationEnum[] operations = new OperationEnum[]
        {
            ADD,
            BIND,
            COMPARE,
            DELETE,
            GET_ROOT_DSE,
            HAS_ENTRY,
            LIST,
            LOOKUP,
            MODIFY,
            MOVE,
            MOVE_AND_RENAME,
            RENAME,
            SEARCH,
            UNBIND
    };


    /**
     * The private constructor
     * @param methodName The associated method name
     */
    OperationEnum( String methodName )
    {
        this.methodName = methodName;
    }


    /**
     * @return The associated method name
     */
    public String getMethodName()
    {
        return methodName;
    }


    /**
     * @return The list of all the operations
     */
    public static OperationEnum[] getOperations()
    {
        return operations;
    }
}
