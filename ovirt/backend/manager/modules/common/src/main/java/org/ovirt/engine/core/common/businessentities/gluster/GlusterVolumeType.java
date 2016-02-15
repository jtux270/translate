package org.ovirt.engine.core.common.businessentities.gluster;


/**
 * Enum for Gluster Volume types.
 * @see GlusterVolumeEntity
 */
public enum GlusterVolumeType {
    /**
     * Files distributed across all servers of the cluster
     */
    DISTRIBUTE,
    /**
     * Files replicated on every server of the cluster
     */
    REPLICATE,
    /**
     * Files replicated across all servers of a replica set (defined by {@link GlusterVolumeEntity#getReplicaCount()} and
     * brick order), and distributed across all replica sets of the volume
     */
    DISTRIBUTED_REPLICATE,
    /**
     * Files striped across all servers of the cluster
     */
    STRIPE,
    /**
     * Files striped across all servers of a stripe set (defined by {@link GlusterVolumeEntity#getStripeCount()} and
     * brick order), and distributed across all stripe sets of the volume
     */
    DISTRIBUTED_STRIPE,
    /**
     * Files striped across all servers of a stripe set (defined by {@link GlusterVolumeEntity#getStripeCount()} and
     * brick order), and replicated across all replica sets of the volume
     */
    STRIPED_REPLICATE,
    /**
     * Distributes striped data across replicated bricks.
     */
    DISTRIBUTED_STRIPED_REPLICATE;

    public String value() {
        return name().toUpperCase();
    }

    public static GlusterVolumeType fromValue(String v) {
        try {
            return valueOf(v.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isStripedType() {
        return value().contains("STRIPE");
    }

    public boolean isReplicatedType() {
        return value().contains("REPLICATE");
    }

    public boolean isDistributedType() {
        return value().contains("DISTRIBUTE");
    }
}
