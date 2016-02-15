package org.ovirt.engine.core.dao.network;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DefaultGenericDaoDbFacade;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class VnicProfileDaoDbFacadeImpl extends DefaultGenericDaoDbFacade<VnicProfile, Guid> implements VnicProfileDao {

    public VnicProfileDaoDbFacadeImpl() {
        super("VnicProfile");
    }

    @Override
    public List<VnicProfile> getAllForNetwork(Guid networkId) {
        return getCallsHandler().executeReadList("GetVnicProfilesByNetworkId",
                VnicProfileRowMapper.INSTANCE,
                getCustomMapSqlParameterSource().addValue("network_id", networkId));
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(VnicProfile profile) {
        return createIdParameterMapper(profile.getId())
                .addValue("name", profile.getName())
                .addValue("network_id", profile.getNetworkId())
                .addValue("network_qos_id", profile.getNetworkQosId())
                .addValue("port_mirroring", profile.isPortMirroring())
                .addValue("description", profile.getDescription())
                .addValue("custom_properties",
                        SerializationFactory.getSerializer().serialize(profile.getCustomProperties()));
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected RowMapper<VnicProfile> createEntityRowMapper() {
        return VnicProfileRowMapper.INSTANCE;
    }

    static abstract class VnicProfileRowMapperBase<T extends VnicProfile> implements RowMapper<T> {

        @Override
        @SuppressWarnings("unchecked")
        public T mapRow(ResultSet rs, int rowNum) throws SQLException {
            T entity = createVnicProfileEntity();
            entity.setId(getGuid(rs, "id"));
            entity.setName(rs.getString("name"));
            entity.setNetworkId(getGuid(rs, "network_id"));
            entity.setNetworkQosId(getGuid(rs, "network_qos_id"));
            entity.setCustomProperties(SerializationFactory.getDeserializer()
                    .deserializeOrCreateNew(rs.getString("custom_properties"), LinkedHashMap.class));
            entity.setPortMirroring(rs.getBoolean("port_mirroring"));
            entity.setDescription(rs.getString("description"));
            return entity;
        }

        abstract protected T createVnicProfileEntity();
    }

    private static class VnicProfileRowMapper extends VnicProfileRowMapperBase<VnicProfile> {

        public static final VnicProfileRowMapper INSTANCE = new VnicProfileRowMapper();

        @Override
        protected VnicProfile createVnicProfileEntity() {
            return new VnicProfile();
        }
    }
}
