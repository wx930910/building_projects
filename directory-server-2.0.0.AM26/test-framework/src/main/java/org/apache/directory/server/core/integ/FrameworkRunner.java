/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import java.lang.reflect.Method;
import java.util.UUID;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The class responsible for running all the tests. t read the annotations, 
 * initialize the DirectoryService, call each test and do the cleanup at the end.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class FrameworkRunner extends BlockJUnit4ClassRunner
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( FrameworkRunner.class );

    /** The 'service' field in the run tests */
    private static final String SET_SERVICE_METHOD_NAME = "setService";

    /** The 'ldapServer' field in the run tests */
    private static final String SET_LDAP_SERVER_METHOD_NAME = "setLdapServer";

    /** The 'kdcServer' field in the run tests */
    private static final String SET_KDC_SERVER_METHOD_NAME = "setKdcServer";

    /** The DirectoryService for this class, if any */
    private DirectoryService classDS;

    /** The LdapServer for this class, if any */
    private LdapServer classLdapServer;

    /** The KdcServer for this class, if any */
    private KdcServer classKdcServer;


    /**
     * Creates a new instance of FrameworkRunner.
     * 
     * @param clazz The class to run
     * @throws InitializationError If the initialization failed
     */
    public FrameworkRunner( Class<?> clazz ) throws InitializationError
    {
        super( clazz );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run( final RunNotifier notifier )
    {
        // Before running any test, check to see if we must create a class DS
        // Get the LdapServerBuilder, if any
        CreateLdapServer classLdapServerBuilder = getDescription().getAnnotation( CreateLdapServer.class );

        try
        {
            classDS = DSAnnotationProcessor.getDirectoryService( getDescription() );
            long revision = 0L;
            DirectoryService directoryService = null;

            if ( classDS != null )
            {
                // We have a class DS defined, update it
                directoryService = classDS;

                DSAnnotationProcessor.applyLdifs( getDescription(), classDS );
            }
            else
            {
                // No : define a default class DS then
                DirectoryServiceFactory dsf = DefaultDirectoryServiceFactory.class.newInstance();

                directoryService = dsf.getDirectoryService();
                // enable CL explicitly cause we are not using DSAnnotationProcessor
                directoryService.getChangeLog().setEnabled( true );

                dsf.init( "default" + UUID.randomUUID().toString() );

                // Stores the defaultDS in the classDS
                classDS = directoryService;

                // Load the schemas
                DSAnnotationProcessor.loadSchemas( getDescription(), directoryService );

                // Apply the class LDIFs
                DSAnnotationProcessor.applyLdifs( getDescription(), directoryService );
            }
            
            // check if it has a LdapServerBuilder
            // then use the DS created above
            if ( classLdapServerBuilder != null )
            {
                classLdapServer = ServerAnnotationProcessor.createLdapServer( getDescription(), directoryService );
            }

            if ( classKdcServer == null )
            {
                int minPort = getMinPort();

                classKdcServer = ServerAnnotationProcessor.getKdcServer( getDescription(), directoryService,
                    minPort + 1 );
            }

            // print out information which partition factory we use
            DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
            PartitionFactory partitionFactory = dsFactory.getPartitionFactory();
            LOG.debug( "Using partition factory {}", partitionFactory.getClass().getSimpleName() );

            // Now run the class
            super.run( notifier );

            if ( classLdapServer != null )
            {
                classLdapServer.stop();
            }

            if ( classKdcServer != null )
            {
                classKdcServer.stop();
            }

            // cleanup classService if it is not the same as suite service or
            // it is not null (this second case happens in the absence of a suite)
            if ( classDS != null )
            {
                LOG.debug( "Shuting down DS for {}", classDS.getInstanceId() );
                classDS.shutdown();
                FileUtils.deleteDirectory( classDS.getInstanceLayout().getInstanceDirectory() );
            }
            else
            {
                // Revert the ldifs
                // We use a class or suite DS, just revert the current test's modifications
                revert( directoryService, revision );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            LOG.error( I18n.err( I18n.ERR_181, getTestClass().getName() ) );
            LOG.error( e.getLocalizedMessage() );
            notifier.fireTestFailure( new Failure( getDescription(), e ) );
        }
        finally
        {
            // help GC to get rid of the directory service with all its references
            classDS = null;
            classLdapServer = null;
            classKdcServer = null;
        }
    }


    /**
     * Get the lower port out of all the transports
     */
    private int getMinPort()
    {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void runChild( FrameworkMethod method, RunNotifier notifier )
    {
        /** The LdapServer for this method, if any */
        LdapServer methodLdapServer = null;

        /** The KdcServer for this method, if any */
        KdcServer methodKdcServer = null;

        // Don't run the test if the @Ignored annotation is used
        if ( method.getAnnotation( Ignore.class ) != null )
        {
            Description description = describeChild( method );
            notifier.fireTestIgnored( description );
            return;
        }

        // Get the applyLdifs for each level
        Description suiteDescription = null;

        Description classDescription = getDescription();
        Description methodDescription = describeChild( method );

        // Before running any test, check to see if we must create a class DS
        // Get the LdapServerBuilder, if any
        CreateLdapServer methodLdapServerBuilder = methodDescription.getAnnotation( CreateLdapServer.class );
        CreateKdcServer methodKdcServerBuilder = methodDescription.getAnnotation( CreateKdcServer.class );

        // Ok, ready to run the test
        try
        {
            DirectoryService directoryService = null;

            // Set the revision to 0, we will revert only if it's set to another value
            long revision = 0L;

            // Check if this method has a dedicated DSBuilder
            DirectoryService methodDS = DSAnnotationProcessor.getDirectoryService( methodDescription );

            // give #1 priority to method level DS if present
            if ( methodDS != null )
            {
                // Apply all the LDIFs
                DSAnnotationProcessor.applyLdifs( suiteDescription, methodDS );
                DSAnnotationProcessor.applyLdifs( classDescription, methodDS );
                DSAnnotationProcessor.applyLdifs( methodDescription, methodDS );

                directoryService = methodDS;
            }
            else if ( classDS != null )
            {
                directoryService = classDS;

                // apply the method LDIFs, and tag for reversion
                revision = getCurrentRevision( directoryService );

                DSAnnotationProcessor.applyLdifs( methodDescription, directoryService );
            }
            // we don't support method level LdapServer so
            // we check for the presence of Class level LdapServer first 
            else if ( classLdapServer != null )
            {
                directoryService = classLdapServer.getDirectoryService();

                revision = getCurrentRevision( directoryService );

                DSAnnotationProcessor.applyLdifs( methodDescription, directoryService );
            }
            else if ( classKdcServer != null )
            {
                directoryService = classKdcServer.getDirectoryService();

                revision = getCurrentRevision( directoryService );

                DSAnnotationProcessor.applyLdifs( methodDescription, directoryService );
            }

            if ( methodLdapServerBuilder != null )
            {
                methodLdapServer = ServerAnnotationProcessor.createLdapServer( methodDescription, directoryService );
            }

            if ( methodKdcServerBuilder != null )
            {
                int minPort = getMinPort();

                methodKdcServer = ServerAnnotationProcessor.getKdcServer( methodDescription, directoryService,
                    minPort + 1 );
            }

            // At this point, we know which service to use.
            // Inject it into the class
            Method setService = null;

            try
            {
                setService = getTestClass().getJavaClass().getMethod( SET_SERVICE_METHOD_NAME,
                    DirectoryService.class );

                setService.invoke( getTestClass().getJavaClass(), directoryService );
            }
            catch ( NoSuchMethodException nsme )
            {
                // Do nothing
            }

            // if we run this class in a suite, tell it to the test
            Method setLdapServer = null;

            try
            {
                setLdapServer = getTestClass().getJavaClass().getMethod( SET_LDAP_SERVER_METHOD_NAME,
                    LdapServer.class );
            }
            catch ( NoSuchMethodException nsme )
            {
                // Do nothing
            }

            Method setKdcServer = null;

            try
            {
                setKdcServer = getTestClass().getJavaClass().getMethod( SET_KDC_SERVER_METHOD_NAME, KdcServer.class );
            }
            catch ( NoSuchMethodException nsme )
            {
                // Do nothing
            }

            DirectoryService oldLdapServerDirService = null;
            DirectoryService oldKdcServerDirService = null;

            if ( methodLdapServer != null )
            {
                // setting the directoryService is required to inject the correct level DS instance in the class or suite level LdapServer
                methodLdapServer.setDirectoryService( directoryService );

                setLdapServer.invoke( getTestClass().getJavaClass(), methodLdapServer );
            }
            else if ( classLdapServer != null )
            {
                oldLdapServerDirService = classLdapServer.getDirectoryService();

                // setting the directoryService is required to inject the correct level DS instance in the class or suite level LdapServer
                classLdapServer.setDirectoryService( directoryService );

                setLdapServer.invoke( getTestClass().getJavaClass(), classLdapServer );
            }

            if ( methodKdcServer != null )
            {
                // setting the directoryService is required to inject the correct level DS instance in the class or suite level KdcServer
                methodKdcServer.setDirectoryService( directoryService );

                setKdcServer.invoke( getTestClass().getJavaClass(), methodKdcServer );
            }
            else if ( classKdcServer != null )
            {
                oldKdcServerDirService = classKdcServer.getDirectoryService();

                // setting the directoryService is required to inject the correct level DS instance in the class or suite level KdcServer
                classKdcServer.setDirectoryService( directoryService );

                setKdcServer.invoke( getTestClass().getJavaClass(), classKdcServer );
            }

            // Run the test
            super.runChild( method, notifier );

            if ( methodLdapServer != null )
            {
                methodLdapServer.stop();
            }

            if ( oldLdapServerDirService != null )
            {
                classLdapServer.setDirectoryService( oldLdapServerDirService );
            }

            if ( oldKdcServerDirService != null )
            {
                classKdcServer.setDirectoryService( oldKdcServerDirService );
            }

            // Cleanup the methodDS if it has been created
            if ( methodDS != null )
            {
                LOG.debug( "Shuting down DS for {}", methodDS.getInstanceId() );
                methodDS.shutdown();
                FileUtils.deleteDirectory( methodDS.getInstanceLayout().getInstanceDirectory() );
            }
            else
            {
                // We use a class or suite DS, just revert the current test's modifications
                revert( directoryService, revision );
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_182, method.getName() ) );
            LOG.error( "", e );
            notifier.fireTestFailure( new Failure( getDescription(), e ) );
        }
    }


    private long getCurrentRevision( DirectoryService dirService ) throws Exception
    {
        if ( ( dirService != null ) && ( dirService.getChangeLog().isEnabled() ) )
        {
            long revision = dirService.getChangeLog().getCurrentRevision();
            LOG.debug( "Create revision {}", revision );

            return revision;
        }

        return 0;
    }


    private void revert( DirectoryService dirService, long revision ) throws Exception
    {
        if ( dirService == null )
        {
            return;
        }

        ChangeLog cl = dirService.getChangeLog();
        if ( cl.isEnabled() && ( revision < cl.getCurrentRevision() ) )
        {
            LOG.debug( "Revert revision {}", revision );
            dirService.revert( revision );
        }
    }
}
