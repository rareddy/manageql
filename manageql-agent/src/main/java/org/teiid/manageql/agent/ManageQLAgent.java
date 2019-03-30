/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.manageql.agent;


import java.lang.instrument.Instrumentation;
import java.util.function.Consumer;

import org.teiid.manageql.server.ManageQLServer;

import main.ManageQLConfig;


public class ManageQLAgent {

    public static final String AGENT_KEY = "manageql.agent";
    private static ManageQLServer server;

    /**
     * Called when started via command line agent.
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        ManageQLConfig config = new ManageQLConfig(agentArgs);
        start(config);
    }

    /**
     * Called when dynamic attach is used.
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        try {
            ManageQLConfig config = new ManageQLConfig(agentArgs);
            if ("stop".equals(config.getProperty("mode"))) {
                stop();
            } else {
                start(config);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    synchronized private static void start(final ManageQLConfig config) {
        if( server == null ) {
            server = new ManageQLServer();
            configureInteger(config, "psql-port", server::setPsqlPortNumber);
            server.start();
            String driverURL = "jdbc:postgresql://localhost:" + server.getPort(0) + "/manageql";
            System.setProperty(AGENT_KEY, driverURL);
            System.err.println("ManageQL agent started. You can connect to: "+driverURL);
        }
    }

    private static void configure(ManageQLConfig config, String key, Consumer<String> target) {
        String value = config.getProperty(key);
        if( value !=null ) {
            target.accept(value);
        }
    }

    private static void configureInteger(ManageQLConfig config, String key, Consumer<Integer> target) {
        configure(config, key, x->target.accept(Integer.parseInt(x)));
    }

    synchronized private static void stop() {
        if( server !=null ) {
            server.stop();
            server = null;
            System.clearProperty(AGENT_KEY);
            System.err.println("ManageQL agent stopped");
        }
    }

}
