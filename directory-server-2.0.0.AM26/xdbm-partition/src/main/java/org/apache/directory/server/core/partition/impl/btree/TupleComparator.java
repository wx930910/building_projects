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
package org.apache.directory.server.core.partition.impl.btree;


import java.io.Serializable;

import org.apache.directory.api.ldap.model.schema.comparators.SerializableComparator;


/**
 * Used to compare the sorting order of binary data.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface TupleComparator<K, V> extends Serializable
{
    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
    SerializableComparator<K> getKeyComparator();


    /**
     * Gets the binary comparator used to compare valuess.  May be null in which
     * case the compareValue method will throw an UnsupportedOperationException.
     *
     * @return the binary comparator for comparing values.
     */
    SerializableComparator<V> getValueComparator();


    /**
     * Compares key Object to determine their sorting order returning a
     * value = to, &lt; or &gt; than 0.
     *
     * @param key1 the first key to compare
     * @param key2 the other key to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second byte array.
     */
    int compareKey( K key1, K key2 );


    /**
     * Comparse value Objects to determine their sorting order returning a
     * value = to, &lt; or &gt; than 0.
     *
     * @param value1 the first value to compare
     * @param value2 the other value to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second Object.
     */
    int compareValue( V value1, V value2 );
}
