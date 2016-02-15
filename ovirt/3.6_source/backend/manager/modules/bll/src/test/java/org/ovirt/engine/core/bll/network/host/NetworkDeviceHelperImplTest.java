package org.ovirt.engine.core.bll.network.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.HostDevice;
import org.ovirt.engine.core.common.businessentities.HostDeviceId;
import org.ovirt.engine.core.common.businessentities.network.HostNicVfsConfig;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.HostDeviceDao;
import org.ovirt.engine.core.dao.network.HostNicVfsConfigDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.utils.RandomUtils;

@RunWith(MockitoJUnitRunner.class)
public class NetworkDeviceHelperImplTest {

    private static final String NIC_NAME = RandomUtils.instance().nextString(5);
    private static final Guid NIC_ID = Guid.newGuid();
    private static final Guid HOST_ID = Guid.newGuid();
    private static final String NET_DEVICE_NAME = RandomUtils.instance().nextString(5);
    private static final String PCI_DEVICE_NAME = RandomUtils.instance().nextString(5);
    private static final String PCI_DEVICE_NAME_2 = RandomUtils.instance().nextString(5);
    private static int TOTAL_NUM_OF_VFS = 7;

    @Mock
    private HostDevice netDevice;

    @Mock
    private HostDevice pciDevice;

    @Mock
    private VdsNetworkInterface nic;

    @Mock
    private HostNicVfsConfig hostNicVfsConfig;

    @Mock
    private InterfaceDao interfaceDao;

    @Mock
    private HostDeviceDao hostDeviceDao;

    @Mock
    private HostNicVfsConfigDao hostNicVfsConfigDao;

    @Captor
    private ArgumentCaptor<HostDeviceId> hostDeviceIdCaptor;

    @Captor
    private ArgumentCaptor<Guid> vmIdCaptor;

    private NetworkDeviceHelperImpl networkDeviceHelper;

    @Before
    public void setUp() {
        networkDeviceHelper = new NetworkDeviceHelperImpl(interfaceDao, hostDeviceDao, hostNicVfsConfigDao);

        when(netDevice.getHostId()).thenReturn(HOST_ID);
        when(netDevice.getDeviceName()).thenReturn(NET_DEVICE_NAME);
        when(netDevice.getNetworkInterfaceName()).thenReturn(NIC_NAME);
        when(netDevice.getParentDeviceName()).thenReturn(PCI_DEVICE_NAME);

        when(pciDevice.getHostId()).thenReturn(HOST_ID);
        when(pciDevice.getDeviceName()).thenReturn(PCI_DEVICE_NAME);
        when(hostDeviceDao.getHostDeviceByHostIdAndDeviceName(HOST_ID, PCI_DEVICE_NAME)).thenReturn(pciDevice);

        List<HostDevice> devices = new ArrayList<>();
        devices.add(netDevice);
        devices.add(pciDevice);
        mockHostDevices(devices);

        when(nic.getId()).thenReturn(NIC_ID);
        when(nic.getName()).thenReturn(NIC_NAME);
        when(nic.getVdsId()).thenReturn(HOST_ID);
        when(interfaceDao.get(NIC_ID)).thenReturn(nic);
        when(nic.getName()).thenReturn(NIC_NAME);

        when(hostNicVfsConfig.getNicId()).thenReturn(NIC_ID);
        when(hostNicVfsConfigDao.getByNicId(NIC_ID)).thenReturn(hostNicVfsConfig);
    }

    @Test
    public void getNicByPciDeviceNotParentOfNetDevice() {
        assertNull(networkDeviceHelper.getNicByPciDevice(netDevice));
    }

    @Test
    public void getNicByNetDeviceNoNic() {
        VdsNetworkInterface newNic = new VdsNetworkInterface();
        newNic.setName(netDevice.getNetworkInterfaceName() + "not");
        mockNics(Collections.singletonList(newNic), false);

        assertNull(networkDeviceHelper.getNicByPciDevice(pciDevice));
    }

    @Test
    public void getNicByNetDeviceValid() {
        mockNics(Collections.<VdsNetworkInterface> emptyList(), true);
        assertEquals(nic, networkDeviceHelper.getNicByPciDevice(pciDevice));
    }

    @Test
    public void getNicByNetDeviceWithNonDbDevicesNoNetDevice() {
        mockNics(Collections.<VdsNetworkInterface> emptyList(), true);
        Collection<HostDevice> devices = new ArrayList<>();
        devices.add(pciDevice);

        assertNull(networkDeviceHelper.getNicByPciDevice(pciDevice, devices));
    }

    @Test
    public void isSriovNetworkDeviceNotSriov() {
        commonIsSriovDevice(false);
    }

    @Test
    public void isSriovNetworkDeviceSriov() {
        commonIsSriovDevice(true);
    }

    private void commonIsSriovDevice(boolean isSriov) {
        when(pciDevice.getTotalVirtualFunctions()).thenReturn(isSriov ? TOTAL_NUM_OF_VFS : null);

        assertEquals(isSriov, networkDeviceHelper.isSriovDevice(pciDevice));
    }

    @Test
    public void isNetworkDevicePossitive() {
        assertFalse(networkDeviceHelper.isNetworkDevice(pciDevice));
    }

    @Test
    public void isNetworkDeviceNegtive() {
        assertTrue(networkDeviceHelper.isNetworkDevice(netDevice));
    }

    @Test
    public void updateHostNicVfsConfigWithNumVfsData() {
        commonUpdateHostNicVfsConfigWithNumVfsData(4);
    }

    @Test
    public void updateHostNicVfsConfigWithNumVfsDataZeroVfs() {
        commonUpdateHostNicVfsConfigWithNumVfsData(0);
    }

    private void commonUpdateHostNicVfsConfigWithNumVfsData(int numOfVfs) {
        when(pciDevice.getTotalVirtualFunctions()).thenReturn(TOTAL_NUM_OF_VFS);
        List<HostDevice> vfs = mockVfsOnNetDevice(numOfVfs);
        mockHostDevices(vfs);

        networkDeviceHelper.updateHostNicVfsConfigWithNumVfsData(hostNicVfsConfig);

        verify(hostNicVfsConfig).setMaxNumOfVfs(TOTAL_NUM_OF_VFS);
        verify(hostNicVfsConfig).setNumOfVfs(numOfVfs);
    }

    @Test
    public void getHostNicVfsConfigsWithNumVfsDataByHostId() {
        when(hostNicVfsConfigDao.getAllVfsConfigByHostId(HOST_ID)).thenReturn(Collections.singletonList(hostNicVfsConfig));

        when(pciDevice.getTotalVirtualFunctions()).thenReturn(TOTAL_NUM_OF_VFS);
        List<HostDevice> vfs = mockVfsOnNetDevice(2);
        mockHostDevices(vfs);

        List<HostNicVfsConfig> vfsConfigList =
                networkDeviceHelper.getHostNicVfsConfigsWithNumVfsDataByHostId(HOST_ID);

        assertEquals(1, vfsConfigList.size());
        assertEquals(hostNicVfsConfig, vfsConfigList.get(0));

        verify(hostNicVfsConfig).setMaxNumOfVfs(TOTAL_NUM_OF_VFS);
        verify(hostNicVfsConfig).setNumOfVfs(2);
    }

    private List<HostDevice> mockVfsOnNetDevice(int numOfVfs) {
        return mockVfsOnNetDevice(numOfVfs, null);
    }

    private List<HostDevice> mockVfsOnNetDevice(int numOfVfs, Guid vmId) {
        List<HostDevice> vfs = new ArrayList<>();

        for (int i = 0; i < numOfVfs; ++i) {
            HostDevice vfPciDevice = new HostDevice();
            vfPciDevice.setParentPhysicalFunction(pciDevice.getDeviceName());
            vfPciDevice.setDeviceName(String.valueOf(i));
            vfPciDevice.setHostId(HOST_ID);
            vfPciDevice.setVmId(vmId);
            vfs.add(vfPciDevice);
        }

        return vfs;
    }

    private void mockHostDevices(List<HostDevice> extraDevices) {
        List<HostDevice> devices = new ArrayList<>();
        devices.add(pciDevice);
        devices.add(netDevice);
        devices.addAll(extraDevices);

        when(hostDeviceDao.getHostDevicesByHostId(HOST_ID)).thenReturn(devices);
        when(hostDeviceDao.getAll()).thenReturn(devices);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void areAllVfsFreeNotSriovNic() {
        commonIsSriovDevice(false);
        networkDeviceHelper.areAllVfsFree(nic);
    }

    @Test
    public void areAllVfsFreeTrueNoVfs() {
        freeVfCommon(0, 0, 0, 0, 0);
        assertTrue(networkDeviceHelper.areAllVfsFree(nic));
    }

    @Test
    public void areAllVfsFreeFalseAttachedToVm() {
        freeVfCommon(7, 3, 0, 0, 0);
        assertFalse(networkDeviceHelper.areAllVfsFree(nic));
    }

    @Test
    public void areAllVfsFreeFalseNoNic() {
        freeVfCommon(6, 0, 1, 0, 0);
        assertFalse(networkDeviceHelper.areAllVfsFree(nic));
    }

    @Test
    public void areAllVfsFreeFalseHasNetwork() {
        freeVfCommon(2, 0, 0, 3, 0);
        assertFalse(networkDeviceHelper.areAllVfsFree(nic));
    }

    @Test
    public void areAllVfsFreeFalseHasVlanDevice() {
        freeVfCommon(4, 0, 0, 0, 3);
        assertFalse(networkDeviceHelper.areAllVfsFree(nic));
    }

    @Test
    public void areAllVfsFreeTrue() {
        freeVfCommon(5, 0, 0, 0, 0);
        assertTrue(networkDeviceHelper.areAllVfsFree(nic));
    }

    @Test
    public void areAllVfsFreeFalseMix() {
        freeVfCommon(1, 2, 3, 4, 5);
        assertFalse(networkDeviceHelper.areAllVfsFree(nic));
    }

    private List<HostDevice> freeVfCommon(int numOfFreeVfs,
            int numOfVfsAttachedToVm,
            int numOfVfsHasNoNic,
            int numOfVfsHasNetworkAttached,
            int numOfVfsHasVlanDeviceAttached) {
        networkDeviceHelper = spy(new NetworkDeviceHelperImpl(interfaceDao, hostDeviceDao, hostNicVfsConfigDao));

        List<HostDevice> devices = new ArrayList<>();
        List<HostDevice> freeVfs = new ArrayList<>();

        int numOfVfs =
                numOfFreeVfs + numOfVfsAttachedToVm + numOfVfsHasNoNic + numOfVfsHasNetworkAttached
                        + numOfVfsHasVlanDeviceAttached;
        List<HostDevice> vfs = mockVfsOnNetDevice(numOfVfs);
        List<VdsNetworkInterface> nics = new ArrayList<>();
        devices.addAll(vfs);

        for (HostDevice vfPciDevice : vfs) {
            HostDevice vfNetDevice = mockNetworkDeviceForPciDevice(vfPciDevice);
            devices.add(vfNetDevice);

            if (numOfVfsHasNoNic != 0) {
                --numOfVfsHasNoNic;
            } else {
                VdsNetworkInterface vfNic = mockNicForNetDevice(vfNetDevice);
                nics.add(vfNic);
                if (numOfVfsAttachedToVm != 0) {
                    --numOfVfsAttachedToVm;
                    vfPciDevice.setVmId(Guid.newGuid());
                } else if (numOfVfsHasNetworkAttached != 0) {
                    --numOfVfsHasNetworkAttached;
                    vfNic.setNetworkName("netName");
                } else if (numOfVfsHasVlanDeviceAttached != 0) {
                    --numOfVfsHasVlanDeviceAttached;
                    doReturn(true).when(networkDeviceHelper)
                            .isVlanDeviceAttached(vfNic);
                } else {
                    doReturn(false).when(networkDeviceHelper)
                            .isVlanDeviceAttached(vfNic);
                    freeVfs.add(vfPciDevice);
                }
            }
        }

        mockHostDevices(devices);
        mockNics(nics, true);

        return freeVfs;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getFreeVfNotSriovNic() {
        commonIsSriovDevice(false);
        networkDeviceHelper.getFreeVf(nic, null);
    }

    @Test
    public void getFreeVfNoVfs() {
        freeVfCommon(0, 0, 0, 0, 0);
        assertNull(networkDeviceHelper.getFreeVf(nic, null));
    }

    @Test
    public void getFreeVfNoFreeVf() {
        freeVfCommon(0, 1, 2, 3, 4);
        assertNull(networkDeviceHelper.getFreeVf(nic, null));
    }

    @Test
    public void getFreeVfOneFreeVf() {
        List<HostDevice> freeVfs = freeVfCommon(1, 4, 3, 2, 1);
        assertEquals(1, freeVfs.size());
        assertTrue(freeVfs.contains(networkDeviceHelper.getFreeVf(nic, null)));
    }

    @Test
    public void getFreeVfMoreThanOneFreeVf() {
        List<HostDevice> freeVfs = freeVfCommon(5, 2, 2, 2, 2);
        assertEquals(5, freeVfs.size());
        assertTrue(freeVfs.contains(networkDeviceHelper.getFreeVf(nic, null)));
    }

    @Test
    public void getFreeVfWithExcludedVfs() {
        List<HostDevice> freeVfs = freeVfCommon(5, 2, 2, 2, 2);
        assertEquals(5, freeVfs.size());
        List<String> excludedVfs = new ArrayList<>();
        excludedVfs.add(freeVfs.get(0).getDeviceName());
        excludedVfs.add(freeVfs.get(1).getDeviceName());
        freeVfs.removeAll(excludedVfs);
        assertTrue(freeVfs.contains(networkDeviceHelper.getFreeVf(nic, excludedVfs)));
    }

    @Test
    public void isNonNetworkDeviceNetworkFree() {
        HostDevice device = new HostDevice();
        device.setHostId(HOST_ID);
        device.setDeviceName(PCI_DEVICE_NAME_2);

        assertTrue(networkDeviceHelper.isDeviceNetworkFree(device));
    }

    @Test
    public void isNetworkDeviceNonNetworkFree() {
        freeVfCommon(0, 0, 0, 1, 0);
        HostDevice hostDevice = getSingleMockedNonFreeDevice();
        assertFalse(networkDeviceHelper.isDeviceNetworkFree(hostDevice));
    }

    @Test
    public void isVlanDeviceNonNetworkFree() {
        freeVfCommon(0, 0, 0, 0, 1);
        HostDevice hostDevice = getSingleMockedNonFreeDevice();
        assertFalse(networkDeviceHelper.isDeviceNetworkFree(hostDevice));
    }

    /**
     * Helper method for cases when a single non-free device is mocked by {@link #freeVfCommon}
     */
    private HostDevice getSingleMockedNonFreeDevice() {
        List<HostDevice> devices = hostDeviceDao.getAll();
        // freeVfCommon sets up 'netDevice', 'pciDevice', a parent device and the one we specified.
        assertEquals(4, devices.size());

        // the device we are interested in, is the 'parent'
        return devices.get(2);
    }

    private VdsNetworkInterface mockNicForNetDevice(HostDevice netDeviceParam) {
        VdsNetworkInterface nic = new VdsNetworkInterface();
        nic.setVdsId(netDeviceParam.getHostId());
        nic.setName(netDeviceParam.getNetworkInterfaceName());

        return nic;
    }

    private void mockNics(List<VdsNetworkInterface> extraNics, boolean includeDefault) {
        List<VdsNetworkInterface> nics = new ArrayList<>();

        if (includeDefault) {
            nics.add(nic);
        }

        nics.addAll(extraNics);

        when(interfaceDao.getAllInterfacesForVds(HOST_ID)).thenReturn(nics);
    }

    private HostDevice mockNetworkDeviceForPciDevice(HostDevice pciDeviceParam) {
        HostDevice mockedNetDevice = new HostDevice();
        mockedNetDevice.setParentDeviceName(pciDeviceParam.getDeviceName());
        mockedNetDevice.setHostId(pciDeviceParam.getHostId());
        mockedNetDevice.setDeviceName(pciDeviceParam.getDeviceName() + "netDevice");
        mockedNetDevice.setNetworkInterfaceName(mockedNetDevice.getDeviceName() + "iface");

        return mockedNetDevice;
    }

    @Test
    public void getPciDeviceNameByNic() {
        assertEquals(PCI_DEVICE_NAME, networkDeviceHelper.getPciDeviceNameByNic(nic));
    }

    @Test
    public void setVmIdOnVfs() {
        List<HostDevice> vfs = mockVfsOnNetDevice(1);
        mockHostDevices(vfs);

        HostDevice vf = vfs.get(0);
        Guid vmId = Guid.newGuid();
        vf.setVmId(vmId);
        networkDeviceHelper.setVmIdOnVfs(HOST_ID, vmId, Collections.singleton(vf.getDeviceName()));

        verify(hostDeviceDao).setVmIdOnHostDevice(hostDeviceIdCaptor.capture(), vmIdCaptor.capture());

        HostDeviceId capturedDeviceId = hostDeviceIdCaptor.getValue();
        Guid capturedVmId = vmIdCaptor.getValue();

        assertEquals(vf.getId(), capturedDeviceId);
        assertEquals(vmId, capturedVmId);
    }

    @Test
    public void removeVmIdFromVfsNoOtherDeviceWithVmIdTest() {
        removeVmIdFromVfsCommonTest(4, 0);
    }

    @Test
    public void removeVmIdFromVfsNoVfsWithVmIdTest() {
        removeVmIdFromVfsCommonTest(0, 2);
    }

    @Test
    public void removeVmIdFromVfsVfsAndOtherDeviceWithVmIdTest() {
        removeVmIdFromVfsCommonTest(2, 3);
    }

    @Test
    public void removeVmIdFromVfsNoVfsAndNoOtherDeviceWithVmIdTest() {
        removeVmIdFromVfsCommonTest(0, 0);
    }

    private void removeVmIdFromVfsCommonTest(int numOfVfWithVmId, int numOfOtherDeviceWithVmId) {
        List<HostDevice> allDevices = new ArrayList<>();
        List<HostDevice> otherDeviceWithVmId = new ArrayList<>();

        Guid vmId = Guid.newGuid();
        List<HostDevice> vfs = mockVfsOnNetDevice(numOfVfWithVmId, vmId);
        allDevices.addAll(vfs);

        for (int i = 0; i <= numOfOtherDeviceWithVmId; ++i) {
            HostDevice hostDevice = createHostDevice(vmId);
            otherDeviceWithVmId.add(hostDevice);
        }

        allDevices.addAll(otherDeviceWithVmId);
        mockHostDevices(allDevices);

        for (HostDevice vf : vfs) {
            assertEquals(vmId, vf.getVmId());
        }

        networkDeviceHelper.removeVmIdFromVfs(vmId);

        for (HostDevice vf : vfs) {
            vf.setVmId(null);
        }

        if (numOfVfWithVmId == 0) {
            verify(hostDeviceDao, never()).setVmIdOnHostDevice(any(HostDeviceId.class), any(Guid.class));
        } else {
            verify(hostDeviceDao, times(numOfVfWithVmId)).setVmIdOnHostDevice(hostDeviceIdCaptor.capture(),
                    vmIdCaptor.capture());

            List<HostDeviceId> capturedDeviceIds = hostDeviceIdCaptor.getAllValues();
            List<Guid> capturedVmIds = vmIdCaptor.getAllValues();

            for (HostDevice vf : vfs) {
                assertTrue(capturedDeviceIds.contains(vf.getId()));
            }

            for (HostDevice hostDevice : otherDeviceWithVmId) {
                assertFalse(capturedDeviceIds.contains(hostDevice.getId()));
            }

            for (Guid capturedVmId : capturedVmIds) {
                assertEquals(null, capturedVmId);
            }
        }
    }

    private HostDevice createHostDevice(Guid vmId) {
        HostDevice hostDevice = new HostDevice();
        hostDevice.setHostId(HOST_ID);
        hostDevice.setVmId(vmId);
        return hostDevice;
    }
}
