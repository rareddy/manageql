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
import java.lang.reflect.Field;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.teiid.language.QueryExpression;
import org.teiid.metadata.Column;
import org.teiid.metadata.Column.SearchType;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Table;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;

public class JmxTranslator extends ExecutionFactory<JmxConnectionFactory, JmxConnection> {

    protected static final String OBJECT_NAME_COLUMN = "$ObjectName";

    public JmxTranslator() {
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
                                                       RuntimeMetadata metadata, JmxConnection connection) throws TranslatorException {
        return new JmxResultSetExecution(command, executionContext, metadata, connection);
    }


    @Override
    public void getMetadata(MetadataFactory mf, JmxConnection conn) throws TranslatorException {

        installDirtyHackForDynamicTableCreation(mf, conn);

        MBeanServerConnection mbsc = conn.mbsc;
        try {
            for (ObjectName objectName : new TreeSet<ObjectName>(mbsc.queryNames(null, null))) {
                MBeanInfo info = mbsc.getMBeanInfo(objectName);
                addOrUpdateTable(mf, info, objectName.toString());
            }
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
            throw new TranslatorException(e);
        }


    }

    private void installDirtyHackForDynamicTableCreation(MetadataFactory mf, JmxConnection conn) throws TranslatorException {

        MBeanServerConnection mbsc = conn.mbsc;
        Schema schema = mf.getSchema();
        TreeMap<String, Table> tables = new TreeMap<String, Table>(String.CASE_INSENSITIVE_ORDER) {
            @Override
            public Table get(Object key) {
                String tableName = (String) key;
                Table table = super.get(tableName);
                if (table == null) {
                    try {

                        // Look for mbeans matching the table name...
                        ObjectName name = new ObjectName(tableName);
                        for (ObjectInstance oi : mbsc.queryMBeans(name, null)) {
                            try {
                                MBeanInfo info = mbsc.getMBeanInfo(oi.getObjectName());
                                table = addOrUpdateTable(mf, info, tableName);
                            } catch (IOException | InstanceNotFoundException | IntrospectionException | ReflectionException e) {
                            }
                        }

                    } catch (MalformedObjectNameException | IOException e) {
                    }
                }
                return table;
            }
        };

        tables.putAll(schema.getTables());
        try {
            Field field = Schema.class.getDeclaredField("tables");
            field.setAccessible(true);
            field.set(schema, tables);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new TranslatorException(e);
        }
    }


    private Table addOrUpdateTable(MetadataFactory mf, MBeanInfo info, String tableName) {
        MBeanAttributeInfo[] attrInfo = info.getAttributes();

        // We do a containsKey due to the installDirtyHackForDynamicTableCreation method..
        // consult it first before modifying.
        if (!mf.getSchema().getTables().containsKey(tableName)) {
            Table table = mf.addTable(tableName);
            table.setSupportsUpdate(false);
        }


        Table table = mf.getSchema().getTable(tableName);
        addOrUpdateCol(mf, table, OBJECT_NAME_COLUMN, "string");
        for (MBeanAttributeInfo attr : attrInfo) {
            // TODO: correct the types with attr.getType()
            addOrUpdateCol(mf, table, attr.getName(), "string");
        }
        return table;
    }

    private void addOrUpdateCol(MetadataFactory mf, Table t, String colName, String sqlType) {
        if (t.getColumnByName(colName) == null) {
            Column c = mf.addColumn(colName, sqlType, t);
            c.setSearchType(SearchType.Unsearchable);
        }
    }

}
