package org.ovirt.engine.core.bll.network.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.HostDevice;
import org.ovirt.engine.core.common.businessentities.network.HostNicVfsConfig;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.HostDeviceDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.utils.RandomUtils;

@RunWith(MockitoJUnitRunner.class)
public class VfSchedulerImplTest {

    @Mock
    private NetworkDeviceHelper networkDeviceHelper;

    @Mock
    private InterfaceDao interfaceDao;

    @Mock
    private HostDeviceDao hostDeviceDao;

    @Mock
    private NetworkDao networkDao;

    @Mock
    private Guid vmId;

    @Mock
    private Guid hostId;

    private VfSchedulerImpl vfScheduler;

    private Map<Guid, String> expectedVnicToVfMap;

    @Before
    public void setUp() {
        vfScheduler = new VfSchedulerImpl(networkDao, interfaceDao, hostDeviceDao, networkDeviceHelper);
        expectedVnicToVfMap = new HashMap<>();
    }

    @Test
    public void hostNotHaveSriovNics() {
        when(networkDeviceHelper.getHostNicVfsConfigsWithNumVfsDataByHostId(hostId)).thenReturn(new ArrayList<HostNicVfsConfig>());
        VmNetworkInterface vnic = mockVnic(true);
        assertHostNotValid(Collections.singletonList(vnic), Collections.singletonList(vnic.getName()));
    }

    @Test
    public void hostNicNotHaveVfsOnSriovNic() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 0, true, false, false, false);

        assertHostNotValid(Collections.singletonList(vnic), Collections.singletonList(vnic.getName()));
    }

    @Test
    public void hostNicNotHaveNetworkInSriovConfig() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 5, false, false, false, true);

        assertHostNotValid(Collections.singletonList(vnic), Collections.singletonList(vnic.getName()));
    }

    @Test
    public void validNetworkInSriovConfig() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 5, false, true, false, true);

        assertHostValid(Collections.singletonList(vnic));
    }

    @Test
    public void validLabelInSriovConfig() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 1, false, false, true, true);

        assertHostValid(Collections.singletonList(vnic));
    }

    @Test
    public void validAllNetworksAllowed() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 5, true, false, false, true);

        assertHostValid(Collections.singletonList(vnic));
    }

    @Test
    public void hostNicNotHaveFreeVfs() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 8, true, false, false, false);

        assertHostNotValid(Collections.singletonList(vnic), Collections.singletonList(vnic.getName()));
    }

    @Test
    public void hostNicHaveOneFreeVfWhichShareIommuGroup() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 1, true, false, false, true, true);

        assertHostNotValid(Collections.singletonList(vnic), Collections.singletonList(vnic.getName()));
    }

    @Test
    public void multipleVnicsValid() {
        multipleVnicCommonTest(true);
    }

    @Test
    public void multipleVnicsNotValid() {
        multipleVnicCommonTest(false);
    }

    private void multipleVnicCommonTest(boolean allNicsValid) {
        VmNetworkInterface vnic1 = mockVnic(true, "net1");
        VmNetworkInterface vnic2 = mockVnic(true, "net2");
        VmNetworkInterface vnic3 = mockVnic(false);
        VmNetworkInterface vnic4 = mockVnic(false);

        HostNicVfsConfig hostNicVfsConfig1 = new HostNicVfsConfig();
        HostDevice vf = updateVfsConfig(hostNicVfsConfig1, vnic1, 1, true, false, false, true);

        HostNicVfsConfig hostNicVfsConfig2 = new HostNicVfsConfig();
        updateVfsConfig(hostNicVfsConfig2, vnic2, 1, false, allNicsValid, false, allNicsValid);

        when(networkDeviceHelper.getFreeVf(eq(getNic(hostNicVfsConfig1)), isNull(List.class))).thenReturn(vf);
        when(networkDeviceHelper.getFreeVf(eq(getNic(hostNicVfsConfig1)),
                eq(Collections.singletonList(vf.getDeviceName())))).thenReturn(null);

        mockVfsConfigsOnHost(Arrays.asList(hostNicVfsConfig1, hostNicVfsConfig2));

        List<VmNetworkInterface> allVnics = Arrays.asList(vnic1, vnic2, vnic3, vnic4);

        if (allNicsValid) {
            assertHostValid(allVnics);
        } else {
            assertHostNotValid(allVnics, Collections.singletonList(vnic2.getName()));
        }
    }

    @Test
    public void multipleVfsConfigsFirstValid() {
        multipleVfsConfigsCommon(true);
    }

    @Test
    public void multipleVfsConfigsLastValid() {
        multipleVfsConfigsCommon(false);
    }

    private void multipleVfsConfigsCommon(boolean firstValid) {
        VmNetworkInterface vnic = mockVnic(true);

        HostNicVfsConfig hostNicVfsConfig1 = new HostNicVfsConfig();
        updateVfsConfig(hostNicVfsConfig1, vnic, 1, firstValid, false, false, firstValid);

        HostNicVfsConfig hostNicVfsConfig2 = new HostNicVfsConfig();
        updateVfsConfig(hostNicVfsConfig2, vnic, 1, !firstValid, false, false, !firstValid);

        mockVfsConfigsOnHost(Arrays.asList(hostNicVfsConfig1, hostNicVfsConfig2));

        assertHostValid(Collections.singletonList(vnic));
    }

    @Test
    public void cleanVmDataTest() {
        VmNetworkInterface vnic = mockVnic(true);
        initHostWithOneVfsConfig(Collections.singletonList(vnic), 1, true, false, false, true);

        assertHostValid(Collections.singletonList(vnic));

        vfScheduler.cleanVmData(vmId);
        assertNull(vfScheduler.getVnicToVfMap(vmId, hostId));
    }

    private void initHostWithOneVfsConfig(List<VmNetworkInterface> passthroughVnics,
            int numOfVfs,
            boolean allNetworksAllowed,
            boolean networkInSriovConfig,
            boolean labelInSriovConfig,
            boolean hasFreeVf,
            boolean freeVfShareIommuGroup) {
        HostNicVfsConfig hostNicVfsConfig = new HostNicVfsConfig();
        for (VmNetworkInterface vnic : passthroughVnics) {
            updateVfsConfig(hostNicVfsConfig,
                    vnic,
                    numOfVfs,
                    allNetworksAllowed,
                    networkInSriovConfig,
                    labelInSriovConfig,
                    hasFreeVf,
                    freeVfShareIommuGroup);
        }
        mockVfsConfigsOnHost(Collections.singletonList(hostNicVfsConfig));
    }

    private void initHostWithOneVfsConfig(List<VmNetworkInterface> passthroughVnics,
            int numOfVfs,
            boolean allNetworksAllowed,
            boolean networkInSriovConfig,
            boolean labelInSriovConfig,
            boolean hasFreeVf) {
        initHostWithOneVfsConfig(passthroughVnics,
                numOfVfs,
                allNetworksAllowed,
                networkInSriovConfig,
                labelInSriovConfig,
                hasFreeVf,
                false);
    }

    private void validateVnics(List<VmNetworkInterface> vnics, List<String> excetedProblematicVnics) {
        List<String> problematicVnics =
                vfScheduler.validatePassthroughVnics(vmId, hostId, vnics);
        assertEquals(excetedProblematicVnics, problematicVnics);
    }

    private VmNetworkInterface mockVnic(boolean passthrough, String networkName) {
        VmNetworkInterface vnic = mock(VmNetworkInterface.class);
        when(vnic.getId()).thenReturn(Guid.newGuid());
        when(vnic.getName()).thenReturn(getRandomString());
        when(vnic.isPassthrough()).thenReturn(passthrough);
        Network network = createNetwork(networkName);
        when(vnic.getNetworkName()).thenReturn(network.getName());
        return vnic;
    }

    private VmNetworkInterface mockVnic(boolean passthrough) {
        return mockVnic(passthrough, getRandomString());
    }

    private void assertHostNotValid(List<VmNetworkInterface> vnics, List<String> exceptedProblematicVnics) {
        validateVnics(vnics, exceptedProblematicVnics);
        assertEquals(expectedVnicToVfMap, vfScheduler.getVnicToVfMap(vmId, hostId));
    }

    private void assertHostValid(List<VmNetworkInterface> vnics) {
        validateVnics(vnics, new ArrayList<String>());
        assertEquals(expectedVnicToVfMap, vfScheduler.getVnicToVfMap(vmId, hostId));
    }

    private HostDevice updateVfsConfig(HostNicVfsConfig hostNicVfsConfig, VmNetworkInterface vnic,
            int numOfVfs,
            boolean allNetworksAllowed,
            boolean vnicNetworkInSriovConfig,
            boolean vnicLabelInSriovConfig,
            boolean hasFreeVf,
            boolean freeVfShareIommuGroup) {
        hostNicVfsConfig.setNicId(Guid.newGuid());
        hostNicVfsConfig.setNumOfVfs(numOfVfs);
        hostNicVfsConfig.setAllNetworksAllowed(allNetworksAllowed);
        updateVfsConfigNetworks(hostNicVfsConfig, vnic, vnicNetworkInSriovConfig);
        updateVfsConfigLabels(hostNicVfsConfig, vnic, vnicLabelInSriovConfig);

        VdsNetworkInterface nic = new VdsNetworkInterface();
        nic.setId(hostNicVfsConfig.getNicId());
        when(getNic(hostNicVfsConfig)).thenReturn(nic);

        HostDevice vf = null;
        if (hasFreeVf) {
            vf = createFreeVf(vnic, hostNicVfsConfig);
            mockVfShareIommuGroup(vf, freeVfShareIommuGroup);

            if (!freeVfShareIommuGroup && (allNetworksAllowed || vnicNetworkInSriovConfig || vnicLabelInSriovConfig)) {
                expectedVnicToVfMap.put(vnic.getId(), vf.getDeviceName());
            }
        }

        return vf;
    }

    private HostDevice updateVfsConfig(HostNicVfsConfig hostNicVfsConfig, VmNetworkInterface vnic,
            int numOfVfs,
            boolean allNetworksAllowed,
            boolean vnicNetworkInSriovConfig,
            boolean vnicLabelInSriovConfig,
            boolean hasFreeVf) {
        return updateVfsConfig(hostNicVfsConfig,
                vnic,
                numOfVfs,
                allNetworksAllowed,
                vnicNetworkInSriovConfig,
                vnicLabelInSriovConfig,
                hasFreeVf,
                false);
    }

    private void mockVfShareIommuGroup(HostDevice vf, boolean share) {
        vf.setIommuGroup(RandomUtils.instance().nextInt());
        List<HostDevice> devices = new ArrayList<>();
        devices.add(vf);

        if (share) {
            HostDevice extraIommuDevice = new HostDevice();
            extraIommuDevice.setHostId(vf.getHostId());
            extraIommuDevice.setIommuGroup(vf.getIommuGroup());
            devices.add(extraIommuDevice);
        }
        when(hostDeviceDao.getHostDevicesByHostIdAndIommuGroup(vf.getHostId(), vf.getIommuGroup())).thenReturn(devices);
    }

    private void updateVfsConfigLabels(HostNicVfsConfig hostNicVfsConfig,
            VmNetworkInterface vnic,
            boolean vnicLabelInSriovConfig) {
        if (hostNicVfsConfig.getNetworkLabels() == null) {
            hostNicVfsConfig.setNetworkLabels(new HashSet<String>());
        }

        if (vnicLabelInSriovConfig) {
            hostNicVfsConfig.getNetworkLabels().add(getVnicNetwork(vnic).getLabel());
        }
    }

    private void updateVfsConfigNetworks(HostNicVfsConfig hostNicVfsConfig,
            VmNetworkInterface vnic,
            boolean vnicNetworkInSriovConfig) {
        if (hostNicVfsConfig.getNetworks() == null) {
            hostNicVfsConfig.setNetworks(new HashSet<Guid>());
        }

        if (vnicNetworkInSriovConfig) {
            hostNicVfsConfig.getNetworks().add(getVnicNetwork(vnic).getId());
        }
    }

    private Network createNetwork(String networkName) {
        Network network = new Network();
        network.setId(Guid.newGuid());
        network.setName(networkName);
        network.setLabel(getRandomString());

        when(networkDao.getByName(network.getName())).thenReturn(network);

        return network;
    }

    private Network getVnicNetwork(VmNetworkInterface vnic) {
        return networkDao.getByName(vnic.getNetworkName());
    }

    private HostDevice createVf() {
        HostDevice hostDevice = new HostDevice();
        hostDevice.setHostId(hostId);
        hostDevice.setDeviceName(getRandomString());
        return hostDevice;
    }

    private HostDevice createFreeVf(VmNetworkInterface vnic,
            HostNicVfsConfig hostNicVfsConfig) {
        HostDevice vf = createVf();
        when(networkDeviceHelper.getFreeVf(eq(getNic(hostNicVfsConfig)), anyListOf(String.class))).thenReturn(vf);
        return vf;
    }

    private void mockVfsConfigsOnHost(List<HostNicVfsConfig> vfsConfigs) {
        when(networkDeviceHelper.getHostNicVfsConfigsWithNumVfsDataByHostId(hostId)).thenReturn(vfsConfigs);
    }

    private VdsNetworkInterface getNic(HostNicVfsConfig hostNicVfsConfig) {
        return interfaceDao.get(hostNicVfsConfig.getNicId());
    }

    private String getRandomString() {
        return RandomStringUtils.random(10);
    }
}
