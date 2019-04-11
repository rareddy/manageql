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
import java.util.TreeSet;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.teiid.language.Call;
import org.teiid.language.QueryExpression;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Column;
import org.teiid.metadata.Column.SearchType;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.metadata.Table;
import org.teiid.query.sql.visitor.SQLStringVisitor;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ExecutionFactory;
import org.teiid.translator.ProcedureExecution;
import org.teiid.translator.ResultSetExecution;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.TypeFacility;

public class JmxTranslator extends ExecutionFactory<JmxConnectionFactory, JmxConnection> {

    protected static final String GET_DYNAMIC_TABLE_DDL = "get_dynamic_table_ddl";
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
    public ProcedureExecution createProcedureExecution(Call command, ExecutionContext executionContext,
            RuntimeMetadata metadata, JmxConnection connection) throws TranslatorException {
        if (command.getProcedureName().equalsIgnoreCase(GET_DYNAMIC_TABLE_DDL)) {
            return new DynamicTableProcedureExecution(command, executionContext, metadata, connection);
        }
        return super.createProcedureExecution(command, executionContext, metadata, connection);
    }

    @Override
    public void getMetadata(MetadataFactory mf, JmxConnection conn) throws TranslatorException {
        defineMetadataForDynamicTable(mf);
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

    private void defineMetadataForDynamicTable(MetadataFactory mf) {
        Procedure p = mf.addProcedure(GET_DYNAMIC_TABLE_DDL);
        p.setAnnotation("Procedure to fetch the DDL for table that has patterns in its name");
        mf.addProcedureResultSetColumn("result", TypeFacility.RUNTIME_NAMES.STRING, p);

        ProcedureParameter param = mf.addProcedureParameter("objectName", TypeFacility.RUNTIME_NAMES.STRING,
                ProcedureParameter.Type.In, p);
        param.setAnnotation("Name of the table pattern");
        param.setNullType(NullType.No_Nulls);

        param = mf.addProcedureParameter("tableName", TypeFacility.RUNTIME_NAMES.STRING,
                ProcedureParameter.Type.In, p);
        param.setAnnotation("alias table name");
        param.setNullType(NullType.No_Nulls);
    }

    private Table addOrUpdateTable(MetadataFactory mf, MBeanInfo info, String tableName) {
        MBeanAttributeInfo[] attrInfo = info.getAttributes();

        // We do a containsKey due to the installDirtyHackForDynamicTableCreation method..
        // consult it first before modifying.
        if (!mf.getSchema().getTables().containsKey(SQLStringVisitor.escapeSinglePart(tableName))) {
            Table table = mf.addTable(tableName);
            table.setSupportsUpdate(false);
        }

        Table table = mf.getSchema().getTable(tableName);
        addOrUpdateCol(mf, table, OBJECT_NAME_COLUMN, "string");
        for (MBeanAttributeInfo attr : attrInfo) {
            // TODO: correct the types with attr.getType()
            addOrUpdateCol(mf, table, SQLStringVisitor.escapeSinglePart(attr.getName()), "string");
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
