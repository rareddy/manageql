package org.teiid.manageql.agent;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;

public class ManageQLAgentConfig {

    private final Properties properties;

    public ManageQLAgentConfig(String pArgs) {
        this(parse(pArgs));
    }

    public ManageQLAgentConfig(Properties properties) {
        this.properties = new Properties(properties);
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private static Properties parse(String config) {
        Properties properties = new Properties();
        if (config == null ) {
            config = "";
        }

        String[] options = config.split(",");
        if( options==null ) {
            options = new String[]{};
        }

        for (String option : options) {
            String[] parts = option.split("=",2);
            if (parts == null || parts.length != 2) {
                throw new IllegalArgumentException("Invalid option '" + option + "'");
            } else {
                String part = parts[0];
                properties.put(decode(part), decode(parts[1]));
            }
        }

        return properties;
    }

    private static String decode(String part) {
        try {
            return URLDecoder.decode(part, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
