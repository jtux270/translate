package org.ovirt.engine.core.bll.gluster;

import static org.mockito.Mockito.doReturn;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.ovirt.engine.core.bll.utils.GlusterGeoRepUtil;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterGeoRepSession;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterStatus;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeSizeInfo;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dao.VdsGroupDao;
import org.ovirt.engine.core.dao.gluster.GlusterGeoRepDao;

public class GeoRepCreateEligibilityBaseTest {

    private Guid MASTER_CLUSTER_ID = Guid.newGuid();

    private Guid SLAVE_CLUSTER_ID = Guid.newGuid();

    private Guid MASTER_VOLUME_ID = Guid.newGuid();

    private Guid SLAVE_VOLUME_1_ID = Guid.newGuid();
    private Guid SLAVE_VOLUME_2_ID = Guid.newGuid();
    private Guid SLAVE_VOLUME_3_ID = Guid.newGuid();
    private Guid SLAVE_VOLUME_4_ID = Guid.newGuid();

    private Version CLUSTER_COMPATIBILITY_VERSION = new Version("3.5");

    public GeoRepCreateEligibilityBaseTest() {
        super();
    }

    protected GlusterVolumeEntity getMasterVolume() {
        return getGlusterVolume(MASTER_VOLUME_ID, MASTER_CLUSTER_ID, GlusterStatus.UP, new GlusterVolumeSizeInfo(10000L, 4000L, 6000L));
    }

    @Before
    public void setupMock(GlusterGeoRepUtil geoRepUtil, GlusterGeoRepDao geoRepDao, VdsGroupDao vdsGroupDao) {
        doReturn(geoRepDao).when(geoRepUtil).getGeoRepDao();
        doReturn(getGeoRepSessions()).when(geoRepDao).getAllSessions();
        doReturn(vdsGroupDao).when(geoRepUtil).getVdsGroupDao();

        doReturn(getVdsGroup(MASTER_CLUSTER_ID, CLUSTER_COMPATIBILITY_VERSION)).when(vdsGroupDao).get(MASTER_CLUSTER_ID);

        doReturn(getVdsGroup(SLAVE_CLUSTER_ID, CLUSTER_COMPATIBILITY_VERSION)).when(vdsGroupDao).get(SLAVE_CLUSTER_ID);
    }

    protected GlusterVolumeEntity getGlusterVolume(Guid volumeId, Guid clusterId, GlusterStatus status, GlusterVolumeSizeInfo sizeInfo) {
        GlusterVolumeEntity volume = new GlusterVolumeEntity();
        volume.setId(volumeId);
        volume.setClusterId(clusterId);
        volume.setStatus(status);
        volume.getAdvancedDetails().setCapacityInfo(sizeInfo);
        volume.setName(volumeId.toString());
        volume.setVolumeType(GlusterVolumeType.DISTRIBUTE);
        return volume;
    }

    protected VDSGroup getVdsGroup(Guid vdsGroupId, Version compatibilityVersion) {
        VDSGroup vdsGroup = new VDSGroup();
        vdsGroup.setId(vdsGroupId);
        vdsGroup.setCompatibilityVersion(compatibilityVersion);
        return vdsGroup;
    }

    protected GlusterGeoRepSession getSession(Guid slaveVolumeId) {
        GlusterGeoRepSession session = new GlusterGeoRepSession();
        session.setId(Guid.newGuid());
        session.setMasterVolumeId(Guid.newGuid());
        session.setSlaveVolumeId(slaveVolumeId);
        return session;
    }

    protected List<GlusterGeoRepSession> getGeoRepSessions() {
        return Collections.singletonList(getSession(SLAVE_VOLUME_2_ID));
    }

    public Guid getMASTER_CLUSTER_ID() {
        return MASTER_CLUSTER_ID;
    }

    public Guid getSLAVE_CLUSTER_ID() {
        return SLAVE_CLUSTER_ID;
    }

    public Guid getMASTER_VOLUME_ID() {
        return MASTER_VOLUME_ID;
    }

    public Guid getSLAVE_VOLUME_1_ID() {
        return SLAVE_VOLUME_1_ID;
    }

    public Guid getSLAVE_VOLUME_2_ID() {
        return SLAVE_VOLUME_2_ID;
    }

    public Guid getSLAVE_VOLUME_3_ID() {
        return SLAVE_VOLUME_3_ID;
    }

    public Guid getSLAVE_VOLUME_4_ID() {
        return SLAVE_VOLUME_4_ID;
    }

    public Version getCLUSTER_COMPATIBILITY_VERSION() {
        return CLUSTER_COMPATIBILITY_VERSION;
    }
}
