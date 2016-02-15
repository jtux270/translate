package org.ovirt.engine.core.dao.scheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.scheduling.ClusterPolicy;
import org.ovirt.engine.core.common.scheduling.PolicyUnit;
import org.ovirt.engine.core.common.scheduling.PolicyUnitType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.DefaultGenericDaoDbFacade;
import org.ovirt.engine.core.utils.SerializationFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class ClusterPolicyDaoImpl extends DefaultGenericDaoDbFacade<ClusterPolicy, Guid> implements ClusterPolicyDao {

    public ClusterPolicyDaoImpl() {
        super("ClusterPolicy");
    }

    @Override
    public void save(ClusterPolicy clusterPolicy) {
        super.save(clusterPolicy);
        List<ClusterPolicyUnit> clusterPolicyUnits = getclusterPolicyUnit(clusterPolicy);
        if (clusterPolicyUnits != null) {
            for (ClusterPolicyUnit clusterPolicyUnit : clusterPolicyUnits) {
                saveClusterPolicyUnit(clusterPolicyUnit);
            }
        }
    }

    @Override
    public void update(ClusterPolicy clusterPolicy) {
        super.update(clusterPolicy);
        getCallsHandler().executeModification("DeleteClusterPolicyUnitsByClusterPolicyId",
                getCustomMapSqlParameterSource().addValue("id", clusterPolicy.getId()));
        List<ClusterPolicyUnit> clusterPolicyUnits = getclusterPolicyUnit(clusterPolicy);
        if (clusterPolicyUnits != null) {
            for (ClusterPolicyUnit clusterPolicyUnit : clusterPolicyUnits) {
                saveClusterPolicyUnit(clusterPolicyUnit);
            }
        }
    }

    @Override
    public List<ClusterPolicy> getAll() {
        List<ClusterPolicy> clusterPolicies = super.getAll();
        Map<Guid, ClusterPolicy> map = new HashMap<Guid, ClusterPolicy>();
        for (ClusterPolicy clusterPolicy : clusterPolicies) {
            map.put(clusterPolicy.getId(), clusterPolicy);
        }
        List<ClusterPolicyUnit> clusterPolicyUnits =
                getCallsHandler().executeReadList("GetAllFromClusterPolicyUnits",
                createClusterPolicyUnitRowMapper(),
                getCustomMapSqlParameterSource());
        fillClusterPolicy(map, clusterPolicyUnits);
        return clusterPolicies;
    }

    @Override
    public ClusterPolicy get(Guid id) {
        ClusterPolicy clusterPolicy = super.get(id);
        if (clusterPolicy == null) {
            return null;
        }
        List<ClusterPolicyUnit> clusterPolicyUnits =
                getCallsHandler().executeReadList("GetClusterPolicyUnitsByClusterPolicyId",
                createClusterPolicyUnitRowMapper(),
                        createIdParameterMapper(id));
        Map<Guid, ClusterPolicy> map = new HashMap<Guid, ClusterPolicy>();
        map.put(clusterPolicy.getId(), clusterPolicy);
        fillClusterPolicy(map, clusterPolicyUnits);
        return clusterPolicy;
    }

    private void fillClusterPolicy(Map<Guid, ClusterPolicy> map, List<ClusterPolicyUnit> clusterPolicyUnits) {
        Map<Guid, PolicyUnit> policyUnitMap = new HashMap<Guid, PolicyUnit>();
        for (PolicyUnit policyUnit : dbFacade.getPolicyUnitDao().getAll()) {
            policyUnitMap.put(policyUnit.getId(), policyUnit);
        }
        for (ClusterPolicyUnit clusterPolicyUnit : clusterPolicyUnits) {
            ClusterPolicy clusterPolicy = map.get(clusterPolicyUnit.getClusterPolicyId());
            if (policyUnitMap.get(clusterPolicyUnit.getPolicyUnitId()).getPolicyUnitType() == PolicyUnitType.FILTER) {
                if (clusterPolicy.getFilters() == null) {
                    clusterPolicy.setFilters(new ArrayList<Guid>());
                }
                clusterPolicy.getFilters().add(clusterPolicyUnit.getPolicyUnitId());
                if (clusterPolicyUnit.getFilterSequence() != 0) {
                    if (clusterPolicy.getFilterPositionMap() == null) {
                        clusterPolicy.setFilterPositionMap(new HashMap<Guid, Integer>());
                    }
                    clusterPolicy.getFilterPositionMap().put(clusterPolicyUnit.getPolicyUnitId(),
                            clusterPolicyUnit.getFilterSequence());
                }
            }
            if (policyUnitMap.get(clusterPolicyUnit.getPolicyUnitId()).getPolicyUnitType() == PolicyUnitType.WEIGHT) {
                if(clusterPolicy.getFunctions() == null){
                    clusterPolicy.setFunctions(new ArrayList<Pair<Guid, Integer>>());
                }
                clusterPolicy.getFunctions().add(new Pair<Guid, Integer>(clusterPolicyUnit.getPolicyUnitId(),
                        clusterPolicyUnit.getFactor()));
            }
            if (policyUnitMap.get(clusterPolicyUnit.getPolicyUnitId()).getPolicyUnitType() == PolicyUnitType.LOAD_BALANCING) {
                clusterPolicy.setBalance(clusterPolicyUnit.getPolicyUnitId());
            }
        }
    }

    private List<ClusterPolicyUnit> getclusterPolicyUnit(ClusterPolicy entity) {
        Map<Guid, ClusterPolicyUnit> map = new HashMap<Guid, ClusterPolicyUnit>();
        ClusterPolicyUnit unit;
        if (entity.getFilters() != null) {
            for (Guid policyUnitId : entity.getFilters()) {
                unit = getClusterPolicyUnit(entity, policyUnitId, map);
                if (entity.getFilterPositionMap() != null) {
                    Integer position = entity.getFilterPositionMap().get(policyUnitId);
                    unit.setFilterSequence(position != null ? position : 0);
                }
            }
        }
        if (entity.getFunctions() != null) {
            for (Pair<Guid, Integer> pair : entity.getFunctions()) {
                unit = getClusterPolicyUnit(entity, pair.getFirst(), map);
                unit.setFactor(pair.getSecond());
            }
        }
        if (entity.getBalance() != null) {
            getClusterPolicyUnit(entity, entity.getBalance(), map);
        }
        return new ArrayList<ClusterPolicyUnit>(map.values());
    }

    private void saveClusterPolicyUnit(ClusterPolicyUnit clusterPolicyUnit) {
        getCallsHandler().executeModification("InsertClusterPolicyUnit",
                getClusterPolicyUnitParameterMap(clusterPolicyUnit));
    }

    private MapSqlParameterSource getClusterPolicyUnitParameterMap(ClusterPolicyUnit clusterPolicyUnit) {
        return getCustomMapSqlParameterSource().addValue("cluster_policy_id", clusterPolicyUnit.getClusterPolicyId())
                .addValue("policy_unit_id", clusterPolicyUnit.getPolicyUnitId())
                .addValue("filter_sequence", clusterPolicyUnit.getFilterSequence())
                .addValue("factor", clusterPolicyUnit.getFactor());
    }

    protected RowMapper<ClusterPolicyUnit> createClusterPolicyUnitRowMapper() {
        return new RowMapper<ClusterPolicyUnit>() {
            @Override
            public ClusterPolicyUnit mapRow(ResultSet rs, int arg1) throws SQLException {
                ClusterPolicyUnit unit = new ClusterPolicyUnit();
                unit.setClusterPolicyId(getGuid(rs, "cluster_policy_id"));
                unit.setPolicyUnitId(getGuid(rs, "policy_unit_id"));
                unit.setFilterSequence(rs.getInt("filter_sequence"));
                unit.setFactor(rs.getInt("factor"));
                return unit;
            }
        };
    }

    private ClusterPolicyUnit getClusterPolicyUnit(ClusterPolicy clusterPolicy,
            Guid policyUnitId,
            Map<Guid, ClusterPolicyUnit> map) {
        ClusterPolicyUnit clusterPolicyUnit = map.get(policyUnitId);
        if (clusterPolicyUnit == null) {
            clusterPolicyUnit = new ClusterPolicyUnit(clusterPolicy.getId(), policyUnitId);
            map.put(policyUnitId, clusterPolicyUnit);
        }
        return clusterPolicyUnit;
    }

    @Override
    protected MapSqlParameterSource createFullParametersMapper(ClusterPolicy entity) {
        return createIdParameterMapper(entity.getId())
                .addValue("name", entity.getName())
                .addValue("description", entity.getDescription())
                .addValue("is_locked", entity.isLocked())
                .addValue("is_default", entity.isDefaultPolicy())
                .addValue("custom_properties",
                        SerializationFactory.getSerializer().serialize(entity.getParameterMap()));
    }

    @Override
    protected MapSqlParameterSource createIdParameterMapper(Guid id) {
        return getCustomMapSqlParameterSource().addValue("id", id);
    }

    @Override
    protected RowMapper<ClusterPolicy> createEntityRowMapper() {
        return new RowMapper<ClusterPolicy>() {

            @Override
            public ClusterPolicy mapRow(ResultSet rs, int arg1) throws SQLException {
                ClusterPolicy clusterPolicy = new ClusterPolicy();
                clusterPolicy.setId(getGuid(rs, "id"));
                clusterPolicy.setName(rs.getString("name"));
                clusterPolicy.setDescription(rs.getString("description"));
                clusterPolicy.setLocked(rs.getBoolean("is_locked"));
                clusterPolicy.setDefaultPolicy(rs.getBoolean("is_default"));
                clusterPolicy.setParameterMap(SerializationFactory.getDeserializer()
                        .deserializeOrCreateNew(rs.getString("custom_properties"), LinkedHashMap.class));
                return clusterPolicy;
            }
        };
    }

    /**
     * Helper class for cluster's policy units. this class is hidden from users of this dao. mapped into cluster policy
     * class.
     */
    private static class ClusterPolicyUnit {
        Guid clusterPolicyId;
        Guid policyUnitId;
        int filterSequence;
        int factor;

        public ClusterPolicyUnit() {
        }

        public ClusterPolicyUnit(Guid clusterPolicyId, Guid policyUnitId) {
            this.clusterPolicyId = clusterPolicyId;
            this.policyUnitId = policyUnitId;
        }

        public Guid getClusterPolicyId() {
            return clusterPolicyId;
        }

        public void setClusterPolicyId(Guid clusterPolicyId) {
            this.clusterPolicyId = clusterPolicyId;
        }

        public Guid getPolicyUnitId() {
            return policyUnitId;
        }

        public void setPolicyUnitId(Guid policyUnitId) {
            this.policyUnitId = policyUnitId;
        }

        public int getFilterSequence() {
            return filterSequence;
        }

        public void setFilterSequence(int filterSequence) {
            this.filterSequence = filterSequence;
        }

        public int getFactor() {
            return factor;
        }

        public void setFactor(int factor) {
            this.factor = factor;
        }

    }
}
