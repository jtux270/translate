package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.businessentities.Entities;
import org.ovirt.engine.core.common.businessentities.storage.LUNs;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.GetVGInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

public class AddExistingBlockStorageDomainCommand<T extends StorageDomainManagementParameter> extends
        AddStorageDomainCommon<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected AddExistingBlockStorageDomainCommand(Guid commandId) {
        super(commandId);
    }

    public AddExistingBlockStorageDomainCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeCommand() {
        updateStaticDataDefaults();
        // Add StorageDomain object to DB and update statistics
        addStorageDomainInDb();
        updateStorageDomainDynamicFromIrs();

        // Add relevant LUNs to DB
        List<LUNs> luns = getLUNsFromVgInfo(getStorageDomain().getStorage());
        saveLUNsInDB(luns);

        setSucceeded(true);
    }

    @Override
    protected boolean canAddDomain() {
        if (getStorageDomainStaticDao().get(getStorageDomain().getId()) != null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_DOMAIN_ALREADY_EXIST);
        }

        List<LUNs> lunsOnStorage = getLUNsFromVgInfo(getStorageDomain().getStorage());
        if (lunsOnStorage.isEmpty()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_PROBLEM_WITH_CANDIDATE_INFO);
        }
        if (CollectionUtils.containsAny(Entities.getIds(lunsOnStorage), Entities.getIds(getAllLuns()))) {
            log.info("There are existing luns in the system which are part of VG id '{}'",
                    getStorageDomain().getStorage());
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_IMPORT_STORAGE_DOMAIN_EXTERNAL_LUN_DISK_EXIST);
        }

        return true;
    }

    protected List<LUNs> getAllLuns() {
        return getDbFacade().getLunDao().getAll();
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(LockProperties.Scope.Execution);
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        if (getParameters().getStorageDomainId() != null) {
            return Collections.singletonMap(getParameters().getStorageDomainId().toString(),
                    LockMessagesMatchUtil.makeLockingPair(LockingGroup.STORAGE, EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED));
        }
        return null;
    }

    protected List<LUNs> getLUNsFromVgInfo(String vgId) {
        List<LUNs> luns = new ArrayList<>();
        VDSReturnValue returnValue;

        try {
            returnValue = runVdsCommand(VDSCommandType.GetVGInfo,
                    new GetVGInfoVDSCommandParameters(getParameters().getVdsId(), vgId));
        } catch (RuntimeException e) {
            log.error("Could not get info for VG ID '{}': {}",
                    vgId, e.getMessage());
            log.debug("Exception", e);
            return luns;
        }

        luns.addAll((ArrayList<LUNs>) returnValue.getReturnValue());

        return luns;
    }

    protected void saveLUNsInDB(final List<LUNs> luns) {
        TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
            @Override
            public Void runInTransaction() {
                for (LUNs lun : luns) {
                    proceedLUNInDb(lun, getStorageDomain().getStorageType(), getStorageDomain().getStorage());
                }
                return null;
            }
        });
    }

}
