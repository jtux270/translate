package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Network;
import org.ovirt.engine.api.model.VnicProfile;
import org.ovirt.engine.api.model.VnicProfiles;
import org.ovirt.engine.api.resource.AssignedVnicProfileResource;
import org.ovirt.engine.api.resource.AssignedVnicProfilesResource;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;

public class BackendAssignedVnicProfilesResource extends AbstractBackendVnicProfilesResource implements AssignedVnicProfilesResource {

    private String networkId;

    public BackendAssignedVnicProfilesResource(String networkId) {
        super();
        this.networkId = networkId;
    }

    @Override
    public VnicProfiles list() {
        return performList();
    }

    @Override
    public Response add(VnicProfile vnicProfile) {
        if (!vnicProfile.isSetNetwork() || !vnicProfile.getNetwork().isSetId()) {
            vnicProfile.setNetwork(new Network());
            vnicProfile.getNetwork().setId(networkId);
        }

        return super.add(vnicProfile);
    }

    @Override
    protected void validateParameters(VnicProfile vnicProfile) {
        validateParameters(vnicProfile, "name");
    }

    @SingleEntityResource
    @Override
    public AssignedVnicProfileResource getAssignedVnicProfileSubResource(@PathParam("id") String id) {
        return inject(new BackendAssignedVnicProfileResource(id, this));
    }

    @Override
    public VnicProfile addParents(VnicProfile vnicProfile) {
        vnicProfile.setNetwork(new Network());
        vnicProfile.getNetwork().setId(networkId);
        return vnicProfile;
    }

    @Override
    protected List<org.ovirt.engine.core.common.businessentities.network.VnicProfile> getVnicProfilesCollection() {
        return getBackendCollection(VdcQueryType.GetVnicProfilesByNetworkId, new IdQueryParameters(asGuid(networkId)));
    }
}
