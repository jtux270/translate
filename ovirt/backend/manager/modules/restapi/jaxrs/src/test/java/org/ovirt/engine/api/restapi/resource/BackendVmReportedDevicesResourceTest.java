package org.ovirt.engine.api.restapi.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.api.model.ReportedDevice;
import org.ovirt.engine.api.model.IP;
import org.ovirt.engine.core.common.businessentities.VmGuestAgentInterface;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendVmReportedDevicesResourceTest extends AbstractBackendCollectionResourceTest<ReportedDevice, VmGuestAgentInterface, BackendVmReportedDevicesResource> {

    protected final static Guid PARENT_ID = GUIDS[1];
    protected static final String[] ADDRESSES = { "10.11.12.13", "13.12.11.10", "10.01.10.01" };

    public BackendVmReportedDevicesResourceTest() {
        super(new BackendVmReportedDevicesResource(PARENT_ID), null, "");

    }

    @Override
    protected List<ReportedDevice> getCollection() {
        return collection.list().getReportedDevices();
    }

    @Override
    protected void verifyModel(ReportedDevice model, int index) {
        assertEquals(NAMES[index], model.getName());
        assertEquals(PARENT_ID.toString(), model.getVm().getId());
        verifyIps(model);
        verifyLinks(model);
    }

    private void verifyIps(ReportedDevice device) {
        List<IP> ips = device.getIps().getIPs();
        assertEquals(ADDRESSES.length, ips.size());
        for (int i = 0; i < ADDRESSES.length; i++) {
            assertEquals(ADDRESSES[i], ips.get(i).getAddress());
        }
    }

    @Override
    protected void setUpQueryExpectations(String query, Object failure) throws Exception {
        setUpEntityQueryExpectations(VdcQueryType.GetVmGuestAgentInterfacesByVmId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { PARENT_ID },
                getEntities(),
                failure);
        control.replay();
    }

    @Override
    protected VmGuestAgentInterface getEntity(int index) {
        VmGuestAgentInterface entity = new VmGuestAgentInterface();
        entity.setInterfaceName(NAMES[index]);
        entity.setIpv4Addresses(Arrays.asList(ADDRESSES));
        entity.setVmId(PARENT_ID);
        return entity;
    }

    protected List<VmGuestAgentInterface> getEntities() {
        List<VmGuestAgentInterface> entities = new ArrayList<VmGuestAgentInterface>();
        for (int i = 0; i < NAMES.length; i++) {
            entities.add(getEntity(i));
        }
        return entities;
    }
}
