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
package org.apache.directory.server.core.jndi;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SchemaViolationException;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.DirStateFactory;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests to make sure that object and state factories work.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
public class ObjStateFactoryIT extends AbstractLdapTestUnit
{

    @Test
    public void testObjectFactory() throws Exception
    {
        LdifEntry akarasulu = getUserAddLdif();
        getService().getAdminSession().add(
            new DefaultEntry( getService().getSchemaManager(), akarasulu.getEntry() ) );

        LdapContext sysRoot = getSystemContext( getService() );
        sysRoot.addToEnvironment( Context.OBJECT_FACTORIES, PersonObjectFactory.class.getName() );
        Object obj = sysRoot.lookup( "uid=akarasulu, ou=users" );
        Attributes attrs = sysRoot.getAttributes( "uid=akarasulu, ou=users" );
        assertEquals( Person.class, obj.getClass() );
        Person me = ( Person ) obj;
        assertEquals( attrs.get( "sn" ).get(), me.getLastname() );
        assertEquals( attrs.get( "cn" ).get(), me.getCn() );
        assertEquals( "test", Strings.utf8ToString( ( byte[] ) attrs.get( "userPassword" ).get() ) );
        assertEquals( attrs.get( "telephonenumber" ).get(), me.getTelephoneNumber() );
        assertNull( me.getSeealso() );
        assertNull( me.getDescription() );
    }


    @Test
    public void testStateFactory() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        sysRoot.addToEnvironment( Context.STATE_FACTORIES, PersonStateFactory.class.getName() );
        Person p = new Person( "Rodriguez", "Mr. Kerberos", "noices", "555-1212", "sn=erodriguez", "committer" );
        sysRoot.bind( "sn=Rodriguez, ou=users", p );
        Attributes attrs = sysRoot.getAttributes( "sn=Rodriguez, ou=users" );
        assertEquals( "Rodriguez", attrs.get( "sn" ).get() );
        assertEquals( "Mr. Kerberos", attrs.get( "cn" ).get() );
        assertEquals( "noices", Strings.utf8ToString( ( byte[] ) attrs.get( "userPassword" ).get() ) );
        assertEquals( "555-1212", attrs.get( "telephonenumber" ).get() );
        assertEquals( "sn=erodriguez", attrs.get( "seealso" ).get() );
        assertEquals( "committer", attrs.get( "description" ).get() );
    }

    public static class PersonStateFactory implements DirStateFactory
    {
        public Result getStateToBind( Object obj, Name name, Context nameCtx, Hashtable environment, Attributes inAttrs )
            throws NamingException
        {
            // Only interested in Person objects
            if ( obj instanceof Person )
            {

                Attributes outAttrs;

                if ( inAttrs == null )
                {
                    outAttrs = new BasicAttributes( true );
                }
                else
                {
                    outAttrs = ( Attributes ) inAttrs.clone();
                }

                // Set up object class
                if ( outAttrs.get( "objectclass" ) == null )
                {
                    Attribute oc = new BasicAttribute( "objectclass", "person" );
                    oc.add( "top" );
                    outAttrs.put( oc );
                }

                Person per = ( Person ) obj;

                // mandatory attributes
                if ( per.getLastname() != null )
                {
                    outAttrs.put( "sn", per.getLastname() );
                }
                else
                {
                    throw new SchemaViolationException( "Person must have surname" );
                }

                if ( per.getCn() != null )
                {
                    outAttrs.put( "cn", per.getCn() );
                }
                else
                {
                    throw new SchemaViolationException( "Person must have common name" );
                }

                // optional attributes
                if ( per.getPassword() != null )
                {
                    outAttrs.put( "userPassword", per.getPassword() );
                }
                if ( per.getTelephoneNumber() != null )
                {
                    outAttrs.put( "telephoneNumber", per.getTelephoneNumber() );
                }
                if ( per.getSeealso() != null )
                {
                    outAttrs.put( "seeAlso", per.getSeealso() );
                }
                if ( per.getDescription() != null )
                {
                    outAttrs.put( "description", per.getDescription() );
                }

                return new DirStateFactory.Result( null, outAttrs );
            }

            return null;
        }


        public Object getStateToBind( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws NamingException
        {
            throw new UnsupportedOperationException( "Please use directory support overload with Attributes argument." );
        }
    }

    public static class PersonObjectFactory implements DirObjectFactory
    {
        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment, Attributes attrs )
            throws Exception
        {
            // Only interested in Attributes with "person" objectclass
            Attribute oc = ( attrs != null ? attrs.get( "objectclass" ) : null );
            if ( oc != null && oc.contains( "person" ) )
            {
                Attribute attr;
                String passwd = null;

                // Extract the password
                attr = attrs.get( "userPassword" );
                if ( attr != null )
                {
                    Object pw = attr.get();

                    if ( pw instanceof String )
                        passwd = ( String ) pw;
                    else
                        passwd = new String( ( byte[] ) pw,  StandardCharsets.UTF_8  );
                }

                return new Person( ( String ) attrs.get( "sn" ).get(), ( String ) attrs.get( "cn" ).get(), passwd,
                    ( attr = attrs.get( "telephoneNumber" ) ) != null ? ( String ) attr.get() : null, ( attr = attrs
                        .get( "seealso" ) ) != null ? ( String ) attr.get() : null,
                    ( attr = attrs.get( "description" ) ) != null ? ( String ) attr.get() : null );
            }
            return null;
        }


        public Object getObjectInstance( Object obj, Name name, Context nameCtx, Hashtable environment )
            throws Exception
        {
            throw new UnsupportedOperationException( "Please use directory support overload with Attributes argument." );
        }
    }

    public static class Person
    {
        private String sn, cn, pwd, tele, seealso, desc;


        public Person( String sn, String cn, String pwd, String tele, String seealso, String desc )
        {
            this.sn = sn;
            this.cn = cn;
            this.pwd = pwd;
            this.tele = tele;
            this.seealso = seealso;
            this.desc = desc;
        }


        public String getLastname()
        {
            return sn;
        }


        public String getCn()
        {
            return cn;
        }


        public String getPassword()
        {
            return pwd;
        }


        public String getTelephoneNumber()
        {
            return tele;
        }


        public String getSeealso()
        {
            return seealso;
        }


        public String getDescription()
        {
            return desc;
        }
    }
}
