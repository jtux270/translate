package org.ovirt.engine.core.bll.storage;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.context.CompensationContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.StorageDomainParametersBase;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMap;
import org.ovirt.engine.core.common.businessentities.StoragePoolIsoMapId;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.storage.LUNStorageServerConnectionMap;
import org.ovirt.engine.core.common.businessentities.storage.LUNStorageServerConnectionMapId;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.eventqueue.Event;
import org.ovirt.engine.core.common.eventqueue.EventQueue;
import org.ovirt.engine.core.common.eventqueue.EventResult;
import org.ovirt.engine.core.common.eventqueue.EventType;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dao.BaseDiskDao;
import org.ovirt.engine.core.dao.CommandEntityDao;
import org.ovirt.engine.core.dao.DiskImageDynamicDao;
import org.ovirt.engine.core.dao.ImageDao;
import org.ovirt.engine.core.dao.ImageStorageDomainMapDao;
import org.ovirt.engine.core.dao.LunDao;
import org.ovirt.engine.core.dao.StorageServerConnectionDao;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.threadpool.ThreadPoolUtil;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public abstract class StorageDomainCommandBase<T extends StorageDomainParametersBase> extends
        StorageHandlingCommandBase<T> {

    @Inject
    protected EventQueue eventQueue;

    protected StorageDomainCommandBase(T parameters) {
        this(parameters, null);
    }

    protected StorageDomainCommandBase(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }


    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected StorageDomainCommandBase(Guid commandId) {
        super(commandId);
    }

    @Override
    public Guid getStorageDomainId() {
        return getParameters() != null ? !Guid.Empty.equals(getParameters().getStorageDomainId()) ? getParameters()
                .getStorageDomainId() : super.getStorageDomainId() : super.getStorageDomainId();
    }

    protected boolean canDetachDomain(boolean isDestroyStoragePool, boolean isRemoveLast, boolean isInternal) {
        return checkStoragePool()
                && checkStorageDomain()
                && checkStorageDomainStatus(StorageDomainStatus.Inactive, StorageDomainStatus.Maintenance)
                && (isMaster() || isDestroyStoragePool || checkMasterDomainIsUp())
                && isNotLocalData(isInternal)
                && isDetachAllowed(isRemoveLast)
                && isCinderStorageHasNoDisks();
    }

    protected boolean isDetachAllowed(final boolean isRemoveLast) {
        boolean returnValue = true;
        if (getStoragePoolIsoMap() == null) {
            returnValue = false;
            addCanDoActionMessage(EngineMessage.STORAGE_DOMAIN_NOT_ATTACHED_TO_STORAGE_POOL);
        } else if (!isRemoveLast
                && isMaster()) {

            StorageDomain storage_domains =
                    LinqUtils.firstOrNull(getStorageDomainDao().getAllForStoragePool
                            (getStorageDomain().getStoragePoolId()),
                            new Predicate<StorageDomain>() {
                                @Override
                                public boolean eval(StorageDomain a) {
                                    return a.getId().equals(getStorageDomain().getId())
                                            && a.getStatus() == StorageDomainStatus.Active;
                                }
                            });
            if (storage_domains == null) {
                returnValue = false;
                addCanDoActionMessage(EngineMessage.ERROR_CANNOT_DETACH_LAST_STORAGE_DOMAIN);
            }
        }
        return returnValue;
    }

    protected boolean isNotLocalData(final boolean isInternal) {
        boolean returnValue = true;
        if (this.getStoragePool().isLocal()
                && getStorageDomain().getStorageDomainType() == StorageDomainType.Data
                && !isInternal) {
            returnValue = false;
            addCanDoActionMessage(EngineMessage.VDS_GROUP_CANNOT_DETACH_DATA_DOMAIN_FROM_LOCAL_STORAGE);
        }
        return returnValue;
    }

    private StoragePoolIsoMap getStoragePoolIsoMap() {
        return getStoragePoolIsoMapDao()
                .get(new StoragePoolIsoMapId(getStorageDomain().getId(),
                        getStoragePoolId()));
    }

    protected boolean isCinderStorageHasNoDisks() {
        if (getStorageDomain().getStorageType() == StorageType.CINDER) {
            return validate(CINDERStorageHelper.isCinderHasNoImages(getStorageDomainId()));
        }
        return true;
    }

    private boolean isMaster() {
        return getStorageDomain().getStorageDomainType() == StorageDomainType.Master;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__TYPE__STORAGE__DOMAIN);
    }

    protected boolean checkStorageDomainNameLengthValid() {
        boolean result = true;
        if (getStorageDomain().getStorageName().length() > Config
                .<Integer> getValue(ConfigValues.StorageDomainNameSizeLimit)) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_NAME_LENGTH_IS_TOO_LONG);
            result = false;
        }
        return result;
    }

    protected boolean checkStorageDomain() {
        return isStorageDomainNotNull(getStorageDomain());
    }

    protected boolean checkStorageDomainStatus(final StorageDomainStatus... statuses) {
        boolean valid = false;
        StorageDomainStatus status = getStorageDomainStatus();
        if (status != null) {
            valid = Arrays.asList(statuses).contains(status);
        }
        if (!valid) {
            if (status.isStorageDomainInProcess()) {
                return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED);
            }
            addStorageDomainStatusIllegalMessage();
        }
        return valid;
    }

    protected boolean checkStorageDomainStatusNotEqual(StorageDomainStatus status) {
        boolean returnValue = false;
        if (getStorageDomainStatus() != null) {
            returnValue = (getStorageDomainStatus() != status);
            if (!returnValue) {
                addStorageDomainStatusIllegalMessage();
            }
        }
        return returnValue;
    }

    protected boolean checkMasterDomainIsUp() {
        boolean returnValue = true;
        List<StorageDomain> storageDomains = getStorageDomainDao().getAllForStoragePool(getStoragePool().getId());
        storageDomains = LinqUtils.filter(storageDomains, new Predicate<StorageDomain>() {
            @Override
            public boolean eval(StorageDomain a) {
                return a.getStorageDomainType() == StorageDomainType.Master
                        && a.getStatus() == StorageDomainStatus.Active;
            }
        });
        if (storageDomains.isEmpty()) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_MASTER_STORAGE_DOMAIN_NOT_ACTIVE);
            returnValue = false;
        }
        return returnValue;
    }

    protected void setStorageDomainStatus(StorageDomainStatus status, CompensationContext context) {
        if (getStorageDomain() != null && getStorageDomain().getStoragePoolId() != null) {
            StoragePoolIsoMap map = getStorageDomain().getStoragePoolIsoMapData();
            if(context != null) {
                context.snapshotEntityStatus(map);
            }
            getStorageDomain().setStatus(status);
            getStoragePoolIsoMapDao().updateStatus(map.getId(), status);
        }
    }

    protected boolean isLunsAlreadyInUse(List<String> lunIds) {
        // Get LUNs from DB
        List<LUNs> lunsFromDb = getLunDao().getAll();
        Set<LUNs> lunsUsedBySDs = new HashSet<>();
        Set<LUNs> lunsUsedByDisks = new HashSet<>();

        for (LUNs lun : lunsFromDb) {
            if (lunIds.contains(lun.getLUN_id())) {
                if (lun.getStorageDomainId() != null) {
                    // LUN is already part of a storage domain
                    lunsUsedBySDs.add(lun);
                }
                if (lun.getDiskId() != null) {
                    // LUN is already used by a disk
                    lunsUsedByDisks.add(lun);
                }
            }
        }

        if (!lunsUsedBySDs.isEmpty()) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_LUNS_ALREADY_PART_OF_STORAGE_DOMAINS);
            Set<String> formattedIds = new HashSet<>();
            for (LUNs lun : lunsUsedBySDs) {
                formattedIds.add(getFormattedLunId(lun, lun.getStorageDomainName()));
            }
            addCanDoActionMessageVariable("lunIds", StringUtils.join(formattedIds, ", "));
        }

        if (!lunsUsedByDisks.isEmpty()) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_LUNS_ALREADY_USED_BY_DISKS);
            Set<String> formattedIds = new HashSet<>();
            for (LUNs lun : lunsUsedByDisks) {
                formattedIds.add(getFormattedLunId(lun, lun.getDiskAlias()));
            }
            addCanDoActionMessageVariable("lunIds", StringUtils.join(formattedIds, ", "));
        }

       return !lunsUsedBySDs.isEmpty() || !lunsUsedByDisks.isEmpty();
    }

    protected String getFormattedLunId(LUNs lun, String usedByEntityName) {
        return String.format("%1$s (%2$s)", lun.getLUN_id(), usedByEntityName);
    }

    public static void proceedLUNInDb(final LUNs lun, StorageType storageType) {
        proceedLUNInDb(lun, storageType, "");
    }

    protected LunDao getLunDao() {
        return DbFacade.getInstance().getLunDao();
    }

    public static void proceedLUNInDb(final LUNs lun, StorageType storageType, String volumeGroupId) {
        lun.setvolume_group_id(volumeGroupId);
        if (DbFacade.getInstance().getLunDao().get(lun.getLUN_id()) == null) {
            DbFacade.getInstance().getLunDao().save(lun);
        } else if (!volumeGroupId.isEmpty()) {
            DbFacade.getInstance().getLunDao().update(lun);
        }

        if (storageType == StorageType.FCP) {
            // No need to handle connections (FCP storage doesn't utilize connections).
            return;
        }

        for (StorageServerConnections connection : lun.getLunConnections()) {
            StorageServerConnections dbConnection = ISCSIStorageHelper.findConnectionWithSameDetails(connection);
            if (dbConnection == null) {
                connection.setid(Guid.newGuid().toString());
                connection.setstorage_type(storageType);
                DbFacade.getInstance().getStorageServerConnectionDao().save(connection);

            } else {
                connection.setid(dbConnection.getid());
            }
            if (DbFacade.getInstance()
                    .getStorageServerConnectionLunMapDao()
                    .get(new LUNStorageServerConnectionMapId(lun.getLUN_id(),
                            connection.getid())) == null) {
                DbFacade.getInstance().getStorageServerConnectionLunMapDao().save(
                        new LUNStorageServerConnectionMap(lun.getLUN_id(), connection.getid()));
            }
        }
    }

    protected List<Pair<Guid, Boolean>> connectHostsInUpToDomainStorageServer() {
        List<VDS> hostsInStatusUp = getAllRunningVdssInPool();
        List<Callable<Pair<Guid, Boolean>>> callables = new LinkedList<>();
        for (final VDS vds : hostsInStatusUp) {
            callables.add(new Callable<Pair<Guid, Boolean>>() {
                @Override
                public Pair<Guid, Boolean> call() throws Exception {
                    Pair<Guid, Boolean> toReturn = new Pair<>(vds.getId(), Boolean.FALSE);
                    try {
                        boolean connectResult = StorageHelperDirector.getInstance().getItem(getStorageDomain().getStorageType())
                                .connectStorageToDomainByVdsId(getStorageDomain(), vds.getId());
                        toReturn.setSecond(connectResult);
                    } catch (RuntimeException e) {
                        log.error("Failed to connect host '{}' to storage domain (name '{}', id '{}'): {}",
                                vds.getName(),
                                getStorageDomain().getName(),
                                getStorageDomain().getId(),
                                e.getMessage());
                        log.debug("Exception", e);
                    }
                    return toReturn;
                }
            });
        }

        return ThreadPoolUtil.invokeAll(callables);
    }

    protected List<Pair<Guid, Boolean>> disconnectHostsInUpToDomainStorageServer() {
        List<VDS> hostsInStatusUp = getAllRunningVdssInPool();
        List<Callable<Pair<Guid, Boolean>>> callables = new LinkedList<>();
        for (final VDS vds : hostsInStatusUp) {
            callables.add(new Callable<Pair<Guid, Boolean>>() {
                @Override
                public Pair<Guid, Boolean> call() throws Exception {
                    Pair<Guid, Boolean> toReturn = new Pair<>(vds.getId(), Boolean.FALSE);
                    try {
                        boolean connectResult = StorageHelperDirector.getInstance().getItem(getStorageDomain().getStorageType())
                                .disconnectStorageFromDomainByVdsId(getStorageDomain(), vds.getId());
                        toReturn.setSecond(connectResult);
                    } catch (RuntimeException e) {
                        log.error("Failed to disconnect host '{}' to storage domain (name '{}', id '{}'): {}",
                                vds.getName(),
                                getStorageDomain().getName(),
                                getStorageDomain().getId(),
                                e.getMessage());
                        log.debug("Exception", e);
                    }
                    return toReturn;
                }
            });
        }

        return ThreadPoolUtil.invokeAll(callables);
    }

    protected void disconnectAllHostsInPool() {
        getEventQueue().submitEventSync(
                new Event(getParameters().getStoragePoolId(),
                        getParameters().getStorageDomainId(),
                        null,
                        EventType.POOLREFRESH,
                        ""),
                new Callable<EventResult>() {
                    @Override
                    public EventResult call() {
                        runSynchronizeOperation(new RefreshStoragePoolAndDisconnectAsyncOperationFactory());
                        return null;
                    }
                });
    }

    /**
     *  The new master is a data domain which is preferred to be in Active/Unknown status, if selectInactiveWhenNoActiveUnknownDomains
     * is set to True, an Inactive domain will be returned in case that no domain in Active/Unknown status was found.
     * @return an elected master domain or null
     */
    protected StorageDomain electNewMaster(boolean duringReconstruct, boolean selectInactiveWhenNoActiveUnknownDomains, boolean canChooseCurrentMasterAsNewMaster) {
        if (getStoragePool() == null) {
            log.warn("Cannot elect new master: storage pool not found");
            return null;
        }

        List<StorageDomain> storageDomains = getStorageDomainDao().getAllForStoragePool(getStoragePool().getId());

        if (storageDomains.isEmpty()) {
            log.warn("Cannot elect new master, no storage domains found for pool {}", getStoragePool().getName());
            return null;
        }

        Collections.sort(storageDomains, LastTimeUsedAsMasterComp.instance);

        StorageDomain newMaster = null;
        StorageDomain storageDomain = getStorageDomain();

        for (StorageDomain dbStorageDomain : storageDomains) {
            if ((storageDomain == null || (duringReconstruct || !dbStorageDomain.getId()
                    .equals(storageDomain.getId())))
                    && ((dbStorageDomain.getStorageDomainType() == StorageDomainType.Data)
                    ||
                    (canChooseCurrentMasterAsNewMaster && dbStorageDomain.getStorageDomainType() == StorageDomainType.Master))) {
                if (dbStorageDomain.getStatus() == StorageDomainStatus.Active
                        || dbStorageDomain.getStatus() == StorageDomainStatus.Unknown) {
                    newMaster = dbStorageDomain;
                    break;
                } else if (selectInactiveWhenNoActiveUnknownDomains && newMaster == null
                        && dbStorageDomain.getStatus() == StorageDomainStatus.Inactive) {
                    // if the found domain is inactive, we don't break to continue and look for
                    // active/unknown domain.
                    newMaster = dbStorageDomain;
                }
            }
        }

        return newMaster;
    }

    /**
     * returns new master domain which is in Active/Unknown status
     * @return an elected master domain or null
     */
    protected StorageDomain electNewMaster() {
        return electNewMaster(false, false, false);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getParameters().getStorageDomainId(),
                VdcObjectType.Storage,
                getActionType().getActionGroup()));
    }

    protected void changeStorageDomainStatusInTransaction(final StoragePoolIsoMap map,
                                                          final StorageDomainStatus status) {
        changeStorageDomainStatusInTransaction(map, status, getCompensationContext());
    }

    protected void changeStorageDomainStatusInTransaction(final StoragePoolIsoMap map,
            final StorageDomainStatus status, final CompensationContext context) {
        executeInNewTransaction(new TransactionMethod<StoragePoolIsoMap>() {
            @SuppressWarnings("synthetic-access")
            @Override
            public StoragePoolIsoMap runInTransaction() {
                context.snapshotEntityStatus(map);
                map.setStatus(status);
                getStoragePoolIsoMapDao().updateStatus(map.getId(), map.getStatus());
                context.stateChanged();
                return null;
            }
        });
    }

    protected void changeDomainStatusWithCompensation(StoragePoolIsoMap map, StorageDomainStatus compensateStatus,
                                                      StorageDomainStatus newStatus, CompensationContext context) {
        map.setStatus(compensateStatus);
        changeStorageDomainStatusInTransaction(map, newStatus, context);
    }

    private StorageDomainStatus getStorageDomainStatus() {
        StorageDomainStatus status = null;
        if (getStorageDomain() != null) {
            status = getStorageDomain().getStatus();
        }
        return status;
    }

    protected void addStorageDomainStatusIllegalMessage() {
        addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_STATUS_ILLEGAL2);
        addCanDoActionMessageVariable("status", getStorageDomainStatus());
    }

    protected BaseDiskDao getBaseDiskDao() {
        return getDbFacade().getBaseDiskDao();
    }

    protected ImageDao getImageDao() {
        return getDbFacade().getImageDao();
    }

    protected DiskImageDynamicDao getDiskImageDynamicDao() {
        return getDbFacade().getDiskImageDynamicDao();
    }

    protected ImageStorageDomainMapDao getImageStorageDomainMapDao() {
        return getDbFacade().getImageStorageDomainMapDao();
    }

    protected StorageServerConnectionDao getStorageServerConnectionDao() {
        return getDbFacade().getStorageServerConnectionDao();
    }

    protected IStorageHelper getStorageHelper(StorageDomain storageDomain) {
        return StorageHelperDirector.getInstance().getItem(storageDomain.getStorageType());
    }

    protected void executeInNewTransaction(TransactionMethod<?> method) {
        TransactionSupport.executeInNewTransaction(method);
    }

    private static final class LastTimeUsedAsMasterComp implements Comparator<StorageDomain>, Serializable {
        private static final long serialVersionUID = -7736904426129973519L;
        public static final LastTimeUsedAsMasterComp instance = new LastTimeUsedAsMasterComp();

        @Override
        public int compare(StorageDomain o1, StorageDomain o2) {
            return Long.compare(o1.getLastTimeUsedAsMaster(), o2.getLastTimeUsedAsMaster());
        }
    }

    protected CommandEntityDao getCommandEntityDao() {
        return getDbFacade().getCommandEntityDao();
    }

    protected boolean isCinderStorageDomain() {
        return getStorageDomain().getStorageType().isCinderDomain();
    }

    protected EventQueue getEventQueue() {
        return eventQueue;
    }
}
