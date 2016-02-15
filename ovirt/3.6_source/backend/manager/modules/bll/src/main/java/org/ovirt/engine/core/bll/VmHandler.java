package org.ovirt.engine.core.bll;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.bll.network.macpoolmanager.MacPoolManagerStrategy;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.VmValidationUtils;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.backendinterfaces.BaseHandler;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.CopyOnNewVersion;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.EditableDeviceOnVmStatusField;
import org.ovirt.engine.core.common.businessentities.EditableField;
import org.ovirt.engine.core.common.businessentities.EditableOnVm;
import org.ovirt.engine.core.common.businessentities.EditableOnVmStatusField;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.GuestAgentStatus;
import org.ovirt.engine.core.common.businessentities.NumaTuneMode;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.Snapshot.SnapshotType;
import org.ovirt.engine.core.common.businessentities.UsbPolicy;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDynamic;
import org.ovirt.engine.core.common.businessentities.VmInit;
import org.ovirt.engine.core.common.businessentities.VmNumaNode;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.SimpleDependecyInjector;
import org.ovirt.engine.core.common.utils.VmDeviceType;
import org.ovirt.engine.core.common.utils.VmDeviceUpdate;
import org.ovirt.engine.core.common.vdscommands.SetVmStatusVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.UpdateVmDynamicDataVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RpmVersion;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VdsDynamicDao;
import org.ovirt.engine.core.dao.VmInitDao;
import org.ovirt.engine.core.utils.ObjectIdentityChecker;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.lock.LockManager;
import org.ovirt.engine.core.utils.lock.LockManagerFactory;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VmHandler {

    private static ObjectIdentityChecker updateVmsStatic;
    private static OsRepository osRepository;

    private static final Logger log = LoggerFactory.getLogger(VmHandler.class);

    private static Set<VdcActionType> COMMANDS_ALLOWED_ON_EXTERNAL_VMS = new HashSet<>();
    private static Set<VdcActionType> COMMANDS_ALLOWED_ON_HOSTED_ENGINE = new HashSet<>();
    /**
     * Initialize static list containers, for identity and permission check. The initialization should be executed
     * before calling ObjectIdentityChecker.
     *
     * @see Backend#initHandlers
     */
    public static void init() {
        Class<?>[] inspectedClassNames = new Class<?>[] {
                VmBase.class,
                VM.class,
                VmStatic.class,
                VmDynamic.class,
                VmManagementParametersBase.class };

        osRepository = SimpleDependecyInjector.getInstance().get(OsRepository.class);

        updateVmsStatic =
                new ObjectIdentityChecker(VmHandler.class, Arrays.asList(inspectedClassNames));

        for (Pair<EditableField, Field> pair : BaseHandler.extractAnnotatedFields(EditableField.class,
                (inspectedClassNames))) {
            updateVmsStatic.AddPermittedFields(pair.getSecond().getName());
        }

        for (Pair<EditableOnVm, Field> pair : BaseHandler.extractAnnotatedFields(EditableOnVm.class, inspectedClassNames)) {
            updateVmsStatic.AddPermittedFields(pair.getSecond().getName());
        }

        for (Pair<EditableOnVmStatusField, Field> pair : BaseHandler.extractAnnotatedFields(EditableOnVmStatusField.class,
                inspectedClassNames)) {
            updateVmsStatic.AddField(Arrays.asList(pair.getFirst().statuses()), pair.getSecond().getName());
            if (pair.getFirst().isHotsetAllowed()) {
                updateVmsStatic.AddHotsetFields(pair.getSecond().getName());
            }
        }

        for (Pair<EditableDeviceOnVmStatusField, Field> pair : BaseHandler.extractAnnotatedFields(EditableDeviceOnVmStatusField.class,
                inspectedClassNames)) {
            updateVmsStatic.AddField(Arrays.asList(pair.getFirst().statuses()), pair.getSecond().getName());
        }

        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.MigrateVm);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.MigrateVmToServer);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.InternalMigrateVm);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.CancelMigrateVm);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.SetVmTicket);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.VmLogon);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.StopVm);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.ShutdownVm);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.RemoveVm);
        COMMANDS_ALLOWED_ON_EXTERNAL_VMS.add(VdcActionType.RebootVm);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.MigrateVm);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.MigrateVmToServer);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.InternalMigrateVm);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.CancelMigrateVm);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.SetVmTicket);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.VmLogon);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.UpdateVm);
        COMMANDS_ALLOWED_ON_HOSTED_ENGINE.add(VdcActionType.RemoveVm);
    }

    public static boolean isUpdateValid(VmStatic source, VmStatic destination, VMStatus status) {
        return updateVmsStatic.IsUpdateValid(source, destination, status);
    }

    public static List<String> getChangedFieldsForStatus(VmStatic source, VmStatic destination, VMStatus status) {
        return updateVmsStatic.getChangedFieldsForStatus(source, destination, status);
    }

    public static boolean isUpdateValid(VmStatic source, VmStatic destination, VMStatus status, boolean hotsetEnabled) {
        return updateVmsStatic.IsUpdateValid(source, destination, status, hotsetEnabled);
    }

    public static boolean isUpdateValid(VmStatic source, VmStatic destination) {
        return updateVmsStatic.IsUpdateValid(source, destination);
    }

    public static boolean isUpdateValidForVmDevice(String fieldName, VMStatus status) {
        return updateVmsStatic.IsFieldUpdatable(status, fieldName, null);
    }

    public static boolean copyNonEditableFieldsToDestination(VmStatic source, VmStatic destination, boolean hotSetEnabled) {
        return updateVmsStatic.copyNonEditableFieldsToDestination(source, destination, hotSetEnabled);
    }

    /**
     * Verifies the add vm command .
     *
     * @param reasons
     *            The reasons.
     * @param nicsCount
     *            How many vNICs need to be allocated.
     * @return
     */
    public static boolean verifyAddVm(List<String> reasons,
            int nicsCount,
            int vmPriority,
            MacPoolManagerStrategy macPool) {
        boolean returnValue = true;
        if (macPool.getAvailableMacsCount() < nicsCount) {
            if (reasons != null) {
                reasons.add(EngineMessage.MAC_POOL_NOT_ENOUGH_MAC_ADDRESSES.toString());
            }
            returnValue = false;
        } else if (!VmTemplateCommand.isVmPriorityValueLegal(vmPriority, reasons)) {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Checks if VM with same name exists in the given DC. If no DC provided, check all VMs in the database.
     */
    public static boolean isVmWithSameNameExistStatic(String vmName, Guid storagePoolId) {
        NameQueryParameters params = new NameQueryParameters(vmName);
        params.setDatacenterId(storagePoolId);
        VdcQueryReturnValue result = Backend.getInstance().runInternalQuery(VdcQueryType.IsVmWithSameNameExist, params);
        return (Boolean) (result.getReturnValue());
    }

    /**
     * Lock the VM in a new transaction, saving compensation data of the old status.
     *
     * @param vm
     *            The VM to lock.
     * @param compensationContext
     *            Used to save the old VM status, for compensation purposes.
     */
    public static void lockVm(final VmDynamic vm, final CompensationContext compensationContext) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                compensationContext.snapshotEntityStatus(vm);
                lockVm(vm.getId());
                compensationContext.stateChanged();
                return null;
            }
        });
    }

    /**
     * Check VM status before locking it, If VM status is not down, we throw an exception.
     *
     * @param status
     *            - The status of the VM
     */
    private static void checkStatusBeforeLock(VMStatus status) {
        if (status == VMStatus.ImageLocked) {
            log.error("VM status cannot change to image locked, since it is already locked");
            throw new EngineException(EngineError.IRS_IMAGE_STATUS_ILLEGAL);
        }
    }

    /**
     * Lock VM after check its status, If VM status is locked, we throw an exception.
     *
     * @param vmId
     *            - The ID of the VM.
     */
    public static void checkStatusAndLockVm(Guid vmId) {
        VmDynamic vmDynamic = DbFacade.getInstance().getVmDynamicDao().get(vmId);
        checkStatusBeforeLock(vmDynamic.getStatus());
        lockVm(vmId);
    }

    /**
     * Lock VM with compensation, after checking its status, If VM status is locked, we throw an exception.
     *
     * @param vmId
     *            - The ID of the VM, which we want to lock.
     * @param compensationContext
     *            - Used to save the old VM status for compensation purposes.
     */
    public static void checkStatusAndLockVm(Guid vmId, CompensationContext compensationContext) {
        VmDynamic vmDynamic = DbFacade.getInstance().getVmDynamicDao().get(vmId);
        checkStatusBeforeLock(vmDynamic.getStatus());
        lockVm(vmDynamic, compensationContext);
    }

    public static void lockVm(Guid vmId) {
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.SetVmStatus,
                        new SetVmStatusVDSCommandParameters(vmId, VMStatus.ImageLocked));
    }

    /**
     * Unlock the VM in a new transaction, saving compensation data of the old status.
     *
     * @param vm
     *            The VM to unlock.
     * @param compensationContext
     *            Used to save the old VM status, for compensation purposes.
     */
    public static void unlockVm(final VM vm, final CompensationContext compensationContext) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {

            @Override
            public Void runInTransaction() {
                compensationContext.snapshotEntityStatus(vm.getDynamicData());
                unLockVm(vm);
                compensationContext.stateChanged();
                return null;
            }
        });
    }

    public static void unLockVm(VM vm) {
        Backend.getInstance()
                .getResourceManager()
                .RunVdsCommand(VDSCommandType.SetVmStatus,
                        new SetVmStatusVDSCommandParameters(vm.getId(), VMStatus.Down));
        vm.setStatus(VMStatus.Down);
    }

    public static void updateDisksFromDb(VM vm) {
        List<Disk> imageList = DbFacade.getInstance().getDiskDao().getAllForVm(vm.getId());
        vm.clearDisks();
        updateDisksForVm(vm, imageList);
    }

    public static void updateDisksForVm(VM vm, Collection<? extends Disk> disks) {
        for (Disk disk : disks) {
            if (disk.isAllowSnapshot() && !disk.isDiskSnapshot()) {
                DiskImage image = (DiskImage) disk;
                vm.getDiskMap().put(image.getId(), image);
                vm.getDiskList().add(image);
            } else {
                vm.getDiskMap().put(disk.getId(), disk);
            }
        }
    }

    /**
     * Fetch VmInit from Database
     * @param vm VmBase to set the VmInit into
     * @param secure if true don't return any password field
     * We want to set false only when running VM becase the VmInitDao
     * decrypt the password.
     */
    public static void updateVmInitFromDB(VmBase vm, boolean secure) {
        VmInitDao db = DbFacade.getInstance().getVmInitDao();
        vm.setVmInit(db.get(vm.getId()));
        if (vm.getVmInit() != null) {
            if (secure) {
                vm.getVmInit().setPasswordAlreadyStored(!StringUtils.isEmpty(vm.getVmInit().getRootPassword()));
                vm.getVmInit().setRootPassword(null);
            } else {
                vm.getVmInit().setPasswordAlreadyStored(false);
            }
        }
    }

    public static void addVmInitToDB(VmBase vm) {
        if (vm.getVmInit() != null) {
            vm.getVmInit().setId(vm.getId());
            VmInitDao db = DbFacade.getInstance().getVmInitDao();
            VmInit oldVmInit = db.get(vm.getId());
            if (oldVmInit == null) {
                db.save(vm.getVmInit());
            } else {
                if (vm.getVmInit().isPasswordAlreadyStored()) {
                    // since we are not always returning the password in
                    // updateVmInitFromDB()
                    // method (we don't want to display it in the UI/API) we
                    // don't want to override
                    // the password if the flag is on
                    vm.getVmInit().setRootPassword(oldVmInit.getRootPassword());
                }
                db.update(vm.getVmInit());
            }
        }
    }

    public static void updateVmInitToDB(VmBase vm) {
        if (vm.getVmInit() != null) {
            VmHandler.addVmInitToDB(vm);
        } else {
            VmHandler.removeVmInitFromDB(vm);
        }
    }

    public static void removeVmInitFromDB(VmBase vm) {
        VmInitDao db = DbFacade.getInstance().getVmInitDao();
        db.remove(vm.getId());
    }

    // if secure is true we don't return the stored password, only
    // indicate that the password is set via the PasswordAlreadyStored property
    public static List<VmInit> getVmInitByIds(List<Guid> ids, boolean secure) {
        VmInitDao db = DbFacade.getInstance().getVmInitDao();
        List<VmInit> all = db.getVmInitByIds(ids);

        for (VmInit vmInit: all) {
            if (secure) {
                vmInit.setPasswordAlreadyStored(!StringUtils.isEmpty(vmInit.getRootPassword()));
                vmInit.setRootPassword(null);
            } else {
                vmInit.setPasswordAlreadyStored(false);
            }

        }
        return all;
    }

    /**
     * Filters the vm image disks/disk devices.<BR/>
     * note: luns will be filtered, only active image disks will be return.
     */
    public static void filterImageDisksForVM(VM vm) {
        List<DiskImage> filteredDisks = ImagesHandler.filterImageDisks(vm.getDiskMap().values(), false, false, true);
        List<CinderDisk> filteredCinderDisks = ImagesHandler.filterDisksBasedOnCinder(vm.getDiskMap().values());
        filteredDisks.addAll(filteredCinderDisks);
        Collection<? extends Disk> vmDisksToRemove = CollectionUtils.subtract(vm.getDiskMap().values(), filteredDisks);
        vm.clearDisks();
        updateDisksForVm(vm, filteredDisks);
        for (Disk diskToRemove : vmDisksToRemove) {
            vm.getManagedVmDeviceMap().remove(diskToRemove.getId());
        }
    }

    public static void updateNetworkInterfacesFromDb(VM vm) {
        List<VmNetworkInterface> interfaces = DbFacade.getInstance().getVmNetworkInterfaceDao().getAllForVm(vm.getId());
        vm.setInterfaces(interfaces);
    }

    private static Version getApplicationVersion(final String part, final String appName) {
        try {
            return new RpmVersion(part, getAppName(part, appName), true);
        } catch (Exception e) {
            log.debug("Failed to create rpm version object, part '{}' appName '{}': {}",
                    part,
                    appName,
                    e.getMessage());
            log.debug("Exception", e);
            return null;
        }
    }

    private static String getAppName(final String part, final String appName) {
        if (StringUtils.contains(part, appName + "64")) { // 64 bit Agent has extension
            // to its name.
            return appName + "64";
        }
        return appName;
    }

    /**
     * Updates the {@link VM}'s {@link VM#getGuestAgentVersion()} and {@link VM#getSpiceDriverVersion()} based on the
     * VM's {@link VM#getAppList()} property.
     *
     * @param vm
     *            the VM
     */
    public static void updateVmGuestAgentVersion(final VM vm) {
        if (vm.getAppList() != null) {
            final String[] parts = vm.getAppList().split("[,]", -1);
            if (parts != null && parts.length != 0) {
                final List<String> possibleAgentAppNames = Config.<List<String>> getValue(ConfigValues.AgentAppName);
                final Map<String, String> spiceDriversInGuest =
                        Config.<Map<String, String>> getValue(ConfigValues.SpiceDriverNameInGuest);
                final String spiceDriverInGuest =
                        spiceDriversInGuest.get(osRepository.getOsFamily(vm.getOs()).toLowerCase());

                for (final String part : parts) {
                    for (String agentName : possibleAgentAppNames) {
                        if (StringUtils.containsIgnoreCase(part, agentName)) {
                            vm.setGuestAgentVersion(getApplicationVersion(part, agentName));
                        }
                        if (StringUtils.containsIgnoreCase(part, spiceDriverInGuest)) {
                            vm.setSpiceDriverVersion(getApplicationVersion(part, spiceDriverInGuest));
                        }
                    }
                }
            }
        }
    }

    public static void updateVmLock(final VM vm) {
        vm.setLockInfo(getLockManager().getLockInfo(String.format("%s%s", vm.getId(), LockingGroup.VM.name())));
    }

    public static void updateOperationProgress(final VM vm) {
        vm.setBackgroundOperationDescription(ResourceManager.getInstance().getVmManager(vm.getId()).getConvertOperationDescription());
        vm.setBackgroundOperationProgress(ResourceManager.getInstance().getVmManager(vm.getId()).getConvertOperationProgress());
    }

    protected static LockManager getLockManager() {
        return LockManagerFactory.getLockManager();
    }

    /**
     * Checks the validity of the given memory size according to OS type.
     *
     * @param vm
     *            a vm|template.
     * @param clusterVersion
     *            the vm's cluster version.
     * @return
     */
    public static void warnMemorySizeLegal(VmBase vm, Version clusterVersion) {
        if (! VmValidationUtils.isMemorySizeLegal(vm.getOsId(), vm.getMemSizeMb(), clusterVersion)) {
            AuditLogableBase logable = new AuditLogableBase();
            logable.setVmId(vm.getId());
            logable.addCustomValue("VmName", vm.getName());
            logable.addCustomValue("VmMemInMb", String.valueOf(vm.getMemSizeMb()));
            logable.addCustomValue("VmMinMemInMb",
                    String.valueOf(VmValidationUtils.getMinMemorySizeInMb(vm.getOsId(), clusterVersion)));
            logable.addCustomValue("VmMaxMemInMb",
                    String.valueOf(VmValidationUtils.getMaxMemorySizeInMb(vm.getOsId(), clusterVersion)));

            new AuditLogDirector().log(logable, AuditLogType.VM_MEMORY_NOT_IN_RECOMMENDED_RANGE);
        }
    }

    /**
     * Check if the OS type is supported.
     *
     * @param osId
     *            Type of the OS.
     * @param architectureType
     *            The architecture type.
     * @param reasons
     *            The reasons.VdsGroups
     * @return
     */
    public static boolean isOsTypeSupported(int osId,
                                            ArchitectureType architectureType,
                                            List<String> reasons) {
        boolean result = VmValidationUtils.isOsTypeSupported(osId, architectureType);
        if (!result) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_OS_TYPE_IS_NOT_SUPPORTED_BY_ARCHITECTURE_TYPE
                    .toString());
        }
        return result;
    }

    /**
     * Check if the graphics and display types are supported.
     *
     * @param osId
     *            Type of the OS.
     * @param graphics
     *            Collection of graphics types (SPICE, VNC).
     * @param displayType
     *            Display type.
     * @param reasons
     *            The reasons.VdsGroups
     * @param clusterVersion
     *            The cluster version.
     * @return
     */
    public static boolean isGraphicsAndDisplaySupported(int osId,
                                                        Collection<GraphicsType> graphics,
                                                        DisplayType displayType,
                                                        List<String> reasons,
                                                        Version clusterVersion) {
        boolean result = VmValidationUtils.isGraphicsAndDisplaySupported(osId, clusterVersion, graphics, displayType);
        if (!result) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_VM_DISPLAY_TYPE_IS_NOT_SUPPORTED_BY_OS.name());
        }
        return result;
    }

    /**
     * Check if the OS type is supported for VirtIO-SCSI.
     *
     * @param osId
     *            Type of the OS
     * @param clusterVersion
     *            Cluster's version
     * @param reasons
     *            Reasons List
     * @return
     */
    public static boolean isOsTypeSupportedForVirtioScsi(int osId,
                                            Version clusterVersion,
                                            List<String> reasons) {
        boolean result = VmValidationUtils.isDiskInterfaceSupportedByOs(osId, clusterVersion, DiskInterface.VirtIO_SCSI);
        if (!result) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_OS_TYPE_DOES_NOT_SUPPORT_VIRTIO_SCSI.name());
        }
        return result;
    }

    /**
     * Check if the interface name is not duplicate in the list of interfaces.
     *
     * @param interfaces
     *            - List of interfaces the VM/Template got.
     * @param interfaceName
     *            - Candidate for interface name.
     * @param messages
     *            - Messages for CanDoAction().
     * @return - True , if name is valid, false, if name already exist.
     */
    public static boolean isNotDuplicateInterfaceName(List<VmNic> interfaces,
                                                      final String interfaceName,
                                                      List<String> messages) {

        // Interface iface = interfaces.FirstOrDefault(i => i.name ==
        // AddVmInterfaceParameters.Interface.name);
        VmNic iface = LinqUtils.firstOrNull(interfaces, new Predicate<VmNic>() {
            @Override
            public boolean eval(VmNic i) {
                return i.getName().equals(interfaceName);
            }
        });

        if (iface != null) {
            messages.add(EngineMessage.NETWORK_INTERFACE_NAME_ALREADY_IN_USE.name());
            return false;
        }
        return true;
    }

    /**
     * Checks number of monitors validation according to VM and Graphics types.
     *
     * @param graphicsTypes
     *            Collection of graphics types of a VM.
     * @param numOfMonitors
     *            Number of monitors
     * @param reasons
     *            Messages for CanDoAction().
     * @return
     */
    public static boolean isNumOfMonitorsLegal(Collection<GraphicsType> graphicsTypes, int numOfMonitors, List<String> reasons) {
        boolean legal = false;

        if (graphicsTypes.contains(GraphicsType.VNC)) {
            legal = (numOfMonitors <= 1);
        } else if (graphicsTypes.contains(GraphicsType.SPICE)) { // contains spice and doesn't contain vnc
            legal = (numOfMonitors <= getMaxNumberOfMonitors());
        }

        if (!legal) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_NUM_OF_MONITORS.toString());
        }

        return legal;
    }

    public static boolean isSingleQxlDeviceLegal(DisplayType displayType, int osId, List<String> reasons,
            Version compatibilityVersion) {
        if (!FeatureSupported.singleQxlPci(compatibilityVersion)) {
             reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_SINGLE_DEVICE_INCOMPATIBLE_VERSION.toString());
             return false;
         }
        if (displayType != DisplayType.qxl) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_SINGLE_DEVICE_DISPLAY_TYPE.toString());
            return false;
        }
        if (!osRepository.isSingleQxlDeviceEnabled(osId)) {
            reasons.add(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_SINGLE_DEVICE_OS_TYPE.toString());
            return false;
        }
        return true;
    }

    /**
     * get max of allowed monitors from config config value is a comma separated list of integers
     *
     * @return
     */
    private static int getMaxNumberOfMonitors() {
        int max = 0;
        String numOfMonitorsStr =
                Config.getValue(ConfigValues.ValidNumOfMonitors).toString().replaceAll("[\\[\\]]", "");
        String values[] = numOfMonitorsStr.split(",");
        for (String val : values) {
            val = val.trim();
            if (Integer.valueOf(val) > max) {
                max = Integer.valueOf(val);
            }
        }
        return max;
    }

    /**
     * Returns the vm's active snapshot, or null if one doesn't exist.
     * Note that this method takes into consideration that the vm snapshots are already loaded from DB.
     * @param vm The vm to get the active snapshot from.
     * @return the vm's active snapshot, or null if one doesn't exist.
     */
    public static Snapshot getActiveSnapshot(VM vm) {
        for (Snapshot snapshot : vm.getSnapshots()) {
            if (snapshot.getType() == SnapshotType.ACTIVE) {
                return snapshot;
            }
        }
        return null;
    }

    /**
     * Checks that the USB policy is legal for the VM. If it is ENABLED_NATIVE then it is legal only in case the cluster
     * level is >= 3.1. If it is ENABLED_LEGACY then it is not legal on Linux VMs.
     *
     * @param usbPolicy
     * @param osId
     * @param vdsGroup
     * @param messages
     *            - Messages for CanDoAction()
     * @return
     */
    public static boolean isUsbPolicyLegal(UsbPolicy usbPolicy,
            int osId,
            VDSGroup vdsGroup,
            List<String> messages) {
        boolean retVal = true;
        if (UsbPolicy.ENABLED_NATIVE.equals(usbPolicy)) {
            if (!Config.<Boolean> getValue(ConfigValues.NativeUSBEnabled, vdsGroup.getCompatibilityVersion()
                    .getValue())) {
                messages.add(EngineMessage.USB_NATIVE_SUPPORT_ONLY_AVAILABLE_ON_CLUSTER_LEVEL.toString());
                retVal = false;
            }
        } else if (UsbPolicy.ENABLED_LEGACY.equals(usbPolicy)) {
            if (osRepository.isLinux(osId)) {
                messages.add(EngineMessage.USB_LEGACY_NOT_SUPPORTED_ON_LINUX_VMS.toString());
                retVal = false;
            }
        }
        return retVal;
    }

    public static void updateImportedVmUsbPolicy(VmBase vmBase) {
        // Enforce disabled USB policy for Linux OS with legacy policy.
        if (osRepository.isLinux(vmBase.getOsId()) && vmBase.getUsbPolicy().equals(UsbPolicy.ENABLED_LEGACY)) {
            vmBase.setUsbPolicy(UsbPolicy.DISABLED);
        }
    }

    public static ValidationResult canRunActionOnNonManagedVm(VM vm, VdcActionType actionType) {
        ValidationResult validationResult = ValidationResult.VALID;

        if ((vm.isHostedEngine() && !COMMANDS_ALLOWED_ON_HOSTED_ENGINE.contains(actionType)) ||
            (vm.isExternalVm() && !COMMANDS_ALLOWED_ON_EXTERNAL_VMS.contains(actionType))) {
            validationResult = new ValidationResult(EngineMessage.ACTION_TYPE_FAILED_CANNOT_RUN_ACTION_ON_NON_MANAGED_VM);
        }

        return validationResult;
    }

    private static VdsDynamicDao getVdsDynamicDao() {
        return DbFacade.getInstance().getVdsDynamicDao();
    }

    public static void updateCurrentCd(Guid vdsId, VM vm, String currentCd) {
        VmDynamic vmDynamic = vm.getDynamicData();
        vmDynamic.setCurrentCd(currentCd);
        Backend.getInstance()
               .getResourceManager()
               .RunVdsCommand(VDSCommandType.UpdateVmDynamicData,
                       new UpdateVmDynamicDataVDSCommandParameters(vmDynamic));
    }

    public static void updateDefaultTimeZone(VmBase vmBase) {
        if (vmBase.getTimeZone() == null) {
            if (osRepository.isWindows(vmBase.getOsId())) {
                vmBase.setTimeZone(Config.<String> getValue(ConfigValues.DefaultWindowsTimeZone));
            } else {
                vmBase.setTimeZone(Config.<String> getValue(ConfigValues.DefaultGeneralTimeZone));
            }
        }
    }

    public static boolean isUpdateValidForVmDevices(Guid vmId, VMStatus vmStatus, Object objectWithEditableDeviceFields) {
        if (objectWithEditableDeviceFields == null) {
            return true;
        }
        return getVmDevicesFieldsToUpdateOnNextRun(vmId, vmStatus, objectWithEditableDeviceFields).isEmpty();
    }

    public static List<VmDeviceUpdate> getVmDevicesFieldsToUpdateOnNextRun(
            Guid vmId, VMStatus vmStatus, Object objectWithEditableDeviceFields) {
        List<VmDeviceUpdate> fieldList = new ArrayList<>();

        if (objectWithEditableDeviceFields == null) {
            return fieldList;
        }

        List<Pair<EditableDeviceOnVmStatusField , Field>> pairList = BaseHandler.extractAnnotatedFields(
                EditableDeviceOnVmStatusField.class, objectWithEditableDeviceFields.getClass());

        for (Pair<EditableDeviceOnVmStatusField, Field> pair : pairList) {
            EditableDeviceOnVmStatusField annotation = pair.getFirst();
            Field field = pair.getSecond();
            field.setAccessible(true);

            if (VmHandler.isUpdateValidForVmDevice(field.getName(), vmStatus)) {
                // field may be updated on the current run, so not including for the next run
                continue;
            }

            try {
                Object value = field.get(objectWithEditableDeviceFields);
                if (value == null) {
                    // preserve current configuration
                } else if (value instanceof Boolean) {
                    addDeviceUpdateOnNextRun(vmId, annotation, null, value, fieldList);
                } else if (value instanceof VmManagementParametersBase.Optional) {
                    VmManagementParametersBase.Optional<?> optional = (VmManagementParametersBase.Optional<?>) value;
                    if (optional.isUpdate()) {
                        addDeviceUpdateOnNextRun(vmId, annotation, null, optional.getValue(), fieldList);
                    }
                } else if (value instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) value;
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        boolean success = addDeviceUpdateOnNextRun(vmId, annotation,
                                entry.getKey(), entry.getValue(), fieldList);
                        if (!success)
                            break;
                    }
                } else {
                    log.warn("getVmDevicesFieldsToUpdateOnNextRun: Unsupported field type: " +
                            value.getClass().getName());
                }
            } catch (IllegalAccessException | ClassCastException e) {
                log.warn("getVmDevicesFieldsToUpdateOnNextRun: Reflection error");
                log.debug("Original exception was:", e);
            }
        }

        return fieldList;
    }

    private static boolean addDeviceUpdateOnNextRun(Guid vmId, EditableDeviceOnVmStatusField annotation,
                                                Object key, Object value, List<VmDeviceUpdate> updates) {
        return addDeviceUpdateOnNextRun(vmId, annotation.generalType(), annotation.type(), annotation.isReadOnly(),
                annotation.name(), key, value, updates);
    }

    private static boolean addDeviceUpdateOnNextRun(Guid vmId, VmDeviceGeneralType generalType, VmDeviceType type,
                boolean readOnly, String name, Object key, Object value, List<VmDeviceUpdate> updates) {

        if (key != null) {
            VmDeviceGeneralType keyGeneralType = VmDeviceGeneralType.UNKNOWN;
            VmDeviceType keyType = VmDeviceType.UNKNOWN;

            if (key instanceof VmDeviceGeneralType) {
                keyGeneralType = (VmDeviceGeneralType) key;
            } else if (key instanceof VmDeviceType) {
                keyType = (VmDeviceType) key;
            } else if (key instanceof GraphicsType) {
                keyType = ((GraphicsType) key).getCorrespondingDeviceType();
            } else {
                log.warn("addDeviceUpdateOnNextRun: Unsupported map key type: " +
                        key.getClass().getName());
                return false;
            }

            if (keyGeneralType != VmDeviceGeneralType.UNKNOWN) {
                generalType = keyGeneralType;
            }
            if (keyType != VmDeviceType.UNKNOWN) {
                type = keyType;
            }
        }

        // if device type is set to unknown, search by general type only
        // because some devices have more than one type, like sound can be ac97/ich6
        String typeName = type != VmDeviceType.UNKNOWN ? type.getName() : null;

        if (value == null) {
            if (VmDeviceUtils.vmDeviceChanged(vmId, generalType, typeName, false)) {
                updates.add(new VmDeviceUpdate(generalType, type, readOnly, name, false));
            }
        } else if (value instanceof Boolean) {
            if (VmDeviceUtils.vmDeviceChanged(vmId, generalType, typeName, (Boolean) value)) {
                updates.add(new VmDeviceUpdate(generalType, type, readOnly, name, (Boolean) value));
            }
        } else if (value instanceof VmDevice) {
            if (VmDeviceUtils.vmDeviceChanged(vmId, generalType, typeName, (VmDevice) value)) {
                updates.add(new VmDeviceUpdate(generalType, type, readOnly, name, (VmDevice) value));
            }
        } else {
            log.warn("addDeviceUpdateOnNextRun: Unsupported value type: " +
                    value.getClass().getName());
            return false;
        }

        return true;
    }

    public static boolean isCpuSupported(int osId, Version version, String cpuName, ArrayList<String> canDoActionMessages) {
        String cpuId = CpuFlagsManagerHandler.getCpuId(cpuName, version);
        if (cpuId == null) {
            canDoActionMessages.add(EngineMessage.CPU_TYPE_UNKNOWN.name());
            return false;
        }
        if (!osRepository.isCpuSupported(
                osId,
                version,
                cpuId)) {
            String unsupportedCpus = osRepository.getUnsupportedCpus(osId, version).toString();
            canDoActionMessages.add(EngineMessage.CPU_TYPE_UNSUPPORTED_FOR_THE_GUEST_OS.name());
            canDoActionMessages.add("$unsupportedCpus " + StringUtils.strip(unsupportedCpus.toString(), "[]"));
            return false;
        }
        return true;
    }

    /**
     * Cannot have more vm NUMA nodes than vm CPU core
     *
     * @param isNumaChanged
     */
    public static ValidationResult checkVmNumaNodesIntegrity(VM paramsVm, VM actualVm, boolean isNumaChanged) {
        /* calculate the actual NUMA nodes */
        List<VmNumaNode> paramVmNumaNodes = paramsVm.getvNumaNodeList();
        boolean emptyParamVmNumaNodes = (paramVmNumaNodes == null) || (paramVmNumaNodes.isEmpty());
        /* origVmNumaNodes = NUMA nodes list prior to update. */
        List<VmNumaNode> origVmNumaNodes =
                DbFacade.getInstance().getVmNumaNodeDao().getAllVmNumaNodeByVmId(actualVm.getId());
        boolean emptyOrigVmNumaNodes = (origVmNumaNodes == null) || (origVmNumaNodes.isEmpty());

        int NUMAnodesCount = 0;

        /* return valid if no NUMA nodes */
        if (emptyParamVmNumaNodes && emptyOrigVmNumaNodes) {
            return ValidationResult.VALID;
        }
        /* if no NUMA nodes in parameters, but there are NUMA nodes in previous vm */
        if (emptyParamVmNumaNodes && !emptyOrigVmNumaNodes) {
            /* REST-api always provide emptyParamVmNumaNodes */
            /* REST-api modifies NUMA nodes via: addVmNumaNodeCommand/updateVmNumaNodeCommand */
            /* count NUMA nodes in previous vm, by default */
            NUMAnodesCount = origVmNumaNodes.size();
            /* if GUI update to reset NUMA nodes */
            if (isNumaChanged == true)
                return ValidationResult.VALID; // no NUMA nodes.
        }
        if (!emptyParamVmNumaNodes) {// An update to NUMA nodes
            NUMAnodesCount = paramVmNumaNodes.size();
        }

        int cpuCount = paramsVm.getNumOfCpus(); // REST-api assigns cpuCount to parameters.
        if (cpuCount < NUMAnodesCount) {
            return new ValidationResult(EngineMessage.VM_NUMA_NODE_MORE_NODES_THAN_CPUS,
                    String.format("$numaNodes %d", NUMAnodesCount),
                    String.format("$cpus %d", cpuCount));
        }

        return ValidationResult.VALID;
    }

    /**
     * preferred supports single pinned vnuma node (without that VM fails to run in libvirt). used by add/update VM
     * commands
     */
    public static ValidationResult checkNumaPreferredTuneMode(NumaTuneMode numaTuneMode,
            List<VmNumaNode> vmNumaNodes,
            Guid vmId) {
        // check tune mode
        if (numaTuneMode != NumaTuneMode.PREFERRED) {
            return ValidationResult.VALID;
        }

        if (vmNumaNodes == null && vmId != null) {
            vmNumaNodes = DbFacade.getInstance().getVmNumaNodeDao().getAllVmNumaNodeByVmId(vmId);
        }

        // check single node pinned
        if (vmNumaNodes != null && vmNumaNodes.size() == 1) {
            List<Pair<Guid, Pair<Boolean, Integer>>> vdsNumaNodeList = vmNumaNodes.get(0).getVdsNumaNodeList();
            boolean pinnedToSingleNode = vdsNumaNodeList != null
                    && vdsNumaNodeList.size() == 1
                    && vdsNumaNodeList.get(0).getSecond() != null
                    && vdsNumaNodeList.get(0).getSecond().getFirst();
            if (pinnedToSingleNode) {
                return ValidationResult.VALID;
            }
        }

        return new ValidationResult(EngineMessage.VM_NUMA_NODE_PREFERRED_NOT_PINNED_TO_SINGLE_NODE);
    }

    public static List<PermissionSubject> getPermissionsNeededToChangeCluster(Guid vmId, Guid clusterId) {
        List<PermissionSubject> permissionList = new ArrayList<>();
        permissionList.add(new PermissionSubject(vmId, VdcObjectType.VM, ActionGroup.EDIT_VM_PROPERTIES));
        permissionList.add(new PermissionSubject(clusterId, VdcObjectType.VdsGroups, ActionGroup.CREATE_VM));
        return permissionList;
    }


    /**
     * Returns graphics types of devices VM/Template is supposed to have after adding/updating.
     * <p/>
     * When adding, VM/Template inherits graphics devices from the template by default.
     * When updating, VM/Template has already some graphics devices set.
     * However - these devices can be customized (overriden) in params
     * (i.e. params can prevent a device to be inherited from a template).
     * <p/>
     *
     * @return graphics types of devices VM/Template is supposed to have after adding/updating.
     */
    public static Set<GraphicsType> getResultingVmGraphics(List<GraphicsType> srcEntityGraphics, Map<GraphicsType, GraphicsDevice> graphicsDevices) {
        Set<GraphicsType> result = new HashSet<>();

        for (GraphicsType type : GraphicsType.values()) {
            if (graphicsDevices.get(type) != null || // grapics set in params
                    srcEntityGraphics.contains(type) && !graphicsResetInParams(type, graphicsDevices)) // graphics is in template and is not nulled in params
            {
                result.add(type);
            }
        }

        return result;
    }

    /**
     * Returns true if given graphics type was reset in params (that means params contain given graphics device which
     * is set to null).
     */
    private static boolean graphicsResetInParams(GraphicsType type, Map<GraphicsType, GraphicsDevice> graphicsDevices) {
        return graphicsDevices.containsKey(type) && graphicsDevices.get(type) == null;
    }

    /**
     * Checks that dedicated host exists on the same cluster as the VM
     *
     * @param vm                  - the VM to check
     * @param canDoActionMessages - Action messages - used for error reporting. null value indicates that no error messages are required.
     * @return
     */
    public static boolean validateDedicatedVdsExistOnSameCluster(VmBase vm, ArrayList<String> canDoActionMessages) {
        boolean result = true;
        for (Guid vdsId : vm.getDedicatedVmForVdsList()) {
            // get dedicated host, checks if exists and compare its cluster to the VM cluster
            VDS vds = DbFacade.getInstance().getVdsDao().get(vdsId);
            if (vds == null) {
                if (canDoActionMessages != null) {
                    canDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_DEDICATED_VDS_DOES_NOT_EXIST.toString());
                }
                result = false;
            } else if (!Objects.equals(vm.getVdsGroupId(), vds.getVdsGroupId())) {
                if (canDoActionMessages != null) {
                    canDoActionMessages.add(EngineMessage.ACTION_TYPE_FAILED_DEDICATED_VDS_NOT_IN_SAME_CLUSTER.toString());
                }
                result = false;
            }
        }
        return result;
    }

    private static final Pattern TOOLS_PATTERN = Pattern.compile(".*rhev-tools\\s+([\\d\\.]+).*");
    // FIXME: currently oVirt-ToolsSetup is not present in app_list when it does
    // ISO_VERSION_PATTERN should address this pattern as well as the TOOLS_PATTERN
    // if the name will be different.
    private static final Pattern ISO_VERSION_PATTERN = Pattern.compile(".*rhev-toolssetup_(\\d\\.\\d\\_\\d).*");

    /**
     * Looking for "RHEV_Tools x.x.x" in VMs app_list if found we look if there is a newer version in the isoList - if
     * so we update the GuestAgentStatus of VmDynamic to UpdateNeeded
     *
     * @param poolId
     *            storage pool id
     * @param isoList
     *            list of iso file names
     */
    public static void refreshVmsToolsVersion(Guid poolId, Set<String> isoList) {
        String latestVersion = getLatestGuestToolsVersion(isoList);
        if (latestVersion == null) {
            return;
        }

        List<VM> vms = DbFacade.getInstance().getVmDao().getAllForStoragePool(poolId);
        for (VM vm : vms) {
            if (vm.getAppList() != null && vm.getAppList().toLowerCase().contains("rhev-tools")) {
                Matcher m = TOOLS_PATTERN.matcher(vm.getAppList().toLowerCase());
                if (m.matches() && m.groupCount() > 0) {
                    String toolsVersion = m.group(1);
                    if (toolsVersion.compareTo(latestVersion) < 0
                            && vm.getGuestAgentStatus() != GuestAgentStatus.UpdateNeeded) {
                        vm.setGuestAgentStatus(GuestAgentStatus.UpdateNeeded);
                        DbFacade.getInstance()
                                .getVmDynamicDao()
                                .updateGuestAgentStatus(vm.getId(), vm.getGuestAgentStatus());
                    }
                }
            }
        }
    }

    /**
     * iso file name that we are looking for: RHEV_toolsSetup_x.x_x.iso returning latest version only: xxx (ie 3.1.2)
     *
     * @param isoList
     *            list of iso file names
     * @return latest iso version or null if no iso tools was found
     */
    private static String getLatestGuestToolsVersion(Set<String> isoList) {
        String latestVersion = null;
        for (String iso: isoList) {
            if (iso.toLowerCase().contains("rhev-toolssetup")) {
                Matcher m = ISO_VERSION_PATTERN.matcher(iso.toLowerCase());
                if (m.matches() && m.groupCount() > 0) {
                    String isoVersion = m.group(1).replace('_', '.');
                    if (latestVersion == null) {
                        latestVersion = isoVersion;
                    } else if (latestVersion.compareTo(isoVersion) < 0) {
                        latestVersion = isoVersion;
                    }
                }
            }
        }
        return latestVersion;
    }

    /**
     * Copy fields that annotated with {@link org.ovirt.engine.core.common.businessentities.CopyOnNewVersion} from the new template version to the vm
     *
     * @param source
     *            - template to copy data from
     * @param dest
     *            - vm to copy data to
     */
    public static boolean copyData(VmBase source, VmBase dest) {
        for (Field srcFld : VmBase.class.getDeclaredFields()) {
            try {
                if (srcFld.getAnnotation(CopyOnNewVersion.class) != null) {
                    srcFld.setAccessible(true);

                    Field dstFld = VmBase.class.getDeclaredField(srcFld.getName());
                    dstFld.setAccessible(true);
                    dstFld.set(dest, srcFld.get(source));
                }
            } catch (Exception exp) {
                log.error("Failed to copy field '{}' of new version to VM '{}' ({}): {}",
                        srcFld.getName(),
                        source.getName(),
                        source.getId(),
                        exp.getMessage());
                log.debug("Exception", exp);
                return false;
            }
        }
        return true;
    }

    /**
     * Automatic selection of display type based on its graphics types in parameters.
     * This method preserves backward compatibility for REST API - legacy REST API doesn't allow to set display and
     * graphics separately.
     */
    public static void autoSelectDefaultDisplayType(Guid srcEntityId,
                                                VmBase parametersStaticData,
                                                VDSGroup cluster,
                                                Map<GraphicsType, GraphicsDevice> graphicsDevices) {
        if (parametersStaticData.getOsId() == OsRepository.AUTO_SELECT_OS ||
                parametersStaticData.getDefaultDisplayType() != null) {
            return;
        }

        DisplayType defaultDisplayType = DisplayType.qxl;
        List<Pair<GraphicsType, DisplayType>> graphicsAndDisplays = osRepository.getGraphicsAndDisplays(
                parametersStaticData.getOsId(),
                // cluster == null for the non cluster entities (e.g. Blank template and instance types)
                cluster != null ? cluster.getCompatibilityVersion() : Version.getLast());

        // map holding display type -> set of supported graphics types for this display type
        Map<DisplayType, Set<GraphicsType>> displayGraphicsSupport = new HashMap<>();

        for (Pair<GraphicsType, DisplayType> graphicsAndDisplay : graphicsAndDisplays) {
            DisplayType display = graphicsAndDisplay.getSecond();
            if (!displayGraphicsSupport.containsKey(display)) {
                displayGraphicsSupport.put(display, new HashSet<GraphicsType>());
            }

            displayGraphicsSupport.get(display).add(graphicsAndDisplay.getFirst());
        }

        for (Map.Entry<DisplayType, Set<GraphicsType>> entry : displayGraphicsSupport.entrySet()) {
            if (entry.getValue().containsAll(VmHandler.getResultingVmGraphics(VmDeviceUtils.getGraphicsTypesOfEntity(srcEntityId),
                    graphicsDevices))) {
                defaultDisplayType = entry.getKey();
                break;
            }
        }

        parametersStaticData.setDefaultDisplayType(defaultDisplayType);
    }
}
