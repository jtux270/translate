/*
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ovirt.engine.api.restapi.types.openstack;

import org.ovirt.engine.api.model.OpenStackVolumeProvider;
import org.ovirt.engine.api.model.OpenstackVolumeAuthenticationKey;
import org.ovirt.engine.api.model.OpenstackVolumeAuthenticationKeyUsageType;
import org.ovirt.engine.api.restapi.types.DateMapper;
import org.ovirt.engine.api.restapi.types.Mapping;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.businessentities.storage.LibvirtSecret;

public class OpenStackVolumeAuthenticationKeyMapper {
    @Mapping(from = LibvirtSecret.class, to = OpenstackVolumeAuthenticationKey.class)
    public static OpenstackVolumeAuthenticationKey map(LibvirtSecret entity, OpenstackVolumeAuthenticationKey template) {
        OpenstackVolumeAuthenticationKey model = template != null ? template : new OpenstackVolumeAuthenticationKey();
        if (entity.getId() != null) {
            model.setId(entity.getId().toString());
            model.setUuid(entity.getId().toString());
        }
        if (entity.getDescription() != null) {
            model.setDescription(entity.getDescription());
        }
        if (entity.getCreationDate() != null) {
            model.setCreationDate(DateMapper.map(entity.getCreationDate(), null));
        }
        if (entity.getUsageType() != null) {
            model.setUsageType(map(entity.getUsageType(), null));
        }
        if (entity.getProviderId() != null) {
            OpenStackVolumeProvider provider = new OpenStackVolumeProvider();
            provider.setId(entity.getProviderId().toString());
            model.setOpenstackVolumeProvider(provider);
        }
        return model;
    }

    @Mapping(from = OpenstackVolumeAuthenticationKey.class, to = LibvirtSecret.class)
    public static LibvirtSecret map(OpenstackVolumeAuthenticationKey model, LibvirtSecret template) {
        LibvirtSecret entity = template != null ? template : new LibvirtSecret();
        if (model.isSetId()) {
            entity.setId(GuidUtils.asGuid(model.getId()));
        }
        if (model.isSetUuid()) {
            entity.setId(GuidUtils.asGuid(model.getUuid()));
        }
        if (model.isSetDescription()) {
            entity.setDescription(model.getDescription());
        }
        if (model.isSetValue()) {
            entity.setValue(model.getValue());
        }
        if (model.isSetUsageType()) {
            OpenstackVolumeAuthenticationKeyUsageType usageType = OpenstackVolumeAuthenticationKeyUsageType.fromValue(model.getUsageType());
            if (usageType != null) {
                entity.setUsageType(map(usageType, null));
            }
        }
        if (model.isSetOpenstackVolumeProvider() && model.getOpenstackVolumeProvider().isSetId()) {
            entity.setProviderId(GuidUtils.asGuid(model.getOpenstackVolumeProvider().getId()));
        }
        return entity;
    }

    @Mapping(from = org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType.class,
            to = String.class)
    public static String map(org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType usageType,
            String template) {
        switch (usageType) {
        case CEPH:
            return OpenstackVolumeAuthenticationKeyUsageType.CEPH.value();
        default:
            return null;
        }
    }

    @Mapping(from = OpenstackVolumeAuthenticationKeyUsageType.class,
            to = org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType.class)
    public static org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType map(
            OpenstackVolumeAuthenticationKeyUsageType usageType,
            org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType outgoing) {
        switch (usageType) {
        case CEPH:
            return org.ovirt.engine.core.common.businessentities.storage.LibvirtSecretUsageType.CEPH;
        default:
            return null;
        }
    }
}
