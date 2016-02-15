/*
* Copyright (c) 2014 Red Hat, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ovirt.engine.api.restapi.types.openstack;

import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.api.model.AgentConfiguration;
import org.ovirt.engine.api.model.MessageBrokerType;
import org.ovirt.engine.api.model.NetworkPluginType;
import org.ovirt.engine.api.model.OpenStackNetworkProvider;
import org.ovirt.engine.api.model.Properties;
import org.ovirt.engine.api.model.Property;
import org.ovirt.engine.api.restapi.types.Mapping;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkPluginType;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;

public class OpenStackNetworkProviderMapper {

    @Mapping(from = OpenStackNetworkProvider.class, to = Provider.class)
    public static Provider<OpenstackNetworkProviderProperties> map(OpenStackNetworkProvider model,
            Provider<OpenstackNetworkProviderProperties> template) {
        Provider<OpenstackNetworkProviderProperties> entity =
             template != null? template: new Provider<OpenstackNetworkProviderProperties>();
        entity.setType(ProviderType.OPENSTACK_NETWORK);
        if (model.isSetId()) {
            entity.setId(GuidUtils.asGuid(model.getId()));
        }
        if (model.isSetName()) {
            entity.setName(model.getName());
        }
        if (model.isSetDescription()) {
            entity.setDescription(model.getDescription());
        }
        if (model.isSetUrl()) {
            entity.setUrl(model.getUrl());
        }
        if (model.isSetRequiresAuthentication()) {
            entity.setRequiringAuthentication(model.isRequiresAuthentication());
        }
        if (model.isSetUsername()) {
            entity.setUsername(model.getUsername());
        }
        if (model.isSetPassword()) {
            entity.setPassword(model.getPassword());
        }
        if (model.isSetAuthenticationUrl()) {
            entity.setAuthUrl(model.getAuthenticationUrl());
        }
        if (model.isSetProperties()) {
            Map<String, String> map = new HashMap<>();
            for (Property property : model.getProperties().getProperties()) {
                map.put(property.getName(), property.getValue());
            }
            entity.setCustomProperties(map);
        }
        OpenstackNetworkProviderProperties additionalProperties = new OpenstackNetworkProviderProperties();
        if (model.isSetTenantName()) {
            additionalProperties.setTenantName(model.getTenantName());
        }
        if (model.isSetPluginType()) {
            additionalProperties.setPluginType(map(NetworkPluginType.fromValue(model.getPluginType())));
        }
        if (model.isSetAgentConfiguration()) {
            additionalProperties.setAgentConfiguration(map(model.getAgentConfiguration(), null));
        }
        entity.setAdditionalProperties(additionalProperties);
        return entity;
    }

    @Mapping(from = Provider.class, to = OpenStackNetworkProvider.class)
    public static OpenStackNetworkProvider map(Provider<OpenstackNetworkProviderProperties> entity,
            OpenStackNetworkProvider template) {
        OpenStackNetworkProvider model = template != null? template: new OpenStackNetworkProvider();
        if (entity.getId() != null) {
            model.setId(entity.getId().toString());
        }
        if (entity.getName() != null) {
            model.setName(entity.getName());
        }
        if (entity.getDescription() != null) {
            model.setDescription(entity.getDescription());
        }
        if (entity.getUrl() != null) {
            model.setUrl(entity.getUrl());
        }
        if (entity.getAuthUrl() != null) {
            model.setAuthenticationUrl(entity.getAuthUrl());
        }
        model.setRequiresAuthentication(entity.isRequiringAuthentication());
        if (entity.getUsername() != null) {
            model.setUsername(entity.getUsername());
        }
        // The password isn't mapped for security reasons.
        // if (entity.getPassword() != null) {
        //     model.setPassword(entity.getPassword());
        // }
        Map<String, String> customProperties = entity.getCustomProperties();
        if (customProperties != null) {
            Properties properties = new Properties();
            for (Map.Entry<String, String> entry : customProperties.entrySet()) {
                Property property = new Property();
                property.setName(entry.getKey());
                property.setValue(entry.getValue());
                properties.getProperties().add(property);
            }
            model.setProperties(properties);
        }
        OpenstackNetworkProviderProperties additionalProperties = entity.getAdditionalProperties();
        if (additionalProperties != null) {
            if (additionalProperties.getTenantName() != null) {
                model.setTenantName(additionalProperties.getTenantName());
            }
            if (additionalProperties.getPluginType() != null) {
                model.setPluginType(map(additionalProperties.getPluginType()).value());
            }
            if (additionalProperties.getAgentConfiguration() != null) {
                model.setAgentConfiguration(map(additionalProperties.getAgentConfiguration(), null));
            }
        }
        return model;
    }

    @Mapping(from = OpenstackNetworkProviderProperties.AgentConfiguration.class, to = AgentConfiguration.class)
    public static AgentConfiguration map(OpenstackNetworkProviderProperties.AgentConfiguration entity,
            AgentConfiguration template) {
        AgentConfiguration model = template != null? template: new AgentConfiguration();
        if (entity.getNetworkMappings() != null) {
            model.setNetworkMappings(entity.getNetworkMappings());
        }
        OpenstackNetworkProviderProperties.MessagingConfiguration messagingConfiguration =
            entity.getMessagingConfiguration();
        if (messagingConfiguration != null) {
            if (messagingConfiguration.getBrokerType() != null) {
                model.setBrokerType(map(messagingConfiguration.getBrokerType()).value());
            }
            if (messagingConfiguration.getAddress() != null) {
                model.setAddress(messagingConfiguration.getAddress());
            }
            if (messagingConfiguration.getPort() != null) {
                model.setPort(messagingConfiguration.getPort());
            }
            if (messagingConfiguration.getUsername() != null) {
                model.setUsername(messagingConfiguration.getUsername());
            }
            // The password isn't mapped for security reasons.
            // if (messagingConfiguration.getPassword() != null) {
            //     model.setPassword(messagingConfiguration.getPassword());
            // }
        }
        return model;
    }

    @Mapping(from = AgentConfiguration.class, to = OpenstackNetworkProviderProperties.AgentConfiguration.class)
    public static OpenstackNetworkProviderProperties.AgentConfiguration map(AgentConfiguration model,
            OpenstackNetworkProviderProperties.AgentConfiguration template) {
        OpenstackNetworkProviderProperties.AgentConfiguration entity =
            template != null? template: new OpenstackNetworkProviderProperties.AgentConfiguration();
        if (model.isSetNetworkMappings()) {
            entity.setNetworkMappings(model.getNetworkMappings());
        }
        OpenstackNetworkProviderProperties.MessagingConfiguration messagingConfiguration =
            new OpenstackNetworkProviderProperties.MessagingConfiguration();
        if (model.isSetBrokerType()) {
            messagingConfiguration.setBrokerType(map(MessageBrokerType.fromValue(model.getBrokerType())));
        }
        if (model.isSetAddress()) {
            messagingConfiguration.setAddress(model.getAddress());
        }
        if (model.isSetPort()) {
            messagingConfiguration.setPort(model.getPort());
        }
        if (model.isSetUsername()) {
            messagingConfiguration.setUsername(model.getUsername());
        }
        if (model.isSetPassword()) {
            messagingConfiguration.setPassword(model.getPassword());
        }
        entity.setMessagingConfiguration(messagingConfiguration);
        return entity;
    }

    private static String map(NetworkPluginType model) {
        switch (model) {
        case OPEN_VSWITCH:
            return OpenstackNetworkPluginType.OPEN_VSWITCH.name();
        default:
            throw new IllegalArgumentException("Unknown network plugin type \"" + model + "\"");
        }
    }

    private static NetworkPluginType map(String entity) {
        if (OpenstackNetworkPluginType.OPEN_VSWITCH.name().equalsIgnoreCase(entity)) {
            return NetworkPluginType.OPEN_VSWITCH;
        }
        throw new IllegalArgumentException("Unknown network plugin type \"" + entity + "\"");
    }

    private static OpenstackNetworkProviderProperties.BrokerType map(MessageBrokerType model) {
        switch (model) {
        case QPID:
            return OpenstackNetworkProviderProperties.BrokerType.QPID;
        case RABBIT_MQ:
            return OpenstackNetworkProviderProperties.BrokerType.RABBIT_MQ;
        default:
            throw new IllegalArgumentException("Unknown message broker type \"" + model + "\"");
        }
    }

    private static MessageBrokerType map(OpenstackNetworkProviderProperties.BrokerType entity) {
        switch (entity) {
        case QPID:
            return MessageBrokerType.QPID;
        case RABBIT_MQ:
            return MessageBrokerType.RABBIT_MQ;
        default:
            throw new IllegalArgumentException("Unknown message broker type \"" + entity + "\"");
        }
    }
}
