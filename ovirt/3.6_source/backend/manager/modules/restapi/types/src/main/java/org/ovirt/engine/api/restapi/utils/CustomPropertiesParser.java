package org.ovirt.engine.api.restapi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.api.model.CustomProperty;
import org.ovirt.engine.api.model.Properties;
import org.ovirt.engine.api.model.Property;

public class CustomPropertiesParser {

     /**
     * Format of @str is <name>=<value>;<name>=<value>;..
     *
     * @param str - The string to parse
     * @param isRegex - defines if CustomProperty is used for regex or value representation
     * @return
    */
    public static List<CustomProperty> parse(String str, boolean isRegex) {
        List<CustomProperty> ret = new ArrayList<CustomProperty>();
        if (str != null) {
            for (String envStr : str.split(";", -1)) {
                String[] parts = getKeyValue(envStr);
                if (parts.length == 2) {
                    CustomProperty env = new CustomProperty();
                    env.setName(parts[0]);
                    if (isRegex) {
                        env.setRegexp(parts[1]);
                    } else {
                        env.setValue(parts[1]);
                    }
                    ret.add(env);
                }
            }
        }
        return ret;
    }

    private static String[] getKeyValue(String str) {
        List<String> keyValue = new ArrayList<String>();
        int index = str.indexOf("=");
        if (index != -1) {
            String key = str.substring(0, index);
            String value = str.substring(index+1);
            keyValue.add(key);
            keyValue.add(value);
        }
        return keyValue.toArray(new String[keyValue.size()]);
    }

    /**
     * Converts VmHooksEnv to @str as <name>=<value>;<name>=<value>;..
     *
     * @param customProperties
     * @return String representing custom properties
     */
    public static String parse(List<CustomProperty> customProperties) {
        StringBuffer buff = new StringBuffer();
        for (CustomProperty hook : customProperties) {
            if (hook.isSetName() && hook.isSetValue()) {
                buff.append(hook.getName() + "=" + hook.getValue() + ";");
            }
        }
        return buff.toString();
    }

    /**
     * Get a map containing the key:value pairs from the given Properties object.
     *
     * @param properties
     *            The key:value pairs.
     * @return A newly-created map containing the key:value pairs.
     */
    public static Map<String, String> toMap(Properties properties) {
        Map<String, String> res = new HashMap<>();
        for (Property property : properties.getProperties()) {
            res.put(property.getName(), property.getValue());
        }
        return res;
    }

    /**
     * Create a Properties object from a map containing key:value pairs.
     *
     * @param properties
     *            The map containing key:value pairs.
     * @return A newly-created Properties object containing the key:value pairs.
     */
    public static Properties fromMap(Map<String, String> properties) {
        Properties res = new Properties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Property property = new Property();
            property.setName(entry.getKey());
            property.setValue(entry.getValue());
            res.getProperties().add(property);
        }
        return res;
    }
}
