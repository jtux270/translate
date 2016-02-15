package org.ovirt.engine.core.vdsbroker.gluster;

import java.util.HashMap;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterSnapshotConfigInfo;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.irsbroker.StatusReturnForXmlRpc;
import org.ovirt.engine.core.vdsbroker.vdsbroker.StatusForXmlRpc;

public class GlusterVolumeSnapshotConfigReturnForXmlRpc extends StatusReturnForXmlRpc {
    private static final String STATUS = "status";
    private static final String SNAPSHOT_CONFIG = "snapshotConfig";
    private static final String SYSTEM_CONFIG = "system";
    private static final String VOLUME_CONFIG = "volume";

    private StatusForXmlRpc status;
    private GlusterSnapshotConfigInfo glusterSnapshotConfigInfo = new GlusterSnapshotConfigInfo();

    public GlusterSnapshotConfigInfo getGlusterSnapshotConfigInfo() {
        return this.glusterSnapshotConfigInfo;
    }

    public GlusterVolumeSnapshotConfigReturnForXmlRpc(Guid clusterId, Map<String, Object> innerMap) {
        super(innerMap);
        status = new StatusForXmlRpc((Map<String, Object>) innerMap.get(STATUS));

        Map<String, Object> configInfo = (Map<String, Object>) innerMap.get(SNAPSHOT_CONFIG);

        Map<String, Object> systemConfig = (Map<String, Object>) configInfo.get(SYSTEM_CONFIG);
        Map<String, String> clusterConfigs = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : systemConfig.entrySet()) {
            String value = (String) entry.getValue();
            clusterConfigs.put(entry.getKey(), value == null ? "" : value);
        }
        glusterSnapshotConfigInfo.setClusterConfigOptions(clusterConfigs);

        glusterSnapshotConfigInfo.setVolumeConfigOptions(parseVolumeConfigDetails((Map<String, Object>) configInfo.get(VOLUME_CONFIG)));
    }

    private Map<String, Map<String, String>> parseVolumeConfigDetails(Map<String, Object> configs) {
        Map<String, Map<String, String>> volumeConfigs =
                new HashMap<String, Map<String, String>>();

        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            Map<String, Object> fetchedVolumeConfig = (Map<String, Object>) entry.getValue();
            Map<String, String> volConfig = new HashMap<String, String>();

            for (Map.Entry<String, Object> config : fetchedVolumeConfig.entrySet()) {
                String value = (String) config.getValue();
                volConfig.put(config.getKey(), value);
            }

            volumeConfigs.put(entry.getKey(), volConfig);
        }

        return volumeConfigs;
    }

    public StatusForXmlRpc getStatus() {
        return status;
    }

    public void setStatus(StatusForXmlRpc status) {
        this.status = status;
    }
}
