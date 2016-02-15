package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.ReportedDevice;
import org.ovirt.engine.api.model.ReportedDevices;
import org.ovirt.engine.api.model.VM;
import org.ovirt.engine.api.resource.VmReportedDeviceResource;
import org.ovirt.engine.api.resource.VmReportedDevicesResource;
import org.ovirt.engine.api.restapi.types.ReportedDeviceMapper;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmReportedDevicesResource extends AbstractBackendCollectionResource<ReportedDevice, VmGuestAgentInterface> implements VmReportedDevicesResource {

    private Guid vmId;

    public Guid getVmId() {
        return vmId;
    }

    public BackendVmReportedDevicesResource(Guid vmId) {
        super(ReportedDevice.class, VmGuestAgentInterface.class);
        this.vmId = vmId;
    }

    @Override
    protected ReportedDevice addParents(ReportedDevice model) {
        model.setVm(new VM());
        model.getVm().setId(vmId.toString());
        return model;
    }

    @Override
    public ReportedDevices list() {
        ReportedDevices model = new ReportedDevices();
        for (VmGuestAgentInterface device : getCollection()) {
            model.getReportedDevices().add(addLinks(ReportedDeviceMapper.map(device, new ReportedDevice())));
        }
        return model;
    }

    @Override
    @Path("{id}")
    public VmReportedDeviceResource getVmReportedDeviceSubResource(@PathParam("id") String id) {
        return inject(new BackendVmReportedDeviceResource(id, this));
    }

    @Override
    protected Response performRemove(String id) {
        throw new UnsupportedOperationException(); // will never be called
    }

    protected List<VmGuestAgentInterface> getCollection() {
        return getBackendCollection(VdcQueryType.GetVmGuestAgentInterfacesByVmId, new IdQueryParameters(vmId));
    }

    public ReportedDevice lookupReportedDevice(Guid deviceId) {
        VmGuestAgentInterface device = getReportedDeviceByDeviceId(deviceId);
        if (device == null) {
            return notFound();
        }
        return addLinks(ReportedDeviceMapper.map(device, new ReportedDevice()));
    }

    private VmGuestAgentInterface getReportedDeviceByDeviceId(Guid deviceId) {
        for (VmGuestAgentInterface vmGuestAgentInterface : getCollection()) {
            if (deviceId.equals(ReportedDeviceMapper.generateDeviceId(vmGuestAgentInterface))) {
                return vmGuestAgentInterface;
            }
        }
        return null;
    }

    @Override
    protected ReportedDevice doPopulate(ReportedDevice model, VmGuestAgentInterface entity) {
        return model;
    }
}
