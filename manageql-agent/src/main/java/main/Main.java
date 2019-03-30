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
package main;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.teiid.manageql.server.ManageQLServer;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        if( args.length < 1 ) {
            System.err.println("Error: not enough arguments.");
            printUsage();
            System.exit(1);
        }

        String jxmURL = args[0];

        JMXServiceURL url = null;
        try {
            url = new JMXServiceURL(jxmURL);
        } catch (MalformedURLException e) {
            System.err.println("Error: invalid JMX url: "+jxmURL);
            printUsage();
            System.exit(1);
        }

        MBeanServerConnection connection = null;
        try {
            JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
            connection = jmxc.getMBeanServerConnection();
        } catch (IOException e) {
            System.err.println("Could not connect to JMX: "+e);
            System.exit(1);
        }

        ManageQLServer server = new ManageQLServer();
        server.setMBeanServerConnection(connection);
        server.start();
        // Lets just wait forever..
        String mutex = "mutex";
        synchronized (mutex) {
            mutex.wait();
        }
    }

    private static void printUsage() {
        System.err.println();
        System.err.println("  usage: java -jar manageql.jar <jmxurl>");
        System.err.println();
        System.err.println("  example: java -jar manageql.jar service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi");
        System.err.println();
    }
}
