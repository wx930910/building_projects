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

package org.apache.directory.server.dns.store.jndi;


import java.util.Map;

import org.apache.directory.server.protocol.shared.catalog.Catalog;


/**
 * A catalog for mapping DNS zones to search base Dn's.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class DnsCatalog implements Catalog
{
    private Map<String, Object> map;


    /**
     * Creates a new instance of DnsCatalog.
     *
     * @param map
     */
    DnsCatalog( Map<String, Object> map )
    {
        this.map = map;
    }


    public String getBaseDn( String name )
    {
        if ( name.endsWith( "." ) )
        {
            int last = name.lastIndexOf( '.' );
            name = name.substring( 0, last );
        }

        while ( name.length() > 0 )
        {
            String candidate = ( String ) map.get( name );
            
            if ( candidate != null )
            {
                return candidate;
            }

            int period = name.indexOf( '.' );

            if ( period > -1 )
            {
                name = name.substring( period + 1 );
            }
            else
            {
                return "";
            }
        }

        return "";
    }
}
