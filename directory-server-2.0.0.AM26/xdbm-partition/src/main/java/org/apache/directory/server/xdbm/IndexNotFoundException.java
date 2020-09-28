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
package org.apache.directory.server.xdbm;


import javax.naming.NamingException;

import org.apache.directory.server.i18n.I18n;


/**
 * NamingException for missing indicies if full table scans are disallowed.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexNotFoundException extends NamingException
{
    private static final long serialVersionUID = 3906088970608981815L;

    /** the name of the index that was not found */
    private final String indexName;


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param indexName the name of the index that was not found 
     */
    public IndexNotFoundException( String indexName )
    {
        super( I18n.err( I18n.ERR_704, indexName ) );
        this.indexName = indexName;
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param message the message associated with the exception.
     * @param indexName the name of the index that was not found 
     */
    public IndexNotFoundException( String message, String indexName )
    {
        super( message );
        this.indexName = indexName;
    }


    /**
     * Constructs an Exception with a detailed message and a root cause 
     * exception.
     * 
     * @param message the message associated with the exception.
     * @param indexName the name of the index that was not found 
     * @param rootCause the root cause of this exception 
     */
    public IndexNotFoundException( String message, String indexName, Throwable rootCause )
    {
        this( message, indexName );
        setRootCause( rootCause );
    }


    /**
     * Gets the name of the attribute the index was missing for.
     *
     * @return the name of the attribute the index was missing for.
     */
    public String getIndexName()
    {
        return indexName;
    }
}
