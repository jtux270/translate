package org.ovirt.engine.core.common.action;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.validation.annotation.ConfiguredRange;
import org.ovirt.engine.core.common.validation.annotation.NoRepetitiveStaticIpInList;

@Deprecated
public class SetupNetworksParameters extends VdsActionParameters {

    private static final long serialVersionUID = 7275844490628744535L;

    @Valid
    @NoRepetitiveStaticIpInList(message = "VALIDATION_REPETITIVE_IP_IN_VDS")
    private List<VdsNetworkInterface> interfaces;

    private CustomPropertiesForVdsNetworkInterface customProperties;

    private boolean force;
    private boolean checkConnectivity;

    @ConfiguredRange(min = 1, maxConfigValue = ConfigValues.NetworkConnectivityCheckTimeoutInSeconds,
            message = "VALIDATION_CONNECTIVITY_TIMEOUT_INVALID")
    private Integer conectivityTimeout;

    private List<String> networksToSync;

    public SetupNetworksParameters() {
        this.interfaces = new ArrayList<>();
        this.customProperties = new CustomPropertiesForVdsNetworkInterface();
    }

    public List<VdsNetworkInterface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<VdsNetworkInterface> interfaces) {
        this.interfaces = interfaces;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isCheckConnectivity() {
        return checkConnectivity;
    }

    public Integer getConectivityTimeout() {
        return conectivityTimeout;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void setCheckConnectivity(boolean checkConnectivity) {
        this.checkConnectivity = checkConnectivity;
    }

    public void setConectivityTimeout(Integer conectivityTimeout) {
        this.conectivityTimeout = conectivityTimeout;
    }

    public List<String> getNetworksToSync() {
        return networksToSync;
    }

    public void setNetworksToSync(List<String> networksToSync) {
        this.networksToSync = networksToSync;
    }

    public CustomPropertiesForVdsNetworkInterface getCustomProperties() {
        return customProperties;
    }

    public void setCustomProperties(CustomPropertiesForVdsNetworkInterface customProperties) {
        this.customProperties = customProperties;
    }
}

