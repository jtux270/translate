package org.ovirt.engine.core.vdsbroker.gluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServer;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.gluster.GlusterDBUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.vdsbroker.irsbroker.StatusReturnForXmlRpc;

/**
 * The XmlRpc return type to receive a list of gluster volumes. The constructor takes cluster id as well, so that
 * correct host can be identified when populating the bricks of a volume
 */
public final class GlusterVolumesListReturnForXmlRpc extends StatusReturnForXmlRpc {
    private static final String VOLUMES = "volumes";
    private static final String VOLUME_NAME = "volumeName";
    private static final String UUID = "uuid";
    private static final String VOLUME_TYPE = "volumeType";
    private static final String TRANSPORT_TYPE = "transportType";
    private static final String VOLUME_STATUS = "volumeStatus";
    private static final String BRICKS = "bricks";
    private static final String OPTIONS = "options";
    private static final String VOLUME_STATUS_ONLINE = "ONLINE";
    private static final String REPLICA_COUNT = "replicaCount";
    private static final String STRIPE_COUNT = "stripeCount";
    private static final String BRICKS_INFO = "bricksInfo"; //contains brick name and server uuid
    private static final String NAME = "name";
    private static final String HOST_UUID = "hostUuid";

    private Guid clusterId;
    private final Map<Guid, GlusterVolumeEntity> volumes = new HashMap<Guid, GlusterVolumeEntity>();
    private static final Log log = LogFactory.getLog(GlusterVolumesListReturnForXmlRpc.class);
    private static final GlusterDBUtils dbUtils = GlusterDBUtils.getInstance();

    @SuppressWarnings("unchecked")
    public GlusterVolumesListReturnForXmlRpc(Guid clusterId, Map<String, Object> innerMap) {
        super(innerMap);
        this.clusterId = clusterId;

        if(mStatus.mCode != 0) {
            return;
        }

        Map<String, Object> volumesMap = (Map<String, Object>) innerMap.get(VOLUMES);

        for (Entry<String, Object> entry : volumesMap.entrySet()) {
            log.debugFormat("received volume {0}", entry.getKey());

            GlusterVolumeEntity volume = getVolume((Map<String, Object>)entry.getValue());
            volumes.put(volume.getId(), volume);
        }
    }

    @SuppressWarnings({ "unchecked", "incomplete-switch" })
    private GlusterVolumeEntity getVolume(Map<String, Object> map) {
        GlusterVolumeEntity volume = new GlusterVolumeEntity();

        volume.setClusterId(clusterId);
        volume.setId(Guid.createGuidFromStringDefaultEmpty((String)map.get(UUID)));
        volume.setName((String)map.get(VOLUME_NAME));
        volume.setVolumeType((String)map.get(VOLUME_TYPE));

        if (volume.getVolumeType() !=null) {
            if (volume.getVolumeType().isReplicatedType()) {
                volume.setReplicaCount(Integer.valueOf((String) map.get(REPLICA_COUNT)));
            }
            if (volume.getVolumeType().isStripedType()) {
                volume.setStripeCount(Integer.valueOf((String) map.get(STRIPE_COUNT)));
            }
        }

        for(Object transportType : (Object[])map.get(TRANSPORT_TYPE)) {
            volume.addTransportType(TransportType.valueOf((String)transportType));
        }

        String volStatus = (String)map.get(VOLUME_STATUS);
        if(volStatus.toUpperCase().equals(VOLUME_STATUS_ONLINE)) {
            volume.setStatus(GlusterStatus.UP);
        } else {
            volume.setStatus(GlusterStatus.DOWN);
        }

        try {
            if (map.get(BRICKS_INFO) != null && ((Object[])map.get(BRICKS_INFO)).length > 0) {
                volume.setBricks(getBricks(volume.getId(), (Object[])map.get(BRICKS_INFO), true));
            } else {
                volume.setBricks(getBricks(volume.getId(), (Object[])map.get(BRICKS), false));
            }
        } catch (Exception e) {
            log.errorFormat("Could not populate bricks of volume {0} on cluster {1}.", volume.getName(), clusterId, e);
        }
        volume.setOptions(getOptions((Map<String, Object>)map.get(OPTIONS)));

        return volume;
    }

    private Map<String, String> getOptions(Map<String, Object> map) {
        Map<String, String> options = new HashMap<String, String>();
        for(Entry<String, Object> entry : map.entrySet()) {
            options.put(entry.getKey(), (String)entry.getValue());
        }
        return options;
    }

    /**
     * Gets list of bricks of the volume from given list of brick representations. This can return null in certain cases
     * of failure e.g. if the brick representation contains an ip address which is mapped to more than servers in the
     * database.
     *
     * @param volumeId
     * @param brickList
     * @return
     * @throws Exception
     */
    private List<GlusterBrickEntity> getBricks(Guid volumeId, Object[] brickList, boolean withUuid) throws Exception {
        List<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();

        int brickOrder = 0;

        try {
            if (withUuid) {
                for (Object brick : brickList) {
                    bricks.add(getBrick(clusterId, volumeId, (Map<String, Object>) brick, brickOrder++));
                }
            } else {
                for (Object brick : brickList) {
                    bricks.add(getBrick(clusterId, volumeId, (String) brick, brickOrder++));
                }
            }
        } catch (Exception e) {
            // We do not want the command to fail if bricks for one of the volumes could not be fetched. Hence log the
            // exception and return null. The client should have special handling if bricks list of any of the volumes
            // is null.
            log.errorFormat("Error while populating bricks of volume {0}.", volumeId, e);
            return null;
        }

        return bricks;
    }

    /**
     * Returns a brick object for given cluster and brick representation of the form hostnameOrIp:brickDir
     * @param clusterId ID of the Cluster to which the brick belongs
     * @param volumeId ID of the Volume to which the brick belongs
     * @param brickInfo brick representation of the form hostnameOrIp:brickDir
     * @param brickOrder Order number of the brick
     * @return The brick object if representation passed is valid
     */
    private GlusterBrickEntity getBrick(Guid clusterId, Guid volumeId, String brickInfo, int brickOrder) {
        String[] brickParts = brickInfo.split(":", -1);
        if (brickParts.length != 2) {
            throw new RuntimeException("Invalid brick representation [" + brickInfo + "]");
        }

        String hostnameOrIp = brickParts[0];
        String brickDir = brickParts[1];

        VdsStatic server = dbUtils.getServer(clusterId, hostnameOrIp);
        if (server == null) {
            log.warnFormat("Could not add brick {0} to volume {1} - server {2} not found in cluster {3}", brickInfo, volumeId, hostnameOrIp, clusterId);
            return null;
        }

        return getBrickEntity(clusterId, volumeId, brickOrder, server, brickDir);
    }

    private GlusterBrickEntity getBrick(Guid clusterId, Guid volumeId, Map<String, Object> brickInfoMap, int brickOrder) {
        String brickName = (String) brickInfoMap.get(NAME);

        String[] brickParts = brickName.split(":", -1);
        if (brickParts.length != 2) {
            throw new RuntimeException("Invalid brick representation [" + brickName + "]");
        }

        String hostUuid = (String) brickInfoMap.get(HOST_UUID);
        String brickDir = brickParts[1];

        GlusterServer glusterServer = dbUtils.getServerByUuid(Guid.createGuidFromString(hostUuid));
        if (glusterServer == null) {
            log.warnFormat("Could not add brick {0} to volume {1} - server uuid {2} not found in cluster {3}", brickName, volumeId, hostUuid, clusterId);
            return null;
        }
        VdsStatic server = DbFacade.getInstance().getVdsStaticDao().get(glusterServer.getId());
        return getBrickEntity(clusterId, volumeId, brickOrder, server, brickDir);
    }

    private GlusterBrickEntity getBrickEntity(Guid clusterId,
            Guid volumeId,
            int brickOrder,
            VdsStatic server,
            String brickDir) {
        GlusterBrickEntity brick = new GlusterBrickEntity();
        brick.setVolumeId(volumeId);
        brick.setBrickOrder(brickOrder);
        brick.setBrickDirectory(brickDir);

        brick.setServerId(server.getId());
        brick.setServerName(server.getHostName());

        return brick;
    }

    public Map<Guid, GlusterVolumeEntity> getVolumes() {
        return volumes;
    }
}
