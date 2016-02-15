package org.ovirt.engine.core.dao;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.HostDevice;
import org.ovirt.engine.core.common.businessentities.HostDeviceId;
import org.ovirt.engine.core.common.businessentities.HostDeviceView;
import org.ovirt.engine.core.compat.Guid;

public interface HostDeviceDao extends GenericDao<HostDevice, HostDeviceId>, MassOperationsDao<HostDevice, HostDeviceId> {

    List<HostDevice> getHostDevicesByHostId(Guid hostId);

    List<HostDevice> getHostDevicesByHostIdAndIommuGroup(Guid hostId, int iommuGroup);

    List<HostDeviceView> getVmExtendedHostDevicesByVmId(Guid vmId);

    List<HostDeviceView> getExtendedHostDevicesByHostId(Guid hostId);

    HostDevice getHostDeviceByHostIdAndDeviceName(Guid hostId, String deviceName);

    boolean checkVmHostDeviceAvailability(Guid vmId, Guid hostId);

    void markHostDevicesUsedByVmId(Guid vmId, Guid hostId);

    void freeHostDevicesUsedByVmId(Guid vmId);

    void setVmIdOnHostDevice(HostDeviceId deviceId, Guid vmId);

    void cleanDownVms();
}
