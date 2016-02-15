package org.ovirt.engine.core.dao.provider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.OpenStackImageProviderProperties;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties.AgentConfiguration;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.Provider.AdditionalProperties;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacadeUtils;
import org.ovirt.engine.core.dao.DefaultGenericDaoDbFacade;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

public class ProviderDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<Provider<?>, Guid> implements ProviderDao {

    public ProviderDaoDbFacadeImpl() {
        super("Provider");
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(Provider<?> entity) {
        MapSqlParameterSource mapper = createBaseProviderParametersMapper(entity);
        String tenantName = null;
        String pluginType = null;
        AgentConfiguration agentConfiguration = null;

        if (entity.getAdditionalProperties() != null) {
            switch (entity.getType()) {
            case OPENSTACK_NETWORK:
                OpenstackNetworkProviderProperties networkProperties =
                        (OpenstackNetworkProviderProperties) entity.getAdditionalProperties();
                tenantName = networkProperties.getTenantName();
                pluginType = networkProperties.getPluginType();
                agentConfiguration = networkProperties.getAgentConfiguration();
                break;
            case OPENSTACK_IMAGE:
                OpenStackImageProviderProperties imageProperties =
                        (OpenStackImageProviderProperties) entity.getAdditionalProperties();
                tenantName = imageProperties.getTenantName();
                break;
            default:
                break;
            }
        }

        // We always add the values since JdbcTeplate expects them to be set, otherwise it throws an exception.
        mapper.addValue("tenant_name", tenantName);
        mapper.addValue("plugin_type", pluginType);
        mapper.addValue("agent_configuration", SerializationFactory.getSerializer().serialize(agentConfiguration));
        return mapper;
    }

    protected MapSqlParameterSource createBaseProviderParametersMapper(Provider<?> entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("name", entity.getName())
                .addValue("description", entity.getDescription())
                .addValue("url", entity.getUrl())
                .addValue("provider_type", EnumUtils.nameOrNull(entity.getType()))
                .addValue("auth_required", entity.isRequiringAuthentication())
                .addValue("auth_username", entity.getUsername())
                .addValue("auth_password", DbFacadeUtils.encryptPassword(entity.getPassword()))
                .addValue("custom_properties",
                        SerializationFactory.getSerializer().serialize(entity.getCustomProperties()));
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected ParameterizedRowMapper<Provider<?>> createEntityRowMapper() {
        return ProviderRowMapper.INSTANCE;
    }

    @Override
    public Provider<?> getByName(String name) {
        return getCallsHandler().executeRead("GetProviderByName",
                createEntityRowMapper(),
                getCustomMapSqlParameterSource().addValue("name", name));
    }

    private static class ProviderRowMapper implements ParameterizedRowMapper<Provider<?>> {

        public final static ProviderRowMapper INSTANCE = new ProviderRowMapper();

        private ProviderRowMapper() {
        }

        @Override
        @SuppressWarnings("unchecked")
        public Provider<?> mapRow(ResultSet rs, int index) throws SQLException {
            Provider<AdditionalProperties> entity = new Provider<AdditionalProperties>();
            entity.setId(getGuidDefaultEmpty(rs, "id"));
            entity.setName(rs.getString("name"));
            entity.setDescription(rs.getString("description"));
            entity.setUrl(rs.getString("url"));
            entity.setType(ProviderType.valueOf(rs.getString("provider_type")));
            entity.setRequiringAuthentication(rs.getBoolean("auth_required"));
            entity.setUsername(rs.getString("auth_username"));
            entity.setPassword(DbFacadeUtils.decryptPassword(rs.getString("auth_password")));
            entity.setCustomProperties(SerializationFactory.getDeserializer()
                    .deserialize(rs.getString("custom_properties"), HashMap.class));
            entity.setAdditionalProperties(mapAdditionalProperties(rs, entity));

            return entity;
        }

        private AdditionalProperties mapAdditionalProperties(ResultSet rs, Provider<?> entity) throws SQLException {
            switch (entity.getType()) {
            case OPENSTACK_NETWORK:
                OpenstackNetworkProviderProperties networkProperties = new OpenstackNetworkProviderProperties();
                networkProperties.setTenantName(rs.getString("tenant_name"));
                networkProperties.setPluginType(rs.getString("plugin_type"));
                networkProperties.setAgentConfiguration(SerializationFactory.getDeserializer()
                        .deserialize(rs.getString("agent_configuration"), AgentConfiguration.class));
                return networkProperties;
            case OPENSTACK_IMAGE:
                OpenStackImageProviderProperties imageProperties = new OpenStackImageProviderProperties();
                imageProperties.setTenantName(rs.getString("tenant_name"));
                return imageProperties;
            default:
                return null;
            }
        }
    }

    @Override
    public List<Provider<?>> getAllByType(ProviderType providerType) {
        return getCallsHandler().executeReadList("GetAllFromProvidersByType",
                                                 ProviderRowMapper.INSTANCE,
                                                 getCustomMapSqlParameterSource().addValue("provider_type", providerType.toString()));
    }

    public List<Provider<?>> getAllWithQuery(String query) {
        return jdbcTemplate.query(query, ProviderRowMapper.INSTANCE);
    }
}
