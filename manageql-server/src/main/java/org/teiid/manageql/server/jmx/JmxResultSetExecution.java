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
package org.teiid.manageql.server.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.teiid.language.NamedTable;
import org.teiid.language.QueryExpression;
import org.teiid.language.Select;
import org.teiid.language.visitor.SQLStringVisitor;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;

@SuppressWarnings("unused")
public class JmxResultSetExecution implements ResultSetExecution {

    private Select command;
    private ExecutionContext executionContext;
    private RuntimeMetadata metadata;
    private JmxConnection connection;
    private ListIterator<ObjectName> resultRows;
    JmxSelectVistor visitor;

    public JmxResultSetExecution(QueryExpression command, ExecutionContext executionContext,
            RuntimeMetadata metadata, JmxConnection connection) {
        this.command = (Select) command;
        this.executionContext = executionContext;
        this.metadata = metadata;
        this.connection = connection;
        visitor = new JmxSelectVistor();
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
        String tableName = SQLStringVisitor.getRecordName(t);
        List<ObjectName> result = new ArrayList<ObjectName>();
        try {
            for (ObjectInstance oi : this.connection.mbsc.queryMBeans(new ObjectName(tableName), null)) {
                result.add(oi.getObjectName());
            }
        } catch (IOException | MalformedObjectNameException e) {
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
        ObjectName objectName = this.resultRows.next();

        List<Attribute> attributes = null;
        try {
            attributes = this.connection.mbsc.getAttributes(objectName, visitor.getColumnNames()).asList();
        } catch (IOException | InstanceNotFoundException | ReflectionException e) {
            throw new TranslatorException(e);
        }
        attributes.add(new Attribute(JmxTranslator.OBJECT_NAME_COLUMN, objectName));

        for (String colName : this.visitor.getColumnNames()) {
            Object value = null;
            for (Attribute a : attributes) {
                if (a.getName().equalsIgnoreCase(colName)) {
                    value = a.getValue();
                }
            }
            if( value == null ) {
                row.add(null);
            } else {
                row.add(value.toString());
            }
        }

        return row;
    }
}
