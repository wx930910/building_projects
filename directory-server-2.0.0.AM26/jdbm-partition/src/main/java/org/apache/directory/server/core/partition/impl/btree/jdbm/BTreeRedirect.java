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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.Serializable;


/**
 * A redirection pointer to another BTree.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BTreeRedirect implements Serializable
{
    private static final long serialVersionUID = -4289810071005184834L;

    final long recId;


    public BTreeRedirect( long recId )
    {
        this.recId = recId;
    }


    public long getRecId()
    {
        return recId;
    }


    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append( "BTreeRedirect[" );
        buf.append( recId );
        buf.append( "]" );
        return buf.toString();
    }
}
