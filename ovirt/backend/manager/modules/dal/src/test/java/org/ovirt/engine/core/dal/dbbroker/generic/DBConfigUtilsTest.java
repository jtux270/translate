package org.ovirt.engine.core.dal.dbbroker.generic;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigCommon;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.config.OptionBehaviour;
import org.ovirt.engine.core.common.config.OptionBehaviourAttribute;
import org.ovirt.engine.core.common.config.TypeConverterAttribute;
import org.ovirt.engine.core.dao.BaseDAOTestCase;

public class DBConfigUtilsTest extends BaseDAOTestCase {
    private DBConfigUtils config;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        config = new DBConfigUtils(false);
        config.refreshVdcOptionCache(dbFacade);
    }

    @Test
    public void testDefaultValues() {
        ConfigValues[] values = ConfigValues.values();

        for (ConfigValues curConfig : values) {
            if (curConfig == ConfigValues.Invalid)
                continue;

            Field configField = null;
            try {
                configField = ConfigValues.class.getField(curConfig.name());
            } catch (Exception e) {
                Assert.fail("Failed to look up" + curConfig.name());
                e.printStackTrace();
            }

            OptionBehaviourAttribute behaviourAttr = configField.getAnnotation(OptionBehaviourAttribute.class);
            if (behaviourAttr != null
                    && (behaviourAttr.behaviour() == OptionBehaviour.Password ||
                            behaviourAttr.behaviour() == OptionBehaviour.DomainsPasswordMap)) {
                continue; // no cert available for password decrypt
            }

            TypeConverterAttribute typeAttr = configField.getAnnotation(TypeConverterAttribute.class);
            assertNotNull("The following field is missing the " + TypeConverterAttribute.class.getSimpleName()
                    + " annotation: " + curConfig.name(), typeAttr);
            Class<?> c = typeAttr.value();

            Object obj = config.getValue(curConfig, ConfigCommon.defaultConfigurationVersion);

            Assert.assertTrue("null return for " + curConfig.name(), obj != null);
            Assert.assertTrue(
                    curConfig.name() + " is a " + obj.getClass().getName() + " but should be a " + c.getName(),
                    c.isInstance(obj));
        }
    }

    @Test
    public void testGetValue() {
        // Verify that values for 3.0 and 3.2 are from DB (since the entries are present in fixtures.xml)
        // and for 3.1, it's the default value from annotation in ConfigValues.
        // 3.0 -> false, 3.1 -> true, 3.2 -> true
        Assert.assertFalse(Config.<Boolean> getValue(ConfigValues.NonVmNetworkSupported, "3.0"));
        Assert.assertTrue(Config.<Boolean> getValue(ConfigValues.NonVmNetworkSupported, "3.1"));
        Assert.assertTrue(Config.<Boolean> getValue(ConfigValues.NonVmNetworkSupported, "3.2"));
    }
}
