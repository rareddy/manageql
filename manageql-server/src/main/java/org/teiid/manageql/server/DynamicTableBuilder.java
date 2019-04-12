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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.management.ObjectName;

import org.teiid.CommandContext;
import org.teiid.PreParser;
import org.teiid.api.exception.query.QueryParserException;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.core.util.StringUtil;
import org.teiid.jdbc.TeiidSQLException;
import org.teiid.query.parser.QueryParser;
import org.teiid.query.sql.lang.Command;
import org.teiid.query.sql.symbol.GroupSymbol;
import org.teiid.query.sql.visitor.GroupCollectorVisitor;
import org.teiid.query.sql.visitor.SQLStringVisitor;

public class DynamicTableBuilder implements PreParser {

    private ManageQLServer server;
    private static ThreadLocal<Boolean> PARSING = new ThreadLocal<>();

    public DynamicTableBuilder(ManageQLServer server) {
        this.server = server;
    }

    @Override
    public String preParse(String command, CommandContext context) {
        Boolean val = PARSING.get();
        if (val != null && val) {
            //the preparser will be used even on the internal connection
            //so we need to prevent recursion (could use a sql comment instead)
            return command;
        }

        try {
            PARSING.set(true);
            Command c = QueryParser.getQueryParser().parseCommand(command);
            Connection conn = context.getConnection();
            for (GroupSymbol g : GroupCollectorVisitor.getGroupsIgnoreInlineViews(c, true)) {
                try {
                    String name = g.getNonCorrelationName();
                    String nonQualifiedName = name.replace(".", "_");
                    if (StringUtil.startsWithIgnoreCase(name, "sys.")
                            || StringUtil.startsWithIgnoreCase(name, "sysadmin.")
                            || StringUtil.startsWithIgnoreCase(name, "pg_catalog.")) {
                        continue;
                    }
                    //make sure it's a pattern
                    if (!name.contains("*") && !name.contains("?")) {
                        if (this.server.tableExists(name)) {
                            continue;
                        }
                    }
                    // if it is pattern, make sure it is valid pattern
                    try {
                        String objectName = name;
                        if (StringUtil.startsWithIgnoreCase(name, "jmx.")) {
                            objectName = objectName.substring(4);
                        }
                        new ObjectName(objectName);
                    } catch (Exception e) {
                        throw new TeiidRuntimeException("Invalid JMX pattern as table \"" + name +"\"");
                    }

                    defineGroup(name, nonQualifiedName, conn);
                    command = command.replace(name, nonQualifiedName);
                } catch (SQLException e) {
                    //ignore
                }
            }
        } catch (TeiidSQLException | QueryParserException e) {
            //ignore
        } finally {
            PARSING.set(false);
        }
        return command;
    }

    private void defineGroup(String objectName, String targetTableName, Connection conn) throws SQLException {
        Statement s = conn.createStatement();
        try {
            s.execute("drop table " + SQLStringVisitor.escapeSinglePart(targetTableName));
        } catch (SQLException e) {
        }

        //run the create
        ResultSet rs = s.executeQuery("call jmx.get_dynamic_table_ddl('" + objectName + "','"+targetTableName+"')");
        if (rs.next()) {
            String sql = rs.getString(1);
            s.execute(sql);
        }
        rs.close();
    }

}