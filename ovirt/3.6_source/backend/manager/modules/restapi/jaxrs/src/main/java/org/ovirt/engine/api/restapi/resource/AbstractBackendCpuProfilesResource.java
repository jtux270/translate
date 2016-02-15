package org.ovirt.engine.api.restapi.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.CpuProfile;
import org.ovirt.engine.api.model.CpuProfiles;
import org.ovirt.engine.api.model.DataCenter;
import org.ovirt.engine.api.model.QoS;
import org.ovirt.engine.core.common.action.CpuProfileParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.qos.CpuQos;
import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QosQueryParameterBase;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

public abstract class AbstractBackendCpuProfilesResource
        extends AbstractBackendCollectionResource<CpuProfile, org.ovirt.engine.core.common.businessentities.profiles.CpuProfile> {

    public AbstractBackendCpuProfilesResource(String... subCollections) {
        super(CpuProfile.class,
                org.ovirt.engine.core.common.businessentities.profiles.CpuProfile.class,
                subCollections);
    }

    protected CpuProfiles mapCollection(List<org.ovirt.engine.core.common.businessentities.profiles.CpuProfile> entities) {
        CpuProfiles collection = new CpuProfiles();
        Map<Guid, QoS> qosMap = new HashMap<>();
        for (org.ovirt.engine.core.common.businessentities.profiles.CpuProfile entity : entities) {
            CpuProfile profile = populate(map(entity), entity);
            collection.getCpuProfiles().add(profile);
            if (entity.getQosId() != null) {
                qosMap.put(entity.getQosId(), profile.getQos());
            }
        }

        handleQosDataCenterLinks(qosMap);
        for (CpuProfile cpuProfile : collection.getCpuProfiles()) {
            addLinks(cpuProfile);
        }
        return collection;
    }

    /**
     * used to set qos's href (requires dc id).
     */
    private void handleQosDataCenterLinks(Map<Guid, QoS> qosMap) {
        if (!qosMap.isEmpty()) {
            List<CpuQos> list = getBackendCollection(
                    CpuQos.class,
                    VdcQueryType.GetAllQosByType,
                    new QosQueryParameterBase(null, QosType.CPU));
            for (CpuQos cpuQos : list) {
                QoS qos = qosMap.get(cpuQos.getId());
                if (qos != null) {
                    qos.setDataCenter(new DataCenter());
                    qos.getDataCenter().setId(cpuQos.getStoragePoolId().toString());
                }
            }
        }
    }

    protected Response add(CpuProfile cpuProfile) {
        validateParameters(cpuProfile);
        CpuProfileParameters parameters =
                new CpuProfileParameters();
        org.ovirt.engine.core.common.businessentities.profiles.CpuProfile map = map(cpuProfile);
        parameters.setProfile(map);
        return performCreate(VdcActionType.AddCpuProfile,
                parameters,
                new QueryIdResolver<Guid>(VdcQueryType.GetCpuProfileById, IdQueryParameters.class));
    }

    protected abstract void validateParameters(CpuProfile cpuProfile);

    protected abstract List<org.ovirt.engine.core.common.businessentities.profiles.CpuProfile> getCpuProfilesCollection();

    protected CpuProfiles performList() {
        return mapCollection(getCpuProfilesCollection());
    }
}
