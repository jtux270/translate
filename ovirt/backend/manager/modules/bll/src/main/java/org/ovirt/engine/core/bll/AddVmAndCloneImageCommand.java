package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.bll.network.vm.VnicProfileHelper;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.DiskImagesValidator;
import org.ovirt.engine.core.bll.validator.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.CopyVolumeType;
import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.ImageDbOperationScope;
import org.ovirt.engine.core.common.businessentities.ImageOperation;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

/**
 * This abstract class holds helper methods for concrete command classes that require to add a VM and clone an image in
 * the process
 */
public abstract class AddVmAndCloneImageCommand<T extends VmManagementParametersBase> extends AddVmCommand<T> {

    protected AddVmAndCloneImageCommand(Guid commandId) {
        super(commandId);
    }

    public AddVmAndCloneImageCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean validateIsImagesOnDomains() {
        return true;
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        return null;
    }

    protected void copyDiskImage(
            DiskImage diskImage,
            Guid srcStorageDomainId,
            Guid destStorageDomainId,
            Guid diskProfileId,
            VdcActionType parentCommandType) {
        DiskImage newDiskImage = cloneDiskImage(getVmId(),
                destStorageDomainId,
                Guid.newGuid(),
                Guid.newGuid(),
                diskImage,
                diskProfileId);
        ImagesHandler.setDiskAlias(newDiskImage, getVm());
        MoveOrCopyImageGroupParameters parameters = createCopyParameters(newDiskImage,
                srcStorageDomainId,
                diskImage.getId(),
                diskImage.getImageId(), parentCommandType);
        parameters.setRevertDbOperationScope(ImageDbOperationScope.IMAGE);
        VdcReturnValueBase result = executeChildCopyingCommand(parameters);
        handleCopyResult(diskImage, newDiskImage, result);
    }

    @Override
    protected void removeVmRelatedEntitiesFromDb() {
        removeVmImages();
        super.removeVmRelatedEntitiesFromDb();
    }

    private void removeVmImages() {
        // Remove vm images, in case they were not already removed by child commands
        List<VdcActionParametersBase> imageParams = getParameters().getImagesParameters();
        for (VdcActionParametersBase param : imageParams) {
            DiskImage diskImage = getDiskImageToRemoveByParam((MoveOrCopyImageGroupParameters) param);
            if (diskImage != null) {
                ImagesHandler.removeDiskImage(diskImage, getVmId());
            }
        }
    }

    @Override
    protected Collection<DiskImage> getImagesToCheckDestinationStorageDomains() {
        return getDiskImagesToBeCloned();
    }

    protected MoveOrCopyImageGroupParameters createCopyParameters(DiskImage diskImage,
            Guid srcStorageDomainId,
            Guid srcImageGroupId,
            Guid srcImageId, VdcActionType parentCommandType) {
        MoveOrCopyImageGroupParameters params =
                new MoveOrCopyImageGroupParameters(getVmId(),
                        srcImageGroupId,
                        srcImageId,
                        diskImage.getId(),
                        diskImage.getImageId(),
                        diskImage.getStorageIds().get(0),
                        ImageOperation.Copy);
        params.setAddImageDomainMapping(false);
        params.setCopyVolumeType(CopyVolumeType.LeafVol);
        params.setVolumeFormat(diskImage.getVolumeFormat());
        params.setVolumeType(diskImage.getVolumeType());
        params.setUseCopyCollapse(true);
        params.setSourceDomainId(srcStorageDomainId);
        params.setWipeAfterDelete(diskImage.isWipeAfterDelete());
        params.setEntityInfo(new EntityInfo(VdcObjectType.VM, getVmId()));
        params.setParentParameters(getParameters());
        params.setParentCommand(parentCommandType);
        return params;
    }

    @Override
    protected boolean canDoAction() {
        List<DiskImage> disksToCheck =
                ImagesHandler.filterImageDisks(getDiskDao().getAllForVm(getSourceVmFromDb().getId()), true, false, true);
        DiskImagesValidator diskImagesValidator = new DiskImagesValidator(disksToCheck);
        if (!validate(diskImagesValidator.diskImagesNotLocked())) {
            return false;
        }

        Set<Guid> storageIds = ImagesHandler.getAllStorageIdsForImageIds(disksToCheck);
        MultipleStorageDomainsValidator storageValidator =
                new MultipleStorageDomainsValidator(getStoragePoolId(), storageIds);
        if (!validate(storageValidator.allDomainsExistAndActive())) {
            return false;
        }

        if (!validate(new VmValidator(getSourceVmFromDb()).vmNotLocked())) {
            return false;
        }

        // Run all checks for AddVm, now that it is determined snapshot exists
        if (!super.canDoAction()) {
            return false;
        }

        for (DiskImage diskImage : getDiskImagesToBeCloned()) {
            if (!checkImageConfiguration(diskImage)) {
                return false;
            }
        }

        return true;
    }

    protected boolean checkImageConfiguration(DiskImage diskImage) {
        return ImagesHandler.checkImageConfiguration(destStorages.get(diskInfoDestinationMap.get(diskImage.getId())
                .getStorageIds()
                .get(0))
                .getStorageStaticData(),
                diskImage,
                getReturnValue().getCanDoActionMessages());
    }

    protected DiskImage cloneDiskImage(Guid newVmId,
                                       Guid storageDomainId,
                                       Guid newImageGroupId,
                                       Guid newImageGuid,
                                       DiskImage srcDiskImage,
                                       Guid diskProfileId) {

        DiskImage clonedDiskImage = DiskImage.copyOf(srcDiskImage);
        clonedDiskImage.setImageId(newImageGuid);
        clonedDiskImage.setParentId(Guid.Empty);
        clonedDiskImage.setImageTemplateId(Guid.Empty);
        clonedDiskImage.setVmSnapshotId(getVmSnapshotId());
        clonedDiskImage.setId(newImageGroupId);
        clonedDiskImage.setLastModifiedDate(new Date());
        clonedDiskImage.setvolumeFormat(srcDiskImage.getVolumeFormat());
        clonedDiskImage.setVolumeType(srcDiskImage.getVolumeType());
        ArrayList<Guid> storageIds = new ArrayList<Guid>();
        storageIds.add(storageDomainId);
        clonedDiskImage.setStorageIds(storageIds);
        clonedDiskImage.setDiskProfileId(diskProfileId);

        // If volume information was changed at client , use its volume information.
        // If volume information was not changed at client - use the volume information of the ancestral image
        if (diskInfoDestinationMap != null && diskInfoDestinationMap.containsKey(srcDiskImage.getId())) {
            DiskImage diskImageFromClient = diskInfoDestinationMap.get(srcDiskImage.getId());
            if (volumeInfoChanged(diskImageFromClient, srcDiskImage)) {
                changeVolumeInfo(clonedDiskImage, diskImageFromClient);
            } else {
                DiskImage ancestorDiskImage = getDiskImageDao().getAncestor(srcDiskImage.getImageId());
                changeVolumeInfo(clonedDiskImage, ancestorDiskImage);
            }
        } else {
            DiskImage ancestorDiskImage = getDiskImageDao().getAncestor(srcDiskImage.getImageId());
            changeVolumeInfo(clonedDiskImage, ancestorDiskImage);
        }

        return clonedDiskImage;
    }

    private boolean volumeInfoChanged(DiskImage diskImageFromClient, DiskImage srcDiskImage) {
        return (diskImageFromClient.getVolumeFormat() != srcDiskImage.getVolumeFormat() || diskImageFromClient.getVolumeType() != srcDiskImage.getVolumeType());
    }

    protected void changeVolumeInfo(DiskImage clonedDiskImage, DiskImage diskImageFromClient) {
        clonedDiskImage.setvolumeFormat(diskImageFromClient.getVolumeFormat());
        clonedDiskImage.setVolumeType(diskImageFromClient.getVolumeType());
    }

    /**
     * Handle the result of copying the image
     * @param srcDiskImage
     *            disk image that represents the source image
     * @param copiedDiskImage
     *            disk image that represents the copied image
     * @param result
     *            result of execution of child command
     */
    private void handleCopyResult(DiskImage srcDiskImage, DiskImage copiedDiskImage, VdcReturnValueBase result) {
        // If a copy cannot be made, abort
        if (!result.getSucceeded()) {
            throw new VdcBLLException(VdcBllErrors.VolumeCreationError);
        } else {
            ImagesHandler.addDiskImageWithNoVmDevice(copiedDiskImage);
            getTaskIdList().addAll(result.getInternalVdsmTaskIdList());
            getSrcDiskIdToTargetDiskIdMapping().put(srcDiskImage.getId(), copiedDiskImage.getId());
        }
    }

    /**
     * Executes the child command responsible for the image copying
     * @param parameters
     *            parameters for copy
     * @return
     */
    protected VdcReturnValueBase executeChildCopyingCommand(VdcActionParametersBase parameters) {
        return runInternalActionWithTasksContext(getChildActionType(), parameters);
    }

    @Override
    protected boolean buildAndCheckDestStorageDomains() {
        if (diskInfoDestinationMap.isEmpty()) {
            List<StorageDomain> domains =
                    DbFacade.getInstance()
                            .getStorageDomainDao()
                            .getAllForStoragePool(getStoragePoolId());
            Map<Guid, StorageDomain> storageDomainsMap = new HashMap<Guid, StorageDomain>();
            for (StorageDomain storageDomain : domains) {
                StorageDomainValidator validator = new StorageDomainValidator(storageDomain);
                if (validate(validator.isDomainExistAndActive()) && validate(validator.domainIsValidDestination())) {
                    storageDomainsMap.put(storageDomain.getId(), storageDomain);
                }
            }
            for (Disk disk : getDiskImagesToBeCloned()) {
                DiskImage image = (DiskImage) disk;
                for (Guid storageId : image.getStorageIds()) {
                    if (storageDomainsMap.containsKey(storageId)) {
                        diskInfoDestinationMap.put(image.getId(), image);
                        break;
                    }
                }
            }
            if (getDiskImagesToBeCloned().size() != diskInfoDestinationMap.size()) {
                logErrorOneOrMoreActiveDomainsAreMissing();
                return false;
            }
           List<Guid> storageDomainDest = new ArrayList<Guid>();
            for (DiskImage diskImage : diskInfoDestinationMap.values()) {
                Guid storageDomainId = diskImage.getStorageIds().get(0);
                if (storageDomainDest.contains(storageDomainId)) {
                    destStorages.put(storageDomainId, storageDomainsMap.get(storageDomainId));
                }
                storageDomainDest.add(storageDomainId);
            }
            return true;
        }
        return super.buildAndCheckDestStorageDomains();
    }

    protected boolean validateFreeSpace(StorageDomainValidator storageDomainValidator, List<DiskImage> disksList) {
        for (DiskImage diskImage : disksList) {
            List<DiskImage> snapshots = getAllImageSnapshots(diskImage);
            diskImage.getSnapshots().addAll(snapshots);
        }
        return validate(storageDomainValidator.hasSpaceForClonedDisks(disksList));
    }

    protected List<DiskImage> getAllImageSnapshots(DiskImage diskImage) {
        return ImagesHandler.getAllImageSnapshots(diskImage.getImageId());
    }

    /**
     * Logs error if one or more active domains are missing for disk images
     */
    protected abstract void logErrorOneOrMoreActiveDomainsAreMissing();

    /**
     * Returns collection of DiskImage objects to use for construction of the imageToDestinationDomainMap
     *
     * @return
     */
    protected Collection<DiskImage> getDiskImagesToBeCloned() {
        return getAdjustedDiskImagesFromConfiguration();
    }

    protected abstract Collection<DiskImage> getAdjustedDiskImagesFromConfiguration();

    protected DiskImage getDiskImageToRemoveByParam(MoveOrCopyImageGroupParameters param) {
        Guid imageGroupId = param.getDestImageGroupId();
        Guid imageId = param.getDestinationImageId();
        DiskImage diskImage = new DiskImage();
        diskImage.setId(imageGroupId);
        diskImage.setImageId(imageId);
        return diskImage;
    }

    @Override
    protected void executeVmCommand() {
        super.executeVmCommand();
        setVm(null);
        getVm().setVmtGuid(VmTemplateHandler.BLANK_VM_TEMPLATE_ID);
        getVmStaticDao().update(getVm().getStaticData());
    }

    @Override
    protected boolean checkTemplateImages(List<String> reasons) {
        return true;
    }

    @Override
    protected abstract Guid getStoragePoolIdFromSourceImageContainer();

    @Override
    protected boolean shouldCheckSpaceInStorageDomains() {
        return !getImagesToCheckDestinationStorageDomains().isEmpty();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();
        permissionList.add(new PermissionSubject(getVdsGroupId(),
                VdcObjectType.VdsGroups,
                getActionType().getActionGroup()));
        for (DiskImage disk : getParameters().getDiskInfoDestinationMap().values()) {
            if (disk.getStorageIds() != null && !disk.getStorageIds().isEmpty()) {
                permissionList.add(new PermissionSubject(disk.getStorageIds().get(0),
                        VdcObjectType.Storage, ActionGroup.CREATE_DISK));
            }
        }
        addPermissionSubjectForAdminLevelProperties(permissionList);
        return permissionList;
    }

    @Override
    protected List<VmNic> getVmInterfaces() {
        if (_vmInterfaces == null) {
            _vmInterfaces = Entities.<VmNic, VmNetworkInterface> upcast(getVmFromConfiguration().getInterfaces());
        }
        return _vmInterfaces;
    }

    @Override
    protected void addVmNetwork() {
        VnicProfileHelper vnicProfileHelper =
                new VnicProfileHelper(getVdsGroupId(),
                        getStoragePoolId(),
                        getVdsGroup().getcompatibility_version(),
                        AuditLogType.ADD_VM_FROM_SNAPSHOT_INVALID_INTERFACES);

        for (VmNetworkInterface iface : getVmFromConfiguration().getInterfaces()) {
            vnicProfileHelper.updateNicWithVnicProfileForUser(iface, getCurrentUser());
        }

        vnicProfileHelper.auditInvalidInterfaces(getVmName());
        super.addVmNetwork();
    }

    @Override
    protected boolean addVmImages() {
        int numberOfStartedCopyTasks = 0;
        try {
            if (!getAdjustedDiskImagesFromConfiguration().isEmpty()) {
                lockEntities();
                for (DiskImage diskImage : getAdjustedDiskImagesFromConfiguration()) {
                    // For illegal image check if it was snapshot as illegal (therefore
                    // still exists at DB, or was it erased after snapshot - therefore the
                    // query returned to UI an illegal image)
                    if (diskImage.getImageStatus() == ImageStatus.ILLEGAL) {
                        DiskImage snapshotImageInDb =
                                getDiskImageDao().getSnapshotById(diskImage.getImageId());
                        if (snapshotImageInDb == null) {
                            // If the snapshot diskImage is null, it means the disk was probably
                            // erased after the snapshot was created.
                            // Create a disk to reflect the fact the disk existed during snapshot
                            saveIllegalDisk(diskImage);
                        }
                    } else {// Only legal images can be copied
                        copyDiskImage(diskImage,
                                diskImage.getStorageIds().get(0),
                                diskInfoDestinationMap.get(diskImage.getId()).getStorageIds().get(0),
                                diskInfoDestinationMap.get(diskImage.getId()).getDiskProfileId(),
                                getActionType());
                        numberOfStartedCopyTasks++;
                    }
                }
            }
        } finally {
            // If no tasks were created, endAction will not be called, but
            // it is still needed to unlock the entities
            if (numberOfStartedCopyTasks == 0) {
                unlockEntities();
            }
        }
        return true;
    }

    private void saveIllegalDisk(final DiskImage diskImage) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                // Allocating new IDs for image and disk as it's possible
                // that more than one clone will be made from this source
                // So this is required to avoid PK violation at DB.
                diskImage.setImageId(Guid.newGuid());
                diskImage.setId(Guid.newGuid());
                diskImage.setParentId(Guid.Empty);
                diskImage.setImageTemplateId(Guid.Empty);

                ImagesHandler.setDiskAlias(diskImage, getVm());
                ImagesHandler.addDiskImage(diskImage, getVmId());
                return null;
            }
        });
    }

    @Override
    protected void copyVmDevices() {
        List<VmDevice> devices = new ArrayList<VmDevice>(getVmFromConfiguration().getVmUnamagedDeviceList());
        devices.addAll(getVmFromConfiguration().getManagedVmDeviceMap().values());
        VmDeviceUtils.copyVmDevices(getSourceVmId(),
                getVmId(),
                getVm(),
                getVm().getStaticData(),
                true,
                devices,
                getSrcDeviceIdToTargetDeviceIdMapping(),
                getParameters().isSoundDeviceEnabled(),
                getParameters().isConsoleEnabled(),
                isVirtioScsiEnabled(),
                isBalloonEnabled(),
                false);
    }

    protected abstract VM getVmFromConfiguration();

    protected abstract Guid getSourceVmId();

    @Override
    protected VdcActionType getChildActionType() {
        return VdcActionType.CopyImageGroup;
    }

    protected abstract VM getSourceVmFromDb();

    protected void unlockEntities() {

    }
    protected void lockEntities() {

    }
}
