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
package org.teiid.manageql.server;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;

import javax.management.MBeanServerConnection;

import org.teiid.adminapi.Model.Type;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.SourceMappingMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.manageql.server.jmx.JmxConnectionFactory;
import org.teiid.manageql.server.jmx.JmxTranslator;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.TranslatorException;
import org.teiid.transport.SocketConfiguration;
import org.teiid.transport.WireProtocol;

public class ManageQLServer extends EmbeddedServer {

    private MBeanServerConnection serverConnection;
    EmbeddedConfiguration ec = new EmbeddedConfiguration();
    SocketConfiguration sc = new SocketConfiguration();

    public ManageQLServer() {
        System.setProperty("org.teiid.addPGMetadata", "false");

        // configuration for Teiid
        ec.setMaxActivePlans(2);
        ec.setMaxAsyncThreads(1);
        ec.setMaxThreads(2);
        ec.setUseDisk(true);
        ec.setMaxReserveKb(0);

        // start a socket for pg protocol
        sc.setHostAddress(InetAddress.getLoopbackAddress());
        sc.setPortNumber(5432);
        sc.setProtocol(WireProtocol.pg);
        ec.addTransport(sc);
    }

    public void start() {

        // add a metadata repo to sniff out the application's metrics providing beans

        // vdb to use the above metrics metadata as virtual database
        VDBMetaData vdb =  new VDBMetaData();
        vdb.setName("manageql");
        vdb.addModel(createJMXModel());

        try {
			start(ec);
			deployVDB(vdb, null);
		} catch (VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
			throw new IllegalStateException("failed to start managemeql server", e);
		}
	}

    private ModelMetaData createJMXModel() {
        ModelMetaData model = new ModelMetaData(){

        };
        model.setModelType(Type.PHYSICAL);
        model.setName("jmx");

        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName("jmx");
        source.setConnectionJndiName("jmx");
        source.setTranslatorName("jmx");
        model.addSourceMapping(source);

        this.addTranslator("jmx", new JmxTranslator());
        MBeanServerConnection jmxConnection = getMBeanServerConnection();
        if( jmxConnection == null ) {
            jmxConnection = ManagementFactory.getPlatformMBeanServer();
        }
        this.addConnectionFactory("jmx", new JmxConnectionFactory(jmxConnection));

        return model;
    }

    public void setMBeanServerConnection(MBeanServerConnection mBeanServer) {
        this.serverConnection = mBeanServer;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return serverConnection;
    }

    public void setPsqlPortNumber(int port) {
        this.sc.setPortNumber(port);
    }

    public int getPsqlPortNumber() {
        return this.sc.getPortNumber();
    }
}
