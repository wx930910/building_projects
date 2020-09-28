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
package org.apache.commons.jelly.tags.jface;

import java.net.URL;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.tags.jface.JFaceTagLibrary;

/**
 * @author <a href="mailto:ckl@dacelo.nl">Christiaan ten Klooster</a>
 */
public class JFaceDemo {

    public static void main(String[] args) {
        try {

            JellyContext context = new JellyContext();

            /** @todo zap the following line once the Jelly core has this default */
            context.registerTagLibrary("jelly:jface", new JFaceTagLibrary());

            URL url = JFaceDemo.class.getResource("JFaceDemo.jelly");

            XMLOutput output = XMLOutput.createXMLOutput(System.out, true);
            context.runScript( url, output );
            output.flush();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
