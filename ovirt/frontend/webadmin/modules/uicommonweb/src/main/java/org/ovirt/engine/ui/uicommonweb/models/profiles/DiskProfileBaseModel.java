package org.ovirt.engine.ui.uicommonweb.models.profiles;

import java.util.List;

import org.ovirt.engine.core.common.action.DiskProfileParameters;
import org.ovirt.engine.core.common.action.ProfileParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.profiles.DiskProfile;
import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.businessentities.qos.StorageQos;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public abstract class DiskProfileBaseModel extends ProfileBaseModel<DiskProfile, StorageQos, StorageDomain> {

    private final static StorageQos EMPTY_QOS;

    static {
        EMPTY_QOS = new StorageQos();
        EMPTY_QOS.setName(ConstantsManager.getInstance().getConstants().unlimitedQoSTitle());
        EMPTY_QOS.setId(Guid.Empty);
    }

    public DiskProfileBaseModel(EntityModel sourceModel,
            Guid dcId,
            Guid defaultQosId,
            VdcActionType vdcActionType) {
        super(sourceModel, dcId, defaultQosId, vdcActionType);
    }

    @Override
    public void flush() {
        if (getProfile() == null) {
            setProfile(new DiskProfile());
        }
        DiskProfile diskProfile = getProfile();
        diskProfile.setName(getName().getEntity());
        diskProfile.setDescription(getDescription().getEntity());
        StorageDomain storageDomain = getParentListModel().getSelectedItem();
        diskProfile.setStorageDomainId(storageDomain != null ? storageDomain.getId() : null);
        StorageQos storageQos = getQos().getSelectedItem();
        diskProfile.setQosId(storageQos != null
                && storageQos.getId() != null
                && !storageQos.getId().equals(Guid.Empty)
                ? storageQos.getId() : null);
    }

    @Override
    protected void postInitQosList(List<StorageQos> qosList) {
        qosList.add(0, EMPTY_QOS);
        getQos().setItems(qosList);
        if (getDefaultQosId() != null) {
            for (StorageQos storageQos : qosList) {
                if (getDefaultQosId().equals(storageQos.getId())) {
                    getQos().setSelectedItem(storageQos);
                    break;
                }
            }
        }
    }

    @Override
    protected QosType getQosType() {
        return QosType.STORAGE;
    }

    @Override
    protected ProfileParametersBase<DiskProfile> getParameters() {
        return new DiskProfileParameters(getProfile(), getProfile().getId());
    }
}
