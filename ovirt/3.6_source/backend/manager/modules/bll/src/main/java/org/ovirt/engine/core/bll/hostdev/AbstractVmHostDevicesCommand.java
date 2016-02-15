package org.ovirt.engine.core.bll.hostdev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.VmCommand;
import org.ovirt.engine.core.bll.validator.LocalizedVmStatus;
import org.ovirt.engine.core.common.action.VmHostDevicesParameters;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.HostDevice;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmHostDevice;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.dao.HostDeviceDao;
import org.ovirt.engine.core.dao.VmDeviceDao;

public abstract class AbstractVmHostDevicesCommand<P extends VmHostDevicesParameters> extends VmCommand<P> {

    private static final String CAPABILITY_PCI = "pci";

    @Inject
    private VmDeviceDao vmDeviceDao;

    @Inject
    private HostDeviceDao hostDeviceDao;

    private List<HostDevice> hostDevices;

    /**
     * Contains device names that are explicitly passed in the command parameters
     */
    private Set<String> primaryDeviceNames;

    public AbstractVmHostDevicesCommand(P parameters) {
        super(parameters);
        primaryDeviceNames = new HashSet<>(parameters.getDeviceNames());
    }

    @Override
    protected boolean canDoAction() {
        if (getParameters().getDeviceNames() == null || getParameters().getDeviceNames().isEmpty()) {
            failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_DEVICE_MUST_BE_SPECIFIED);
        }

        if (getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        // hot(un)plug not supported (yet)
        if (getVm().getStatus() != VMStatus.Down) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL, LocalizedVmStatus.from(getVm().getStatus()));
        }

        if (getVm().getDedicatedVmForVdsList().isEmpty()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_PINNED_TO_HOST);
        }

        if (getVm().getDedicatedVmForVdsList().size() > 1) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_PINNED_TO_MULTIPLE_HOSTS);
        }

        if (getHostDevices() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_HOST_DEVICE_NOT_FOUND);
        }

        return true;
    }

    protected Set<String> getPrimaryDeviceNames() {
        return primaryDeviceNames;
    }

    protected VmDeviceDao getVmDeviceDao() {
        return vmDeviceDao;
    }

    private List<HostDevice> getHostDevices() {
        if (hostDevices == null) {
            hostDevices = new ArrayList<>();
            for (String deviceName : getParameters().getDeviceNames()) {
                HostDevice hostDevice = fetchHostDevice(deviceName);
                if (hostDevice == null) {
                    return null;
                }
                hostDevices.add(hostDevice);
            }
        }
        return hostDevices;
    }

    protected Set<HostDevice> getAffectedHostDevices() {
        Set<HostDevice> affectedDevices = new HashSet<>();
        for (HostDevice hostDevice : getHostDevices()) {
            affectedDevices.addAll(getDeviceAtomicGroup(hostDevice));
        }
        return affectedDevices;
    }

    private Collection<HostDevice> getDeviceAtomicGroup(HostDevice hostDevice) {
        if (!hasIommu(hostDevice)) {
            return Collections.singleton(hostDevice);
        }
        // only single dedicated host allowed
        return hostDeviceDao.getHostDevicesByHostIdAndIommuGroup(getVm().getDedicatedVmForVdsList().get(0),
                hostDevice.getIommuGroup());
    }

    protected boolean hasIommu(HostDevice hostDevice) {
        // iommu group restriction only applicable to 'pci' devices
        return CAPABILITY_PCI.equals(hostDevice.getCapability()) && hostDevice.getIommuGroup() != null;
    }

    private HostDevice fetchHostDevice(String deviceName) {
        // single dedicated host allowed.
        return hostDeviceDao.getHostDeviceByHostIdAndDeviceName(getVm().getDedicatedVmForVdsList().get(0), deviceName);
    }

    protected Map<String, VmHostDevice> getExistingVmHostDevicesByName() {
        List<VmDevice> existingDevices = vmDeviceDao.getVmDeviceByVmIdAndType(getVmId(), VmDeviceGeneralType.HOSTDEV);
        List<VmHostDevice> result = new ArrayList<>();
        for (VmDevice device : existingDevices) {
            result.add(new VmHostDevice(device));
        }
        return Entities.vmDevicesByDevice(result);
    }
}
