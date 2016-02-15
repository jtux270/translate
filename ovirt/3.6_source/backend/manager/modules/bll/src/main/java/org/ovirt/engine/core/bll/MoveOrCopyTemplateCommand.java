package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.storage.StorageDomainCommandBase;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.storage.MultipleStorageDomainsValidator;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.MoveOrCopyParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.common.businessentities.OvfEntityData;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.ImageOperation;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.utils.MacAddressValidationPatterns;
import org.ovirt.engine.core.common.vdscommands.GetImagesListVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@Deprecated
public class MoveOrCopyTemplateCommand<T extends MoveOrCopyParameters> extends StorageDomainCommandBase<T> {

    private static final Pattern VALIDATE_MAC_ADDRESS =
            Pattern.compile(MacAddressValidationPatterns.UNICAST_MAC_ADDRESS_FORMAT);

    /**
     * Map which contains the disk id (new generated id if the disk is cloned) and the disk parameters from the export
     * domain.
     */
    protected final Map<Guid, DiskImage> newDiskIdForDisk = new HashMap<>();
    protected Map<Guid, Guid> imageToDestinationDomainMap;
    protected Map<Guid, DiskImage> imageFromSourceDomainMap;
    private List<PermissionSubject> permissionCheckSubject;
    private List<DiskImage> _templateDisks;
    private StorageDomain sourceDomain;
    private Guid sourceDomainId = Guid.Empty;

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected MoveOrCopyTemplateCommand(Guid commandId) {
        super(commandId);
    }

    public MoveOrCopyTemplateCommand(T parameters) {
        this(parameters, null);
    }

    public MoveOrCopyTemplateCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected void init(T parameters) {
        super.init(parameters);
        setVmTemplateId(parameters.getContainerId());
        parameters.setEntityInfo(new EntityInfo(VdcObjectType.VmTemplate, getVmTemplateId()));
        imageToDestinationDomainMap = getParameters().getImageToDestinationDomainMap();
        imageFromSourceDomainMap = new HashMap<>();
    }

    protected StorageDomain getSourceDomain() {
        if (sourceDomain == null && !Guid.Empty.equals(sourceDomainId)) {
            sourceDomain = getStorageDomainDao().getForStoragePool(sourceDomainId, getStoragePool().getId());
        }
        return sourceDomain;
    }

    protected void setSourceDomainId(Guid storageId) {
        sourceDomainId = storageId;
    }

    protected ImageOperation getMoveOrCopyImageOperation() {
        return ImageOperation.Copy;
    }

    protected List<DiskImage> getTemplateDisks() {
        if (_templateDisks == null && getVmTemplate() != null) {
            VmTemplateHandler.updateDisksFromDb(getVmTemplate());
            _templateDisks = getVmTemplate().getDiskList();
        }
        return _templateDisks;
    }

    @Override
    protected boolean canDoAction() {
        boolean retValue = true;
        if (getVmTemplate() == null) {
            retValue = false;
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
        } else if (getTemplateDisks() != null && !getTemplateDisks().isEmpty()) {
            ensureDomainMap(getTemplateDisks(), getParameters().getStorageDomainId());
            // check that images are ok
            ImagesHandler.fillImagesMapBasedOnTemplate(getVmTemplate(),
                    imageFromSourceDomainMap,
                    null);
            if (getVmTemplate().getDiskTemplateMap().values().size() != imageFromSourceDomainMap.size()) {
                log.error("Can not found any default active domain for one of the disks of template with id '{}'",
                        getVmTemplate().getId());
                addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_MISSED_STORAGES_FOR_SOME_DISKS);
                retValue = false;
            }
            retValue = retValue
                    && VmTemplateCommand.isVmTemplateImagesReady(getVmTemplate(), null,
                            getReturnValue().getCanDoActionMessages(), true, true, true, false, getTemplateDisks());
            if (retValue) {
                setStoragePoolId(getVmTemplate().getStoragePoolId());
                StorageDomainValidator sdValidator = createStorageDomainValidator(getStorageDomain());
                retValue = validate(sdValidator.isDomainExistAndActive())
                        && validate(sdValidator.isDomainWithinThresholds())
                        && (getParameters().getForceOverride() || (!isImagesAlreadyOnTarget() && checkIfDisksExist(getTemplateDisks())))
                        && validateFreeSpaceOnDestinationDomain(sdValidator, getTemplateDisks());
            }
            if (retValue
                    && DbFacade.getInstance()
                            .getStoragePoolIsoMapDao()
                            .get(new StoragePoolIsoMapId(getStorageDomain().getId(),
                                    getVmTemplate().getStoragePoolId())) == null) {
                retValue = false;
                addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_STORAGE_POOL_NOT_MATCH);
            }
        }
        return retValue;
    }

    private StorageDomainValidator createStorageDomainValidator(StorageDomain storageDomain) {
        return new StorageDomainValidator(storageDomain);
    }

    protected boolean validateUnregisteredEntity(IVdcQueryable entityFromConfiguration, OvfEntityData ovfEntityData) {
        if (ovfEntityData == null && !getParameters().isImportAsNewEntity()) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_UNSUPPORTED_OVF);
            return false;
        }
        if (entityFromConfiguration == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_OVF_CONFIGURATION_NOT_SUPPORTED);
        }

        for (DiskImage image : getImages()) {
            StorageDomain sd = getStorageDomainDao().getForStoragePool(
                    image.getStorageIds().get(0), getStoragePool().getId());
            if (!validate(new StorageDomainValidator(sd).isDomainExistAndActive())) {
                return false;
            }
        }
        if (!getStorageDomain().getStorageDomainType().isDataDomain()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_TYPE_UNSUPPORTED,
                    String.format("$domainId %1$s", getParameters().getStorageDomainId()),
                    String.format("$domainType %1$s", getStorageDomain().getStorageDomainType()));
        }
        return true;
    }

    protected List<DiskImage> getImages() {
        return null;
    }

    protected boolean isImagesAlreadyOnTarget() {
        return getParameters().isImagesExistOnTargetStorageDomain();
    }

    @Override
    protected void setActionMessageParameters() {
        if (getMoveOrCopyImageOperation() == ImageOperation.Move) {
            addCanDoActionMessage(EngineMessage.VAR__ACTION__MOVE);
        } else {
            addCanDoActionMessage(EngineMessage.VAR__ACTION__COPY);
        }
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM_TEMPLATE);
    }

    private boolean validateFreeSpaceOnDestinationDomain(StorageDomainValidator storageDomainValidator, List<DiskImage> disksList) {
        return validate(storageDomainValidator.hasSpaceForClonedDisks(disksList));
    }

    @Override
    protected void executeCommand() {
    }

    protected void moveOrCopyAllImageGroups() {
        moveOrCopyAllImageGroups(getVmTemplateId(), getTemplateDisks());
    }

    protected void moveOrCopyAllImageGroups(final Guid containerID, final Iterable<DiskImage> disks) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                for (DiskImage disk : disks) {
                    VdcReturnValueBase vdcRetValue = runInternalActionWithTasksContext(
                            getImagesActionType(),
                            buildModeOrCopyImageGroupParameters(containerID, disk));

                    getReturnValue().getVdsmTaskIdList().addAll(vdcRetValue.getInternalVdsmTaskIdList());
                }
                return null;
            }

            private MoveOrCopyImageGroupParameters buildModeOrCopyImageGroupParameters(
                    final Guid containerID, DiskImage disk) {
                MoveOrCopyImageGroupParameters params = new MoveOrCopyImageGroupParameters(
                        containerID, disk.getId(), disk.getImageId(),
                        getParameters().getStorageDomainId(), getMoveOrCopyImageOperation());
                params.setParentCommand(getActionType());
                params.setEntityInfo(getParameters().getEntityInfo());
                params.setAddImageDomainMapping(getMoveOrCopyImageOperation() == ImageOperation.Copy);
                params.setSourceDomainId(imageFromSourceDomainMap.get(disk.getId()).getStorageIds().get(0));
                params.setParentParameters(getParameters());
                return params;
            }
        });
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
        case EXECUTE:
            return getSucceeded() ? (getMoveOrCopyImageOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_TEMPLATE
                    : AuditLogType.USER_COPIED_TEMPLATE
                    : (getMoveOrCopyImageOperation() == ImageOperation.Move) ? AuditLogType.USER_FAILED_MOVE_TEMPLATE
                            : AuditLogType.USER_FAILED_COPY_TEMPLATE;

        case END_SUCCESS:
            return getSucceeded() ? (getMoveOrCopyImageOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_TEMPLATE_FINISHED_SUCCESS
                    : AuditLogType.USER_COPIED_TEMPLATE_FINISHED_SUCCESS
                    : (getMoveOrCopyImageOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_TEMPLATE_FINISHED_FAILURE
                            : AuditLogType.USER_COPIED_TEMPLATE_FINISHED_FAILURE;

        default:
            return (getMoveOrCopyImageOperation() == ImageOperation.Move) ? AuditLogType.USER_MOVED_TEMPLATE_FINISHED_FAILURE
                    : AuditLogType.USER_COPIED_TEMPLATE_FINISHED_FAILURE;
        }
    }

    protected boolean checkIfDisksExist(Iterable<DiskImage> disksList) {
        Map<Guid, List<Guid>> alreadyRetrieved = new HashMap<>();
        for (DiskImage disk : disksList) {
            Guid targetStorageDomainId = imageToDestinationDomainMap.get(disk.getId());
            List<Guid> imagesOnStorageDomain = alreadyRetrieved.get(targetStorageDomainId);

            if (imagesOnStorageDomain == null) {
                VDSReturnValue returnValue = runVdsCommand(
                        VDSCommandType.GetImagesList,
                        new GetImagesListVDSCommandParameters(targetStorageDomainId, getStoragePoolId())
                );

                if (returnValue.getSucceeded()) {
                    imagesOnStorageDomain = (List<Guid>) returnValue.getReturnValue();
                    alreadyRetrieved.put(targetStorageDomainId, imagesOnStorageDomain);
                } else {
                    addCanDoActionMessageVariable("sdName", getStorageDomain(targetStorageDomainId).getName());
                    addCanDoActionMessage(EngineMessage.ERROR_GET_IMAGE_LIST);
                    return false;
                }
            }

            if (imagesOnStorageDomain.contains(disk.getId())) {
                addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_ALREADY_CONTAINS_DISK);
                return false;
            }
        }
        return true;
    }

    protected void endMoveOrCopyCommand() {
        endActionOnAllImageGroups();
        endVmTemplateRelatedOps();
        setSucceeded(true);
    }

    protected final void endVmTemplateRelatedOps() {
        if (getVmTemplate() != null) {
            VmDeviceUtils.setVmDevices(getVmTemplate());
            VmHandler.updateVmInitFromDB(getVmTemplate(), true);
            incrementDbGeneration();
            VmTemplateHandler.unlockVmTemplate(getVmTemplateId());
        }
        else {
            setCommandShouldBeLogged(false);
            log.warn("MoveOrCopyTemplateCommand::EndMoveOrCopyCommand: VmTemplate is null, not performing full endAction");
        }
    }

    protected void incrementDbGeneration() {
        getVmStaticDao().incrementDbGeneration(getVmTemplate().getId());
    }

    @Override
    protected void endSuccessfully() {
        endMoveOrCopyCommand();
    }

    @Override
    protected void endWithFailure() {
        endMoveOrCopyCommand();
    }

    protected void endActionOnAllImageGroups() {
        for (VdcActionParametersBase p : getParameters().getImagesParameters()) {
            getBackend().endAction(getImagesActionType(),
                    p,
                    getContext().clone().withoutCompensationContext().withoutExecutionContext().withoutLock());
        }
    }

    protected VdcActionType getImagesActionType() {
        return VdcActionType.CopyImageGroup;
    }

    protected StorageDomain getStorageDomain(Guid domainId) {
        return getStorageDomainDao().getForStoragePool(domainId, getStoragePool().getId());
    }

    /**
     * Space Validations are done using data extracted from the disks. The disks in question in this command
     * don't have all the needed data, and in order not to contaminate the command's data structures, an alter
     * one is created specifically fo this validation - hence dummy.
     * @param disksList
     * @return
     */
    protected List<DiskImage> createDiskDummiesForSpaceValidations(List<DiskImage> disksList) {
        List<DiskImage> dummies = new ArrayList<>(disksList.size());
        for (DiskImage image : disksList) {
            Guid targetSdId = imageToDestinationDomainMap.get(image.getId());
            DiskImage dummy = ImagesHandler.createDiskImageWithExcessData(image, targetSdId);
            dummies.add(dummy);
        }
        return dummies;
    }

    protected boolean validateSpaceRequirements(Collection<DiskImage> diskImages) {
        MultipleStorageDomainsValidator sdValidator = createMultipleStorageDomainsValidator(diskImages);
        if (!validate(sdValidator.allDomainsExistAndActive())
                || !validate(sdValidator.allDomainsWithinThresholds())) {
            return false;
        }

        if (getParameters().getCopyCollapse()) {
            return validate(sdValidator.allDomainsHaveSpaceForClonedDisks(diskImages));
        }

        return validate(sdValidator.allDomainsHaveSpaceForDisksWithSnapshots(diskImages));
    }

    protected MultipleStorageDomainsValidator createMultipleStorageDomainsValidator(Collection<DiskImage> diskImages) {
        return new MultipleStorageDomainsValidator(getStoragePoolId(),
                ImagesHandler.getAllStorageIdsForImageIds(diskImages));
    }

    protected void ensureDomainMap(Collection<DiskImage> images, Guid defaultDomainId) {
        if (imageToDestinationDomainMap == null) {
            imageToDestinationDomainMap = new HashMap<>();
        }
        if (imageToDestinationDomainMap.isEmpty() && images != null && defaultDomainId != null) {
            for (DiskImage image : images) {
                if (isImagesAlreadyOnTarget()) {
                    imageToDestinationDomainMap.put(image.getId(), image.getStorageIds().get(0));
                } else if (!Guid.Empty.equals(defaultDomainId)) {
                    imageToDestinationDomainMap.put(image.getId(), defaultDomainId);
                }
            }
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        if (permissionCheckSubject == null) {
            if (imageToDestinationDomainMap == null || imageToDestinationDomainMap.isEmpty()) {
                permissionCheckSubject = super.getPermissionCheckSubjects();
            } else {
                permissionCheckSubject = new ArrayList<>();
                Set<PermissionSubject> permissionSet = new HashSet<>();
                for (Guid storageId : imageToDestinationDomainMap.values()) {
                    permissionSet.add(new PermissionSubject(storageId,
                            VdcObjectType.Storage,
                            getActionType().getActionGroup()));
                }
                permissionCheckSubject.addAll(permissionSet);
            }

        }
        return permissionCheckSubject;
    }

    protected boolean validateMacAddress(List<VmNic> ifaces) {
        int freeMacs = 0;
        for (VmNic iface : ifaces) {
            if (!StringUtils.isEmpty(iface.getMacAddress())) {
                if(!VALIDATE_MAC_ADDRESS.matcher(iface.getMacAddress()).matches()) {
                    addCanDoActionMessageVariable("IfaceName ", iface.getName());
                    addCanDoActionMessageVariable("MacAddress ", iface.getMacAddress());
                    addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_NETWORK_INTERFACE_MAC_INVALID);
                    return false;
                }
            }
            else {
                freeMacs++;
            }
        }
        if (freeMacs > 0 && !(getMacPool().getAvailableMacsCount() >= freeMacs)) {
            addCanDoActionMessage(EngineMessage.MAC_POOL_NOT_ENOUGH_MAC_ADDRESSES);
            return false;
        }
        return true;
    }

    /**
     * Cloning a new disk and all its volumes with a new generated id.<br/>
     * The disk will have the same parameters as <code>disk</code>.<br/>
     * Also adding the disk to <code>newDiskGuidForDisk</code> map, so we will be able to link between the new cloned disk
     * and the old disk id.
     *
     * @param diskImagesList
     *            - All the disk volumes
     * @param disk
     *            - The disk which is about to be cloned
     */
    protected void generateNewDiskId(List<DiskImage> diskImagesList, DiskImage disk) {
        Guid generatedGuid = generateNewDiskId(disk);
        for (DiskImage diskImage : diskImagesList) {
            diskImage.setId(generatedGuid);
        }
    }

    /**
     * Updating managed device map of VM, with the new disk {@link Guid}s.<br/>
     * The update of managedDeviceMap is based on the newDiskIdForDisk map,
     * so this method should be called only after newDiskIdForDisk is initialized.
     *
     * @param disk
     *            - The disk which is about to be cloned
     * @param managedDeviceMap
     *            - The managed device map contained in the VM.
     */
    protected void updateManagedDeviceMap(DiskImage disk, Map<Guid, VmDevice> managedDeviceMap) {
        Guid oldDiskId = newDiskIdForDisk.get(disk.getId()).getId();
        managedDeviceMap.put(disk.getId(), managedDeviceMap.get(oldDiskId));
        managedDeviceMap.remove(oldDiskId);
    }

    /**
     * Cloning a new disk with a new generated id, with the same parameters as <code>disk</code>. Also
     * adding the disk to <code>newDiskGuidForDisk</code> map, so we will be able to link between the new cloned disk
     * and the old disk id.
     *
     * @param disk
     *            - The disk which is about to be cloned
     */
    protected Guid generateNewDiskId(DiskImage disk) {
        Guid newGuidForDisk = Guid.newGuid();

        // Copy the disk so it will preserve the old disk id and image id.
        newDiskIdForDisk.put(newGuidForDisk, DiskImage.copyOf(disk));
        disk.setId(newGuidForDisk);
        disk.setImageId(Guid.newGuid());
        return newGuidForDisk;
    }

    protected void setImagesWithStoragePoolId(Guid storagePoolId, List<DiskImage> diskImages) {
        for (DiskImage diskImage : diskImages) {
            diskImage.setStoragePoolId(storagePoolId);
        }
    }
}
