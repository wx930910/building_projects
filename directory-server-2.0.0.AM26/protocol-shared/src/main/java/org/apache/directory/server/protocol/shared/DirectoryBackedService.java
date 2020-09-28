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


import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;


/**
 * Base class shared by all protocol providers for configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class DirectoryBackedService extends AbstractProtocolService
{
    /**
     * The single location where entries are stored.  If this service
     * is catalog based the store will search the system partition
     * configuration for catalog entries.  Otherwise it will use this
     * search base as a single point of searching the DIT.
     */
    private String searchBaseDn = ServerDNConstants.USER_EXAMPLE_COM_DN;

    /** determines if the search base is pointer to a catalog or a single entry point */
    private boolean catelogBased;

    /** directory service core where protocol data is backed */
    private DirectoryService directoryService;


    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    /**
     * Set the DirectoryService
     * 
     * @param directoryService The DirectoryService instance
     */
    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    /**
     * Returns the search base Dn.
     *
     * @return The search base Dn.
     */
    public String getSearchBaseDn()
    {
        return searchBaseDn;
    }


    /**
     * @param searchBaseDn The searchBaseDn to set.
     */
    public void setSearchBaseDn( String searchBaseDn )
    {
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * Gets true if this service uses a catalog for searching different
     * regions of the DIT for its data.
     *
     * @return true if the search base dn is for a catalog, false otherwise
     */
    public boolean isCatelogBased()
    {
        return catelogBased;
    }


    /**
     * Set true if this service uses a catalog for searching different
     * regions of the DIT for its data.
     *
     * @param  catelogBased if the search base dn is for a catalog, false otherwise
     */
    public void setCatelogBased( boolean catelogBased )
    {
        this.catelogBased = catelogBased;
    }
}
