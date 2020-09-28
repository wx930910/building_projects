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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.File;
import java.io.IOException;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A special index which stores Rdn objects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotRdnIndex extends MavibotIndex<ParentIdAndRdn>
{

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotRdnIndex.class );


    public MavibotRdnIndex()
    {
        super( ApacheSchemaConstants.APACHE_RDN_AT_OID, true );
        initialized = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws LdapException, IOException
    {
        LOG.debug( "Initializing an Index for attribute '{}'", attributeType.getName() );

        this.attributeType = attributeType;

        if ( attributeId == null )
        {
            setAttributeId( attributeType.getName() );
        }

        if ( this.wkDirPath == null )
        {
            throw new NullPointerException( "The index working directory has not be set" );
        }

        String path = new File( this.wkDirPath, attributeType.getOid() ).getAbsolutePath();

        try
        {
            initTables( schemaManager );
        }
        catch ( IOException e )
        {
            // clean up
            close( null );
            throw e;
        }

        initialized = true;
    }


    /**
     * Initializes the forward and reverse tables used by this Index.
     *
     * @param schemaManager The server schemaManager
     * @throws IOException if we cannot initialize the forward and reverse
     * tables
     * @throws NamingException
     */
    private void initTables( SchemaManager schemaManager ) throws IOException
    {
        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            throw new IOException( I18n.err( I18n.ERR_574, attributeType.getName() ) );
        }

        MavibotParentIdAndRdnSerializer.setSchemaManager( schemaManager );
        MavibotParentIdAndRdnSerializer parentIdAndSerializer = new MavibotParentIdAndRdnSerializer();

        String forwardTableName = attributeType.getOid() + FORWARD_BTREE;
        forward = new MavibotTable<ParentIdAndRdn, String>( recordMan, schemaManager, forwardTableName,
            parentIdAndSerializer, StringSerializer.INSTANCE, false );

        String reverseTableName = attributeType.getOid() + REVERSE_BTREE;
        reverse = new MavibotTable<String, ParentIdAndRdn>( recordMan, schemaManager, reverseTableName,
            StringSerializer.INSTANCE, parentIdAndSerializer, false );
    }
}
