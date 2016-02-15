package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;

/**
 * A class representing extended fencing policy (extended in the meaning that not only host status and power management
 * settings will be used to decide to fence the host)
 */
public class FencingPolicy implements Serializable {
    /**
     * Enable fencing
     */
    private boolean fencingEnabled;

    /**
     * Skip fencing of host of it's connected to at least one storage domain.
     */
    private boolean skipFencingIfSDActive;
    private boolean skipFencingIfConnectivityBroken;
    private int hostsWithBrokenConnectivityThreshold;

    public FencingPolicy() {
        this(null);
    }

    public FencingPolicy(FencingPolicy fencingPolicy) {
        if (fencingPolicy == null) {
            fencingEnabled = true;
            skipFencingIfSDActive = false;
            skipFencingIfConnectivityBroken = false;
            hostsWithBrokenConnectivityThreshold = 50;
        } else {
            fencingEnabled = fencingPolicy.fencingEnabled;
            skipFencingIfSDActive = fencingPolicy.skipFencingIfSDActive;
            skipFencingIfConnectivityBroken = fencingPolicy.skipFencingIfConnectivityBroken;
            hostsWithBrokenConnectivityThreshold = fencingPolicy.hostsWithBrokenConnectivityThreshold;
        }
    }

    public boolean isFencingEnabled() {
        return fencingEnabled;
    }

    public void setFencingEnabled(boolean fencingEnabled) {
        this.fencingEnabled = fencingEnabled;
    }

    public boolean isSkipFencingIfSDActive() {
        return skipFencingIfSDActive;
    }

    public void setSkipFencingIfSDActive(boolean skipFencingIfSDActive) {
        this.skipFencingIfSDActive = skipFencingIfSDActive;
    }

    public boolean isSkipFencingIfConnectivityBroken() {
        return skipFencingIfConnectivityBroken;
    }

    public void setSkipFencingIfConnectivityBroken(boolean skipFencingIfConnectivityBroken) {
        this.skipFencingIfConnectivityBroken = skipFencingIfConnectivityBroken;
    }

    public int getHostsWithBrokenConnectivityThreshold() {
        return hostsWithBrokenConnectivityThreshold;
    }

    public void setHostsWithBrokenConnectivityThreshold(int hostsWithBrokenConnectivityThreshold) {
        this.hostsWithBrokenConnectivityThreshold = hostsWithBrokenConnectivityThreshold;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FencingPolicy)) {
            return false;
        }
        FencingPolicy other = (FencingPolicy) obj;

        return fencingEnabled == other.fencingEnabled &&
                skipFencingIfSDActive == other.skipFencingIfSDActive &&
                skipFencingIfConnectivityBroken == other.skipFencingIfConnectivityBroken &&
                hostsWithBrokenConnectivityThreshold == other.hostsWithBrokenConnectivityThreshold;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (fencingEnabled ? 1231 : 1237);
        result = prime * result + (skipFencingIfSDActive ? 1231 : 1237);
        result = prime * result + (skipFencingIfConnectivityBroken ? 1231 : 1237);
        result = prime * result + hostsWithBrokenConnectivityThreshold;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ fencingEnabled=");
        sb.append(fencingEnabled);
        sb.append(", skipFencingIfSDActive=");
        sb.append(skipFencingIfSDActive);
        sb.append(", skipFencingIfConnectivityBroken=");
        sb.append(skipFencingIfConnectivityBroken);
        sb.append(", hostsWithBrokenConnectivityThreshold=");
        sb.append(hostsWithBrokenConnectivityThreshold);
        sb.append(" }");
        return sb.toString();
    }
}
