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
package org.apache.directory.server.osgi.integ;


import org.apache.directory.server.kerberos.ChangePasswordConfig;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.changepwd.ChangePasswordServer;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.protocol.codec.KerberosProtocolCodecFactory;
import org.apache.directory.server.kerberos.sam.SamSubsystem;
import org.apache.directory.server.kerberos.sam.TimestampChecker;


public class ServerProtocolKerberosOsgiTest extends ServerOsgiTestBase
{

    @Override
    protected String getBundleName()
    {
        return "org.apache.directory.server.protocol.kerberos";
    }


    @Override
    protected void useBundleClasses() throws Exception
    {
        new KerberosConfig();
        new ChangePasswordConfig();
        new KdcServer();
        KerberosProtocolCodecFactory.getInstance().getDecoder( null );
        KerberosProtocolCodecFactory.getInstance().getEncoder( null );
        new ChangePasswordServer();
        new TimestampChecker();
        SamSubsystem.getInstance();
    }

}
