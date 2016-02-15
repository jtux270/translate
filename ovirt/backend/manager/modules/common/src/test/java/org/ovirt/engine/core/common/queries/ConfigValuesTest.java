package org.ovirt.engine.core.common.queries;

import static org.junit.Assert.assertFalse;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.ovirt.engine.core.common.config.ConfigValues;

public class ConfigValuesTest {
    private static final Log log = LogFactory.getLog(ConfigValuesTest.class);
    private static final Set<String> configValuesEnumNames = getConfigValuesEnumNames();

    private static Set<String> getConfigValuesEnumNames() {
        Set<String> configValuesEnumNames = new HashSet<String>();
        for (ConfigValues configValue : ConfigValues.values()) {
            configValuesEnumNames.add(configValue.toString());
        }
        return configValuesEnumNames;
    }

    @Test
    public void findMissingEnumNames() {
        // Find missing ConfigurationValues enum names in ConfigValues enum
        boolean missingKey = false;
        for (ConfigurationValues configValue : ConfigurationValues.values()) {
            if (!configValuesEnumNames.contains(configValue.toString())) {
                log.error("Found missing key: " + configValue.toString());
                missingKey = true;
                break;
            }
        }

        assertFalse("Found missing key: ", missingKey);
    }
}
