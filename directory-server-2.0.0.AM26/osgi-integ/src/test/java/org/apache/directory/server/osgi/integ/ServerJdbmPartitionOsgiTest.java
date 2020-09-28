/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.osgi.integ;


import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.server.core.partition.impl.btree.jdbm.DnSerializer;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.ParentIdAndRdnSerializer;
import org.apache.directory.server.core.shared.DefaultDnFactory;


public class ServerJdbmPartitionOsgiTest extends ServerOsgiTestBase
{

    @Override
    protected String getBundleName()
    {
        return "org.apache.directory.server.jdbm.partition";
    }


    @Override
    protected void useBundleClasses() throws Exception
    {
        new JdbmIndex<String>( "foo", false );
        SchemaManager schemaManager = new DefaultSchemaManager();
        new JdbmPartition( schemaManager, new DefaultDnFactory( schemaManager, 100 ) );
        new ParentIdAndRdnSerializer( schemaManager );
        new DnSerializer( schemaManager ).serialize( new Dn( "cn=foo" ) );
    }

}
