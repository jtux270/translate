package org.ovirt.engine.ui.frontend.server.gwt;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.ovirt.engine.core.common.config.ConfigCommon;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.common.queries.GetConfigurationValueParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.frontend.server.gwt.plugin.PluginData;
import org.ovirt.engine.ui.frontend.server.gwt.plugin.PluginDataManager;

/**
 * WebAdmin GWT application host page servlet.
 * <p>
 * Note that this servlet <em>does</em> {@linkplain PluginDataManager#reloadData reload} UI plugin
 * descriptor/configuration data as part of its request handling.
 */
public class WebAdminHostPageServlet extends GwtDynamicHostPageServlet {

    private static final long serialVersionUID = -6393009825835028397L;

    protected static final String ATTR_APPLICATION_MODE = "applicationMode"; //$NON-NLS-1$
    protected static final String ATTR_PLUGIN_DEFS = "pluginDefinitions"; //$NON-NLS-1$
    protected static final String ATTR_ENGINE_SESSION_TIMEOUT = "engineSessionTimeout"; //$NON-NLS-1$

    @Override
    protected String getSelectorScriptName() {
        return "webadmin.nocache.js"; //$NON-NLS-1$
    }

    @Override
    protected boolean filterQueries() {
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
        ServletException {
        // Set attribute for applicationMode object
        Integer applicationMode = getApplicationMode(getEngineSessionId(request));
        request.setAttribute(ATTR_APPLICATION_MODE, getApplicationModeObject(applicationMode));

        // Set attribute for pluginDefinitions array
        List<PluginData> pluginData = getPluginData();
        request.setAttribute(ATTR_PLUGIN_DEFS, getPluginDefinitionsArray(pluginData));

        // Set attribute for engineSessionTimeout object
        Integer engineSessionTimeout = getEngineSessionTimeout(getEngineSessionId(request));
        request.setAttribute(ATTR_ENGINE_SESSION_TIMEOUT, getEngineSessionTimeoutObject(engineSessionTimeout));

        super.doGet(request, response);
    }

    @Override
    protected MessageDigest getMd5Digest(HttpServletRequest request) throws NoSuchAlgorithmException {
        MessageDigest digest = super.getMd5Digest(request);

        // Update based on applicationMode object
        digest.update(request.getAttribute(ATTR_APPLICATION_MODE).toString().getBytes());

        // Update based on pluginDefinitions array
        digest.update(request.getAttribute(ATTR_PLUGIN_DEFS).toString().getBytes());

        // Update based on engineSessionTimeout object
        digest.update(request.getAttribute(ATTR_ENGINE_SESSION_TIMEOUT).toString().getBytes());

        return digest;
    }

    protected Integer getApplicationMode(String sessionId) {
        return (Integer) runPublicQuery(VdcQueryType.GetConfigurationValue,
                new GetConfigurationValueParameters(ConfigurationValues.ApplicationMode,
                        ConfigCommon.defaultConfigurationVersion), sessionId);
    }

    protected ObjectNode getApplicationModeObject(Integer applicationMode) {
        ObjectNode obj = createObjectNode();
        obj.put("value", String.valueOf(applicationMode)); //$NON-NLS-1$
        return obj;
    }

    protected List<PluginData> getPluginData() {
        List<PluginData> currentData = new ArrayList<PluginData>(
                PluginDataManager.getInstance().reloadAndGetCurrentData());
        Collections.sort(currentData);
        return currentData;
    }

    protected ArrayNode getPluginDefinitionsArray(List<PluginData> pluginData) {
        ArrayNode arr = createArrayNode();
        for (PluginData data : pluginData) {
            ObjectNode dataObj = createObjectNode();
            dataObj.put("name", data.getName()); //$NON-NLS-1$
            dataObj.put("url", data.getUrl()); //$NON-NLS-1$
            dataObj.put("config", data.mergeConfiguration()); //$NON-NLS-1$
            dataObj.put("enabled", data.isEnabled()); //$NON-NLS-1$
            arr.add(dataObj);
        }
        return arr;
    }

    protected Integer getEngineSessionTimeout(String sessionId) {
        return (Integer) runPublicQuery(VdcQueryType.GetConfigurationValue,
                new GetConfigurationValueParameters(ConfigurationValues.UserSessionTimeOutInterval,
                        ConfigCommon.defaultConfigurationVersion), sessionId);
    }

    protected ObjectNode getEngineSessionTimeoutObject(Integer engineSessionTimeout) {
        ObjectNode obj = createObjectNode();
        obj.put("value", String.valueOf(engineSessionTimeout)); //$NON-NLS-1$
        return obj;
    }

}
