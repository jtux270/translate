package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.BaseResource;
import org.ovirt.engine.api.model.Host;
import org.ovirt.engine.api.model.HostNIC;
import org.ovirt.engine.api.resource.NetworkAttachmentResource;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendHostNicNetworkAttachmentsResource extends AbstractBackendNetworkAttachmentsResource {

    private Guid nicId;

    public BackendHostNicNetworkAttachmentsResource(Guid nicId, Guid hostId) {
        super(hostId);
        this.nicId = nicId;
    }

    @Override
    public NetworkAttachmentResource getNetworkAttachmentSubResource(String id) {
        return inject(new BackendHostNicNetworkAttachmentResource(id, this));
    }

    protected List<NetworkAttachment> getNetworkAttachments() {
        verifyHostAndNicExistence();

        return getBackendCollection(VdcQueryType.GetNetworkAttachmentsByHostNicId, new IdQueryParameters(nicId));
    }

    protected void verifyHostAndNicExistence() {
        verifyHostExistenceToHandle404StatusCode();
        verifyNicExistenceToHandle404StatusCode();
    }

    private void verifyHostExistenceToHandle404StatusCode() {
        Guid hostId = getHostId();
        getEntity(VDS.class, VdcQueryType.GetVdsByVdsId, new IdQueryParameters(hostId), hostId.toString(), true);
    }

    private void verifyNicExistenceToHandle404StatusCode() {
        List<VdsNetworkInterface> hostInterfaces = getBackendCollection(VdsNetworkInterface.class,
                        VdcQueryType.GetVdsInterfacesByVdsId,
                        new IdQueryParameters(getHostId()));

        boolean found = false;
        for (VdsNetworkInterface hostInterface : hostInterfaces) {
            if (hostInterface.getId().equals(nicId)) {
                found = true;
                break;
            }
        }

        if (!found) {
            notFound(VdsNetworkInterface.class);
        }
    }

    @Override
    protected Class<? extends BaseResource> getParentClass() {
        return HostNIC.class;
    }

    @Override
    protected org.ovirt.engine.api.model.NetworkAttachment addParents(org.ovirt.engine.api.model.NetworkAttachment model) {
        model.setHostNic(new HostNIC());
        model.getHostNic().setId(nicId.toString());
        model.getHostNic().setHost(new Host());
        model.getHostNic().getHost().setId(getHostId().toString());
        return model;
    }

    @Override
    public Response add(org.ovirt.engine.api.model.NetworkAttachment attachment) {
        verifyHostAndNicExistence();

        if (attachment.isSetHostNic()) {
            Guid hostNicGuid = Guid.createGuidFromString(attachment.getHostNic().getId());

            if (!nicId.equals(hostNicGuid)) {
                //TODO MM: add message.
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        } else {
            HostNIC hostNIC = new HostNIC();
            hostNIC.setId(nicId.toString());
            attachment.setHostNic(hostNIC);
        }

        return super.add(attachment);
    }
}
