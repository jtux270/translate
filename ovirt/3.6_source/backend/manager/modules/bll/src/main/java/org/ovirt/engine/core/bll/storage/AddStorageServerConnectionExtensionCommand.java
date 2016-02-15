package org.ovirt.engine.core.bll.storage;

import java.util.Collections;
import java.util.Map;

import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.action.StorageServerConnectionExtensionParameters;
import org.ovirt.engine.core.common.businessentities.storage.StorageServerConnectionExtension;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;

public class AddStorageServerConnectionExtensionCommand<T extends StorageServerConnectionExtensionParameters> extends StorageServerConnectionExtensionCommandBase<StorageServerConnectionExtensionParameters> {
    public AddStorageServerConnectionExtensionCommand(T parameters) {
        super(parameters);
    }

    public AddStorageServerConnectionExtensionCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
    }

    @Override
    protected void init() {
        super.init();
        setVdsId(((StorageServerConnectionExtensionParameters) getParameters()).getStorageServerConnectionExtension().getHostId());
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(EngineMessage.VAR__ACTION__ADD);
    }

    @Override
    protected boolean canDoAction() {
        StorageServerConnectionExtension newConnExt = getParameters().getStorageServerConnectionExtension();
        StorageServerConnectionExtension existingConnExt =
                getStorageServerConnectionExtensionDao().getByHostIdAndTarget(newConnExt.getHostId(),
                        newConnExt.getIqn());
        if (existingConnExt != null) {
            addCanDoActionMessageVariable("target", newConnExt.getIqn());
            addCanDoActionMessageVariable("vdsName", getVdsName());
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_STORAGE_CONNECTION_EXTENSION_ALREADY_EXISTS);
        }
        return true;
    }

    @Override
    protected void executeCommand() {
        getStorageServerConnectionExtensionDao().save(getParameters().getStorageServerConnectionExtension());
        getReturnValue().setActionReturnValue(getParameters().getStorageServerConnectionExtension().getId());
        getReturnValue().setSucceeded(true);
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        StorageServerConnectionExtension connExt = getParameters().getStorageServerConnectionExtension();
        String lock = connExt.getHostId().toString() + connExt.getIqn();
        return Collections.singletonMap(lock,
                LockMessagesMatchUtil.makeLockingPair(LockingGroup.STORAGE_CONNECTION_EXTENSION,
                        EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED));
    }
}
