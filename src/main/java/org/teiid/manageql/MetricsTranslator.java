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

import java.io.IOException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.teiid.language.QueryExpression;
import org.teiid.metadata.Column;
import org.teiid.metadata.Column.SearchType;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;

public class MetricsTranslator extends ExecutionFactory<MetricsConnectionFactory, MetricsConnection> {

	protected static final String BEANCOLUMNNAME = "beanname";
	protected static final String DOMAIN_NAME = "DOMAIN_NAME";
	protected static final String BEAN_NAMES = "BEAN_NAMES";

	public MetricsTranslator() {
		setTransactionSupport(TransactionSupport.NONE);
	}
	
	@Override
	public boolean isSourceRequired() {
		return true;
	}

	@Override
	public boolean isSourceRequiredForMetadata() {
		return true;
	}

	@Override
	public boolean isSourceRequiredForCapabilities() {
		return false;
	}

	@Override
	public boolean isThreadBound() {
		return true;
	}
	
	@Override
	public ResultSetExecution createResultSetExecution(QueryExpression command, ExecutionContext executionContext,
			RuntimeMetadata metadata, MetricsConnection connection) throws TranslatorException {
		return new MetricsResultSetExecution(command, executionContext, metadata, connection);
	}

	@Override
	public void getMetadata(MetadataFactory mf, MetricsConnection conn) throws TranslatorException {
		MBeanServerConnection mbsc = conn.mbsc;
		
		try {
			Set<ObjectName> beans = new TreeSet<ObjectName>(mbsc.queryNames(null, null));
			for (ObjectName bean : beans) {
				String beanName = bean.getCanonicalName();
				int idx = beanName.indexOf(':');
				String domainName = beanName.substring(0, idx);
				StringTokenizer st = new StringTokenizer(beanName.substring(idx+1), ",");
				Table t = null;
				String tblName = null;
				while(st.hasMoreTokens()) {
					String str = st.nextToken();
					String[] parts = str.split("=");
					if (parts[0].equals("type")) {
						if (mf.getSchema().getTable(parts[1]) == null) {
							t = mf.addTable(parts[1]);
							t.setSupportsUpdate(false);
							t.setProperty(DOMAIN_NAME, domainName);
							
							// implicit column 
							mf.addColumn(BEANCOLUMNNAME, "string", t);
							
						    MBeanInfo info = mbsc.getMBeanInfo(bean);
						    MBeanAttributeInfo[] attrInfo = info.getAttributes();
						    for (MBeanAttributeInfo attr : attrInfo){
						    	// TODO: correct the types with attr.getType()
						        Column c = mf.addColumn(attr.getName(), "string", t);
						        c.setSearchType(SearchType.Unsearchable);;
						    }												
						} else {
							t = mf.getSchema().getTable(parts[1]);
						}
					} else if (parts[0].equals("name")) {
						tblName = parts[1];
					}
				}
				
				if (tblName != null) {
					String names = t.getProperty(BEAN_NAMES, false);
					if (names == null) {
						t.setProperty(BEAN_NAMES, tblName);
					} else {
						t.setProperty(BEAN_NAMES, names+","+tblName);
					}
				}
			}
		} catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
			throw new TranslatorException(e);
		}
	}

}
