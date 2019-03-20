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
package org.teiid.manageql;

import org.teiid.adminapi.Model.Type;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.SourceMappingMetadata;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.runtime.EmbeddedConfiguration;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.TranslatorException;
import org.teiid.transport.SocketConfiguration;
import org.teiid.transport.WireProtocol;

public class ManageQLServer extends EmbeddedServer {

	public void start() {
		System.setProperty("org.teiid.addPGMetadata", "false");
		
		// configuration for Teiid
		EmbeddedConfiguration ec = new EmbeddedConfiguration();
		ec.setMaxActivePlans(2);
		ec.setMaxAsyncThreads(1);
		ec.setMaxThreads(2);
		ec.setUseDisk(true);
		ec.setMaxReserveKb(0);
	
		// start a socket for pg protocol
        SocketConfiguration sc = new SocketConfiguration();
        sc.setPortNumber(35432);
        sc.setProtocol(WireProtocol.pg);
        ec.addTransport(sc);

        // add a metadata repo to sniff out the application's metrics providing beans
        this.addTranslator("metrics", new MetricsTranslator());
        this.addConnectionFactory("metricsSource", new MetricsConnectionFactory());
        
        // vdb to use the above metrics metadata as virtual database
        VDBMetaData vdb =  new VDBMetaData();
        vdb.setName("management");
        ModelMetaData model = new ModelMetaData();
        model.setModelType(Type.PHYSICAL);
        model.setName("model");
        
        SourceMappingMetadata source = new SourceMappingMetadata();
        source.setName("metricsSource");
        source.setTranslatorName("metrics");
        source.setConnectionJndiName("metricsSource");
        model.addSourceMapping(source);
        
        vdb.addModel(model);
        
        try {
			start(ec);
			deployVDB(vdb, null);
		} catch (VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
			throw new IllegalStateException("failed to start the teiid for management", e);
		}        
	}
}
