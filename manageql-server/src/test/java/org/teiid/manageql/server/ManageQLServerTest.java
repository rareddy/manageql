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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.NoOpStatementRewriter;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

/**
 * Unit test for simple App.
 */
public class ManageQLServerTest {
    ManageQLServer server;
    DBI dbi;

    @Before
    public void before() {
        server = new ManageQLServer();
        server.setPsqlPortNumber(0);
        server.start();
        dbi = new DBI(new ConnectionFactory() {
            @Override
            public Connection openConnection() throws SQLException {
                return server.getDriver().connect("jdbc:teiid:manageql", null);
            }
        });
        dbi.setStatementRewriter(new NoOpStatementRewriter());
    }

    @After
    public void after() {
        server.stop();
    }


    @Test
    public void canSelectFromRuntimeMBean() throws SQLException {
        String specName = dbi.withHandle(db ->
                db.createQuery("SELECT SpecName FROM \"jmx.java.lang:type=Runtime\"").mapTo(String.class).first());
        assertEquals("Java Virtual Machine Specification", specName);
    }

    @Test
    public void canSelectObjectNameFromRuntimeMBean() throws SQLException {
        String objectName = dbi.withHandle(db ->
                db.createQuery("SELECT \"$ObjectName\" FROM \"jmx.java.lang:type=Runtime\"").mapTo(String.class).first());
        assertEquals("java.lang:type=Runtime", objectName);
    }


    @Test
    public void canSelectFromUsingObjectNamePattern() throws SQLException {
        HashSet<String> objectNames = new HashSet<>(dbi.withHandle(db ->
                db.createQuery("SELECT \"$ObjectName\" FROM \"jmx.java.lang:*\"").mapTo(String.class).list()));
        assertTrue(objectNames.contains("java.lang:type=Runtime"));
    }
}
