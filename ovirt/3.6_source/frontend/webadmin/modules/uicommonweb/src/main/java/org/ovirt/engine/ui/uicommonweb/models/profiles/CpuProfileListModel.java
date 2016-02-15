package org.ovirt.engine.ui.uicommonweb.models.profiles;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.profiles.CpuProfile;
import org.ovirt.engine.core.common.businessentities.qos.CpuQos;
import org.ovirt.engine.core.common.businessentities.qos.QosType;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import com.google.inject.Inject;

public class CpuProfileListModel extends ProfileListModel<CpuProfile, CpuQos, VDSGroup> {

    @Inject
    public CpuProfileListModel(final PermissionListModel<CpuProfile> permissionListModel) {
        super(permissionListModel);
        setTitle(ConstantsManager.getInstance().getConstants().cpuProfileTitle());
        setHelpTag(HelpTag.cpu_profiles);
        setHashName("cpu_profiles"); //$NON-NLS-1$
    }

    @Override
    protected ProfileBaseModel<CpuProfile, CpuQos, VDSGroup> getNewProfileModel() {
        return new NewCpuProfileModel(this, getStoragePoolId());
    }

    @Override
    protected ProfileBaseModel<CpuProfile, CpuQos, VDSGroup> getEditProfileModel() {
        return new EditCpuProfileModel(this, getSelectedItem(), getStoragePoolId());
    }

    @Override
    protected RemoveProfileModel<CpuProfile> getRemoveProfileModel() {
        return new RemoveCpuProfileModel(this, getSelectedItems());
    }

    @Override
    protected QosType getQosType() {
        return QosType.CPU;
    }

    @Override
    protected Guid getStoragePoolId() {
        return getEntity() != null ? getEntity().getStoragePoolId() : null;
    }

    @Override
    protected VdcQueryType getQueryType() {
        return VdcQueryType.GetCpuProfilesByClusterId;
    }

    @Override
    protected String getListName() {
        return "CpuProfileListModel"; //$NON-NLS-1$
    }
}
