package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VdsSpmStatus;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.vdscommands.DisconnectStoragePoolVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;

public class DisconnectStoragePoolAsyncOperation extends ActivateDeactivateSingleAsyncOperation {
    public DisconnectStoragePoolAsyncOperation(ArrayList<VDS> vdss, StoragePool storagePool) {
        super(vdss, null, storagePool);
    }

    @Override
    public void execute(int iterationId) {
        try {
            if (getVdss().get(iterationId).getSpmStatus() == VdsSpmStatus.None) {
                log.infoFormat("Disconnect storage pool treatment vds: {0},pool {1}", getVdss().get(iterationId)
                        .getName(), getStoragePool().getName());
                Backend.getInstance()
                        .getResourceManager()
                        .RunVdsCommand(
                                VDSCommandType.DisconnectStoragePool,
                                new DisconnectStoragePoolVDSCommandParameters(getVdss().get(iterationId).getId(),
                                        getStoragePool().getId(), getVdss().get(iterationId).getVdsSpmId()));
            }
        } catch (RuntimeException e) {
            log.errorFormat(
                    "Failed to DisconnectStoragePool storagePool. Host {0} from storage pool {1}. Exception: {3}",
                    getVdss().get(iterationId).getName(), getStoragePool().getName(), e);
        }
    }

    private static final Log log = LogFactory.getLog(DisconnectStoragePoolAsyncOperation.class);
}
