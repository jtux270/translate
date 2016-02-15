package org.ovirt.engine.core.common.businessentities;

import java.io.Serializable;

import javax.validation.constraints.Min;

import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;

/**
 * <code>QuotaStorage</code> is a business entity that reflects storage limitation.
 */
public class QuotaStorage extends IVdcQueryable implements Serializable {

    public static final Long UNLIMITED = -1L;
    /**
     * Automatic generated serial version ID.
     */
    private static final long serialVersionUID = 2131698554756838711L;

    /**
     * The quota storage Id.
     */
    private Guid quotaStorageId;

    /**
     * The quota Id.
     */
    private Guid quotaId;

    /**
     * The storage Id which this limitation is enforced on.
     */
    private Guid storageId;

    /**
     * Transient field indicates the storage name.
     */
    private String storageName;

    /**
     * The storage limitation indicated in gigabytes.
     */
    @Min(-1)
    private Long storageLimitGigaByte;

    /**
     * Transient field indicates the storage usage of the storage.
     */
    private Double storageLimitGigaByteUsage;

    public QuotaStorage() {
    }

    public QuotaStorage(Guid quotaStorageId,
            Guid quotaId,
            Guid storageId,
            Long storageLimitGigaByte,
            Double storageLimitGigaByteUsage) {
        this.quotaStorageId = quotaStorageId;
        this.quotaId = quotaId;
        this.storageId = storageId;
        this.setStorageSizeGB(storageLimitGigaByte);
        this.storageLimitGigaByteUsage = storageLimitGigaByteUsage;
    }

    public QuotaStorage(QuotaStorage quotaStorage) {
        this.quotaStorageId = quotaStorage.getQuotaStorageId();
        this.quotaId = quotaStorage.getQuotaId();
        this.storageId = quotaStorage.getStorageId();
        this.setStorageSizeGB(quotaStorage.getStorageSizeGB());
        this.storageLimitGigaByteUsage = quotaStorage.getStorageSizeGBUsage();
    }

    /**
     * @return the storageId
     */
    public Guid getStorageId() {
        return storageId;
    }

    /**
     * @param storageId
     *            the storageId to set
     */
    public void setStorageId(Guid storageId) {
        this.storageId = storageId;
    }

    /**
     * @return the quotaId
     */
    public Guid getQuotaId() {
        return quotaId;
    }

    /**
     * @param quotaId
     *            the quotaId to set
     */
    public void setQuotaId(Guid quotaId) {
        this.quotaId = quotaId;
    }

    /**
     * @return the quotaStorageId
     */
    public Guid getQuotaStorageId() {
        return quotaStorageId;
    }

    /**
     * @param quotaStorageId
     *            the quotaStorageId to set
     */
    public void setQuotaStorageId(Guid quotaStorageId) {
        this.quotaStorageId = quotaStorageId;
    }

    /**
     * @return the storageLimitGigaByteUsage
     */
    public Double getStorageSizeGBUsage() {
        return storageLimitGigaByteUsage;
    }

    /**
     * @param storageLimitGigaByteUsage
     *            the storageLimitGigaByteUsage to set
     */
    public void setStorageSizeGBUsage(Double storageLimitGigaByteUsage) {
        this.storageLimitGigaByteUsage = storageLimitGigaByteUsage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((quotaId == null) ? 0 : quotaId.hashCode());
        result = prime * result + ((quotaStorageId == null) ? 0 : quotaStorageId.hashCode());
        result = prime * result + ((storageId == null) ? 0 : storageId.hashCode());
        result = prime * result + ((storageLimitGigaByte == null) ? 0 : storageLimitGigaByte.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        QuotaStorage other = (QuotaStorage) obj;
        return (ObjectUtils.objectsEqual(quotaId, other.quotaId)
                && ObjectUtils.objectsEqual(quotaStorageId, other.quotaStorageId)
                && ObjectUtils.objectsEqual(storageId, other.storageId)
                && ObjectUtils.objectsEqual(storageLimitGigaByteUsage, other.storageLimitGigaByteUsage)
                && ObjectUtils.objectsEqual(storageLimitGigaByte, other.storageLimitGigaByte));
    }

    /**
     * @return the storageLimitGigaByte
     */
    public Long getStorageSizeGB() {
        return storageLimitGigaByte;
    }

    /**
     * @param storageLimitGigaByte
     *            the storageLimitGigaByte to set
     */
    public void setStorageSizeGB(Long storageLimitGigaByte) {
        this.storageLimitGigaByte = storageLimitGigaByte;
    }

    /**
     * @return the storageName
     */
    public String getStorageName() {
        return storageName;
    }

    /**
     * @param storageName
     *            the storageName to set
     */
    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    @Override
    public Object getQueryableId() {
        return getQuotaStorageId();
    }
}
