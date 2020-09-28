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
package org.apache.bsf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.imageio.spi.ServiceRegistry;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * This is the main driver for BSF to be run on the command line
 * to eval scripts directly. Also provides information on the
 * engines, factories and providers.
 *
 * @author   Sanjiva Weerawarana
 * @author   Matthew J. Duftler
 * @author   Sam Ruby
 */
public class Main {
    private static final String SHOW_ENGINE = "engine";
    private static final String SHOW_FACTORIES = "factories";
    private static final String SHOW_PROVIDERS = "providers";
    private static final String ARG_IN = "-in";
    private static final String ARG_LANG = "-lang";
    private static final String ARG_EXT = "-ext";
    private static final String ARG_SHOW = "-show";
    private static final String DEFAULT_IN_FILE_NAME = "<STDIN>";

    /**
     * Static driver to be able to run BSF scripts from the command line.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) throws IOException {
        if ((args.length == 0) || (args.length % 2 != 0)) {
            printHelp();
            System.exit(1);
        }

        Hashtable argsTable = new Hashtable();

        for (int i = 0; i < args.length; i += 2) {
            argsTable.put(args[i], args[i + 1]);
        }

        String inFileName = (String) argsTable.get(ARG_IN);
        String extension = (String) argsTable.get(ARG_EXT);
        String language = (String) argsTable.get(ARG_LANG);
        String show = (String) argsTable.get(ARG_SHOW);

        if (SHOW_PROVIDERS.equalsIgnoreCase(show)) {
            processProviders(false);         
            return;
        }
        ScriptEngineManager mgr;
        try {
            mgr = new ScriptEngineManager();
        } catch (Error e1) {
            System.out.println("Could not create ScriptEngineManager: "+e1);
            System.out.println("Checking which factories cannot be instantiated ...");
            processProviders(true);
            return;
        }
        final List engineFactories = mgr.getEngineFactories();
        if (engineFactories.isEmpty()){
            throw new RuntimeException("Could not find any engine factories");
        }

        final int engineCount = engineFactories.size();
        if (SHOW_FACTORIES.equalsIgnoreCase(show)) {
            System.err.println("Found "+engineCount+ " engine factories");
            for (Iterator iter = engineFactories.iterator(); iter.hasNext();){
                ScriptEngineFactory fac = (ScriptEngineFactory) iter.next();
                showFactory(fac, false);
            }
            return;
        }

        if (language == null && extension == null && inFileName != null) {
            int i = inFileName.lastIndexOf('.');
            if (i > 0) {
                extension = inFileName.substring(i+1);
            }
        }
        if (extension == null && language == null) {
            throw new IllegalArgumentException("unable to determine language");
        }

        try {
            ScriptEngine engine;
            if (language != null) {
                engine = mgr.getEngineByName(language);
                if (engine == null){
                    throw new IllegalArgumentException("unable to find engine using Language: "+language);
                }
            } else {
                engine = mgr.getEngineByExtension(extension);
                if (engine == null){
                    throw new IllegalArgumentException("unable to find engine using Extension: "+extension);
                }
            }
            if (SHOW_ENGINE.equalsIgnoreCase(show)){
                ScriptEngineFactory fac = engine.getFactory();
                showFactory(fac, true);
                System.err.println("Engine="+engine.getClass().getName());
                if (engine instanceof Compilable){
                    System.err.println("Engine supports Compilable interface.");
                }
                if (engine instanceof Invocable){
                    System.err.println("Engine supports Invocable interface.");
                }
                System.err.println();
                return;
            }
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("args", args);

            Reader in;
            if (inFileName != null) {
                in = new FileReader(inFileName);
            } else {
                in = new InputStreamReader(System.in);
                inFileName = DEFAULT_IN_FILE_NAME;
            }
            Object obj = engine.eval(in);
            System.err.println("Result: " + obj);
            in.close();
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process provider files - check or print contents.
     * @param check if true, check which factories cannot be instantiate; otherwise print file contents
     * @throws IOException if the resource cannot be opened/read etc.
     */
    private static void processProviders(boolean check) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        HashSet set = new HashSet();
        if (check){
            System.out.println("Generating list of providers ...");
        }
        Enumeration en =cl.getResources("META-INF/services/javax.script.ScriptEngineFactory");
        while(en.hasMoreElements()){
            URL url = (URL) en.nextElement();
            if (!check){
                System.out.println(url);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
            String line;
            while((line = br.readLine()) != null){
                if (check){
                    // Store the factory name
                    String nocomment=line.split("#", 2)[0].trim();
                    if (nocomment.length() > 0){
                        set.add(nocomment);
                    }
                } else {
                    System.out.println(line);
                }
            }
            br.close();
        }
        if (check){
            System.out.println("Checking for provider errors ...");
            Iterator iterator = ServiceRegistry.lookupProviders(ScriptEngineFactory.class, cl);
            while(iterator.hasNext()){
                try {
                    Object provider = iterator.next();
                    set.remove(provider.getClass().getName());
                } catch (ThreadDeath td) { // must not ignore this
                    throw td;
                // See BSF-30 - iterator may throw Error
                } catch (Error error) {
                    System.out.println(error);
                }
            }
            System.out.println("Following factories could not be instantiated:");
            System.out.println(set.toString());
        }
    }

    private static void showFactory(ScriptEngineFactory fac, boolean full) {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append(fac.getClass().getName());
        sb.append("]\n");
        
        try {
            sb.append(fac.getEngineName());
            sb.append(" ");
            try {
                sb.append(fac.getEngineVersion());
            } catch (NoClassDefFoundError e) {
                sb.append("N/A");
            }
            sb.append(" for ");
            sb.append(fac.getLanguageName());
            sb.append(" ");
            try {
                sb.append(fac.getLanguageVersion());
            } catch (NoClassDefFoundError e) {
                sb.append("N/A");
            }
            sb.append("\n");

            Iterator iter=fac.getNames().iterator();
            char sep = '{';
            while(iter.hasNext()){
                sb.append(sep);
                sb.append(iter.next());
                sep=',';
            }
            if (sep == ','){
                sb.append("} ");
            }
            iter=fac.getExtensions().iterator();
            sep='[';
            while(iter.hasNext()){
                sb.append(sep);
                sb.append('.');
                sb.append(iter.next());
                sep=',';
            }
            if (sep == ','){
                sb.append("] ");
            }
            iter=fac.getMimeTypes().iterator();
            sep='(';
            while(iter.hasNext()){
                sb.append(sep);
                sb.append(iter.next());
                sep=',';
            }
            if (sep == ','){
                sb.append(")");
            }
            sb.append("\n");
            if (full){
                sb.append("OutputStatement: ");
                try {
                    sb.append(fac.getOutputStatement("String"));
                } catch (Exception e) {
                    sb.append(e.toString());
                }
                sb.append("\n");
                sb.append("Program: ");
                try {
                    sb.append(fac.getProgram(new String[]{"Line1","Line2"}));
                } catch (Exception e) {
                    sb.append(e.toString());
                }
                sb.append("\n");
                sb.append("Method call: ");
                try {
                    sb.append(fac.getMethodCallSyntax("object", "method", new String[]{"Arg1","Arg2"}));
                } catch (Exception e) {
                    sb.append(e.toString());
                }
                sb.append("\n");
            }
        } catch (Throwable t) {
            sb.append(t.toString());
        } finally {
            System.err.println(sb.toString());
        }
    }

    private static void printHelp() {
        System.err.println("Usage:");
        System.err.println();
        System.err.println("  java " + Main.class.getName() + " [opts] [args]");
        System.err.println();
        System.err.println("    opts:");
        System.err.println();
        System.err.println(
            "      [-lang     shortname]   (e.g. jexl, jython)  Overrides -ext.");
        System.err.println(
            "      [-in        fileName]   default: " + DEFAULT_IN_FILE_NAME);
        System.err.println(
            "      [-ext      extension]   default: "
                + "<If -in is specified and neither -ext nor -lang");
        System.err.println(
            "                                       "
                + " are, attempt to determine");
        System.err.println(
            "                                       "
                + " extension from file extension;");
        System.err.println(
            "                                       "
                + " otherwise, -ext or -lang is required.>");
        System.err.println(
            "      [-show          item]");
        System.err.println(
            "                 " + SHOW_FACTORIES + " - list all engine factories and exit");
        System.err.println(
            "                    " +SHOW_ENGINE + " - list details of selected factory and engine");
        System.err.println(
            "                 " +SHOW_PROVIDERS + " - list details of providers (services files)");
        System.err.println();
        System.err.println();
        System.err.println(
        "    The command-line arguments are stored in the array 'args'");
    }
}