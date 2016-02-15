package org.ovirt.engine.core.dao.scheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.scheduling.PolicyUnitType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DefaultGenericDaoDbFacade;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class PolicyUnitDaoImpl extends DefaultGenericDaoDbFacade<PolicyUnit, Guid> implements PolicyUnitDao {

    public PolicyUnitDaoImpl() {
        super("PolicyUnit");
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(PolicyUnit entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("name", entity.getName())
                .addValue("is_internal", entity.isInternal())
                .addValue("type",
                        entity.getPolicyUnitType() == null ? PolicyUnitType.FILTER.getValue()
                                : entity.getPolicyUnitType()
                                .getValue())
                .addValue("description", entity.getDescription())
                .addValue("custom_properties_regex",
                        SerializationFactory.getSerializer().serialize(entity.getParameterRegExMap()))
                .addValue("enabled", entity.isEnabled());
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected RowMapper<PolicyUnit> createEntityRowMapper() {
        return new RowMapper<PolicyUnit>() {

            @Override
            public PolicyUnit mapRow(ResultSet rs, int arg1) throws SQLException {
                PolicyUnit policyUnit = new PolicyUnit();
                policyUnit.setId(getGuid(rs, "id"));
                policyUnit.setName(rs.getString("name"));
                policyUnit.setInternal(rs.getBoolean("is_internal"));
                policyUnit.setPolicyUnitType(PolicyUnitType.forValue(rs.getInt("type")));
                policyUnit.setDescription(rs.getString("description"));
                policyUnit.setParameterRegExMap(SerializationFactory.getDeserializer()
                        .deserializeOrCreateNew(rs.getString("custom_properties_regex"), LinkedHashMap.class));
                policyUnit.setEnabled(rs.getBoolean("enabled"));
                return policyUnit;
            }
        };
    }

}
