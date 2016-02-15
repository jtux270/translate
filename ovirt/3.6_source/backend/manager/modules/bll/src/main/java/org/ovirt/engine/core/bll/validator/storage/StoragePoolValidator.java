package org.ovirt.engine.core.bll.validator.storage;

import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.StoragePoolIsoMapDao;
import org.ovirt.engine.core.dao.VdsGroupDao;

/**
 * CanDoAction validation methods for storage pool handling
 */
public class StoragePoolValidator {
    private StoragePool storagePool;

    public StoragePoolValidator(StoragePool storagePool) {
        this.storagePool = storagePool;
    }

    protected VdsGroupDao getVdsGroupDao() {
        return DbFacade.getInstance().getVdsGroupDao();
    }

    public StoragePoolIsoMapDao getStoragePoolIsoMapDao() {
        return DbFacade.getInstance().getStoragePoolIsoMapDao();
    }

    public ValidationResult isNotLocalfsWithDefaultCluster() {
        if (storagePool.isLocal() && containsDefaultCluster()) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_WITH_DEFAULT_VDS_GROUP_CANNOT_BE_LOCALFS);
        }
        return ValidationResult.VALID;
    }

    protected boolean containsDefaultCluster() {
        List<VDSGroup> clusters = getVdsGroupDao().getAllForStoragePool(storagePool.getId());
        boolean hasDefaultCluster = false;
        for (VDSGroup cluster : clusters) {
            if (cluster.getId().equals(Config.getValue(ConfigValues.AutoRegistrationDefaultVdsGroupID))) {
                hasDefaultCluster = true;
                break;
            }
        }
        return hasDefaultCluster;
    }

    public ValidationResult isUp() {
        if (storagePool == null || storagePool.getStatus() != StoragePoolStatus.Up) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_IMAGE_REPOSITORY_NOT_FOUND);
        }

        return ValidationResult.VALID;
    }

    public ValidationResult isAnyDomainInProcess() {
        List<StoragePoolIsoMap> poolIsoMaps = getStoragePoolIsoMapDao().getAllForStoragePool(storagePool.getId());

        for (StoragePoolIsoMap domainIsoMap : poolIsoMaps) {
            if (domainIsoMap.getStatus() != null && domainIsoMap.getStatus().isStorageDomainInProcess()) {
                return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2,
                        String.format("$status %1$s", domainIsoMap.getStatus()));
            }
        }

        return ValidationResult.VALID;
    }

    public ValidationResult checkStoragePoolNotInStatus(StoragePoolStatus... statuses) {
        List<StoragePoolStatus> statusList = Arrays.asList(statuses);
        if (!statusList.contains(storagePool.getStatus())) {
            return new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_STATUS_ILLEGAL);
        }
        return ValidationResult.VALID;
    }
}
