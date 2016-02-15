package org.ovirt.engine.core.bll.network.host;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.HostDevice;
import org.ovirt.engine.core.common.businessentities.network.HostNicVfsConfig;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.compat.Guid;

public interface NetworkDeviceHelper {

    /**
     * Retrieves the <code>VdsNetworkInterface</code> that the specified <code>pciDevice</code> represents.
     *
     * @param pciDevice
     * @return the <code>VdsNetworkInterface</code> that the specified <code>pciDevice</code> represents. If the device
     *         is not parent of network interface device or doesn't exist in the VdsInterface table a <code>null</code>
     *         is returned.
     */
    public VdsNetworkInterface getNicByPciDevice(final HostDevice pciDevice);

    /**
     * Retrieves the <code>VdsNetworkInterface</code> that the specified <code>pciDevice</code> represents.
     * This method uses the specified <code>devices</code> and doesn't fetch data from the DB.
     *
     * @param pciDevice
     * @param devices collection of all the devices.
     * @return the <code>VdsNetworkInterface</code> that the specified <code>pciDevice</code> represents. If the device
     *         is not parent of network interface device or doesn't exist in the VdsInterface table a <code>null</code>
     *         is returned.
     */
    public VdsNetworkInterface getNicByPciDevice(final HostDevice pciDevice, Collection<HostDevice> devices);

    /**
     * Retrieves whether the specified <code>device</code> is SR-IOV enabled.
     *
     * @param device
     * @return whether the specified <code>device</code> is SR-IOV enabled
     */
    public boolean isSriovDevice(HostDevice device);

    /**
     * Retrieves whether the specified <code>device</code> represents a physical nic.
     *
     * @param device
     * @return whether the specified <code>device</code> represents a physical nic
     */
    public boolean isNetworkDevice(HostDevice device);

    /**
     * Adds <code>maxNumOfVfs</code> and <code>numOfVfs</code> info to the <code>hostNicVfsConfig</code>
     *
     * @param hostNicVfsConfig
     */
    public void updateHostNicVfsConfigWithNumVfsData(HostNicVfsConfig hostNicVfsConfig);

    /**
     * Retrieves all the HostDevices of the specified host, adds <code>maxNumOfVfs</code> and <code>numOfVfs</code> info
     * to each <code>HostDevice</code>
     *
     * @param hostId
     * @return all the HostDevices of the specified host, adds <code>maxNumOfVfs</code> and <code>numOfVfs</code> info
     *         to each <code>HostDevice</code>
     */
    public List<HostNicVfsConfig> getHostNicVfsConfigsWithNumVfsDataByHostId(Guid hostId);

    /**
     * Retrieves whether all the VFs on the nic are free to use by a VM
     *
     * @param nic
     *            physical SR-IOV enabled nic
     * @return whether all the VFs on the nic are free to use by a VM.
     * @throws <code>UnsupportedOperationException</code> in case the nic is not SR-IOV enabled
     */
    public boolean areAllVfsFree(VdsNetworkInterface nic);

    /**
     * Retrieves whether the device is occupied by virtual network or VLAN
     *
     * @param hostDevice arbitrary physical host device (not only network)
     * @return whether this device is not occupied for networking purposes
     */
    public boolean isDeviceNetworkFree(HostDevice hostDevice);

    /**
     * Retrieves the first free VF on the nic
     *
     * @param nic
     *            physical SR-IOV enabled nic
     * @param excludeVfs
     *            vfs that should be considered as non-free
     * @return the first free VF on the nic
     * @throws <code>UnsupportedOperationException</code> in case the nic is not SR-IOV enabled
     */
    public HostDevice getFreeVf(VdsNetworkInterface nic, List<String> excludeVfs);

    /**
     * Retrieves the pciDevice name of the specified <code>nic</code>
     *
     * @param nic
     * @return the pciDevice name of the specified <code>nic</code>
     */
    public String getPciDeviceNameByNic(VdsNetworkInterface nic);

    /**
     * This method updated the DB to reflect the specified VFs are attached the specified VM. Passing <code>null</code>
     * as <code>vmId</code> means the VF should not be attached to any VM.
     *
     * @param hostId
     * @param vmId
     * @param vfsNames
     */
    public void setVmIdOnVfs(Guid hostId, Guid vmId, final Set<String> vfsNames);

    /**
     * Removes the <code>vmId</code> from all the VFs that were attached to the VM
     *
     * @param vmId
     * @return the id of the affected Host or null if there were no VFs attached to the VM
     */
    public Guid removeVmIdFromVfs(final Guid vmId);
}
