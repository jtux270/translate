package org.ovirt.engine.api.restapi.types;

import org.ovirt.engine.api.model.FencingPolicy;
import org.ovirt.engine.api.model.SkipIfConnectivityBroken;
import org.ovirt.engine.api.model.SkipIfSDActive;

public class FencingPolicyMapper {
    @Mapping(from = FencingPolicy.class, to = org.ovirt.engine.core.common.businessentities.FencingPolicy.class)
    public static org.ovirt.engine.core.common.businessentities.FencingPolicy map(FencingPolicy model, org.ovirt.engine.core.common.businessentities.FencingPolicy template) {
        org.ovirt.engine.core.common.businessentities.FencingPolicy entity = template != null ? template : new org.ovirt.engine.core.common.businessentities.FencingPolicy();
        entity.setFencingEnabled(model.isEnabled());
        if (model.isSetSkipIfSdActive()) {
            entity.setSkipFencingIfSDActive(model.getSkipIfSdActive().isEnabled());
        }
        if (model.isSetSkipIfConnectivityBroken()) {
            entity.setSkipFencingIfConnectivityBroken(model.getSkipIfConnectivityBroken().isEnabled());
            if (model.getSkipIfConnectivityBroken().getThreshold() != null) {
                entity.setHostsWithBrokenConnectivityThreshold(model.getSkipIfConnectivityBroken().getThreshold());
            }
            else {
                entity.setHostsWithBrokenConnectivityThreshold(50);
            }
        }
        return entity;
    }

    @Mapping(from = org.ovirt.engine.core.common.businessentities.FencingPolicy.class, to = FencingPolicy.class)
    public static FencingPolicy map(org.ovirt.engine.core.common.businessentities.FencingPolicy entity, FencingPolicy template) {
        FencingPolicy model = template != null ? template : new FencingPolicy();
        SkipIfSDActive skipIfSdActive = new SkipIfSDActive();
        SkipIfConnectivityBroken skipIfConnBroken = new SkipIfConnectivityBroken();
        skipIfSdActive.setEnabled(entity.isSkipFencingIfSDActive());
        skipIfConnBroken.setEnabled(entity.isSkipFencingIfConnectivityBroken());
        skipIfConnBroken.setThreshold(entity.getHostsWithBrokenConnectivityThreshold());
        model.setEnabled(entity.isFencingEnabled());
        model.setSkipIfSdActive(skipIfSdActive);
        model.setSkipIfConnectivityBroken(skipIfConnBroken);
        return model;
    }
}
