package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.ISingleAsyncOperation;

public class AfterDeactivateSingleAsyncOperationFactory extends ActivateDeactivateSingleAsyncOperationFactory {
    private boolean _isLastMaster;
    private Guid _newMasterStorageDomainId = Guid.Empty;

    @Override
    public ISingleAsyncOperation createSingleAsyncOperation() {
        return new AfterDeactivateSingleAsyncOperation(getVdss(), getStorageDomain(), getStoragePool(), _isLastMaster,
                _newMasterStorageDomainId);
    }

    @Override
    public void initialize(ArrayList parameters) {
        super.initialize(parameters);
        if (!(parameters.get(3) instanceof Boolean)) {
            throw new IllegalArgumentException();
        }
        _isLastMaster = (Boolean) (parameters.get(3));
        if (!(parameters.get(4) instanceof Guid)) {
            throw new IllegalArgumentException();
        }
        _newMasterStorageDomainId = (Guid) parameters.get(4);
    }
}
