package org.apache.commons.jcs3.utils.discovery;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.jcs3.engine.behavior.ICompositeCacheManager;
import org.apache.commons.jcs3.engine.behavior.IProvideScheduler;
import org.apache.commons.jcs3.log.Log;
import org.apache.commons.jcs3.log.LogManager;

/**
 * This manages UDPDiscovery Services. We should end up with one service per Lateral Cache Manager
 * Instance. One service works for multiple regions. We don't want a connection for each region.
 * <p>
 * @author Aaron Smuts
 */
public class UDPDiscoveryManager
{
    /** The logger */
    private static final Log log = LogManager.getLog( UDPDiscoveryManager.class );

    /** Singleton instance */
    private static UDPDiscoveryManager INSTANCE = new UDPDiscoveryManager();

    /** Known services */
    private final ConcurrentMap<String, UDPDiscoveryService> services = new ConcurrentHashMap<>();

    /** private for singleton */
    private UDPDiscoveryManager()
    {
        // noopt
    }

    /**
     * Singleton
     * <p>
     * @return UDPDiscoveryManager
     */
    public static UDPDiscoveryManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Creates a service for the address and port if one doesn't exist already.
     * <p>
     * We need to key this using the listener port too. TODO think of making one discovery service
     * work for multiple types of clients.
     * <p>
     * @param discoveryAddress
     * @param discoveryPort
     * @param servicePort
     * @param cacheMgr
     * @return UDPDiscoveryService
     */
    public UDPDiscoveryService getService( String discoveryAddress, int discoveryPort, int servicePort,
                                                        ICompositeCacheManager cacheMgr )
    {
        String key = discoveryAddress + ":" + discoveryPort + ":" + servicePort;

        UDPDiscoveryService service = services.computeIfAbsent(key, k -> {
            log.info( "Creating service for address:port:servicePort [{0}]", key );

            UDPDiscoveryAttributes attributes = new UDPDiscoveryAttributes();
            attributes.setUdpDiscoveryAddr( discoveryAddress );
            attributes.setUdpDiscoveryPort( discoveryPort );
            attributes.setServicePort( servicePort );

            UDPDiscoveryService newService = new UDPDiscoveryService( attributes );

            // register for shutdown notification
            cacheMgr.registerShutdownObserver( newService );

            // inject scheduler
            if ( cacheMgr instanceof IProvideScheduler)
            {
                newService.setScheduledExecutorService(((IProvideScheduler)cacheMgr)
                        .getScheduledExecutorService());
            }

            newService.startup();
            return newService;
        });

        log.debug( "Returning service [{0}] for key [{1}]", service, key );

        return service;
    }
}
