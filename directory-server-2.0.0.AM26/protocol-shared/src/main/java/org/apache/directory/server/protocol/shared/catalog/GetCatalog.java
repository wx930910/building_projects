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

package org.apache.directory.server.protocol.shared.catalog;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.FilterParser;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.protocol.shared.store.DirectoryServiceOperation;


/**
 * A Session operation for building a catalog.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GetCatalog implements DirectoryServiceOperation
{
    /**
     * Note that the base is relative to the existing context.
     */
    public Object execute( CoreSession session, Dn base ) throws Exception
    {
        String filter = "(objectClass=" + ApacheSchemaConstants.APACHE_CATALOG_ENTRY_OC + ")";

        Cursor<Entry> list = session.search(
            Dn.ROOT_DSE,
            SearchScope.SUBTREE,
            FilterParser.parse( session.getDirectoryService().getSchemaManager(), filter ),
            AliasDerefMode.DEREF_ALWAYS );

        Map<String, String> catalog = new HashMap<>();

        list.beforeFirst();

        while ( list.next() )
        {
            Entry result = list.get();

            String name = null;
            Attribute attribute = result.get( ApacheSchemaConstants.APACHE_CATALOGUE_ENTRY_NAME_AT );

            if ( attribute != null )
            {
                name = attribute.getString();
            }

            String basedn = null;
            attribute = result.get( ApacheSchemaConstants.APACHE_CATALOGUE_ENTRY_BASE_DN_AT );

            if ( attribute != null )
            {
                basedn = attribute.getString();
            }

            catalog.put( name, basedn );
        }

        return catalog;
    }
}
