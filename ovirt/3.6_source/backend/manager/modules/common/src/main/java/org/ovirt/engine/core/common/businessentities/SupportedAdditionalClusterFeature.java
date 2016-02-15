package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;
import java.util.Objects;

import org.ovirt.engine.core.common.utils.ToStringBuilder;
import org.ovirt.engine.core.compat.Guid;

public class SupportedAdditionalClusterFeature implements Serializable {

    private static final long serialVersionUID = -1063480824650271898L;
    private Guid clusterId;
    private boolean enabled;
    private AdditionalFeature feature;

    public SupportedAdditionalClusterFeature() {
    }

    public SupportedAdditionalClusterFeature(Guid clusterId, boolean enabled, AdditionalFeature feature) {
        this.clusterId = clusterId;
        this.setEnabled(enabled);
        this.feature = feature;
    }

    public Guid getClusterId() {
        return clusterId;
    }

    public void setClusterId(Guid clusterId) {
        this.clusterId = clusterId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AdditionalFeature getFeature() {
        return feature;
    }

    public void setFeature(AdditionalFeature feature) {
        this.feature = feature;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, feature, enabled);
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof SupportedAdditionalClusterFeature)) {
            SupportedAdditionalClusterFeature feature = (SupportedAdditionalClusterFeature) obj;
            if (enabled == feature.isEnabled()
                    && Objects.equals(getClusterId(), feature.getClusterId())
                    && Objects.equals(getFeature(), feature.getFeature())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return ToStringBuilder.forInstance(this)
                .append("feature", getFeature())
                .append("clusterId", getClusterId())
                .append("enabled", isEnabled())
                .build();
    }

}
