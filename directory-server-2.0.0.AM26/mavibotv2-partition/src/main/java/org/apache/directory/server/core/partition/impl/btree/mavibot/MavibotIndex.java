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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.serializer.ByteArraySerializer;
import org.apache.directory.mavibot.btree.serializer.ElementSerializer;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndex;
import org.apache.directory.server.xdbm.EmptyIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Mavibot based index implementation. It creates an Index for a give AttributeType.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotIndex<K> extends AbstractIndex<K, String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotIndex.class.getSimpleName() );

    /** default duplicate limit before duplicate keys switch to using a btree for values */
    public static final int DEFAULT_DUPLICATE_LIMIT = 512;

    /**  the key used for the forward btree name */
    public static final String FORWARD_BTREE = "_forward";

    /**  the key used for the reverse btree name */
    public static final String REVERSE_BTREE = "_reverse";

    /**
     * the forward btree where the btree key is the value of the indexed attribute and
     * the value of the btree is the entry id of the entry containing an attribute with
     * that value
     */
    protected MavibotTable<K, String> forward;

    /**
     * the reverse btree where the btree key is the entry id of the entry containing a
     * value for the indexed attribute, and the btree value is the value of the indexed
     * attribute
     */
    protected MavibotTable<String, K> reverse;

    /** a custom working directory path when specified in configuration */
    protected File wkDirPath;

    /** The recordManager */
    protected RecordManager recordMan;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ----------------------------------------------------------------------
    /**
     * Creates a JdbmIndex instance for a give AttributeId
     */
    public MavibotIndex( String attributeId, boolean withReverse )
    {
        super( attributeId, withReverse );

        initialized = false;
    }


    /**
     * Initialize the index for an Attribute, with a specific working directory (may be null).
     *
     * @param schemaManager The schemaManager to use to get back the Attribute
     * @param attributeType The attributeType this index is created for
     * @throws IOException If the initialization failed
     */
    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws IOException
    {
        LOG.debug( "Initializing an Index for attribute '{}'", attributeType.getName() );

        // check if the RecordManager reference is null, if yes, then throw an IllegalStateException
        if ( recordMan == null )
        {
            throw new IllegalStateException( "No RecordManager reference was set in the index " + getAttributeId() );
        }

        this.attributeType = attributeType;

        if ( attributeId == null )
        {
            setAttributeId( attributeType.getName() );
        }

        if ( this.wkDirPath == null )
        {
            throw new NullPointerException( "The index working directory has not be set" );
        }

        try
        {
            initTables( schemaManager );
        }
        catch ( IOException e )
        {
            // clean up
            close();
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
     */
    private void initTables( SchemaManager schemaManager ) throws IOException
    {
        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            throw new IOException( I18n.err( I18n.ERR_574, attributeType.getName() ) );
        }

        SerializableComparator<K> comp = new SerializableComparator<>( mr.getOid() );
        comp.setSchemaManager( schemaManager );

        /*
         * The forward key/value map stores attribute values to master table
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */

        ElementSerializer<K> forwardKeySerializer = null;

        if ( !attributeType.getSyntax().isHumanReadable() )
        {
            forwardKeySerializer = ( ElementSerializer<K> ) new ByteArraySerializer( ( Comparator<byte[]> ) comp );
        }
        else
        {
            forwardKeySerializer = ( ElementSerializer<K> ) new StringSerializer( ( Comparator<String> ) comp );
        }

        boolean forwardDups = true;

        String oid = attributeType.getOid();
        // disable duplicates for entryCSN and entryUUID attribute indices
        if ( oid.equals( SchemaConstants.ENTRY_CSN_AT_OID ) || oid.equals( SchemaConstants.ENTRY_UUID_AT_OID ) )
        {
            forwardDups = false;
        }

        String forwardTableName = attributeType.getOid() + FORWARD_BTREE;
        forward = new MavibotTable<>( recordMan, schemaManager, forwardTableName, forwardKeySerializer,
            StringSerializer.INSTANCE, forwardDups, AbstractBTreePartition.DEFAULT_CACHE_SIZE );

        /*
         * Now the reverse map stores the primary key into the master table as
         * the key and the values of attributes as the value.  If an attribute
         * is single valued according to its specification based on a schema
         * then duplicate keys should not be allowed within the reverse table.
         */
        if ( withReverse )
        {
            String reverseTableName = attributeType.getOid() + REVERSE_BTREE;
            reverse = new MavibotTable<>( recordMan, schemaManager, reverseTableName, StringSerializer.INSTANCE,
                forwardKeySerializer, !attributeType.isSingleValued() );
        }

        String path = new File( this.wkDirPath, attributeType.getOid() ).getAbsolutePath();
        
        // finally write a text file in the format <OID>-<attribute-name>.txt
        try ( FileWriter fw = new FileWriter( new File( path + "-" + attributeType.getName() + ".txt" ) ) )
        {
            // write the AttributeType description
            fw.write( attributeType.toString() );
        } 
    }


    /**
     * Sets the RecordManager
     *
     * @param rm the RecordManager instance
     */
    public void setRecordManager( RecordManager rm )
    {
        this.recordMan = rm;
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------

    /**
     * Sets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @param wkDirPath optional working directory path
     */
    public void setWkDirPath( URI wkDirPath )
    {
        protect( "wkDirPath" );
        this.wkDirPath = new File( wkDirPath );
    }


    /**
     * Gets the working directory path to something other than the default. Sometimes more
     * performance is gained by locating indices on separate disk spindles.
     *
     * @return optional working directory path
     */
    public URI getWkDirPath()
    {
        return wkDirPath != null ? wkDirPath.toURI() : null;
    }


    // ------------------------------------------------------------------------
    // Scan Count Methods
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public long count() throws IOException
    {
        return forward.count();
    }


    /**
     * {@inheritDoc}
     */
    public long count( K attrVal ) throws Exception
    {
        return forward.count( attrVal );
    }


    public long greaterThanCount( K attrVal ) throws Exception
    {
        return forward.greaterThanCount( attrVal );
    }


    /**
     * @see org.apache.directory.server.xdbm.Index#lessThanCount(java.lang.Object)
     */
    public long lessThanCount( K attrVal ) throws Exception
    {
        return forward.lessThanCount( attrVal );
    }


    // ------------------------------------------------------------------------
    // Forward and Reverse Lookups
    // ------------------------------------------------------------------------

    /**
     * @see Index#forwardLookup(java.lang.Object)
     */
    public String forwardLookup( K attrVal ) throws Exception
    {
        return forward.get( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public K reverseLookup( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        if ( withReverse )
        {
            return reverse.get( partitionTxn, id );
        }
        else
        {
            return null;
        }
    }


    // ------------------------------------------------------------------------
    // Add/Drop Methods
    // ------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void add( K attrVal, String id ) throws Exception
    {
        // The pair to be removed must exists
        forward.put( attrVal, id );

        if ( withReverse )
        {
            reverse.put( id, attrVal );
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void drop( K attrVal, String id ) throws Exception
    {
        // The pair to be removed must exists
        if ( forward.has( attrVal, id ) )
        {
            forward.remove( attrVal, id );

            if ( withReverse )
            {
                reverse.remove( id, attrVal );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void drop( String entryId ) throws Exception
    {
        if ( withReverse )
        {
            if ( isDupsEnabled() )
            {
                // Build a cursor to iterate on all the keys referencing
                // this entryId
                Cursor<Tuple<String, K>> values = reverse.cursor( entryId );

                while ( values.next() )
                {
                    // Remove the Key -> entryId from the index
                    forward.remove( values.get().getValue(), entryId );
                }

                values.close();
            }
            else
            {
                K key = reverse.get( entryId );

                forward.remove( key );
            }

            // Remove the id -> key from the reverse index
            reverse.remove( entryId );
        }
    }


    // ------------------------------------------------------------------------
    // Index Cursor Operations
    // ------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> reverseCursor() throws Exception
    {
        if ( withReverse )
        {
            return new IndexCursorAdaptor<>( ( Cursor ) reverse.cursor(), false );
        }
        else
        {
            return new EmptyIndexCursor<>();
        }
    }


    public Cursor<IndexEntry<K, String>> forwardCursor() throws LdapException
    {
        return new IndexCursorAdaptor<K>( ( Cursor ) forward.cursor(), true );
    }


    @SuppressWarnings("unchecked")
    public Cursor<IndexEntry<K, String>> reverseCursor( String id ) throws Exception
    {
        if ( withReverse )
        {
            return new IndexCursorAdaptor<>( ( Cursor ) reverse.cursor( id ), false );
        }
        else
        {
            return new EmptyIndexCursor<>();
        }
    }


    public Cursor<IndexEntry<K, String>> forwardCursor( PartitionTxn partitionTxn, K key ) throws LdapException
    {
        return new IndexCursorAdaptor<>( ( Cursor ) forward.cursor( partitionTxn, key ), true );
    }


    public Cursor<K> reverseValueCursor( String id ) throws Exception
    {
        if ( withReverse )
        {
            return reverse.valueCursor( id );
        }
        else
        {
            return new EmptyCursor<>();
        }
    }


    public Cursor<String> forwardValueCursor( K key ) throws Exception
    {
        return forward.valueCursor( key );
    }


    // ------------------------------------------------------------------------
    // Value Assertion (a.k.a Index Lookup) Methods //
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public boolean forward( K attrVal ) throws Exception
    {
        return forward.has( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( PartitionTxn partitionTxn, K attrVal, String id ) throws LdapException
    {
        return forward.has( partitionTxn, attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        if ( withReverse )
        {
            return reverse.has( partitionTxn, id );
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( PartitionTxn partitionTxn, String id, K attrVal ) throws LdapException
    {
        return forward.has( partitionTxn, attrVal, id );
    }


    // ------------------------------------------------------------------------
    // Maintenance Methods
    // ------------------------------------------------------------------------
    /**
     * @see org.apache.directory.server.xdbm.Index#close()
     */
    public synchronized void close() throws IOException
    {
        try
        {
            if ( forward != null )
            {
                forward.close();
            }

            if ( reverse != null )
            {
                reverse.close();
            }
        }
        catch ( Exception e )
        {
            throw new IOException( e );
        }
    }


    /**
     * @see Index#sync()
     */
    public synchronized void sync() throws IOException
    {
        // Nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDupsEnabled()
    {
        if ( withReverse )
        {
            return reverse.isDupsEnabled();
        }
        else
        {
            return false;
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Index<" + attributeId + ">";
    }
}
