package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.CpuProfile;
import org.ovirt.engine.api.model.CpuProfiles;
import org.ovirt.engine.api.resource.CpuProfileResource;
import org.ovirt.engine.api.resource.CpuProfilesResource;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryParametersBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendCpuProfilesResource extends AbstractBackendCpuProfilesResource implements CpuProfilesResource {

    static final String[] SUB_COLLECTIONS = { "permissions" };

    public BackendCpuProfilesResource() {
        super(SUB_COLLECTIONS);
    }

    @Override
    public CpuProfiles list() {
        return performList();
    }

    @Override
    protected List<org.ovirt.engine.core.common.businessentities.profiles.CpuProfile> getCpuProfilesCollection() {
        return getBackendCollection(VdcQueryType.GetAllCpuProfiles, new VdcQueryParametersBase());
    }

    @Override
    public Response add(CpuProfile cpuProfile) {
        return super.add(cpuProfile);
    }

    @Override
    protected void validateParameters(CpuProfile cpuProfile) {
        validateParameters(cpuProfile, "name", "cluster.id");
        String clusterId = cpuProfile.getCluster().getId();
        // verify the cluster.id is well provided
        getEntity(VDSGroup.class,
                VdcQueryType.GetVdsGroupById,
                new IdQueryParameters(asGuid(clusterId)),
                "cluster: id="
                        + clusterId);
    }

    @SingleEntityResource
    @Override
    public CpuProfileResource getCpuProfileSubResource(@PathParam("id") String id) {
        return inject(new BackendCpuProfileResource(id));
    }
}
