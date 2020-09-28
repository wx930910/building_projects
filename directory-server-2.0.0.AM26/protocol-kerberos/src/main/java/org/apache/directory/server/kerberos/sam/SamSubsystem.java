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
package org.apache.directory.server.kerberos.sam;


import java.util.EnumMap;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.security.auth.kerberos.KerberosKey;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.codec.types.SamType;


/**
 * The Subsystem that enables the Kerberos server to use plugable Single-use
 * Authentication mechanisms.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class SamSubsystem
{
    /** the property key base used for SAM algorithm verifiers */
    public static final String PROPKEY_BASE = "kerberos.sam.type.";

    /** the SAM subsystem instance */
    private static SamSubsystem instance;

    /** a map of verifiers so we do not need to create a new one every time */
    private final EnumMap<SamType, SamVerifier> verifiers = new EnumMap<>( SamType.class );

    /** the key integrity checker used by the subsystem for all sam types */
    private KeyIntegrityChecker keyChecker;

    /** the user context the SamSubsystem would use to verify passwords */
    private DirContext userContext;
    private String userBaseRdn;


    /**
     * Gets the singleton instance of the SamSubsystem.
     *
     * @return the singleton for the SamSubsystem
     */
    public static SamSubsystem getInstance()
    {
        if ( instance == null )
        {
            instance = new SamSubsystem();
        }

        return instance;
    }


    /**
     * Sets the KeyIntegrityChecker used by the entire SamSubsystem.
     *
     * @param keyChecker the KeyIntegrityChecker used by the entire SamSubsystem
     */
    public void setIntegrityChecker( KeyIntegrityChecker keyChecker )
    {
        this.keyChecker = keyChecker;
    }


    /**
     * Uses the principal entry information to load the approapriate SamVerifier
     * and verify the Single-use password.
     *
     * @param entry the store entry for the Kerberos principal
     * @param sad the single-use authentication data encrypted timestamp payload
     * @return true if verification passed, false otherwise
     * @throws SamException thrown when there is a failure within the verifier
     * or a verifier cannot be found.
     */
    public KerberosKey verify( PrincipalStoreEntry entry, byte[] sad ) throws SamException
    {
        SamVerifier verifier = null;

        if ( keyChecker == null )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_651 ) );
        }

        if ( entry.getSamType() == null )
        {
            throw new SamException( entry.getSamType(), I18n.err( I18n.ERR_652 ) );
        }

        if ( verifiers.containsKey( entry.getSamType() ) )
        {
            verifier = verifiers.get( entry.getSamType() );

            return verifier.verify( entry.getPrincipal(), sad );
        }

        String key = PROPKEY_BASE + entry.getSamType().getOrdinal();

        Hashtable<Object, Object> env = new Hashtable<>();

        try
        {
            env.putAll( userContext.getEnvironment() );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }

        if ( !env.containsKey( key ) )
        {
            String msg = I18n.err( I18n.ERR_653, key );

            throw new SamException( entry.getSamType(), msg );
        }

        String fqcn = ( String ) env.get( key );

        try
        {
            Class c = Class.forName( fqcn );

            verifier = ( SamVerifier ) c.newInstance();

            try
            {
                verifier.setUserContext( ( DirContext ) userContext.lookup( userBaseRdn ) );
            }
            catch ( NamingException e )
            {
                e.printStackTrace();

            }

            verifier.setIntegrityChecker( keyChecker );

            verifier.startup();

            if ( !verifier.getSamType().equals( entry.getSamType() ) )
            {
                String msg = I18n.err( I18n.ERR_654, verifier.getSamType(), entry.getSamType() );

                throw new SamException( entry.getSamType(), msg );
            }

            verifiers.put( verifier.getSamType(), verifier );

            return verifier.verify( entry.getPrincipal(), sad );
        }
        catch ( ClassNotFoundException e )
        {
            String msg = I18n.err( I18n.ERR_655, fqcn, entry.getSamType() );

            throw new SamException( entry.getSamType(), msg, e );
        }
        catch ( IllegalAccessException e )
        {
            String msg = I18n.err( I18n.ERR_656, fqcn, entry.getSamType() );

            throw new SamException( entry.getSamType(), msg, e );
        }
        catch ( InstantiationException e )
        {
            String msg = I18n.err( I18n.ERR_657, fqcn, entry.getSamType() );

            throw new SamException( entry.getSamType(), msg, e );
        }
    }


    /**
     * Sets the context under which user entries can be found.
     *
     * @param userContext the jndi context under which users can be found.
     * @param userBaseRdn the container with users
     */
    public void setUserContext( DirContext userContext, String userBaseRdn )
    {
        this.userContext = userContext;
        this.userBaseRdn = userBaseRdn;
    }
}
