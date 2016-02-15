package org.ovirt.engine.core.bll.validator;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.failsWith;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.isValid;
import static org.ovirt.engine.core.utils.MockConfigRule.mockConfig;

import java.util.Collection;
import java.util.Collections;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.bll.DbDependentTestBase;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.network.VmInterfaceManager;
import org.ovirt.engine.core.bll.network.cluster.ManagementNetworkUtil;
import org.ovirt.engine.core.common.businessentities.BusinessEntityMap;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.network.IPv4Address;
import org.ovirt.engine.core.common.businessentities.network.IpConfiguration;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkAttachment;
import org.ovirt.engine.core.common.businessentities.network.NetworkBootProtocol;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkClusterId;
import org.ovirt.engine.core.common.businessentities.network.ProviderNetwork;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.VdsDao;
import org.ovirt.engine.core.dao.VmDao;
import org.ovirt.engine.core.dao.network.NetworkClusterDao;
import org.ovirt.engine.core.dao.network.NetworkDao;
import org.ovirt.engine.core.utils.MockConfigRule;
import org.ovirt.engine.core.utils.ReplacementUtils;

@RunWith(MockitoJUnitRunner.class)
public class NetworkAttachmentValidatorTest extends DbDependentTestBase {

    @Mock
    private NetworkDao networkDaoMock;

    @Mock
    private NetworkClusterDao networkClusterDaoMock;

    @Mock
    private VdsDao vdsDaoMock;

    @Mock
    private NetworkValidator networkValidatorMock;

    @Mock
    VmInterfaceManager vmInterfaceManager;

    @Mock
    private ManagementNetworkUtil managementNetworkUtilMock;

    @Mock
    private VmDao vmDao;

    @ClassRule
    public static final MockConfigRule mcr = new MockConfigRule(
        mockConfig(ConfigValues.MultipleGatewaysSupported, Version.v3_5.toString(), false),
        mockConfig(ConfigValues.MultipleGatewaysSupported, Version.v3_6.toString(), true),
        mockConfig(ConfigValues.ChangeNetworkUnderBridgeInUseSupported, Version.v3_5.toString(), false),
        mockConfig(ConfigValues.ChangeNetworkUnderBridgeInUseSupported, Version.v3_6.toString(), true));


    private final VDS host;

    public NetworkAttachmentValidatorTest() {
        host = new VDS();
        host.getStaticData().setName("hostName");
        host.setId(Guid.newGuid());
        host.setVdsGroupId(Guid.newGuid());
    }

    private NetworkAttachmentValidator createNetworkAttachmentValidator(NetworkAttachment attachment) {
        return new NetworkAttachmentValidator(attachment,
            host,
            managementNetworkUtilMock,
            vmInterfaceManager,
            networkClusterDaoMock,
            networkDaoMock,
            vdsDaoMock,
            vmDao);
    }

    @Test
    public void testNetworkAttachmentIsSetWhenAttachmentIsNull() {
        EngineMessage engineMessage = EngineMessage.NETWORK_ATTACHMENT_NOT_EXISTS;
        assertThat(createNetworkAttachmentValidator(null).networkAttachmentIsSet(),
            failsWith(engineMessage,
                ReplacementUtils.getVariableAssignmentString(engineMessage, "null")));
    }

    @Test
    public void testNetworkAttachmentIsSetWhenAttachmentIsNotNull() {
        assertThat(createNetworkAttachmentValidator(new NetworkAttachment()).networkAttachmentIsSet(),
                isValid());
    }

    /* tests whether validation is properly delegated. NetworkAttachmentValidator#networkExists
    delegates to NetworkValidator#networkIsSet. This test spies on creation of NetworkValidator, and returns mocked
    implementation which returns failing ValidationResult on NetworkValidator#networkIsSet. Finally it's tested, whether
    this ValidationResult was propagated correctly.
     */
    @Test
    public void testNetworkExistsWhenValidationFails() {
        NetworkAttachmentValidator networkAttachmentValidatorSpy = Mockito.spy(
            createNetworkAttachmentValidator(new NetworkAttachment()));

        doReturn(networkValidatorMock).when(networkAttachmentValidatorSpy).getNetworkValidator();

        String variableReplacements = "a";
        ValidationResult propagatedResult = new ValidationResult(EngineMessage.NETWORK_NOT_EXISTS,
            variableReplacements);
        when(networkValidatorMock.networkIsSet()).thenReturn(propagatedResult);

        assertThat("ValidationResult is not correctly propagated",
                networkAttachmentValidatorSpy.networkExists(),
            failsWith(EngineMessage.NETWORK_NOT_EXISTS, variableReplacements));
    }

    /* tests whether validation is properly delegated. NetworkAttachmentValidator#networkExists
    delegates to NetworkValidator#networkIsSet. This test spies on creation of NetworkValidator, and returns mocked
    implementation which returns valid ValidationResult on NetworkValidator#networkIsSet. Finally it's tested, whether
    this ValidationResult was propagated correctly.
     */
    @Test
    public void testNetworkExists() {
        NetworkAttachmentValidator networkAttachmentValidatorSpy = Mockito.spy(
            createNetworkAttachmentValidator(new NetworkAttachment()));

        doReturn(networkValidatorMock).when(networkAttachmentValidatorSpy).getNetworkValidator();
        when(networkValidatorMock.networkIsSet()).thenReturn(ValidationResult.VALID);

        assertThat("ValidationResult is not correctly propagated",
            networkAttachmentValidatorSpy.networkExists(), isValid());
    }

    @Test
    public void testNotExternalNetworkWhenExternalNetworkIsProvided() {
        Network externalNetwork = new Network();
        externalNetwork.setId(Guid.newGuid());
        externalNetwork.setProvidedBy(new ProviderNetwork(Guid.newGuid(), ""));

        when(networkDaoMock.get(eq(externalNetwork.getId()))).thenReturn(externalNetwork);

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(externalNetwork.getId());
        assertThat(createNetworkAttachmentValidator(attachment).notExternalNetwork(),
            failsWith(EngineMessage.EXTERNAL_NETWORK_CANNOT_BE_PROVISIONED));
    }

    @Test
    public void testNotExternalNetwork() {
        Network notExternalNetwork = new Network();
        notExternalNetwork.setId(Guid.newGuid());
        notExternalNetwork.setProvidedBy(null);

        when(networkDaoMock.get(eq(notExternalNetwork.getId()))).thenReturn(notExternalNetwork);

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(notExternalNetwork.getId());
        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(attachment);
        assertThat(validator.notExternalNetwork(), isValid());
    }

    @Test
    public void testNotRemovingManagementNetwork() {
        NetworkAttachmentValidator networkAttachmentValidatorSpy = Mockito.spy(
            createNetworkAttachmentValidator(new NetworkAttachment()));

        doReturn(networkValidatorMock).when(networkAttachmentValidatorSpy).getNetworkValidator();

        ValidationResult propagatedResult =
            new ValidationResult(EngineMessage.NETWORK_CANNOT_REMOVE_MANAGEMENT_NETWORK, "a");
        when(networkValidatorMock.notRemovingManagementNetwork()).thenReturn(propagatedResult);

        assertThat("ValidationResult is not correctly propagated",
                networkAttachmentValidatorSpy.notRemovingManagementNetwork(),
            failsWith(EngineMessage.NETWORK_CANNOT_REMOVE_MANAGEMENT_NETWORK, "a"));
    }

    @Test
    public void testNetworkAttachedToClusterWhenAttached() {
        Network network = new Network();
        network.setId(Guid.newGuid());

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(network.getId());

        NetworkClusterId networkClusterId = new NetworkClusterId(host.getVdsGroupId(), attachment.getNetworkId());
        when(networkClusterDaoMock.get(eq(networkClusterId))).thenReturn(new NetworkCluster());
        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);

        assertThat(createNetworkAttachmentValidator(attachment).networkAttachedToCluster(), isValid());
    }

    @Test
    public void testNetworkAttachedToClusterWhenNotAttached() {
        Network network = new Network();
        network.setId(Guid.newGuid());

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(network.getId());

        NetworkClusterId networkClusterId = new NetworkClusterId(host.getVdsGroupId(), network.getId());
        when(networkClusterDaoMock.get(eq(networkClusterId))).thenReturn(null);
        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);

        assertThat(createNetworkAttachmentValidator(attachment).networkAttachedToCluster(),
            failsWith(EngineMessage.NETWORK_NOT_EXISTS_IN_CLUSTER));
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNull() {
        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setIpConfiguration(null);

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(attachment);
        assertThat(validator.ipConfiguredForStaticBootProtocol(), isValid());
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsDhcp() {
        doTestIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullForBootProtocol(NetworkBootProtocol.DHCP);
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsNone() {
        doTestIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullForBootProtocol(NetworkBootProtocol.NONE);
    }

    private void doTestIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullForBootProtocol(
        NetworkBootProtocol bootProtocol) {
        NetworkAttachment networkAttachmentWithIpConfiguration =
            createNetworkAttachmentWithIpConfiguration(bootProtocol, null, null);

        NetworkAttachmentValidator networkAttachmentValidator =
            createNetworkAttachmentValidator(networkAttachmentWithIpConfiguration);
        assertThat(networkAttachmentValidator.ipConfiguredForStaticBootProtocol(), isValid());
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsStaticAndAddressIsNull() {
        doTestIpConfiguredForStaticBootProtocol(null, "255.255.255.0");
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsStaticAndAddressIsEmpty() {
        doTestIpConfiguredForStaticBootProtocol("", "255.255.255.0");
    }

    private void doTestIpConfiguredForStaticBootProtocol(String address, String netmask) {
        Matcher<ValidationResult> matcher = failsWith(EngineMessage.NETWORK_ADDR_MANDATORY_IN_STATIC_IP);

        doTestIpConfiguredForStaticBootProtocol(address, netmask, matcher);
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsStaticAndNetmaskIsNull() {
        doTestIpConfiguredForStaticBootProtocol("192.168.1.1", null);
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsStaticAndNetmaskIsEmpty() {
        doTestIpConfiguredForStaticBootProtocol("192.168.1.1", "");
    }

    @Test
    public void testIpConfiguredForStaticBootProtocolWhenIpConfigurationIsNotNullAndBootProtocolIsStaticAndAddressAndNetmaskIsNotNull() {
        doTestIpConfiguredForStaticBootProtocol("192.168.1.1", "255.255.255.0", isValid());
    }

    private void doTestIpConfiguredForStaticBootProtocol(String address,
        String netmask,
        Matcher<ValidationResult> matcher) {
        NetworkAttachment attachment =
            createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.STATIC_IP, address, netmask);

        assertThat(createNetworkAttachmentValidator(attachment).ipConfiguredForStaticBootProtocol(), matcher);
    }

    private NetworkAttachment createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol bootProtocol,
            String address,
            String netmask) {

        IpConfiguration ipConfiguration = new IpConfiguration();
        IPv4Address primaryAddress = new IPv4Address();
        primaryAddress.setAddress(address);
        primaryAddress.setNetmask(netmask);
        primaryAddress.setBootProtocol(bootProtocol);
        ipConfiguration.getIPv4Addresses().add(primaryAddress);

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setIpConfiguration(ipConfiguration);

        return attachment;
    }

    @Test
    public void testBootProtocolSetForRoleNetworkWhenNullValuedIpConfigurationAndWhenNetworkHasNoRole() {
        doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(false,
                createNetwork(), isValid());
    }

    @Test
    public void testBootProtocolSetForRoleNetworkWhenIpConfigurationNullAndNotRoleNetwork() {
        Network network = createNetwork();

        doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(
                true,
                network,
                failsWith(EngineMessage.ACTION_TYPE_FAILED_ROLE_NETWORK_HAS_NO_BOOT_PROTOCOL,
                        ReplacementUtils.createSetVariableString(
                                NetworkAttachmentValidator.VAR_ACTION_TYPE_FAILED_ROLE_NETWORK_HAS_NO_BOOT_PROTOCOL_ENTITY,
                                network.getName())));
    }

    private void doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(
            boolean displayNetwork, Network network, Matcher<ValidationResult> matcher) {

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setIpConfiguration(null);
        attachment.setNetworkId(network.getId());

        doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(
                displayNetwork,
                false,
                false,
                matcher,
                network,
                attachment);
    }

    private void doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(
            boolean displayNetwork,
            boolean migrationNetwork,
            boolean glusterNetwork,
            Matcher<ValidationResult> matcher,
            Network network,
            NetworkAttachment attachment) {

        NetworkCluster networkCluster = new NetworkCluster();
        networkCluster.setDisplay(displayNetwork);
        networkCluster.setMigration(migrationNetwork);
        networkCluster.setGluster(glusterNetwork);

        NetworkClusterId networkClusterId = new NetworkClusterId(host.getVdsGroupId(), attachment.getNetworkId());
        when(networkClusterDaoMock.get(eq(networkClusterId))).thenReturn(networkCluster);
        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);

        assertThat(createNetworkAttachmentValidator(attachment).bootProtocolSetForRoleNetwork(), matcher);
    }

    @Test
    public void testBootProtocolSetForRoleNetworkWhenIpConfigurationIsNull() {
        Network network = createNetwork();

        NetworkAttachment attachment =
                createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.NONE, null, null);
        attachment.setNetworkId(network.getId());

        doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(
                true,
                true,
                false,
                failsWith(EngineMessage.ACTION_TYPE_FAILED_ROLE_NETWORK_HAS_NO_BOOT_PROTOCOL,
                        ReplacementUtils.createSetVariableString(
                                NetworkAttachmentValidator.VAR_ACTION_TYPE_FAILED_ROLE_NETWORK_HAS_NO_BOOT_PROTOCOL_ENTITY,
                                network.getName())),
                network,
                attachment);
    }

    private Network createNetwork() {
        Network network = new Network();
        network.setId(Guid.newGuid());
        network.setName("networkName");
        return network;
    }

    @Test
    public void testBootProtocolSetForRoleNetworkWhenBootProtocolIsNone() {
        Network network = createNetwork();

        NetworkAttachment attachment =
            createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.NONE, null, null);
        attachment.setNetworkId(network.getId());


        doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(false,
                true,
                true,
                failsWith(EngineMessage.ACTION_TYPE_FAILED_ROLE_NETWORK_HAS_NO_BOOT_PROTOCOL,
                        ReplacementUtils.createSetVariableString(
                                NetworkAttachmentValidator.VAR_ACTION_TYPE_FAILED_ROLE_NETWORK_HAS_NO_BOOT_PROTOCOL_ENTITY,
                                network.getName())),
                network,
                attachment);
    }

    @Test
    public void testBootProtocolSetForRoleNetworkWhenBootProtocolIsDhcp() {
        Network network = createNetwork();

        NetworkAttachment attachment =
            createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.DHCP, null, null);
        attachment.setNetworkId(network.getId());

        doTestBootProtocolSetForRoleNetworkWhenNullValuedIpConfiguration(true,
                false,
                false,
                isValid(),
                network,
                attachment);
    }

    @Test
    public void testNicExistsWhenNicNameIsNull() {
        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNicName(null);
        assertThat(createNetworkAttachmentValidator(attachment).nicExists(),
                failsWith(EngineMessage.HOST_NETWORK_INTERFACE_NOT_EXIST));
    }

    @Test
    public void testNicExistsWhenNicNameIsNotNull() {
        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNicId(null);
        attachment.setNicName("whatever");

        assertThat(createNetworkAttachmentValidator(attachment).nicExists(), isValid());
    }

    @Test
    public void testNetworkIpAddressWasSameAsHostnameAndChangedWhenIpConfigurationIsNull() {

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setIpConfiguration(null);

        assertThat(createNetworkAttachmentValidator(attachment).networkIpAddressWasSameAsHostnameAndChanged(null),
                isValid());
    }

    @Test
    public void testNetworkIpAddressWasSameAsHostnameAndChangedWhenIpConfigurationIsDhcp() {
        doTestNetworkIpAddressWasSameAsHostnameAndChangedForBootProtocol(NetworkBootProtocol.DHCP);
    }

    @Test
    public void testNetworkIpAddressWasSameAsHostnameAndChangedWhenIpConfigurationIsNone() {
        doTestNetworkIpAddressWasSameAsHostnameAndChangedForBootProtocol(NetworkBootProtocol.NONE);
    }

    private void doTestNetworkIpAddressWasSameAsHostnameAndChangedForBootProtocol(NetworkBootProtocol bootProtocol) {
        NetworkAttachment attachment = createNetworkAttachmentWithIpConfiguration(bootProtocol, null, null);

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(attachment);
        ValidationResult actual = validator.networkIpAddressWasSameAsHostnameAndChanged(null);
        assertThat(actual, isValid());
    }

    @Test
    public void testNetworkIpAddressWasSameAsHostnameAndChangedWhenIfaceDoesNotExist() {

        NetworkAttachment attachment =
                createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.STATIC_IP, null, null);
        attachment.setNicName("nicName");

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(attachment);
        BusinessEntityMap<VdsNetworkInterface> nics =
            new BusinessEntityMap<>(Collections.<VdsNetworkInterface> emptyList());
        assertThat(validator.networkIpAddressWasSameAsHostnameAndChanged(nics), isValid());
    }

    @Test
    public void testNetworkIpAddressWasSameAsHostnameAndChanged() {
        Network network = new Network();
        network.setName("networkName");
        network.setId(Guid.newGuid());


        VdsNetworkInterface existingInterface = new VdsNetworkInterface();
        existingInterface.setName("nicName");
        existingInterface.setAddress("anyAddress");
        existingInterface.setNetworkName(network.getName());

        NetworkAttachment attachment =
            createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.STATIC_IP, null, null);
        attachment.setNicName(existingInterface.getName());
        attachment.setNetworkId(network.getId());

        host.setHostName(existingInterface.getAddress());

        when(networkDaoMock.get(attachment.getNetworkId())).thenReturn(network);

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(attachment);
        BusinessEntityMap<VdsNetworkInterface> nics =
            new BusinessEntityMap<>(Collections.singletonList(existingInterface));
        assertThat(validator.networkIpAddressWasSameAsHostnameAndChanged(nics),
            failsWith(EngineMessage.ACTION_TYPE_FAILED_NETWORK_ADDRESS_CANNOT_BE_CHANGED,
                    ReplacementUtils.createSetVariableString(NetworkAttachmentValidator.VAR_ACTION_TYPE_FAILED_NETWORK_ADDRESS_CANNOT_BE_CHANGED_LIST,
                            existingInterface.getNetworkName())));
    }

    @Test
    public void testNetworkNotChangedWhenOldAttachmentIsNull() {
        assertThat(createNetworkAttachmentValidator(null).networkNotChanged(null), isValid());
    }

    @Test
    public void testNetworkNotChangedWhenDifferentNetworkIds() {
        NetworkAttachment oldAttachment = new NetworkAttachment();
        oldAttachment.setId(Guid.newGuid());
        oldAttachment.setNetworkId(Guid.newGuid());

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(Guid.newGuid());

        assertThat(createNetworkAttachmentValidator(attachment).networkNotChanged(oldAttachment),
            failsWith(EngineMessage.CANNOT_CHANGE_ATTACHED_NETWORK,
                    ReplacementUtils.createSetVariableString(NetworkAttachmentValidator.VAR_NETWORK_ATTACHMENT_ID,
                            oldAttachment.getId())));
    }

    @Test
    public void testNetworkNotChanged() {
        Guid networkId = Guid.newGuid();

        NetworkAttachment oldAttachment = new NetworkAttachment();
        oldAttachment.setNetworkId(networkId);

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(networkId);

        assertThat(createNetworkAttachmentValidator(attachment).networkNotChanged(oldAttachment), isValid());
    }

    @Test
    public void testValidateGateway() {
        host.setVdsGroupCompatibilityVersion(Version.v3_5);
        doTestValidateGateway("someGateway", false, failsWith(EngineMessage.NETWORK_ATTACH_ILLEGAL_GATEWAY));
    }

    @Test
    public void testValidateGatewayWhenIpConfigurationIsNotSet() {
        NetworkAttachment attachment = new NetworkAttachment();
        assertThat(createNetworkAttachmentValidator(attachment).validateGateway(), isValid());
    }

    @Test
    public void testValidateGatewayWhenGatewayIsNotSpecified() {
        doTestValidateGateway("", false, isValid());
    }

    @Test
    public void testValidateGatewayWhenGatewayIsNull() {
        doTestValidateGateway(null, false, isValid());
    }

    @Test
    public void testValidateGatewayWhenRelatedNetworkIsManagementNetwork() {
        doTestValidateGateway("someGateway", true, isValid());
    }

    @Test
    public void testValidateGatewayWhenMultipleGatewayIsSupported() {
        host.setVdsGroupCompatibilityVersion(Version.v3_6);
        doTestValidateGateway("someGateway", false, isValid());

    }

    private void doTestValidateGateway(String gatewayValue,
        boolean managementNetwork,
        Matcher<ValidationResult> resultMatcher) {

        Network network = createNetwork();

        NetworkAttachment attachment = createNetworkAttachmentWithIpConfiguration(NetworkBootProtocol.NONE, null, null);
        attachment.setNetworkId(network.getId());
        attachment.getIpConfiguration().getPrimaryAddress().setGateway(gatewayValue);

        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);
        when(managementNetworkUtilMock.isManagementNetwork(network.getId())).thenReturn(managementNetwork);

        assertThat(createNetworkAttachmentValidator(attachment).validateGateway(), resultMatcher);
    }

    @Test
    public void testNetworkNotAttachedToHost() {
        Network network = createNetwork();

        when(networkDaoMock.get(network.getId())).thenReturn(network);

        // no vds for network id.
        when(vdsDaoMock.getAllForNetwork(eq(network.getId()))).thenReturn(Collections.<VDS> emptyList());

        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(network.getId());
        assertThat(createNetworkAttachmentValidator(attachment).networkNotAttachedToHost(),
            isValid());
    }

    @Test
    public void testNetworkNotAttachedToHostWhenAttached() {
        Network network = createNetwork();

        when(networkDaoMock.get(network.getId())).thenReturn(network);
        when(vdsDaoMock.getAllForNetwork(eq(network.getId()))).thenReturn(Collections.singletonList(host));

        String networkName = "networkName";
        NetworkAttachment attachment = new NetworkAttachment();
        attachment.setNetworkId(network.getId());
        attachment.setNetworkName(networkName);
        assertThat(createNetworkAttachmentValidator(attachment).networkNotAttachedToHost(),
            failsWith(EngineMessage.NETWORK_ALREADY_ATTACHED_TO_HOST,
                    ReplacementUtils.createSetVariableString("networkName", networkName),
                    ReplacementUtils.createSetVariableString("hostName", host.getName())));
    }

    @Test
    public void testNetworkNotUsedByVmsWhenNotUsed() {
        Network network = new Network();
        network.setId(Guid.newGuid());
        network.setName("name");
        host.setVdsGroupCompatibilityVersion(Version.v3_5);

        NetworkAttachment networkAttachment = new NetworkAttachment();
        networkAttachment.setNetworkId(network.getId());

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(networkAttachment);

        when(vmInterfaceManager
            .findActiveVmsUsingNetworks(eq(host.getId()), collectionContainingOneGivenNetworkName(network.getName())))
            .thenReturn(Collections.<String>emptyList());

        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);

        assertThat(validator.networkNotUsedByVms(), isValid());
    }

    @Test
    public void testNetworkNotUsedByVmsWhenUsedChangeNotSupported() {
        Network network = new Network();
        network.setId(Guid.newGuid());
        network.setName("name");

        NetworkAttachment networkAttachment = new NetworkAttachment();
        networkAttachment.setNetworkId(network.getId());

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(networkAttachment);

        when(vmInterfaceManager
            .findActiveVmsUsingNetworks(eq(host.getId()), collectionContainingOneGivenNetworkName(network.getName())))
            .thenReturn(Collections.singletonList("networkName"));

        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);

        host.setVdsGroupCompatibilityVersion(Version.v3_5);

        assertThat(validator.networkNotUsedByVms(), failsWith(EngineMessage.ACTION_TYPE_FAILED_NETWORK_IN_ONE_USE));
    }

    @Test
    public void testNetworkNotUsedByVmsWhenUsedChangeSupported() {
        Network network = new Network();
        network.setId(Guid.newGuid());
        network.setName("name");

        NetworkAttachment networkAttachment = new NetworkAttachment();
        networkAttachment.setNetworkId(network.getId());

        when(networkDaoMock.get(eq(network.getId()))).thenReturn(network);
        host.setVdsGroupCompatibilityVersion(Version.v3_6);

        NetworkAttachmentValidator validator = createNetworkAttachmentValidator(networkAttachment);

        assertThat(validator.networkNotUsedByVms(), isValid());
    }

    private Collection<String> collectionContainingOneGivenNetworkName(final String name) {
        return argThat(new ArgumentMatcher<Collection<String>>() {
            @Override
            public boolean matches(Object argument) {
                //noinspection unchecked
                return ((Collection<String>) argument).contains(name);
            }
        });
    }

}
