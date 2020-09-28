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
package org.apache.directory.server.installers;


import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.introspection.ReflectionValueExtractor;


/**
 * Took this from the maven resource plugin in trunk or else I would have added it 
 * as a dep.  This can be removed once the 2.2 release of the resource plugin comes
 * out.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReflectionProperties extends Properties
{
    private static final long serialVersionUID = -4748409484925117841L;
    private MavenProject project;
    boolean escapedBackslashesInFilePath;


    public ReflectionProperties( MavenProject aProject, boolean escapedBackslashesInFilePath )
    {
        super();
        project = aProject;
        this.escapedBackslashesInFilePath = escapedBackslashesInFilePath;
    }


    @Override
    public Object get( Object key )
    {
        Object value = null;
        try
        {
            value = ReflectionValueExtractor.evaluate( "" + key, project );
            if ( escapedBackslashesInFilePath && value != null
                && ( value instanceof String ) )
            {
                String val = ( String ) value;

                // Check if it's a windows path
                if ( val.indexOf( ":\\", 1 ) > -1 )
                {
                    value = StringUtils.replace( ( String ) value, "\\", "\\\\" );
                    value = StringUtils.replace( ( String ) value, ":", "\\:" );
                }
            }
        }
        catch ( Exception e )
        {
            //TODO: remove the try-catch block when ReflectionValueExtractor.evaluate() throws no more exceptions
        }
        return value;
    }
}
