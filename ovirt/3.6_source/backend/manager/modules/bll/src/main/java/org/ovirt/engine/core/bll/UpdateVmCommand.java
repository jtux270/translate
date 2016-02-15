package org.ovirt.engine.core.bll;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.cluster.NetworkHelper;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaSanityParameter;
import org.ovirt.engine.core.bll.quota.QuotaVdsDependent;
import org.ovirt.engine.core.bll.quota.QuotaVdsGroupConsumptionParameter;
import org.ovirt.engine.core.bll.snapshots.SnapshotsManager;
import org.ovirt.engine.core.bll.utils.IconUtils;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.IconValidator;
import org.ovirt.engine.core.bll.validator.VmValidator;
import org.ovirt.engine.core.bll.validator.VmWatchdogValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.GraphicsParameters;
import org.ovirt.engine.core.common.action.HotSetAmountOfMemoryParameters;
import org.ovirt.engine.core.common.action.HotSetNumberOfCpusParameters;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.PlugAction;
import org.ovirt.engine.core.common.action.RngDeviceParameters;
import org.ovirt.engine.core.common.action.UpdateVmVersionParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmManagementParametersBase;
import org.ovirt.engine.core.common.action.VmNumaNodeOperationParameters;
import org.ovirt.engine.core.common.action.WatchdogParameters;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.businessentities.GraphicsType;
import org.ovirt.engine.core.common.businessentities.MigrationSupport;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmNumaNode;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.utils.customprop.VmPropertiesUtils;
import org.ovirt.engine.core.common.validation.group.UpdateVm;
import org.ovirt.engine.core.compat.DateTime;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.dao.VmDeviceDao;
import org.ovirt.engine.core.dao.VmNumaNodeDao;
import org.ovirt.engine.core.dao.provider.ProviderDao;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;

public class UpdateVmCommand<T extends VmManagementParametersBase> extends VmManagementCommandBase<T>
        implements QuotaVdsDependent, RenamedEntityInfoProvider{

    private static final Base64 BASE_64 = new Base64(0, null);

    @Inject
    private ProviderDao providerDao;

    private VM oldVm;
    private boolean quotaSanityOnly = false;
    private VmStatic newVmStatic;
    private VdcReturnValueBase setNumberOfCpusResult;
    private List<GraphicsDevice> cachedGraphics;
    private boolean isUpdateVmTemplateVersion = false;

    public UpdateVmCommand(T parameters) {
        this(parameters, null);
    }

    public UpdateVmCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
        if (getVdsGroup() != null) {
            setStoragePoolId(getVdsGroup().getStoragePoolId());
        }

        if (isVmExist()) {
            Version clusterVersion = getVdsGroup().getCompatibilityVersion();
            getVmPropertiesUtils().separateCustomPropertiesToUserAndPredefined(clusterVersion, parameters.getVmStaticData());
            getVmPropertiesUtils().separateCustomPropertiesToUserAndPredefined(clusterVersion, getVm().getStaticData());
        }
        VmHandler.updateDefaultTimeZone(parameters.getVmStaticData());

        VmHandler.autoSelectDefaultDisplayType(getVmId(),
            getParameters().getVmStaticData(),
            getVdsGroup(),
            getParameters().getGraphicsDevices());


        updateParametersVmFromInstanceType();
    }


    private VmPropertiesUtils getVmPropertiesUtils() {
        return VmPropertiesUtils.getInstance();
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return isInternalExecution() ?
                getSucceeded() ? AuditLogType.SYSTEM_UPDATE_VM : AuditLogType.SYSTEM_FAILED_UPDATE_VM
                : getSucceeded() ? AuditLogType.USER_UPDATE_VM : AuditLogType.USER_FAILED_UPDATE_VM;
    }

    @Override
    protected void executeVmCommand() {
        oldVm = getVm(); // needs to be here for post-actions
        if (isUpdateVmTemplateVersion) {
            updateVmTemplateVersion();
            return; // template version was changed, no more work is required
        }
        if (isRunningConfigurationNeeded()) {
            createNextRunSnapshot();
        }

        VmHandler.warnMemorySizeLegal(getParameters().getVm().getStaticData(), getVdsGroup().getCompatibilityVersion());
        getVmStaticDao().incrementDbGeneration(getVm().getId());
        newVmStatic = getParameters().getVmStaticData();
        newVmStatic.setCreationDate(oldVm.getStaticData().getCreationDate());

        // save user selected value for hotplug before overriding with db values (when updating running vm)
        int cpuPerSocket = newVmStatic.getCpuPerSocket();
        int numOfSockets = newVmStatic.getNumOfSockets();
        int memSizeMb = newVmStatic.getMemSizeMb();

        if (newVmStatic.getCreationDate().equals(DateTime.getMinValue())) {
            newVmStatic.setCreationDate(new Date());
        }

        if (getVm().isRunningOrPaused()) {
            if (!VmHandler.copyNonEditableFieldsToDestination(oldVm.getStaticData(), newVmStatic, isHotSetEnabled())) {
                // fail update vm if some fields could not be copied
                throw new EngineException(EngineError.FAILED_UPDATE_RUNNING_VM);
            }

        }

        UpdateVmNetworks();
        updateVmNumaNodes();
        if (isHotSetEnabled()) {
            hotSetCpus(cpuPerSocket, numOfSockets);
            hotSetMemory(memSizeMb);
        }
        final List<Guid> oldIconIds = IconUtils.updateVmIcon(
                oldVm.getStaticData(), newVmStatic, getParameters().getVmLargeIcon());
        getVmStaticDao().update(newVmStatic);
        if (getVm().isNotRunning()) {
            updateVmPayload();
            VmDeviceUtils.updateVmDevices(getParameters(), oldVm);
            updateWatchdog();
            updateRngDevice();
            updateGraphicsDevice();
            updateVmHostDevices();
        }
        IconUtils.removeUnusedIcons(oldIconIds);
        VmHandler.updateVmInitToDB(getParameters().getVmStaticData());

        checkTrustedService();
        setSucceeded(true);
    }

    private void updateVmHostDevices() {
        if (isDedicatedVmForVdsChanged()) {
            log.info("Pinned host changed for VM: {}. Dropping configured host devices.", getVm().getName());
            getVmDeviceDao().removeVmDevicesByVmIdAndType(getVmId(), VmDeviceGeneralType.HOSTDEV);
        }
    }

    /**
     * Handles a template-version update use case.
     * If vm is down -> updateVmVersionCommand will handle the rest and will preform the actual change.
     * if it's running -> a NEXT_RUN snapshot will be created and the change will take affect only on power down.
     * in both cases the command should end after this function as no more changes are possible.
     */
    private void updateVmTemplateVersion() {
        if (getVm().getStatus() == VMStatus.Down) {
            VdcReturnValueBase result =
                    runInternalActionWithTasksContext(
                            VdcActionType.UpdateVmVersion,
                            new UpdateVmVersionParameters(getVmId(),
                                    getParameters().getVm().getVmtGuid(),
                                    getParameters().getVm().isUseLatestVersion()),
                            getLock()
                    );
            if (result.getSucceeded()) {
                getTaskIdList().addAll(result.getInternalVdsmTaskIdList());
            }
            setSucceeded(result.getSucceeded());
            setActionReturnValue(VdcActionType.UpdateVmVersion);
        } else {
            createNextRunSnapshot();
            setSucceeded(true);
        }
    }

    private boolean updateRngDevice() {
        // do not update if this flag is not set
        if (getParameters().isUpdateRngDevice()) {
            VdcQueryReturnValue query =
                    runInternalQuery(VdcQueryType.GetRngDevice, new IdQueryParameters(getParameters().getVmId()));

            @SuppressWarnings("unchecked")
            List<VmRngDevice> rngDevs = query.getReturnValue();

            VdcReturnValueBase rngCommandResult = null;
            if (rngDevs.isEmpty()) {
                if (getParameters().getRngDevice() != null) {
                    RngDeviceParameters params = new RngDeviceParameters(getParameters().getRngDevice(), true);
                    rngCommandResult = runInternalAction(VdcActionType.AddRngDevice, params, cloneContextAndDetachFromParent());
                }
            } else {
                if (getParameters().getRngDevice() == null) {
                    RngDeviceParameters params = new RngDeviceParameters(rngDevs.get(0), true);
                    rngCommandResult = runInternalAction(VdcActionType.RemoveRngDevice, params, cloneContextAndDetachFromParent());
                } else {
                    RngDeviceParameters params = new RngDeviceParameters(getParameters().getRngDevice(), true);
                    params.getRngDevice().setDeviceId(rngDevs.get(0).getDeviceId());
                    rngCommandResult = runInternalAction(VdcActionType.UpdateRngDevice, params, cloneContextAndDetachFromParent());
                }
            }

            if (rngCommandResult != null && !rngCommandResult.getSucceeded()) {
                return false;
            }
        }

        return true;
    }

    private void createNextRunSnapshot() {
        // first remove existing snapshot
        Snapshot runSnap = getSnapshotDao().get(getVmId(), Snapshot.SnapshotType.NEXT_RUN);
        if (runSnap != null) {
            getSnapshotDao().remove(runSnap.getId());
        }

        VM vm = new VM();
        vm.setStaticData(getParameters().getVmStaticData());

        // create new snapshot with new configuration
        new SnapshotsManager().addSnapshot(Guid.newGuid(),
                "Next Run configuration snapshot",
                Snapshot.SnapshotStatus.OK,
                Snapshot.SnapshotType.NEXT_RUN,
                vm,
                true,
                StringUtils.EMPTY,
                Collections.<DiskImage> emptyList(),
                VmDeviceUtils.getVmDevicesForNextRun(getVm(), getParameters()),
                getCompensationContext());
    }

    private void hotSetCpus(int cpuPerSocket, int newNumOfSockets) {
        int currentSockets = getVm().getNumOfSockets();
        int currentCpuPerSocket = getVm().getCpuPerSocket();

        // try hotplug only if topology (cpuPerSocket) hasn't changed
        if (getVm().getStatus() == VMStatus.Up && currentSockets != newNumOfSockets
                && currentCpuPerSocket == cpuPerSocket) {
            HotSetNumberOfCpusParameters params =
                    new HotSetNumberOfCpusParameters(
                            newVmStatic,
                            currentSockets < newNumOfSockets ? PlugAction.PLUG : PlugAction.UNPLUG);
            setNumberOfCpusResult =
                    runInternalAction(
                            VdcActionType.HotSetNumberOfCpus,
                            params, cloneContextAndDetachFromParent());
            newVmStatic.setNumOfSockets(setNumberOfCpusResult.getSucceeded() ? newNumOfSockets : currentSockets);
            auditLogHotSetCpusCandos(params);
        }
    }

    private void hotSetMemory(int newAmountOfMemory) {
        int currentMemory = getVm().getMemSizeMb();

        if (getVm().getStatus() == VMStatus.Up && currentMemory != newAmountOfMemory) {
            HotSetAmountOfMemoryParameters params =
                    new HotSetAmountOfMemoryParameters(
                            newVmStatic,
                            currentMemory < newAmountOfMemory ? PlugAction.PLUG : PlugAction.UNPLUG,
                            // We always use node 0, auto-numa should handle the allocation
                            0);

            VdcReturnValueBase setAmountOfMemoryResult =
                    runInternalAction(
                            VdcActionType.HotSetAmountOfMemory,
                            params, cloneContextAndDetachFromParent());
            newVmStatic.setMemSizeMb(setAmountOfMemoryResult.getSucceeded() ? newAmountOfMemory : currentMemory);
            auditLogHotSetMemCandos(params, setAmountOfMemoryResult);
        }
    }

    /**
     * add audit log msg for failed hot set in case error was in CDA
     * otherwise internal command will audit log the result
     * @param params
     */
    private void auditLogHotSetCpusCandos(HotSetNumberOfCpusParameters params) {
        if (!setNumberOfCpusResult.getCanDoAction()) {
            AuditLogableBase logable = new HotSetNumberOfCpusCommand<>(params);
            List<String> canDos = getBackend().getErrorsTranslator().
                    TranslateErrorText(setNumberOfCpusResult.getCanDoActionMessages());
            logable.addCustomValue(HotSetNumberOfCpusCommand.LOGABLE_FIELD_ERROR_MESSAGE, StringUtils.join(canDos, ","));
            auditLogDirector.log(logable, AuditLogType.FAILED_HOT_SET_NUMBER_OF_CPUS);
        }
    }

    /**
     * add audit log msg for failed hot set in case error was in CDA
     * otherwise internal command will audit log the result
     * @param params
     */
    private void auditLogHotSetMemCandos(HotSetAmountOfMemoryParameters params, VdcReturnValueBase setAmountOfMemoryResult) {
        if (!setAmountOfMemoryResult.getCanDoAction()) {
            AuditLogableBase logable = new HotSetAmountOfMemoryCommand<>(params);
            List<String> canDos = getBackend().getErrorsTranslator().
                    TranslateErrorText(setAmountOfMemoryResult.getCanDoActionMessages());
            logable.addCustomValue(HotSetAmountOfMemoryCommand.LOGABLE_FIELD_ERROR_MESSAGE, StringUtils.join(canDos, ","));
            auditLogDirector.log(logable, AuditLogType.FAILED_HOT_SET_NUMBER_OF_CPUS);
        }
    }

    private void checkTrustedService() {
        AuditLogableBase logable = new AuditLogableBase();
        logable.addCustomValue("VmName", getVmName());
        if (getParameters().getVm().isTrustedService() && !getVdsGroup().supportsTrustedService()) {
            auditLogDirector.log(logable, AuditLogType.USER_UPDATE_VM_FROM_TRUSTED_TO_UNTRUSTED);
        }
        else if (!getParameters().getVm().isTrustedService() && getVdsGroup().supportsTrustedService()) {
            auditLogDirector.log(logable, AuditLogType.USER_UPDATE_VM_FROM_UNTRUSTED_TO_TRUSTED);
        }
    }

    private void updateWatchdog() {
        // do not update if this flag is not set
        if (getParameters().isUpdateWatchdog()) {
            VdcQueryReturnValue query =
                    runInternalQuery(VdcQueryType.GetWatchdog, new IdQueryParameters(getParameters().getVmId()));
            List<VmWatchdog> watchdogs = query.getReturnValue();
            if (watchdogs.isEmpty()) {
                if (getParameters().getWatchdog() == null) {
                    // nothing to do, no watchdog and no watchdog to create
                } else {
                    WatchdogParameters parameters = new WatchdogParameters();
                    parameters.setId(getParameters().getVmId());
                    parameters.setAction(getParameters().getWatchdog().getAction());
                    parameters.setModel(getParameters().getWatchdog().getModel());
                    runInternalAction(VdcActionType.AddWatchdog, parameters, cloneContextAndDetachFromParent());
                }
            } else {
                WatchdogParameters watchdogParameters = new WatchdogParameters();
                watchdogParameters.setId(getParameters().getVmId());
                if (getParameters().getWatchdog() == null) {
                    // there is a watchdog in the vm, there should not be any, so let's delete
                    runInternalAction(VdcActionType.RemoveWatchdog, watchdogParameters, cloneContextAndDetachFromParent());
                } else {
                    // there is a watchdog in the vm, we have to update.
                    watchdogParameters.setAction(getParameters().getWatchdog().getAction());
                    watchdogParameters.setModel(getParameters().getWatchdog().getModel());
                    runInternalAction(VdcActionType.UpdateWatchdog, watchdogParameters, cloneContextAndDetachFromParent());
                }
            }

        }
    }

    private void updateGraphicsDevice() {
        for (GraphicsType type : getParameters().getGraphicsDevices().keySet()) {
            GraphicsDevice vmGraphicsDevice = getGraphicsDevOfType(type);
            if (vmGraphicsDevice == null) {
                if (getParameters().getGraphicsDevices().get(type) != null) {
                    getParameters().getGraphicsDevices().get(type).setVmId(getVmId());
                    getBackend().runInternalAction(VdcActionType.AddGraphicsDevice,
                            new GraphicsParameters(getParameters().getGraphicsDevices().get(type)));
                }
            } else {
                if (getParameters().getGraphicsDevices().get(type) == null) {
                    getBackend().runInternalAction(VdcActionType.RemoveGraphicsDevice,
                            new GraphicsParameters(vmGraphicsDevice));
                } else {
                    getParameters().getGraphicsDevices().get(type).setVmId(getVmId());
                    getBackend().runInternalAction(VdcActionType.UpdateGraphicsDevice,
                            new GraphicsParameters(getParameters().getGraphicsDevices().get(type)));
                }
            }
        }
    }

    // first dev or null
    private GraphicsDevice getGraphicsDevOfType(GraphicsType type) {
        List<GraphicsDevice> graphicsDevices = getGraphicsDevices();

        for (GraphicsDevice dev : graphicsDevices) {
            if (dev.getGraphicsType() == type) {
                return dev;
            }
        }

        return null;
    }

    private List<GraphicsDevice> getGraphicsDevices() {
        if (cachedGraphics == null) {
            cachedGraphics = getBackend()
                    .runInternalQuery(VdcQueryType.GetGraphicsDevices, new IdQueryParameters(getParameters().getVmId())).getReturnValue();
        }
        return cachedGraphics;
    }

    protected void updateVmPayload() {
        VmDeviceDao dao = getVmDeviceDao();
        VmPayload payload = getParameters().getVmPayload();

        if (payload != null || getParameters().isClearPayload()) {
            List<VmDevice> disks = dao.getVmDeviceByVmIdAndType(getVmId(), VmDeviceGeneralType.DISK);
            VmDevice oldPayload = null;
            for (VmDevice disk : disks) {
                if (VmPayload.isPayload(disk.getSpecParams())) {
                    oldPayload = disk;
                    break;
                }
            }

            if (oldPayload != null) {
                List<VmDeviceId> devs = new ArrayList<>();
                devs.add(oldPayload.getId());
                dao.removeAll(devs);
            }

            if (!getParameters().isClearPayload()) {
                VmDeviceUtils.addManagedDevice(new VmDeviceId(Guid.newGuid(), getVmId()),
                        VmDeviceGeneralType.DISK,
                        payload.getDeviceType(),
                        payload.getSpecParams(),
                        true,
                        true);
            }
        }
    }

    private void UpdateVmNetworks() {
        // check if the cluster has changed
        if (!Objects.equals(getVm().getVdsGroupId(), getParameters().getVmStaticData().getVdsGroupId())) {
            List<Network> networks =
                    getNetworkDao().getAllForCluster(getParameters().getVmStaticData().getVdsGroupId());
            List<VmNic> interfaces = getVmNicDao().getAllForVm(getParameters().getVmStaticData().getId());

            for (final VmNic iface : interfaces) {
                final Network network = NetworkHelper.getNetworkByVnicProfileId(iface.getVnicProfileId());
                Network net = LinqUtils.firstOrNull(networks, new Predicate<Network>() {
                    @Override
                    public boolean eval(Network n) {
                        return ObjectUtils.equals(n.getId(), network.getId());
                    }
                });

                // if network not exists in cluster we remove the network from the interface
                if (net == null) {
                    iface.setVnicProfileId(null);
                    getVmNicDao().update(iface);
                }

            }
        }
    }

    private void updateVmNumaNodes() {
        if (!getParameters().isUpdateNuma()) {
            return;
        }
        VmNumaNodeDao dao = DbFacade.getInstance().getVmNumaNodeDao();
        List<VmNumaNode> addList = new ArrayList<>();
        List<VmNumaNode> oldList = dao.getAllVmNumaNodeByVmId(getVmId());
        Map<Guid, VmNumaNode> removeMap = new HashMap<>();
        for (VmNumaNode node : oldList) {
            removeMap.put(node.getId(), node);
        }
        List<VmNumaNode> newList = getParameters().getVmStaticData().getvNumaNodeList();
        List<VmNumaNode> updateList = new ArrayList<>();
        if (newList != null) {
            for (VmNumaNode node : newList) {
                // no id means new entity
                if (node.getId() == null) {
                    addList.add(node);
                } else {
                    updateList.add(node);
                }
            }
        }
        for (VmNumaNode vmNumaNode : updateList) {
            removeMap.remove(vmNumaNode.getId());
        }
        VmNumaNodeOperationParameters params;
        if (!removeMap.isEmpty()) {
            params = new VmNumaNodeOperationParameters(getVmId(), new ArrayList<>(removeMap.values()));
            addAddtionalParams(params);
            addLogMessages(getBackend().runInternalAction(VdcActionType.RemoveVmNumaNodes, params));
        }
        if (!updateList.isEmpty()) {
            params = new VmNumaNodeOperationParameters(getVmId(), updateList);
            addAddtionalParams(params);
            addLogMessages(getBackend().runInternalAction(VdcActionType.UpdateVmNumaNodes, params));
        }
        if (!addList.isEmpty()) {
            params = new VmNumaNodeOperationParameters(getVmId(), addList);
            addAddtionalParams(params);
            addLogMessages(getBackend().runInternalAction(VdcActionType.AddVmNumaNodes, params));
        }
    }

    private void addLogMessages(VdcReturnValueBase returnValueBase) {
        if (!returnValueBase.getSucceeded()) {
            auditLogDirector.log(this, AuditLogType.NUMA_UPDATE_VM_NUMA_NODE_FAILED);
        }
    }

    private void addAddtionalParams(VmNumaNodeOperationParameters params) {
        params.setDedicatedHostList(getParameters().getVmStaticData().getDedicatedVmForVdsList());
        params.setNumaTuneMode(getParameters().getVmStaticData().getNumaTuneMode());
        params.setMigrationSupport(getParameters().getVmStaticData().getMigrationSupport());
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateVm.class);
        return super.getValidationGroups();
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__UPDATE);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
    }

    @Override
    protected boolean canDoAction() {
        if (!super.canDoAction()) {
            return false;
        }

        VM vmFromDB = getVm();
        VM vmFromParams = getParameters().getVm();

        // check if VM was changed to use latest
        if (vmFromDB.isUseLatestVersion() != vmFromParams.isUseLatestVersion() && vmFromParams.isUseLatestVersion()) {
            // check if a version change is actually required or just let the local command to update this field
            vmFromParams.setVmtGuid(getVmTemplateDao().getTemplateWithLatestVersionInChain(getVm().getVmtGuid()).getId());
        }

        // pool VMs are allowed to change template id, this verifies that the change is only between template versions.
        if (!vmFromDB.getVmtGuid().equals(vmFromParams.getVmtGuid())) {
            VmTemplate origTemplate = getVmTemplateDao().get(vmFromDB.getVmtGuid());
            VmTemplate newTemplate = getVmTemplateDao().get(vmFromParams.getVmtGuid());
            if (newTemplate == null) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
            } else if (origTemplate != null && !origTemplate.getBaseTemplateId().equals(newTemplate.getBaseTemplateId())) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_IS_ON_DIFFERENT_CHAIN);

            // check if pool vm - if not, the field is not legal and command will fail later on
            } else if (vmFromDB.getVmPoolId() != null) {
                isUpdateVmTemplateVersion = true;
                return true; // no more tests are needed because no more changes are allowed in this state
            }
        }

        if (getVdsGroup() == null) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_CAN_NOT_BE_EMPTY);
            return false;
        }

        if (vmFromDB.getVdsGroupId() == null) {
            failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_CAN_NOT_BE_EMPTY);
            return false;
        }

        if (!isVmExist()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (StringUtils.isEmpty(vmFromParams.getName())) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_NAME_MAY_NOT_BE_EMPTY);
        }

        // check that VM name is not too long
        boolean vmNameValidLength = isVmNameValidLength(vmFromParams);
        if (!vmNameValidLength) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
        }

        // Checking if a desktop with same name already exists
        if (!StringUtils.equals(vmFromDB.getName(), vmFromParams.getName())) {
            boolean exists = isVmWithSameNameExists(vmFromParams.getName(), getStoragePoolId());

            if (exists) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_NAME_ALREADY_USED);
            }
        }

        if (!validateCustomProperties(vmFromParams.getStaticData(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (!VmHandler.isOsTypeSupported(vmFromParams.getOs(),
                getVdsGroup().getArchitecture(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (!isCpuSupported(vmFromParams)) {
            return false;
        }

        if (vmFromParams.getSingleQxlPci() &&
                !VmHandler.isSingleQxlDeviceLegal(vmFromParams.getDefaultDisplayType(),
                        vmFromParams.getOs(),
                        getReturnValue().getCanDoActionMessages(),
                        getVdsGroup().getCompatibilityVersion())) {
            return false;
        }

        if (!areUpdatedFieldsLegal()) {
            return failCanDoAction(EngineMessage.VM_CANNOT_UPDATE_ILLEGAL_FIELD);
        }

        if (!vmFromDB.getVdsGroupId().equals(vmFromParams.getVdsGroupId())) {
            return failCanDoAction(EngineMessage.VM_CANNOT_UPDATE_CLUSTER);
        }

        if (!isDedicatedVdsExistOnSameCluster(vmFromParams.getStaticData(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // Check if number of monitors passed is legal
        if (!VmHandler.isNumOfMonitorsLegal(
                VmHandler.getResultingVmGraphics(VmDeviceUtils.getGraphicsTypesOfEntity(getVmId()), getParameters().getGraphicsDevices()),
                getParameters().getVmStaticData().getNumOfMonitors(),
                getReturnValue().getCanDoActionMessages())) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_ILLEGAL_NUM_OF_MONITORS);
        }

        // Check PCI and IDE limits are ok
        if (!isValidPciAndIdeLimit(vmFromParams)) {
            return false;
        }

        if (!VmTemplateCommand.isVmPriorityValueLegal(vmFromParams.getPriority(),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        if (vmFromDB.getVmPoolId() != null && vmFromParams.isStateless()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_FROM_POOL_CANNOT_BE_STATELESS);
        }

        if (!AddVmCommand.checkCpuSockets(vmFromParams.getNumOfSockets(),
                vmFromParams.getCpuPerSocket(), getVdsGroup().getCompatibilityVersion()
                .toString(), getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // check for Vm Payload
        if (getParameters().getVmPayload() != null) {
            if (!checkPayload(getParameters().getVmPayload(), vmFromParams.getIsoPath())) {
                return false;
            }
            // we save the content in base64 string
            for (Map.Entry<String, String> entry : getParameters().getVmPayload().getFiles().entrySet()) {
                entry.setValue(new String(BASE_64.encode(entry.getValue().getBytes()), Charset.forName(CharEncoding.UTF_8)));
            }
        }

        // check for Vm Watchdog Model
        if (getParameters().getWatchdog() != null) {
            if (!validate((new VmWatchdogValidator(vmFromParams.getOs(),
                    getParameters().getWatchdog(),
                    getVdsGroup().getCompatibilityVersion())).isValid())) {
                return false;
            }
        }

        // Check that the USB policy is legal
        if (!VmHandler.isUsbPolicyLegal(vmFromParams.getUsbPolicy(),
                vmFromParams.getOs(),
                getVdsGroup(),
                getReturnValue().getCanDoActionMessages())) {
            return false;
        }

        // Check if the graphics and display from parameters are supported
        if (!VmHandler.isGraphicsAndDisplaySupported(vmFromParams.getOs(),
                VmHandler.getResultingVmGraphics(VmDeviceUtils.getGraphicsTypesOfEntity(getVmId()), getParameters().getGraphicsDevices()),
                vmFromParams.getDefaultDisplayType(),
                getReturnValue().getCanDoActionMessages(),
                getVdsGroup().getCompatibilityVersion())) {
            return false;
        }

        if (!FeatureSupported.isMigrationSupported(getVdsGroup().getArchitecture(), getVdsGroup().getCompatibilityVersion())
                && vmFromParams.getMigrationSupport() != MigrationSupport.PINNED_TO_HOST) {
            return failCanDoAction(EngineMessage.VM_MIGRATION_IS_NOT_SUPPORTED);
        }

        // check cpuPinning
        if (!isCpuPinningValid(vmFromParams.getCpuPinning(), vmFromParams.getStaticData())) {
            return false;
        }

        if (!validatePinningAndMigration(getReturnValue().getCanDoActionMessages(),
                getParameters().getVm().getStaticData(), getParameters().getVm().getCpuPinning())) {
            return false;
        }

        if (vmFromParams.isUseHostCpuFlags()
                && vmFromParams.getMigrationSupport() != MigrationSupport.PINNED_TO_HOST) {
            return failCanDoAction(EngineMessage.VM_HOSTCPU_MUST_BE_PINNED_TO_HOST);
        }

        if (!isCpuSharesValid(vmFromParams)) {
            return failCanDoAction(EngineMessage.QOS_CPU_SHARES_OUT_OF_RANGE);
        }

        if (isVirtioScsiEnabled())  {
            // Verify cluster compatibility
            if (!FeatureSupported.virtIoScsi(getVdsGroup().getCompatibilityVersion())) {
                return failCanDoAction(EngineMessage.VIRTIO_SCSI_INTERFACE_IS_NOT_AVAILABLE_FOR_CLUSTER_LEVEL);
            }

            // Verify OS compatibility
            if (!VmHandler.isOsTypeSupportedForVirtioScsi(vmFromParams.getOs(), getVdsGroup().getCompatibilityVersion(),
                    getReturnValue().getCanDoActionMessages())) {
                return false;
            }
        }

        VmValidator vmValidator = createVmValidator(vmFromParams);
        if (Boolean.FALSE.equals(getParameters().isVirtioScsiEnabled()) && !validate(vmValidator.canDisableVirtioScsi(null))) {
            return false;
        }

        if (vmFromParams.getMinAllocatedMem() > vmFromParams.getMemSizeMb()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_MIN_MEMORY_CANNOT_EXCEED_MEMORY_SIZE);
        }

        if (!setAndValidateCpuProfile()) {
            return false;
        }

        if (isBalloonEnabled() && !osRepository.isBalloonEnabled(getParameters().getVmStaticData().getOsId(),
                getVdsGroup().getCompatibilityVersion())) {
            addCanDoActionMessageVariable("clusterArch", getVdsGroup().getArchitecture());
            return failCanDoAction(EngineMessage.BALLOON_REQUESTED_ON_NOT_SUPPORTED_ARCH);
        }

        if (!validate(VmHandler.checkNumaPreferredTuneMode(getParameters().getVmStaticData().getNumaTuneMode(),
                getParameters().getVmStaticData().getvNumaNodeList(),
                getVmId()))) {
            return false;
        }
        if (getParameters().getVm().getMigrationSupport() == MigrationSupport.PINNED_TO_HOST &&
                !validate(VmHandler.checkVmNumaNodesIntegrity(getParameters().getVm(),
                        getVm(),
                        getParameters().isUpdateNuma()))) {
            return false;
        }

        if (getParameters().getVmLargeIcon() != null && !validate(IconValidator.validate(
                IconValidator.DimensionsType.LARGE_CUSTOM_ICON,
                getParameters().getVmLargeIcon()))) {
            return false;
        }

        if (getParameters().getVmStaticData() != null
                && getParameters().getVmStaticData().getSmallIconId() != null
                && !validate(IconValidator.validateIconId(getParameters().getVmStaticData().getSmallIconId(), "Small"))) {
            return false;
        }

        if (getParameters().getVmStaticData() != null
                && getParameters().getVmStaticData().getLargeIconId() != null
                && !validate(IconValidator.validateIconId(getParameters().getVmStaticData().getLargeIconId(), "Large"))) {
            return false;
        }

        if (vmFromParams.getProviderId() != null) {
            Provider<?> provider = providerDao.get(vmFromParams.getProviderId());
            if (provider == null) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_PROVIDER_DOESNT_EXIST);
            }

            if (provider.getType() != ProviderType.FOREMAN) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_HOST_PROVIDER_TYPE_MISMATCH);
            }
        }

        return true;
    }

    protected boolean isDedicatedVdsExistOnSameCluster(VmBase vm,
            ArrayList<String> canDoActionMessages) {
        return VmHandler.validateDedicatedVdsExistOnSameCluster(vm, canDoActionMessages);
    }

    protected boolean isValidPciAndIdeLimit(VM vmFromParams) {
        List<Disk> allDisks = getDbFacade().getDiskDao().getAllForVm(getVmId());
        List<VmNic> interfaces = getVmNicDao().getAllForVm(getVmId());

        return checkPciAndIdeLimit(
                vmFromParams.getOs(),
                getVdsGroup().getCompatibilityVersion(),
                vmFromParams.getNumOfMonitors(),
                interfaces,
                allDisks,
                isVirtioScsiEnabled(),
                hasWatchdog(),
                isBalloonEnabled(),
                isSoundDeviceEnabled(),
                getReturnValue().getCanDoActionMessages());
    }

    private boolean isVmExist() {
        return getParameters().getVmStaticData() != null && getVm() != null;
    }

    protected boolean areUpdatedFieldsLegal() {
        return VmHandler.isUpdateValid(getVm().getStaticData(),
                getParameters().getVmStaticData(),
                VMStatus.Down);
    }

    /**
     * check if we need to use running-configuration
     * @return true if vm is running and we change field that has @EditableOnVmStatusField annotation
     *          or runningConfiguration already exist
     */
    private boolean isRunningConfigurationNeeded() {
        return getVm().isNextRunConfigurationExists() ||
                !VmHandler.isUpdateValid(getVm().getStaticData(),
                        getParameters().getVmStaticData(),
                        getVm().getStatus(),
                        isHotSetEnabled()) ||
                !VmHandler.isUpdateValidForVmDevices(getVmId(), getVm().getStatus(), getParameters());
    }

    private boolean isHotSetEnabled() {
        return !getParameters().isApplyChangesLater();
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        final List<PermissionSubject> permissionList = super.getPermissionCheckSubjects();

        if (isVmExist()) {
            // user need specific permission to change custom properties
            if (!StringUtils.equals(
                    getVm().getPredefinedProperties(),
                    getParameters().getVmStaticData().getPredefinedProperties())
                    || !StringUtils.equals(
                            getVm().getUserDefinedProperties(),
                            getParameters().getVmStaticData().getUserDefinedProperties())) {
                permissionList.add(new PermissionSubject(getParameters().getVmId(),
                        VdcObjectType.VM,
                        ActionGroup.CHANGE_VM_CUSTOM_PROPERTIES));
            }

            // host-specific parameters can be changed by administration role only
            if (isDedicatedVmForVdsChanged() || isCpuPinningChanged()) {
                permissionList.add(
                        new PermissionSubject(getParameters().getVmId(),
                                VdcObjectType.VM,
                                ActionGroup.EDIT_ADMIN_VM_PROPERTIES));
            }
        }

        return permissionList;
    }

    private boolean isDedicatedVmForVdsChanged() {
        List<Guid> paramList = getParameters().getVmStaticData().getDedicatedVmForVdsList();
        List<Guid> vmList = getVm().getDedicatedVmForVdsList();
        if (vmList == null && paramList == null){
            return false;
        }
        if (vmList == null || paramList == null){
            return true;
        }
        //  vmList.equals(paramList) not good enough, the lists order could change
        if (vmList.size() != paramList.size()){
            return true;
        }
        for (Guid origGuid : vmList) {
            if (paramList.contains(origGuid) == false){
                return true;
            }
        }
        return false;
    }

    private boolean isCpuPinningChanged() {
        return !(getVm().getCpuPinning() == null ?
                getParameters().getVmStaticData().getCpuPinning() == null :
                getVm().getCpuPinning().equals(getParameters().getVmStaticData().getCpuPinning()));
    }

    @Override
    public Guid getVmId() {
        if (super.getVmId().equals(Guid.Empty)) {
            super.setVmId(getParameters().getVmStaticData().getId());
        }
        return super.getVmId();
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        if (!StringUtils.isBlank(getParameters().getVm().getName())) {
            return Collections.singletonMap(getParameters().getVm().getName(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.VM_NAME, EngineMessage.ACTION_TYPE_FAILED_VM_IS_BEING_UPDATED));
        }
        return null;
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        return Collections.singletonMap(
                getVmId().toString(),
                LockMessagesMatchUtil.makeLockingPair(
                        LockingGroup.VM,
                        EngineMessage.ACTION_TYPE_FAILED_VM_IS_BEING_UPDATED));
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaVdsConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();

        // The cases must be persistent with the create_functions_sp
        if (!getQuotaManager().isVmStatusQuotaCountable(getVm().getStatus())) {
            list.add(new QuotaSanityParameter(getParameters().getVmStaticData().getQuotaId(), null));
            quotaSanityOnly = true;
        } else {
            if (getParameters().getVmStaticData().getQuotaId() == null
                    || getParameters().getVmStaticData().getQuotaId().equals(Guid.Empty)
                    || !getParameters().getVmStaticData().getQuotaId().equals(getVm().getQuotaId())) {
                list.add(new QuotaVdsGroupConsumptionParameter(getVm().getQuotaId(),
                        null,
                        QuotaConsumptionParameter.QuotaAction.RELEASE,
                        getVdsGroupId(),
                        getVm().getVmtCpuPerSocket() * getVm().getNumOfSockets(),
                        getVm().getMemSizeMb()));
                list.add(new QuotaVdsGroupConsumptionParameter(getParameters().getVmStaticData().getQuotaId(),
                        null,
                        QuotaConsumptionParameter.QuotaAction.CONSUME,
                        getParameters().getVmStaticData().getVdsGroupId(),
                        getParameters().getVmStaticData().getCpuPerSocket()
                                * getParameters().getVmStaticData().getNumOfSockets(),
                        getParameters().getVmStaticData().getMemSizeMb()));
            }

        }
        return list;
    }
    @Override
    public String getEntityType() {
        return VdcObjectType.VM.getVdcObjectTranslation();
    }

    @Override
    public String getEntityOldName() {
        return oldVm.getName();
    }

    @Override
    public String getEntityNewName() {
        return getParameters().getVmStaticData().getName();
    }

    @Override
    public void setEntityId(AuditLogableBase logable) {
       logable.setVmId(oldVm.getId());
    }
    @Override
    public void addQuotaPermissionSubject(List<PermissionSubject> quotaPermissionList) {
        // if only quota sanity is checked the user may use a quota he cannot consume
        // (it will be consumed only when the vm will run)
        if (!quotaSanityOnly) {
            super.addQuotaPermissionSubject(quotaPermissionList);
        }
    }

    protected boolean isVirtioScsiEnabled() {
        Boolean virtioScsiEnabled = getParameters().isVirtioScsiEnabled();
        return virtioScsiEnabled != null ? virtioScsiEnabled : isVirtioScsiEnabledForVm(getVmId());
    }

    public boolean isVirtioScsiEnabledForVm(Guid vmId) {
        return VmDeviceUtils.hasVirtioScsiController(vmId);
    }

    protected boolean isBalloonEnabled() {
        Boolean balloonEnabled = getParameters().isBalloonEnabled();
        return balloonEnabled != null ? balloonEnabled : VmDeviceUtils.hasMemoryBalloon(getVmId());
    }

    protected boolean isSoundDeviceEnabled() {
        Boolean soundDeviceEnabled = getParameters().isSoundDeviceEnabled();
        return soundDeviceEnabled != null ? soundDeviceEnabled :
                VmDeviceUtils.hasSoundDevice(getVmId());
    }

    protected boolean hasWatchdog() {
        return getParameters().getWatchdog() != null;
    }

    public VmValidator createVmValidator(VM vm) {
        return new VmValidator(vm);
    }
}
