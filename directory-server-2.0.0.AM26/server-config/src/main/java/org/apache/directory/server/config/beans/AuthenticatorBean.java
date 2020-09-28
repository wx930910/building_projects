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
package org.apache.directory.server.config.beans;


import org.apache.directory.server.config.ConfigurationElement;


/**
 * Base authenticator bean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AuthenticatorBean extends AdsBaseBean
{
    /** The authenticator id */
    @ConfigurationElement(attributeType = "ads-authenticatorId", isRdn = true)
    private String authenticatorId;

    /** The base DN which will be the starting point from which we use the authenticator */
    @ConfigurationElement(attributeType = "ads-baseDn", isOptional = false)
    protected String baseDn;


    /**
     * @return the authenticatorId
     */
    public String getAuthenticatorId()
    {
        return authenticatorId;
    }


    /**
     * @param authenticatorId the authenticatorId to set
     */
    public void setAuthenticatorId( String authenticatorId )
    {
        this.authenticatorId = authenticatorId;
    }


    /**
     * @return the baseDn
     */
    public String getBaseDn()
    {
        return baseDn;
    }


    /**
     * @param baseDn the baseDn to set
     */
    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }
}
