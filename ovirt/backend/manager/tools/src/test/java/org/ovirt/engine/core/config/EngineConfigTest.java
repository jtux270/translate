package org.ovirt.engine.core.config;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ovirt.engine.core.config.entity.ConfigKey;

public class EngineConfigTest {

    private static final Logger log = Logger.getLogger(EngineConfigTest.class);
    private EngineConfig config = EngineConfig.getInstance();

    @BeforeClass
    public static void setConfigFilePathProperty() throws UnsupportedEncodingException {
        final String path = URLDecoder.decode(ClassLoader.getSystemResource("engine-config.conf").getPath(), "UTF-8");
        System.setProperty(EngineConfig.CONFIG_FILE_PATH_PROPERTY, path);
    }

    @Test
    public void testConfigDirWithFlagSet() throws Exception {
        // get the real path of the config file
        final String path = URLDecoder.decode(ClassLoader.getSystemResource("engine-config.conf").getPath(), "UTF-8");
        Assert.assertNotNull(path);
        EngineConfigExecutor.main("-a", "--config=" + path);
    }

    @Test
    public void getValueWithMultipleVersions() throws Exception {
        final String key = "MaxNumOfVmSockets";
        log.info("getValue: Testing fetch multiple version of " + key);
        List<ConfigKey> keys = config.getEngineConfigLogic().getConfigDAO().getKeysForName(key);
        for (ConfigKey configKey : keys) {
            log.info(configKey.getDisplayValue() + " version: " + configKey.getVersion());
        }
        Assert.assertTrue(keys.size() > 0);
    }

    @Test(expected = IllegalAccessException.class)
    public void setOutOfRangeValue() throws Exception {
        final String outOfRangeForFenceQuietTime = "601";
        final String key = "FenceQuietTimeBetweenOperationsInSec";
        // Should throw IllegalAccessException since the given value is out of range
        config.getEngineConfigLogic().persist(key, outOfRangeForFenceQuietTime, "");
    }

    @Test
    public void setInvalidStringValue() throws Exception {
        final String key = "LDAP_Security_mode";
        Assert.assertFalse(config.getEngineConfigLogic().persist(key, "GSSAPI-invalid-value")); // not valid
    }

    @Test
    public void setStringValueFromFlag() throws Exception {
        final String certificateFileNameKey = "CertificateFileName";
        // Backing up current CertificateFileName
        ConfigKey originalAuthenticationMethod = config.getEngineConfigLogic().fetchConfigKey(certificateFileNameKey, "general");

        final String certificateFileNameNewValue = "/certs/";
        setKeyAndValidate(certificateFileNameKey, certificateFileNameNewValue, "general");

        // Restoring original value and making sure it was restored successfully
        restoreOriginalValue(certificateFileNameKey, originalAuthenticationMethod);
    }

    private void setKeyAndValidate(final String keyName, final String value, final String version)
            throws IllegalAccessException {
        config.getEngineConfigLogic().persist(keyName, value, version);
        ConfigKey currentConfigKey = config.getEngineConfigLogic().fetchConfigKey(keyName, "general");
        Assert.assertEquals(value, currentConfigKey.getValue());
    }

    private void restoreOriginalValue(final String keyName, ConfigKey originialValue)
            throws IllegalAccessException {
        config.getEngineConfigLogic().persist(keyName, originialValue.getValue(), originialValue.getVersion());
        ConfigKey currentConfigKey = config.getEngineConfigLogic().fetchConfigKey(keyName, "general");
        Assert.assertEquals(originialValue.getValue(), currentConfigKey.getValue());
    }
}
