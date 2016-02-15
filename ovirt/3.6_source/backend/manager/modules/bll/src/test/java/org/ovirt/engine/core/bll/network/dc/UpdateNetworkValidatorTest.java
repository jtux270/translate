package org.ovirt.engine.core.bll.network.dc;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.failsWith;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.isValid;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.network.dc.UpdateNetworkCommand.UpdateNetworkValidator;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.ProviderNetwork;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.VdsGroupDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.network.VmNetworkInterfaceDao;
import org.ovirt.engine.core.utils.MockConfigRule;

@RunWith(MockitoJUnitRunner.class)
public class UpdateNetworkValidatorTest {

    private static final Guid NETWORK_ID = Guid.newGuid();
    private static final Guid CLUSTER_ID = Guid.newGuid();
    private static final Guid VM_ID = Guid.newGuid();

    @Mock
    private Network network;
    @Mock
    private VmNetworkInterfaceDao vmNetworkInterfaceDao;
    @Mock
    private VdsGroupDao vdsGroupDao;
    @Mock
    private VM vm;
    @Mock
    private DbFacade dbFacade;
    @Mock
    private VmDao vmDao;
    @Mock
    private VDSGroup cluster;
    @Mock
    private VmNetworkInterface vNic;

    private UpdateNetworkValidator validator;

    @Rule
    public MockConfigRule mockConfigRule = new MockConfigRule();

    @Before
    public void setup() {
        mockConfigRule.mockConfigValue(ConfigValues.ChangeNetworkUnderBridgeInUseSupported, Version.v3_5, false);
        mockConfigRule.mockConfigValue(ConfigValues.ChangeNetworkUnderBridgeInUseSupported, Version.v3_6, true);

        validator = new UpdateNetworkValidator(network, vmNetworkInterfaceDao, vdsGroupDao, vmDao);
    }

    private Network mockExternalNetwork() {
        Network externalNetwork = mock(Network.class);
        ProviderNetwork providerNetwork = mock(ProviderNetwork.class);
        when(network.getProvidedBy()).thenReturn(providerNetwork);
        when(externalNetwork.getProvidedBy()).thenReturn(providerNetwork);

        return externalNetwork;
    }

    @Test
    public void externalNetworkNameChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();

        when(externalNetwork.getName()).thenReturn("aaa");
        when(network.getName()).thenReturn("bbb");

        assertThat(validator.externalNetworkDetailsUnchanged(externalNetwork), isValid());
    }

    @Test
    public void externalNetworkDescriptionChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();

        when(externalNetwork.getDescription()).thenReturn("aaa");
        when(network.getDescription()).thenReturn("bbb");

        assertThat(validator.externalNetworkDetailsUnchanged(externalNetwork), isValid());
    }

    private void assertThatExternalNetworkDetailsUnchangedFails(Network externalNetwork) {
        assertThat(validator.externalNetworkDetailsUnchanged(externalNetwork),
                failsWith((EngineMessage.ACTION_TYPE_FAILED_EXTERNAL_NETWORK_DETAILS_CANNOT_BE_EDITED)));
    }

    @Test
    public void externalNetworkMtuChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();

        when(externalNetwork.getMtu()).thenReturn(0);
        when(network.getMtu()).thenReturn(1);

        assertThatExternalNetworkDetailsUnchangedFails(externalNetwork);
    }

    @Test
    public void externalNetworkStpChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();

        boolean stp = true;
        when(externalNetwork.getStp()).thenReturn(stp);
        when(network.getStp()).thenReturn(!stp);

        assertThatExternalNetworkDetailsUnchangedFails(externalNetwork);
    }

    @Test
    public void externalNetworkVlanIdChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();

        when(externalNetwork.getVlanId()).thenReturn(0);
        when(network.getVlanId()).thenReturn(1);

        assertThatExternalNetworkDetailsUnchangedFails(externalNetwork);
    }

    @Test
    public void externalNetworkVmNetworkChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();

        boolean vmNetwork = true;
        when(externalNetwork.isVmNetwork()).thenReturn(vmNetwork);
        when(network.isVmNetwork()).thenReturn(!vmNetwork);

        assertThatExternalNetworkDetailsUnchangedFails(externalNetwork);
    }

    @Test
    public void externalNetworkProvidedByChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();
        when(externalNetwork.getProvidedBy()).thenReturn(mock(ProviderNetwork.class));

        assertThatExternalNetworkDetailsUnchangedFails(externalNetwork);
    }

    @Test
    public void internalNetworkProvidedByChanged() throws Exception {
        Network externalNetwork = mockExternalNetwork();
        when(network.getProvidedBy()).thenReturn(null);

        assertThatExternalNetworkDetailsUnchangedFails(externalNetwork);
    }

    @Test
    public void networkChangeUnderRunningVmNegative() {
        prepareMocksForNetworkChangeUnderRunningVm(true, Version.v3_5);
        assertThat(validator.networkNotUsedByRunningVms(),
                failsWith((EngineMessage.ACTION_TYPE_FAILED_NETWORK_IN_ONE_USE)));
    }

    @Test
    public void networkChangeUnderRunningVmPositive() {
        prepareMocksForNetworkChangeUnderRunningVm(true, Version.v3_6);
        assertThat(validator.networkNotUsedByRunningVms(), isValid());
    }

    @Test
    public void networkChangeUnderNotRunningVmPositive() {
        prepareMocksForNetworkChangeUnderRunningVm(false, Version.v3_5);
        assertThat(validator.networkNotUsedByRunningVms(), isValid());
    }

    private void prepareMocksForNetworkChangeUnderRunningVm(boolean isVmRunning, Version clusterVersion) {
        when(network.getId()).thenReturn(NETWORK_ID);
        when(vmNetworkInterfaceDao.getAllForNetwork(NETWORK_ID)).thenReturn(Collections.singletonList(vNic));
        when(vNic.getVmId()).thenReturn(VM_ID);
        when(vNic.isPlugged()).thenReturn(true);
        when(vmDao.getAllForNetwork(NETWORK_ID)).thenReturn(Collections.singletonList(vm));
        when(vm.getId()).thenReturn(VM_ID);
        when(vm.isRunningOrPaused()).thenReturn(isVmRunning);
        when(vm.getVdsGroupId()).thenReturn(CLUSTER_ID);
        when(vdsGroupDao.get(CLUSTER_ID)).thenReturn(cluster);
        when(cluster.getCompatibilityVersion()).thenReturn(clusterVersion);
    }
}
