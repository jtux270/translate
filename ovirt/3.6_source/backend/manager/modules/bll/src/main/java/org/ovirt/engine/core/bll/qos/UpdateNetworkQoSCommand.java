package org.ovirt.engine.core.bll.qos;


import org.ovirt.engine.core.bll.validator.NetworkQosValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.QosParametersBase;
import org.ovirt.engine.core.common.businessentities.network.NetworkQoS;
import org.ovirt.engine.core.dao.qos.QosDao;

public class UpdateNetworkQoSCommand extends UpdateQosCommandBase<NetworkQoS, NetworkQosValidator> {

    public UpdateNetworkQoSCommand(QosParametersBase<NetworkQoS> parameters) {
        super(parameters);
    }

    @Override
    protected QosDao<NetworkQoS> getQosDao() {
        return getDbFacade().getNetworkQosDao();
    }

    @Override
    protected NetworkQosValidator getQosValidator(NetworkQoS qos) {
        return new NetworkQosValidator(qos);
    }


    @Override
    protected boolean canDoAction() {
        return super.canDoAction() &&
                validate(getQosValidator(getQos()).peakConsistentWithAverage());
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_UPDATED_NETWORK_QOS : AuditLogType.USER_FAILED_TO_UPDATE_NETWORK_QOS;
    }
}
