package main;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ManageQLConfig {

    private final Properties properties;

    public ManageQLConfig(String pArgs) {
        this(parseMulti(pArgs));
    }

    public ManageQLConfig(Properties properties) {
        this.properties = properties;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String setProperty(String key, String value) {
        return (String) properties.setProperty(key, value);
    }

    static Properties parseMulti(String config) {
        Properties properties = new Properties();
        if (config == null) {
            config = "";
        }

        String[] options = config.split(",");
        if (options == null) {
            options = new String[]{};
        }

        for (String option : options) {
            parseOption(properties, option, true);
        }

        return properties;
    }

    static void parseOption(Properties properties, String option, boolean encoded) {
        String[] parts = option.split("=", 2);
        if (parts == null || parts.length == 0) {
            throw new IllegalArgumentException("Invalid option '" + option + "'");
        } else if (parts.length == 1) {
            properties.put(parts[0], "true");
        } else { // we split with limit 2, so that the max number of parts.
            String value = encoded ? decode(parts[1]) : parts[1];
            properties.put(parts[0], value);
        }
    }

    private static String decode(String part) {
        try {
            return URLDecoder.decode(part, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encode(String part) {
        try {
            return URLEncoder.encode(part, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return new TreeMap<String, String>((Map) properties).entrySet().stream()
                .map(x -> {
                    String key = x.getKey();
                    String value = x.getValue();
                    if ("true".equals(value)) {
                        return encode(key);
                    } else {
                        return encode(key) + "=" + encode(value);
                    }
                })
                .collect(Collectors.joining(","));
    }


    public void configure(String key, Consumer<String> target) {
        String value = getProperty(key);
        if( value !=null ) {
            target.accept(value);
        }
    }

    public void configureInteger(String key, Consumer<Integer> target) {
        configure(key, x->target.accept(Integer.parseInt(x)));
    }

}
