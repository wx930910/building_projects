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
package org.apache.directory.server.core.avltree;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor for an AvlTree.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlTreeCursor<E> extends AbstractCursor<E>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** The underlying AVL tree */
    private AvlTree<E> tree;

    /** The current node */
    private LinkedAvlNode<E> node;

    /** The current position of this cursor, relative to the node */
    private Position position = Position.BEFORE_FIRST;


    public AvlTreeCursor( AvlTree<E> tree )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating AvlTreeCursor {}", this );
        }

        this.tree = tree;
    }


    /**
     * {@inheritDoc}
     */
    public void after( E element ) throws LdapException, CursorException
    {
        checkNotClosed();

        if ( element == null )
        {
            afterLast();
            return;
        }

        node = tree.findGreater( element );

        if ( node == null )
        {
            position = Position.AFTER_LAST;
        }
        else
        {
            // the cursor should be positioned after the given element
            // we just fetched the next greater element so the cursor
            // is positioned before the fetched element
            position = Position.BEFORE_NODE;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        node = null;
        position = Position.AFTER_LAST;
    }


    public boolean available()
    {
        return position == Position.ON_NODE;
    }


    /**
     * {@inheritDoc}
     */
    public void before( E element ) throws LdapException, CursorException
    {
        checkNotClosed();

        if ( element == null )
        {
            beforeFirst();
            return;
        }

        node = tree.findLess( element );

        if ( node == null )
        {
            position = Position.BEFORE_FIRST;
        }
        else
        {
            // the cursor should be positioned before the given element
            // we just fetched the next less element so the cursor
            // is positioned after the fetched element
            position = Position.AFTER_NODE;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        node = null;
        position = Position.BEFORE_FIRST;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        checkNotClosed();

        node = tree.getFirst();

        if ( node == null )
        {
            position = Position.BEFORE_FIRST;
            return false;
        }
        else
        {
            position = Position.ON_NODE;
            return true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public E get() throws CursorException
    {
        checkNotClosed();

        if ( position == Position.ON_NODE )
        {
            return node.getKey();
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        checkNotClosed();

        node = tree.getLast();

        if ( node == null )
        {
            position = Position.AFTER_LAST;
            return false;
        }
        else
        {
            position = Position.ON_NODE;
            return true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed();

        switch ( position )
        {
            case BEFORE_FIRST:
                return first();

            case BEFORE_NODE:
                position = Position.ON_NODE;
                return true;

            case ON_NODE:
            case AFTER_NODE:
                node = node.next;
                if ( node == null )
                {
                    afterLast();
                    return false;
                }
                else
                {
                    position = Position.ON_NODE;
                    return true;
                }

            case AFTER_LAST:
                return false;

            default:
                throw new IllegalStateException( "Unexpected position " + position );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed();

        switch ( position )
        {
            case BEFORE_FIRST:
                return false;

            case BEFORE_NODE:
            case ON_NODE:
                node = node.previous;
                if ( node == null )
                {
                    beforeFirst();
                    return false;
                }
                else
                {
                    position = Position.ON_NODE;
                    return true;
                }

            case AFTER_NODE:
                position = Position.ON_NODE;
                return true;

            case AFTER_LAST:
                return last();

            default:
                throw new IllegalStateException( "Unexpected position " + position );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AvlTreeCursor {}", this );
        }

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception reason ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AvlTreeCursor {}", this );
        }

        super.close( reason );
    }
}
