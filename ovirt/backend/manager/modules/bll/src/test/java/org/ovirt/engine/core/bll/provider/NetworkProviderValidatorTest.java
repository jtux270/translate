package org.ovirt.engine.core.bll.provider;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.failsWith;
import static org.ovirt.engine.core.bll.validator.ValidationResultMatchers.isValid;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties.AgentConfiguration;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties.MessagingConfiguration;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.utils.RandomUtils;

@RunWith(MockitoJUnitRunner.class)
public class NetworkProviderValidatorTest extends ProviderValidatorTest {

    private static final ProviderType NON_NETWORK_PROVIDER_TYPE = ProviderType.FOREMAN;

    private NetworkProviderValidator validator = new NetworkProviderValidator(provider);

    @Test
    public void validProviderType() {
        when(provider.getType()).thenReturn(ProviderType.OPENSTACK_NETWORK);
        assertThat(validator.providerTypeValid(), isValid());
    }

    @Test
    public void invalidProviderType() {
        when(provider.getType()).thenReturn(NON_NETWORK_PROVIDER_TYPE);
        assertThat(validator.providerTypeValid(), failsWith(VdcBllMessages.ACTION_TYPE_FAILED_PROVIDER_TYPE_MISMATCH));
    }

    @Test
    public void networkMappingsProvidedByParameters() throws Exception {
        assertThat(validator.networkMappingsProvided(RandomUtils.instance().nextString(10)), isValid());
    }

    @Test
    public void networkMappingsProvidedByProvider() throws Exception {
        mockProviderAdditionalProperties();
        when(getProviderAgentConfiguration().getNetworkMappings()).thenReturn(RandomUtils.instance().nextString(10));
        assertThat(validator.networkMappingsProvided(null), isValid());
    }

    @Test
    public void missingNetworkMappings() throws Exception {
        assertThat(validator.networkMappingsProvided(null),
                failsWith(VdcBllMessages.ACTION_TYPE_FAILED_MISSING_NETWORK_MAPPINGS));
    }

    @Test
    public void messagingBrokerProvided() throws Exception {
        mockMessagingBrokerAddress("1.1.1.1");

        assertThat(validator.messagingBrokerProvided(), isValid());
    }

    @Test
    public void missingAgentConfigurationForMessagingBrokerValidation() throws Exception {
        mockProviderAdditionalProperties();
        assertThat(validator.messagingBrokerProvided(),
                failsWith(VdcBllMessages.ACTION_TYPE_FAILED_MISSING_MESSAGING_BROKER_PROPERTIES));
    }

    @Test
    public void missingMessagingConfigurationForMessagingBrokerValidation() throws Exception {
        mockMessagingConfiguration();

        assertThat(validator.messagingBrokerProvided(),
                failsWith(VdcBllMessages.ACTION_TYPE_FAILED_MISSING_MESSAGING_BROKER_PROPERTIES));
    }

    private void mockProviderAdditionalProperties() {
        AgentConfiguration agentConfiguration = mock(AgentConfiguration.class);
        OpenstackNetworkProviderProperties properties = mock(OpenstackNetworkProviderProperties.class);
        when(properties.getAgentConfiguration()).thenReturn(agentConfiguration);
        when(provider.getAdditionalProperties()).thenReturn(properties);
    }

    private void mockMessagingConfiguration() {
        mockProviderAdditionalProperties();
        MessagingConfiguration messagingConfiguration = mock(MessagingConfiguration.class);
        when(getProviderAgentConfiguration().getMessagingConfiguration()).thenReturn(messagingConfiguration);
    }

    private void mockMessagingBrokerAddress(String address) {
        mockMessagingConfiguration();
        when(getProviderAgentConfiguration().getMessagingConfiguration().getAddress()).thenReturn(address);
    }

    private AgentConfiguration getProviderAgentConfiguration() {
        return ((OpenstackNetworkProviderProperties) provider.getAdditionalProperties()).getAgentConfiguration();
    }
}
