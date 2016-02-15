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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Actionable;
import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.resource.gluster.GlusterHooksResource;
import org.ovirt.engine.api.resource.gluster.GlusterVolumesResource;

@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
public interface ClusterResource extends UpdatableResource<Cluster> {
    @DELETE
    Response remove();

    @Path("networks")
    public AssignedNetworksResource getAssignedNetworksSubResource();

    @Path("permissions")
    public AssignedPermissionsResource getPermissionsResource();

    @Path("glustervolumes")
    public GlusterVolumesResource getGlusterVolumesResource();

    @Path("glusterhooks")
    public GlusterHooksResource getGlusterHooksResource();

    @Path("affinitygroups")
    public AffinityGroupsResource getAffinityGroupsResource();

    @Path("cpuprofiles")
    public AssignedCpuProfilesResource getCpuProfilesResource();

    @POST
    @Consumes({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
    @Actionable
    @Path("resetemulatedmachine")
    public Response resetEmulatedMachine(Action action);

}
