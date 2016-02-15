package org.ovirt.engine.ui.uicommonweb.models.gluster;

import java.util.Set;

import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

public class VolumeGeneralModel extends EntityModel {
    private String name;
    private String volumeId;
    private String volumeType;
    private String replicaCount;
    private String stripeCount;
    private String numOfBricks;
    private String glusterMountPoint;
    private String nfsMountPoint;
    private Set<TransportType> transportTypes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
        onPropertyChanged(new PropertyChangedEventArgs("VolumeType")); //$NON-NLS-1$
    }

    public void setVolumeTypeSilently(String volumeType) {
        this.volumeType = volumeType;
    }

    public String getReplicaCount() {
        return replicaCount;
    }

    public void setReplicaCount(String replicaCount) {
        this.replicaCount = replicaCount;
    }

    public String getStripeCount() {
        return stripeCount;
    }

    public void setStripeCount(String stripeCount) {
        this.stripeCount = stripeCount;
    }

    public String getNumOfBricks() {
        return numOfBricks;
    }

    public void setNumOfBricks(String numOfBricks) {
        this.numOfBricks = numOfBricks;
    }

    public String getGlusterMountPoint() {
        return glusterMountPoint;
    }

    public void setGlusterMountPoint(String glusterMountPoint) {
        this.glusterMountPoint = glusterMountPoint;
    }

    public String getNfsMountPoint() {
        return nfsMountPoint;
    }

    public void setNfsMountPoint(String nfsMountPoint) {
        this.nfsMountPoint = nfsMountPoint;
    }

    public VolumeGeneralModel() {
        setTitle(ConstantsManager.getInstance().getConstants().generalTitle());
        setHelpTag(HelpTag.general);
        setHashName("general"); //$NON-NLS-1$
    }

    @Override
    protected void onEntityChanged() {
        super.onEntityChanged();
        updatePropeties();
    }

    private void updatePropeties() {
        if (getEntity() == null) {
            return;
        }
        GlusterVolumeEntity entity = (GlusterVolumeEntity) getEntity();
        setName(entity.getName());
        setVolumeId(entity.getId() != null ? entity.getId().toString() : null);
        setVolumeType(entity.getVolumeType() != null ? entity.getVolumeType().toString() : null);
        setReplicaCount(entity.getReplicaCount() != null ? Integer.toString(entity.getReplicaCount()) : null);
        setStripeCount(entity.getStripeCount() != null ? Integer.toString(entity.getStripeCount()) : null);
        setNumOfBricks(entity.getBricks() != null ? Integer.toString(entity.getBricks().size()) : null);
        setTransportTypes(entity.getTransportTypes());
    }

    public Set<TransportType> getTransportTypes() {
        return transportTypes;
    }

    public void setTransportTypes(Set<TransportType> transportTypes) {
        this.transportTypes = transportTypes;
    }
}
