/*
* Copyright (c) 2010 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*           http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Actionable;
import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.resource.externalhostproviders.KatelloErrataResource;

@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
public interface HostResource extends UpdatableResource<Host>, MeasurableResource {
    @DELETE
    Response remove();

    @DELETE
    @Consumes({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
    Response remove(Action action);

    @Path("{action: (approve|install|upgrade|fence|activate|deactivate|commitnetconfig|iscsidiscover|iscsilogin|" +
            "forceselectspm|setupnetworks|enrollcertificate)}/{oid}")
    ActionResource getActionSubresource(@PathParam("action")String action, @PathParam("oid")String oid);

    @POST
    @Actionable
    @Path("approve")
    Response approve(Action action);

    @POST
    @Actionable
    @Path("install")
    Response install(Action action);

    @POST
    @Actionable
    @Path("upgrade")
    Response upgrade(Action action);

    @POST
    @Actionable
    @Path("fence")
    Response fence(Action action);

    @POST
    @Actionable
    @Path("activate")
    Response activate(Action action);

    @POST
    @Actionable
    @Path("deactivate")
    Response deactivate(Action action);

    @POST
    @Actionable
    @Path("commitnetconfig")
    Response commitNetConfig(Action action);

    @POST
    @Actionable
    @Path("iscsidiscover")
    Response iscsiDiscover(Action action);

    @POST
    @Actionable
    @Path("iscsilogin")
    Response iscsiLogin(Action action);

    @POST
    @Actionable
    @Path("unregisteredstoragedomainsdiscover")
    Response unregisteredStorageDomainsDiscover(Action action);

    @POST
    @Actionable
    @Path("refresh")
    Response refresh(Action action);

    @POST
    @Actionable
    @Path("forceselectspm")
    Response forceSelectSPM(Action action);

    @POST
    @Actionable
    @Path("enrollcertificate")
    Response enrollCertificate(Action action);

    @Path("numanodes")
    HostNumaNodesResource getHostNumaNodesResource();

    @Path("nics")
    HostNicsResource getHostNicsResource();

    @Path("networkattachments")
    public NetworkAttachmentsResource getNetworkAttachmentsResource();

    @Path("storage")
    HostStorageResource getHostStorageResource();

    @Path("tags")
    AssignedTagsResource getTagsResource();

    @Path("hooks")
    HostHooksResource getHooksResource();

    @Path("permissions")
    AssignedPermissionsResource getPermissionsResource();

    @Path("fenceagents")
    FenceAgentsResource getFenceAgentsResource();

    @Path("katelloerrata")
    KatelloErrataResource getKatelloErrataResource();

    @Path("devices")
    HostDevicesResource getHostDevicesResource();

    @Path("unmanagednetworks")
    public UnmanagedNetworksResource getUnmanagedNetworksResource();

    @POST
    @Consumes({ ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML })
    @Actionable
    @Path("setupnetworks")
    Response setupNetworks(Action action);

    @Path("storageconnectionextensions")
    public StorageServerConnectionExtensionsResource getStorageConnectionExtensionsResource();
}
