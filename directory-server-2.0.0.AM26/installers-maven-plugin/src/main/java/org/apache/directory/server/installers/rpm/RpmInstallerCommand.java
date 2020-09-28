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
package org.apache.directory.server.installers.rpm;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.LinuxInstallerCommand;
import org.apache.directory.server.installers.MojoHelperUtils;
import org.apache.directory.server.installers.Target;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;


/**
 * The IzPack installer command.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RpmInstallerCommand extends LinuxInstallerCommand<RpmTarget>
{
    /** The rpm extension */
    private static final String DOT_RPM = ".rpm";

    /** The RPM specification file */
    private static final String APACHEDS_SPEC_FILE = "apacheds.spec";

    /** The names used inside the rpm file */
    private static final String RPMS = "RPMS";
    private static final String SOURCES = "SOURCES";
    private static final String SPECS = "SPECS";
    private static final String BUILD = "BUILD";
    private static final String SRPMS = "SRPMS";


    /**
     * Creates a new instance of RpmInstallerCommand.
     *
     * @param mojo the Server Installers Mojo
     * @param target the RPM target
     */
    public RpmInstallerCommand( GenerateMojo mojo, RpmTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for linux or current machine is not linux (no rpm builder)</li>
     *   <li>Filter and copy project supplied spec file into place if it has been specified and exists</li>
     *   <li>If no spec file exists filter and deposite into place bundled spec template</li>
     *   <li>Bail if we cannot find the rpm builder executable</li>
     *   <li>Execute rpm build on the filtered spec file</li>
     * </ol> 
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target
        if ( !verifyTarget() )
        {
            return;
        }

        log.info( "  Creating Rpm installer..." );

        // Creating the target directory
        if ( !getTargetDirectory().mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, getTargetDirectory() ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying Rpm installer files" );

        try
        {
            // Create Rpm directories (BUILD, RPMS, SOURCES, SPECS & SRPMS)
            File rpmBuild = new File( getTargetDirectory(), BUILD );

            if ( !rpmBuild.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, rpmBuild ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            File rpmRpms = new File( getTargetDirectory(), RPMS );

            if ( !rpmRpms.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, rpmRpms ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            File rpmSources = new File( getTargetDirectory(), SOURCES );

            if ( !rpmSources.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, rpmSources ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            File rpmSpecs = new File( getTargetDirectory(), SPECS );

            if ( !rpmSpecs.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, rpmSpecs ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            File rpmSrpms = new File( getTargetDirectory(), SRPMS );

            if ( !rpmSrpms.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, rpmSrpms ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            // Creating the installation and instance layouts
            createLayouts();

            // Copying the init script for /etc/init.d/
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + ETC_INITD_SCRIPT,
                getClass().getResourceAsStream( INSTALLERS_PATH + ETC_INITD_SCRIPT ),
                new File( getAdsSourcesDirectory(), ETC_INITD_SCRIPT ), true );

            // Creating the spec file
            createSpecFile();

            // Generating tar.gz file
            MojoHelperUtils.exec( new String[]
                {
                    TAR,
                    "-zcf",
                    APACHEDS_DASH + getVersion() + ".tar.gz",
                    APACHEDS_DASH + getVersion()
            },
                new File( getTargetDirectory(), File.separator + SOURCES ),
                false );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy Rpm installer files." );
        }

        // Generating the Rpm
        log.info( "    Generating Rpm installer" );

        MojoHelperUtils.exec( new String[]
            {
                mojo.getRpmbuildUtility().getAbsolutePath(),
                "--quiet",
                "-ba",
                "--target",
                target.getOsArch() + "-linux",
                "--define",
                "_topdir " + getTargetDirectory(),
                "--define",
                "_tmppath /tmp",
                SPECS + "/" + APACHEDS_SPEC_FILE
        },
            getTargetDirectory(),
            false );

        // Copying the rpm at the final destination
        try
        {
            String rpmName = APACHEDS_DASH + getVersion() + "-1." + target.getOsArch() + DOT_RPM;
            String finalName = target.getFinalName();

            if ( !finalName.endsWith( DOT_RPM ) )
            {
                finalName = finalName + DOT_RPM;
            }

            File finalFile = new File( mojo.getOutputDirectory(), finalName );

            FileUtils.copyFile( new File( getTargetDirectory(), RPMS + File.separator + target.getOsArch() + File.separator + rpmName ),
                finalFile );

            log.info( "=> RPM generated at " + finalFile );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy generated Rpm installer file." );
        }
    }


    /**
     * Verifies the target.
     *
     * @return
     *      <code>true</code> if the target is correct, 
     *      <code>false</code> if not.
     */
    private boolean verifyTarget()
    {
        // Verifying the target is Linux
        if ( !target.isOsNameLinux() )
        {
            log.warn( "Rpm installer can only be targeted for Linux platforms!" );
            log.warn( "The build will continue, but please check the the platform of this installer target" );
            return false;
        }

        // Verifying the currently used OS to build the installer is Linux or Mac OS X
        String osName = System.getProperty( OS_NAME );

        if ( !( Target.OS_NAME_LINUX.equalsIgnoreCase( osName ) || Target.OS_NAME_MAC_OS_X.equalsIgnoreCase( osName ) ) )
        {
            log.warn( "Rpm package installer can only be built on a machine running Linux or Mac OS X!" );
            log.warn( "The build will continue, generation of this target is skipped." );
            return false;
        }

        // Verifying the rpmbuild utility exists
        if ( !mojo.getRpmbuildUtility().exists() )
        {
            log.warn( "Cannot find rpmbuild utility at this location: " + mojo.getRpmbuildUtility() );
            log.warn( "The build will continue, but please check the location of your rpmbuild utility." );
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void initializeFilterProperties()
    {
        super.initializeFilterProperties();

        filterProperties.put( INSTALLATION_DIRECTORY_PROP, OPT_APACHEDS_DIR + getVersion() );
        filterProperties.put( INSTANCES_DIRECTORY_PROP, VAR_LIB_APACHEDS_DIR + getVersion() );
        filterProperties.put( DEFAULT_INSTANCE_NAME_PROP, DEFAULT );
        filterProperties.put( USER_PROP, APACHEDS );
        filterProperties.put( GROUP_PROP, APACHEDS );
        filterProperties.put( WRAPPER_JAVA_COMMAND_PROP, WRAPPER_JAVA_COMMAND );
        filterProperties.put( DOUBLE_QUOTE_PROP, "" );
        filterProperties.put( VERSION_PROP, getVersion() );
    }


    /**
     * Creates the spec file.
     * 
     * @throws IOException 
     */
    private void createSpecFile() throws IOException
    {
        // Creating two strings for libraries
        StringBuilder installLibs = new StringBuilder();
        StringBuilder filesLibs = new StringBuilder();

        // Getting the lib directory
        File libDirectory = getInstallationLayout().getLibDirectory();

        if ( libDirectory.exists() )
        {
            // Iterating on each file in the lib directory
            for ( File file : libDirectory.listFiles() )
            {
                if ( file.isFile() )
                {
                    installLibs.append( "install -m 644 " + getBuidDirectory() + "/%{name}-%{version}/server/lib/"
                        + file.getName() + " $RPM_BUILD_ROOT%{adshome}/lib/" + file.getName() + "\n" );
                    filesLibs.append( "%{adshome}/lib/" + file.getName() + "\n" );
                }
            }
        }

        // Creating properties based on these values
        Properties properties = new Properties();
        properties.put( VERSION_PROP, getVersion() );
        properties.put( "build.dir", getBuidDirectory() );
        properties.put( "install.libs", installLibs.toString() );
        properties.put( "files.libs", filesLibs.toString() );

        // Copying and filtering the spec file
        MojoHelperUtils.copyAsciiFile( mojo, properties, APACHEDS_SPEC_FILE,
            getClass().getResourceAsStream( APACHEDS_SPEC_FILE ),
            new File( getTargetDirectory(), SPECS + File.separator + APACHEDS_SPEC_FILE ), true );
    }


    /**
     * Gets the 'apacheds-${version}' directory inside 'SOURCES'.
     *
     * @return the 'apacheds-${version}' directory inside 'SOURCES'
     */
    private File getAdsSourcesDirectory()
    {
        return new File( getTargetDirectory(), SOURCES + File.separator + APACHEDS_DASH + getVersion() );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return new File( getAdsSourcesDirectory(), "server" );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getAdsSourcesDirectory(), INSTANCE_DEFAULT_DIR );
    }


    /**
     * Gets the version number.
     *
     * @return the version number
     */
    private String getVersion()
    {
        return mojo.getProject().getVersion().replace( '-', '_' );
    }


    /**
     * Gets the BUILD directory path.
     *
     * @return the BUILD directory path
     */
    private String getBuidDirectory()
    {
        return getTargetDirectory().getAbsolutePath() + "/" + BUILD;
    }
}
