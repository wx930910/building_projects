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

package org.apache.directory.server.core.api.sp.java;


import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.exception.LdapException;


/**
 * A class loader that loads a class from an attribute.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapJavaStoredProcClassLoader extends ClassLoader
{
    private Attribute javaByteCodeAttr;


    public LdapJavaStoredProcClassLoader( Attribute javaByteCodeAttr )
    {
        // Critical call to super class constructor. Required for true plumbing of class loaders.
        super( LdapJavaStoredProcClassLoader.class.getClassLoader() );

        this.javaByteCodeAttr = javaByteCodeAttr;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> findClass( String name ) throws ClassNotFoundException
    {
        byte[] classBytes;

        try
        {
            classBytes = javaByteCodeAttr.getBytes();
        }
        catch ( LdapException e )
        {
            throw new ClassNotFoundException();
        }

        return defineClass( name, classBytes, 0, classBytes.length );
    }
}
