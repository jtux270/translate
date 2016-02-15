package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.List;

import org.ovirt.engine.core.common.action.RemoveVmTemplateInterfaceParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class RemoveVmTemplateInterfaceModel extends RemoveVmInterfaceModel{

    public RemoveVmTemplateInterfaceModel(ListModel sourceListModel, List<VmNetworkInterface> vnics, boolean isFullMsg) {
        super(sourceListModel, vnics, isFullMsg);
        setHelpTag(HelpTag.remove_network_interface_tmps);
        setHashName("remove_network_interface_tmps"); //$NON-NLS-1$
    }

    @Override
    protected String getRemoveVnicFullMsg(VmNetworkInterface vnic){
        return ConstantsManager.getInstance().getMessages().vnicFromTemplate(vnic.getName(), vnic.getVmName());
    }

    @Override
    protected VdcActionParametersBase getRemoveVmInterfaceParams(VmNetworkInterface vnic) {
        return new RemoveVmTemplateInterfaceParameters(vnic.getVmTemplateId(), vnic.getId());
    }

    @Override
    protected VdcActionType getActionType() {
        return VdcActionType.RemoveVmTemplateInterface;
    }

}
