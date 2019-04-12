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

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

import org.teiid.core.types.ClobImpl;
import org.teiid.core.types.JsonType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JmxJsonUtil {

    public static  Object converToJson(Object value) {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(build(value));
        return new JsonType(new ClobImpl(json));
    }

    private static JsonElement buildTabularData(TabularData data) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> mapdata = ((Map<Object, Object>)data);
        JsonObject jo = new JsonObject();
        for (Entry<Object, Object> entry : mapdata.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            jsonAdd(jo, key, value);
        }
        return jo;
    }

    private static void jsonAdd(JsonObject jo, String key, Object value) {
        if (value instanceof String) {
            jo.addProperty(key, (String)value);
        } else if (value instanceof Boolean) {
            jo.addProperty(key, (Boolean)value);
        } else if (value instanceof Number) {
            jo.addProperty(key, (Number)value);
        } else if (value instanceof Character) {
            jo.addProperty(key, (Character)value);
        } else {
            jo.add(key, build(value));
        }
    }

    private static JsonElement buildCompositeData(CompositeData data) {
        CompositeType type = data.getCompositeType();
        JsonObject jo = new JsonObject();
        for (String key: type.keySet()) {
            Object value = data.get(key);
            jsonAdd(jo, key, value);
        }
        return jo;
    }

    private static JsonElement build(Object value) {
        if (value instanceof CompositeData) {
            return buildCompositeData((CompositeData)value);
        } else if (value instanceof TabularData) {
            return buildTabularData((TabularData)value);
        } else if (value.getClass().isArray()) {
            JsonArray jo = new JsonArray();
            for(int i = 0; i < Array.getLength(value); i++){
                jo.add(build(Array.get(value, i)));
            }
            return jo;
        } else {
            if (value instanceof String) {
                return new JsonPrimitive((String)value);
            } else if (value instanceof Boolean) {
                return new JsonPrimitive((Boolean)value);
            } else if (value instanceof Number) {
                return new JsonPrimitive((Number)value);
            } else if (value instanceof Character) {
                return new JsonPrimitive((Character)value);
            } else {
                return new JsonPrimitive(value.toString());
            }
        }
    }
}
