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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.teiid.manageql.agent.ManageQLAgent;
import org.teiid.manageql.server.ManageQLServer;

import net.bytebuddy.agent.ByteBuddyAgent;

public class Main {

    public static void main(String[] argsArray) throws Exception {

        // Lets process the CLI args.
        LinkedList<String> args = new LinkedList<>(Arrays.asList(argsArray));

        Properties config = new Properties();
        LinkedList<String> commandArgs = new LinkedList<>();
        while (!args.isEmpty()) {
            String arg = args.removeFirst();
            if (arg.startsWith("--")) {
                arg = arg.replaceFirst("--", "");
                ManageQLConfig.parseOption(config, arg, false);
            } else {
                commandArgs.add(arg);
            }
        }

        if (commandArgs.isEmpty()) {
            exitWithInvalidUsage("no command selected.");
        }
        String command = commandArgs.removeFirst();

        ManageQLConfig mqlc = new ManageQLConfig(config);
        if ("gateway".equals(command)) {
            gateway(mqlc, commandArgs);
        } else if ("start".equals(command)) {
            start(mqlc, commandArgs);
        } else if ("stop".equals(command)) {
            stop(mqlc, commandArgs);
        } else {
            exitWithInvalidUsage("invalid command: " + command);
        }
    }


    private static void gateway(ManageQLConfig mqlc, LinkedList<String> args) throws InterruptedException {
        if( args.isEmpty() ) {
            exitWithInvalidUsage("the jmx host:port url argument is missing.");
        }
        String hostAndPort = args.removeFirst();

        JMXServiceURL url = null;
        try {
            url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+hostAndPort+"/jmxrmi");
        } catch (MalformedURLException e) {
            exitWithInvalidUsage("invalid jmx host:port argument: " + hostAndPort);
        }

        MBeanServerConnection connection = null;
        try {
            JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
            connection = jmxc.getMBeanServerConnection();
        } catch (IOException e) {
            System.err.println("could not connect to JMX: " + e);
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

    private static void start(ManageQLConfig mqlc, LinkedList<String> args) throws URISyntaxException {
        if( args.isEmpty() ) {
            exitWithInvalidUsage("the pid argument is missing.");
        }
        String pid = args.removeFirst();

        URL location = ManageQLAgent.class.getProtectionDomain().getCodeSource().getLocation();
        File agentJar = new File(location.toURI().getPath());

        ByteBuddyAgent.attach(agentJar, pid, mqlc.toString());
    }

    private static void stop(ManageQLConfig mqlc, LinkedList<String> args) throws URISyntaxException {
        if( args.isEmpty() ) {
            exitWithInvalidUsage("the pid argument is missing.");
        }
        String pid = args.removeFirst();


        URL location = ManageQLAgent.class.getProtectionDomain().getCodeSource().getLocation();
        File agentJar = new File(location.toURI().getPath());

        mqlc.setProperty("mode", "stop");
        ByteBuddyAgent.attach(agentJar, pid, mqlc.toString());
    }


    private static void exitWithInvalidUsage(String usageError) {
        System.err.println("error: "+usageError);
        System.err.println();
        printUsage();
        System.exit(1);
    }

    private static void printUsage() {
        System.err.println("usage: java -jar manageql.jar [<options>] <command> [<args> | <options>]");
        System.err.println();
        System.err.println("commands:");
        System.err.println();
        System.err.println("   gateway <jmx host:port>   Runs as a gateway server the connects to a remote JMX server");
        System.err.println("   start <pid>               Starts the server as an agent on a running JVM");
        System.err.println("   stop <pid>                Stops the server running as an agent on a running JVM");
        System.err.println();
        System.err.println("options:");
        System.err.println("   --psql-port=<port>    Configures the psql port that listens for connections");
        System.err.println();
        System.err.println("examples: ");
        System.err.println();
        System.err.println("   java -jar manageql.jar gateway localhost:1099");
        System.err.println("   java -jar manageql.jar start 1521 --psql-port=7777");
        System.err.println("   java -jar manageql.jar stop 1521");
        System.err.println();
    }
}
