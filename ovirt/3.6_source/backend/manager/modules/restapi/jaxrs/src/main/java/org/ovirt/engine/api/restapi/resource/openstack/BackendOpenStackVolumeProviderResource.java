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

package org.ovirt.engine.api.restapi.resource.openstack;

import static org.ovirt.engine.api.restapi.resource.openstack.BackendOpenStackVolumeProvidersResource.SUB_COLLECTIONS;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.OpenStackVolumeProvider;
import org.ovirt.engine.api.resource.openstack.OpenStackVolumeAuthenticationKeysResource;
import org.ovirt.engine.api.resource.openstack.OpenStackVolumeProviderResource;
import org.ovirt.engine.api.resource.openstack.OpenStackVolumeTypesResource;
import org.ovirt.engine.api.restapi.resource.AbstractBackendExternalProviderResource;
import org.ovirt.engine.api.restapi.resource.BackendExternalProviderHelper;
import org.ovirt.engine.core.common.action.ProviderParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Provider;

public class BackendOpenStackVolumeProviderResource
        extends AbstractBackendExternalProviderResource<OpenStackVolumeProvider>
        implements OpenStackVolumeProviderResource {

    private BackendOpenStackVolumeProvidersResource parent;

    public BackendOpenStackVolumeProviderResource(String id, BackendOpenStackVolumeProvidersResource parent) {
        super(id, OpenStackVolumeProvider.class, SUB_COLLECTIONS);
        this.parent = parent;
    }

    @Override
    public OpenStackVolumeTypesResource getOpenStackVolumeTypes() {
        return inject(new BackendOpenStackVolumeTypesResource(id));
    }

    @Override
    public OpenStackVolumeAuthenticationKeysResource getOpenStackVolumeAuthenticationKeys() {
        return inject(new BackendOpenStackVolumeAuthenticationKeysResource(id));
    }

    @Override
    protected OpenStackVolumeProvider doPopulate(OpenStackVolumeProvider model, Provider entity) {
        return parent.doPopulate(model, entity);
    }

    BackendOpenStackVolumeProvidersResource getParent() {
        return parent;
    }

    @Override
    public Response remove() {
        Provider provider = BackendExternalProviderHelper.getProvider(this, id);
        ProviderParameters parameters = new ProviderParameters(provider);
        return performAction(VdcActionType.RemoveProvider, parameters);
    }
}
