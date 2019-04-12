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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.teiid.core.util.StringUtil;
import org.teiid.language.Argument;
import org.teiid.language.Call;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.query.sql.visitor.SQLStringVisitor;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.ProcedureExecution;
import org.teiid.translator.TranslatorException;

public class DynamicTableProcedureExecution implements ProcedureExecution {

    @SuppressWarnings("unused")
    private RuntimeMetadata metadata;
    @SuppressWarnings("unused")
    private ExecutionContext context;
    private Call command;
    private JmxConnection conn;
    private StringBuilder resultDDL = new StringBuilder();
    private boolean served = false;

    public DynamicTableProcedureExecution(Call command, ExecutionContext executionContext, RuntimeMetadata metadata,
            JmxConnection connection) {
        this.command = command;
        this.context = executionContext;
        this.metadata = metadata;
        this.conn = connection;
    }

    @Override
    public void execute() throws TranslatorException {
        MBeanServerConnection mbsc = conn.mbsc;
        List<Argument> arguments = this.command.getArguments();

        try {
            String objectName = (String)arguments.get(0).getArgumentValue().getValue();
            String tableName = (String)arguments.get(1).getArgumentValue().getValue();
            if (StringUtil.startsWithIgnoreCase(objectName, "jmx.")) {
                objectName = objectName.substring(4);
            }
            this.resultDDL.append("CREATE FOREIGN TEMPORARY TABLE ").append(SQLStringVisitor.escapeSinglePart(tableName));
            ObjectName name = new ObjectName(objectName);
            this.resultDDL.append(" (").append(SQLStringVisitor.escapeSinglePart(JmxTranslator.OBJECT_NAME_COLUMN)).append(" string");
            HashSet<String> attributes = new HashSet<String>();
            for (ObjectInstance oi : mbsc.queryMBeans(name, null)) {
                MBeanInfo info = mbsc.getMBeanInfo(oi.getObjectName());
                MBeanAttributeInfo[] attrInfo = info.getAttributes();
                for (MBeanAttributeInfo attr: attrInfo) {
                    if (attributes.contains(attr.getName())) {
                        continue;
                    }
                    String type = JmxTranslator.getRuntimeType(attr.getType());
                    this.resultDDL.append(", ");
                    this.resultDDL.append(SQLStringVisitor.escapeSinglePart(attr.getName())).append(" ").append(type);
                    attributes.add(attr.getName());
                }
            }
            this.resultDDL.append(") OPTIONS (UPDATABLE false, NAMEINSOURCE '").append(objectName).append("') ON jmx");
        } catch (MalformedObjectNameException | InstanceNotFoundException | IntrospectionException | ReflectionException
                | IOException e) {
            throw new TranslatorException(e);
        }
    }

    @Override
    public List<?> next() throws TranslatorException, DataNotAvailableException {
        if (!this.served) {
            this.served = true;
            return Arrays.asList(this.resultDDL.toString());
        }
        return null;
    }

    @Override
    public List<?> getOutputParameterValues() throws TranslatorException {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public void cancel() throws TranslatorException {
    }
}
