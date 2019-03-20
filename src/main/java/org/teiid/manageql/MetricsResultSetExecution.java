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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.teiid.language.NamedTable;
import org.teiid.language.QueryExpression;
import org.teiid.language.Select;
import org.teiid.metadata.Column;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;

@SuppressWarnings("unused")
public class MetricsResultSetExecution implements ResultSetExecution {

	private Select command;
	private ExecutionContext executionContext;
	private RuntimeMetadata metadata;
	private MetricsConnection connection;
	private ListIterator<List<Attribute>> resultRows;
	MetricsSelectVistor visitor;

	public MetricsResultSetExecution(QueryExpression command, ExecutionContext executionContext,
			RuntimeMetadata metadata, MetricsConnection connection) {
		this.command = (Select) command;
		this.executionContext = executionContext;
		this.metadata = metadata;
		this.connection = connection;
		visitor = new MetricsSelectVistor();
		visitor.visitNode(this.command);
	}

	@Override
	public void close() {
	}

	@Override
	public void cancel() throws TranslatorException {
	}

	@Override
	public void execute() throws TranslatorException {
		
		NamedTable tblRef = (NamedTable) command.getFrom().get(0);
		Table t = tblRef.getMetadataObject();
		List<List<Attribute>> result = new ArrayList<List<Attribute>>();
		try {
			String name = t.getProperty(MetricsTranslator.DOMAIN_NAME, false) + ":type=" + t.getName();
			String beanNames = t.getProperty(MetricsTranslator.BEAN_NAMES, false);
			if (beanNames == null) {
				List<Attribute> la = this.connection.mbsc.getAttributes(new ObjectName(name), visitor.getColumnNames()).asList();
				la.add(new Attribute(MetricsTranslator.BEANCOLUMNNAME, t.getName()));
				result.add(la);
			} else {
				StringTokenizer st = new StringTokenizer(beanNames, ",");
				while (st.hasMoreElements()) {
					String beanName = st.nextToken();
					List<Attribute> la = this.connection.mbsc.getAttributes(new ObjectName(name + ",name=" + beanName),
							visitor.getColumnNames()).asList();
					la.add(new Attribute(MetricsTranslator.BEANCOLUMNNAME, beanName));
					result.add(la);
				}
			}
		} catch (InstanceNotFoundException | MalformedObjectNameException | ReflectionException | IOException e) {
			throw new TranslatorException(e);
		}
		this.resultRows = result.listIterator();
	}

	@Override
	public List<?> next() throws TranslatorException, DataNotAvailableException {
		if (!this.resultRows.hasNext()) {
			return null;
		}
		List<String> row = new ArrayList<String>();
		List<Attribute> attributes = this.resultRows.next();
		
		for (String colName : this.visitor.getColumnNames()) {
			for (Attribute a : attributes) {
				if (a.getName().equalsIgnoreCase(colName)) {
					row.add(a.getValue().toString());
				}
			}
		}
		return row;
	}
}
