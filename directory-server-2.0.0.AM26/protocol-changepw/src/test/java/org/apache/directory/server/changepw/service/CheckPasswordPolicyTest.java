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
package org.apache.directory.server.changepw.service;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * Tests {@link CheckPasswordPolicy}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class CheckPasswordPolicyTest
{
    private static final int passwordLength = 6;
    private static final int categoryCount = 3;
    private static final int tokenSize = 3;

    private static final CheckPasswordPolicy policy = new CheckPasswordPolicy();


    /**
     * Tests that a good password is valid according to all policy checks.
     */
    @Test
    public void testGoodPassword()
    {
        String username = "Enrique Rodriguez";
        String password = "d1r3ct0rY";
        assertTrue( policy.isValidPasswordLength( password, passwordLength ) );
        assertTrue( policy.isValidCategoryCount( password, categoryCount ) );
        assertTrue( policy.isValidUsernameSubstring( username, password, tokenSize ) );
        assertTrue( policy.isValid( username, password, passwordLength, categoryCount, tokenSize ) );
    }


    /**
     * Tests that a bad password fails all validity checks.
     */
    @Test
    public void testBadPassword()
    {
        String username = "Erin Randall";
        String password = "erin1";
        assertFalse( policy.isValidPasswordLength( password, passwordLength ) );
        assertFalse( policy.isValidCategoryCount( password, categoryCount ) );
        assertFalse( policy.isValidUsernameSubstring( username, password, tokenSize ) );
        assertFalse( policy.isValid( username, password, passwordLength, categoryCount, tokenSize ) );
    }


    /**
     * Tests variations of a password where the password includes tokens of the username.
     */
    @Test
    public void testPrincipalAsUsername()
    {
        String username = new KerberosPrincipal( "erodriguez@EXAMPLE.COM" ).getName();
        String password1 = "d1r3ct0rY";
        String password2 = "ERodriguez@d1r3ct0rY";
        String password3 = "Example@d1r3ct0rY";

        assertTrue( policy.isValidUsernameSubstring( username, password1, tokenSize ) );

        assertFalse( policy.isValidUsernameSubstring( username, password2, tokenSize ) );
        assertFalse( policy.isValidUsernameSubstring( username, password3, tokenSize ) );
    }
}
