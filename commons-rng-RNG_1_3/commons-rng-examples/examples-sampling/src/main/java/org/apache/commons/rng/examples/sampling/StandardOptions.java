/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.rng.examples.sampling;

import picocli.CommandLine.Command;

/**
 * Standard options for all commands. This sets the formatting for usage messages and
 * adds common parameters (help, version).
 */
@Command(sortOptions = true,
         mixinStandardHelpOptions = true,
         versionProvider      = ManifestVersionProvider.class,
         synopsisHeading      = "%n",
         descriptionHeading   = "%n",
         parameterListHeading = "%nParameters:%n",
         optionListHeading    = "%nOptions:%n",
         commandListHeading   = "%nCommands:%n")
class StandardOptions {
    // Empty class used for annotations
}
