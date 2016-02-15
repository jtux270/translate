package org.ovirt.engine.core.bll.storage;

import java.util.Collections;
import java.util.Map;

import org.ovirt.engine.core.bll.InternalCommandAttribute;
import org.ovirt.engine.core.bll.UpdateVmDiskCommand;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.common.action.UpdateVmDiskParameters;
import org.ovirt.engine.core.common.businessentities.storage.CinderDisk;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InternalCommandAttribute
public class ExtendCinderDiskCommand<T extends UpdateVmDiskParameters> extends UpdateVmDiskCommand<T> {

    private static final Logger log = LoggerFactory.getLogger(ExtendCinderDiskCommand.class);

    public ExtendCinderDiskCommand(T parameters) {
        super(parameters);
    }

    public ExtendCinderDiskCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    public void executeCommand() {
        CinderDisk cinderDisk = (CinderDisk) getNewDisk();
        getCinderBroker().extendDisk(cinderDisk, (int) getNewDiskSizeInGB());

        persistCommand(getParameters().getParentCommand(), true);
        getReturnValue().setActionReturnValue(cinderDisk.getId());
        setSucceeded(true);
    }

    protected void performDiskUpdate() {
        CinderDisk cinderDisk = (CinderDisk) getNewDisk();
        getImageDao().updateImageSize(cinderDisk.getImageId(), cinderDisk.getSize());
        performDiskUpdate(true);
    }

    @Override
    public CommandCallback getCallback() {
        return new ExtendCinderDiskCommandCallback();
    }

    @Override
    public boolean canDoAction() {
        return true;
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.emptyMap();
    }

    @Override
    protected Map<String, Pair<String, String>> getSharedLocks() {
        return Collections.emptyMap();
    }

    @Override
    public Guid getStorageDomainId() {
        CinderDisk cinderDisk = (CinderDisk) getNewDisk();
        return cinderDisk.getStorageIds().get(0);
    }

    @Override
    protected void endWithFailure() {
        setSucceeded(true);
    }
}
