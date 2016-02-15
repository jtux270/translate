package org.ovirt.engine.core.utils.pm;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigCommon;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

import java.util.HashMap;

public class FenceConfigHelper {
    private static HashMap<String, String> keyValidatorMap;
    private static HashMap<String, String> keyValidatorExampleMap;
    private static HashMap<String, String> keySeparatorMap;
    private static Log log;
    private static boolean initialized=false;
    private static final String FenceAgentMappingExpr = "((\\w)+[=](\\w)+[,]{0,1})+";
    private static final String FenceAgentDefaultParamsExpr = "([\\w]+([=][\\w]+){0,1}[,]{0,1})+";
    private static final String VdsFenceOptionMappingExpr = "([\\w]+[:]([\\w]*[=][\\w]*[,]{0,1}[;]{0,1}){0,3}[;]{0,1})+";
    private static final String CustomVdsFenceTypeExpr = "((\\w)+[,]{0,1})+";
    private static final String FencePowerWaitParamExpr = "((\\w)+[=](\\w)+[,]{0,1})+";
    private static final String COMMA = ",";
    private static final String SEMICOLON = ";";

   private static void init() {
       if (!initialized) {
           log = LogFactory.getLog(FenceConfigHelper.class);
           keyValidatorMap = new HashMap<String, String>();
           keyValidatorMap.put("FenceAgentMapping", FenceAgentMappingExpr);
           keyValidatorMap.put("FenceAgentDefaultParams", FenceAgentDefaultParamsExpr);
           keyValidatorMap.put("FenceAgentDefaultParamsForPPC", FenceAgentDefaultParamsExpr);
           keyValidatorMap.put("VdsFenceOptionMapping", VdsFenceOptionMappingExpr);
           keyValidatorMap.put("VdsFenceType", CustomVdsFenceTypeExpr);
           keyValidatorMap.put("FencePowerWaitParam", FencePowerWaitParamExpr);
           keyValidatorMap.put("CustomFenceAgentMapping", FenceAgentMappingExpr);
           keyValidatorMap.put("CustomFenceAgentDefaultParams", FenceAgentDefaultParamsExpr);
           keyValidatorMap.put("CustomFenceAgentDefaultParamsForPPC", FenceAgentDefaultParamsExpr);
           keyValidatorMap.put("CustomVdsFenceOptionMapping", VdsFenceOptionMappingExpr);
           keyValidatorMap.put("CustomVdsFenceType", CustomVdsFenceTypeExpr);
           keyValidatorMap.put("CustomFencePowerWaitParam", FencePowerWaitParamExpr);
           keyValidatorExampleMap = new HashMap<String, String>();
           keyValidatorExampleMap.put("CustomFenceAgentMapping", "agent1=agent2,agent3=agent4");
           keyValidatorExampleMap.put("CustomFenceAgentDefaultParams", "agent1=key1=val1,flag;key2=val2");
           keyValidatorExampleMap.put("CustomFenceAgentDefaultParamsForPPC", "agent1=key1=val1,flag;key2=val2");
           keyValidatorExampleMap.put("CustomVdsFenceOptionMapping", "agent1:secure=secure;agent2:port=ipport,slot=slot");
           keyValidatorExampleMap.put("CustomVdsFenceType", "agent1,agent2");
           keyValidatorExampleMap.put("CustomFencePowerWaitParam", "agent1=power_wait,agent2=delay");

           keySeparatorMap = new HashMap<String, String>();
           keySeparatorMap.put("FenceAgentMapping", COMMA);
           keySeparatorMap.put("FenceAgentDefaultParams", SEMICOLON);
           keySeparatorMap.put("FenceAgentDefaultParamsForPPC", SEMICOLON);
           keySeparatorMap.put("VdsFenceOptionMapping", SEMICOLON);
           keySeparatorMap.put("VdsFenceType", COMMA);
           keySeparatorMap.put("FencePowerWaitParam", COMMA);
           initialized = true;
       }
     }

    private static String getCustomKey(String key) {
        return "Custom" + key;
    }

    private static String merge(String key, String value, String customValue) {
        init();
        StringBuilder sb = new StringBuilder();
        sb.append(value);
        if (StringUtils.isNotEmpty(value) && StringUtils.isNotEmpty(customValue)) {
            if (isValid(key, customValue)) {
                sb.append(keySeparatorMap.get(key));
                sb.append(customValue);
            }
            else {
                log.errorFormat("Configuration key {0} has illegal value {1}. Expression should match {2}", key, customValue, keyValidatorMap.get(key));
            }
        }
        return sb.toString();
    }

    private static boolean isValid(String key, String value) {
        if (keyValidatorMap.containsKey(key) && keySeparatorMap.containsKey(key)) {
            return value.matches(keyValidatorMap.get(key));
        }
        return false;
    }

    public static String getFenceConfigurationValue(String key, String version) {
        init();
        String returnValue = null;
        String customReturnValue = null;
        ConfigValues value = ConfigValues.valueOf(key);
        ConfigValues customValue = ConfigValues.valueOf(getCustomKey(key));
        returnValue = Config.getValue(value, version);
        customReturnValue = Config.getValue(customValue, ConfigCommon.defaultConfigurationVersion);
        return merge(key, returnValue, customReturnValue);
    }

    public static String getValidator(String key) {
        init();
        return keyValidatorMap.get(key);
    }

    public static String getValidatorExample(String key) {
        init();
        return keyValidatorExampleMap.get(key);
    }
}
