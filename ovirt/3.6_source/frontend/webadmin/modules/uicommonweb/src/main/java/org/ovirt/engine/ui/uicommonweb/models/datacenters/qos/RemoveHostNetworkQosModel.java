package org.ovirt.engine.ui.uicommonweb.models.datacenters.qos;

import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.network.HostNetworkQos;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class RemoveHostNetworkQosModel extends RemoveQosModel<HostNetworkQos> {

    public RemoveHostNetworkQosModel(ListModel<HostNetworkQos> sourceListModel) {
        super(sourceListModel);
    }

    @Override
    public String getTitle() {
        return ConstantsManager.getInstance().getConstants().removeHostNetworkQosTitle();
    }

    @Override
    protected VdcQueryType getUsingEntitiesByQosIdQueryType() {
        return VdcQueryType.GetAllNetworksByQosId;
    }

    @Override
    protected String getRemoveQosMessage(int size) {
        return ConstantsManager.getInstance().getMessages().removeHostNetworkQosMessage(size);
    }

    @Override
    protected String getRemoveQosHashName() {
        return "remove_host_network_qos"; //$NON-NLS-1$
    }

    @Override
    protected HelpTag getRemoveQosHelpTag() {
        return HelpTag.remove_host_network_qos;
    }

    @Override
    protected VdcActionType getRemoveActionType() {
        return VdcActionType.RemoveHostNetworkQos;
    }

}
