package org.ovirt.engine.ui.uicommonweb.models.datacenters.qos;

import org.ovirt.engine.core.common.action.QosParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.qos.StorageQos;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class EditStorageQosModel extends EditQosModel<StorageQos, StorageQosParametersModel> {

    public EditStorageQosModel(StorageQos qos, Model sourceModel, StoragePool dataCenter) {
        super(qos, sourceModel, dataCenter);
    }

    @Override
    protected VdcActionType getVdcAction() {
        return VdcActionType.UpdateStorageQos;
    }

    @Override
    protected QosParametersBase<StorageQos> getParameters() {
        QosParametersBase<StorageQos> qosParametersBase = new QosParametersBase<StorageQos>();
        qosParametersBase.setQos(getQos());
        qosParametersBase.setQosId(getQos().getId());
        return qosParametersBase;
    }

    @Override
    public void init(StorageQos qos) {
        super.init(qos);

        setQosParametersModel(new StorageQosParametersModel());
        getQosParametersModel().init(qos);
    }

    @Override
    public String getTitle() {
        return ConstantsManager.getInstance().getConstants().editStorageQoSTitle();
    }

    @Override
    public HelpTag getHelpTag() {
        return HelpTag.edit_storage_qos;
    }

    @Override
    public String getHashName() {
        return "edit_storage_qos"; //$NON-NLS-1$
    }

}

