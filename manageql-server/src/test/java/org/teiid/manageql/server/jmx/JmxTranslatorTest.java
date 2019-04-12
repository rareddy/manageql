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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JmxTranslatorTest {

    @Test
    public void testType() {
        assertEquals("string", JmxTranslator.getRuntimeType("javax.management.ObjectName"));
        assertEquals("integer", JmxTranslator.getRuntimeType("int"));
        assertEquals("boolean", JmxTranslator.getRuntimeType("boolean"));
        assertEquals("long", JmxTranslator.getRuntimeType("long"));
        assertEquals("string[]", JmxTranslator.getRuntimeType("[Ljava.lang.String;"));

        assertEquals("long[]", JmxTranslator.getRuntimeType("[J"));
        assertEquals("integer[]", JmxTranslator.getRuntimeType("[I"));
        assertEquals("double[]", JmxTranslator.getRuntimeType("[D"));
        assertEquals("boolean[]", JmxTranslator.getRuntimeType("[Z"));
        assertEquals("byte[]", JmxTranslator.getRuntimeType("[B"));
        assertEquals("char[]", JmxTranslator.getRuntimeType("[C"));
        assertEquals("float[]", JmxTranslator.getRuntimeType("[F"));
        assertEquals("short[]", JmxTranslator.getRuntimeType("[S"));
        assertEquals("json", JmxTranslator.getRuntimeType("javax.management.openmbean.TabularData"));
        assertEquals("json", JmxTranslator.getRuntimeType("javax.management.openmbean.CompositeData"));
        assertEquals("json", JmxTranslator.getRuntimeType("[Ljavax.management.openmbean.CompositeData;"));
        assertEquals("json", JmxTranslator.getRuntimeType("[Ljavax.management.openmbean.CompositeData;"));
    }
}
