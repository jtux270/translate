package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.ImportVmTemplateParameters;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.OvfEntityData;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.image_storage_domain_map;
import org.ovirt.engine.core.common.vdscommands.GetImagesListVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute
public class ImportVmTemplateFromConfigurationCommand<T extends ImportVmTemplateParameters> extends ImportVmTemplateCommand {

    private static final Log log = LogFactory.getLog(ImportVmFromConfigurationCommand.class);
    private OvfEntityData ovfEntityData;
    VmTemplate vmTemplateFromConfiguration;

    protected ImportVmTemplateFromConfigurationCommand(Guid commandId) {
        super(commandId);
    }

    public ImportVmTemplateFromConfigurationCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution);
    }

    @Override
    public Guid getVmTemplateId() {
        if (isImagesAlreadyOnTarget()) {
            return getParameters().getContainerId();
        }
        return super.getVmTemplateId();
    }

    @Override
    protected boolean canDoAction() {
        initVmTemplate();
        ArrayList<DiskImage> disks = new ArrayList(getVmTemplate().getDiskTemplateMap().values());
        setImagesWithStoragePoolId(getStorageDomain().getStoragePoolId(), disks);
        getVmTemplate().setImages(disks);
        if (isImagesAlreadyOnTarget() && !validateUnregisteredEntity(vmTemplateFromConfiguration, ovfEntityData)) {
            return false;
        }
        return super.canDoAction();
    }

    private void initVmTemplate() {
        OvfHelper ovfHelper = new OvfHelper();
        List<OvfEntityData> ovfEntityList =
                getUnregisteredOVFDataDao().getByEntityIdAndStorageDomain(getParameters().getContainerId(),
                        getParameters().getStorageDomainId());
        if (!ovfEntityList.isEmpty()) {
            try {
                // We should get only one entity, since we fetched the entity with a specific Storage Domain
                ovfEntityData = ovfEntityList.get(0);
                vmTemplateFromConfiguration = ovfHelper.readVmTemplateFromOvf(ovfEntityData.getOvfData());
                vmTemplateFromConfiguration.setVdsGroupId(getParameters().getVdsGroupId());
                setVmTemplate(vmTemplateFromConfiguration);
                getParameters().setVmTemplate(vmTemplateFromConfiguration);
                getParameters().setDestDomainId(ovfEntityData.getStorageDomainId());
                getParameters().setSourceDomainId(ovfEntityData.getStorageDomainId());

                // For quota, update disks when required
                if (getParameters().getDiskTemplateMap() != null) {
                    ArrayList imageList = new ArrayList<>(getParameters().getDiskTemplateMap().values());
                    vmTemplateFromConfiguration.setDiskList(imageList);
                    ensureDomainMap(imageList, getParameters().getDestDomainId());
                }
            } catch (OvfReaderException e) {
                log.errorFormat("failed to parse a given ovf configuration: \n" + ovfEntityData.getOvfData(), e);
            }
        }
        setVdsGroupId(getParameters().getVdsGroupId());
        setStoragePoolId(getVdsGroup().getStoragePoolId());
    }

    @Override
    protected ArrayList<DiskImage> getImages() {
        return new ArrayList<>(getParameters().getDiskTemplateMap() != null ?
                getParameters().getDiskTemplateMap().values() : getVmTemplate().getDiskTemplateMap().values());
    }

    @Override
    public void executeCommand() {
        super.executeCommand();
        if (isImagesAlreadyOnTarget()) {
            if (!getImages().isEmpty()) {
                findAndSaveDiskCopies();
            }
            getUnregisteredOVFDataDao().removeEntity(ovfEntityData.getEntityId(), null);
        }
        setActionReturnValue(getVmTemplate().getId());
        setSucceeded(true);
    }

    private void findAndSaveDiskCopies() {
        List<OvfEntityData> ovfEntityDataList =
                getUnregisteredOVFDataDao().getByEntityIdAndStorageDomain(ovfEntityData.getEntityId(), null);
        List<image_storage_domain_map> copiedTemplateDisks = new LinkedList<>();
        for (OvfEntityData ovfEntityDataFetched : ovfEntityDataList) {
            populateDisksCopies(copiedTemplateDisks,
                    getImages(),
                    ovfEntityDataFetched.getStorageDomainId());
        }
        saveImageStorageDomainMapList(copiedTemplateDisks);
    }

    private void populateDisksCopies(List<image_storage_domain_map> copiedTemplateDisks,
            List<DiskImage> originalTemplateImages,
            Guid storageDomainId) {
        List<Guid> imagesContainedInStorageDomain = getImagesGuidFromStorage(storageDomainId, getStoragePoolId());
        for (DiskImage templateDiskImage : originalTemplateImages) {
            if (storageDomainId.equals(templateDiskImage.getStorageIds().get(0))) {
                // The original Storage Domain was already saved. skipping it.
                continue;
            }
            if (imagesContainedInStorageDomain.contains(templateDiskImage.getId())) {
                log.infoFormat("Found a copied image of {0} on Storage Domain id {1}",
                        templateDiskImage.getId(),
                        storageDomainId);
                image_storage_domain_map imageStorageDomainMap =
                        new image_storage_domain_map(templateDiskImage.getImageId(),
                                storageDomainId,
                                templateDiskImage.getQuotaId(),
                                templateDiskImage.getDiskProfileId());
                copiedTemplateDisks.add(imageStorageDomainMap);
            }
        }
    }

    private List<Guid> getImagesGuidFromStorage(Guid storageDomainId, Guid storagePoolId) {
        List<Guid> imagesList = Collections.emptyList();
        try {
            VDSReturnValue imagesListResult = runVdsCommand(VDSCommandType.GetImagesList,
                    new GetImagesListVDSCommandParameters(storageDomainId, storagePoolId));
            if (imagesListResult.getSucceeded()) {
                imagesList = (List<Guid>) imagesListResult.getReturnValue();
            } else {
                log.errorFormat("Unable to get images list for storage domain, can not update copied template disks related to Storage Domain id {0}",
                        storageDomainId);
            }
        } catch (Exception e) {
            log.errorFormat("Unable to get images list for storage domain, can not update copied template disks related to Storage Domain id {0}. error is: {1}",
                    storageDomainId,
                    e);
        }
        return imagesList;
    }

    private void saveImageStorageDomainMapList(final List<image_storage_domain_map> copiedTemplateDisks) {
        if (!copiedTemplateDisks.isEmpty()) {
            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    for (image_storage_domain_map imageStorageDomainMap : copiedTemplateDisks) {
                        getImageStorageDomainMapDao().save(imageStorageDomainMap);
                    }
                    return null;
                }
            });
        }
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.TEMPLATE_IMPORT_FROM_CONFIGURATION_SUCCESS :
                AuditLogType.TEMPLATE_IMPORT_FROM_CONFIGURATION_FAILED;
    }
}
