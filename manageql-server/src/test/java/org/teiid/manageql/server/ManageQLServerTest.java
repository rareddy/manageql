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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.result.ResultIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

/**
 * Unit test for simple App.
 */
public class ManageQLServerTest {
    ManageQLServer server;
    Jdbi dbi;

    @Before
    public void before() {
        server = new ManageQLServer();
        server.setPsqlPortNumber(0);
        server.start();
        dbi = Jdbi.create(new ConnectionFactory() {
            @Override
            public Connection openConnection() throws SQLException {
                return server.getDriver().connect("jdbc:teiid:manageql", null);
            }
        });
        // dbi.setStatementRewriter(new NoOpStatementRewriter());
    }

    @After
    public void after() {
        server.stop();
    }


    @Test
    public void canSelectFromRuntimeMBean() throws SQLException {
        String specName = dbi.withHandle(db ->
                db.createQuery("SELECT SpecName FROM \"jmx.java.lang:type=Runtime\"").mapTo(String.class).findOnly());
        assertEquals("Java Virtual Machine Specification", specName);
    }

    @Test
    public void canSelectObjectNameFromRuntimeMBean() throws SQLException {
        String objectName = dbi.withHandle(db ->
                db.createQuery("SELECT \"$ObjectName\" FROM \"jmx.java.lang:type=Runtime\"").mapTo(String.class).findOnly());
        assertEquals("java.lang:type=Runtime", objectName);
    }


    @Test
    public void canSelectFromUsingObjectNamePattern() throws SQLException {
        HashSet<String> objectNames = new HashSet<>(dbi.withHandle(db ->
                db.createQuery("SELECT \"$ObjectName\" FROM \"jmx.java.lang:*\"").mapTo(String.class).list()));
        assertTrue(objectNames.contains("java.lang:type=Runtime"));
    }

    @Test
    public void canSelectArrayAttribute() throws SQLException, InterruptedException {
        String[] value = dbi.withHandle(db ->
                db.createQuery("SELECT MemoryPoolNames FROM \"jmx.java.lang:type=GarbageCollector,name=PS MarkSweep\"")
                        .mapTo(String[].class)
                        .findOnly());
        // System.out.println(Arrays.asList(value));
        assertEquals("PS Eden Space", value[0]);
    }

    @Test
    public void canSelectMapAttribute() throws SQLException, InterruptedException {

        String value = dbi.withHandle(db ->
                db.createQuery("SELECT LastGcInfo FROM \"jmx.java.lang:type=GarbageCollector,name=PS MarkSweep\"")
                        .mapTo(String.class)
                        .findOnly());

        Gson gson = new Gson();
        Map<String, Object> data = gson.fromJson(value, HashMap.class);
        Map<String, Object> memoryUsageBeforeGc = (Map<String, Object>)((List<Object>) data.get("memoryUsageBeforeGc")).get(0);

        System.out.println(memoryUsageBeforeGc);
        assertTrue(memoryUsageBeforeGc.containsKey("key"));
        assertTrue(memoryUsageBeforeGc.containsKey("value"));

    }

    @Test
    public void canSelectComplexAttribute() throws SQLException, InterruptedException {

        String value = dbi.withHandle(db ->
                db.createQuery("SELECT DiagnosticOptions FROM \"jmx.com.sun.management:type=HotSpotDiagnostic\"")
                        .mapTo(String.class)
                        .findOnly());

        Gson gson = new Gson();
        Map<String, Object> data = (Map<String, Object>) gson.fromJson(value, List.class).get(0);
        assertTrue(data.containsKey("name"));
        assertTrue(data.containsKey("origin"));
        assertTrue(data.containsKey("value"));
        assertTrue(data.containsKey("writeable"));
    }




}
