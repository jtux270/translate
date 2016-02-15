package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.errors.VdcBllErrors;
import org.ovirt.engine.core.common.utils.EnumUtils;
import org.ovirt.engine.core.common.utils.SizeConverter;
import org.ovirt.engine.core.common.vdscommands.GetStorageDomainStatsVDSCommandParameters;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.vdsbroker.irsbroker.IrsBrokerCommand;
import org.ovirt.engine.core.vdsbroker.xmlrpc.XmlRpcObjectDescriptor;

public class GetStorageDomainStatsVDSCommand<P extends GetStorageDomainStatsVDSCommandParameters>
        extends VdsBrokerCommand<P> {
    private OneStorageDomainStatsReturnForXmlRpc _result;

    public GetStorageDomainStatsVDSCommand(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        _result = getBroker().getStorageDomainStats(getParameters().getStorageDomainId().toString());
        proceedProxyReturnValue();
        StorageDomain domain = buildStorageDynamicFromXmlRpcStruct(_result.mStorageStats);
        domain.setId(getParameters().getStorageDomainId());
        setReturnValue(domain);
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return _result.mStatus;
    }

    @SuppressWarnings("unchecked")
    public static StorageDomain buildStorageDynamicFromXmlRpcStruct(Map<String, Object> xmlRpcStruct) {
        try {
            StorageDomain domain = new StorageDomain();
            if (xmlRpcStruct.containsKey("status")) {
                if ("Attached".equals(xmlRpcStruct.get("status").toString())) {
                    domain.setStatus(StorageDomainStatus.Inactive);
                } else {
                    domain.setStatus(EnumUtils.valueOf(StorageDomainStatus.class, xmlRpcStruct.get("status")
                            .toString(), true));
                }
            }
            Long size = IrsBrokerCommand.assignLongValue(xmlRpcStruct, "diskfree");
            domain.setAvailableDiskSize((size == null) ? null : (int) (size / SizeConverter.BYTES_IN_GB));
            size = IrsBrokerCommand.assignLongValue(xmlRpcStruct, "disktotal");
            domain.setUsedDiskSize((size == null || domain.getAvailableDiskSize() == null) ? null :
                    (int) (size / SizeConverter.BYTES_IN_GB) - domain.getAvailableDiskSize());
            if (xmlRpcStruct.containsKey("alerts")) {
                Object[] rawAlerts = (Object[]) xmlRpcStruct.get("alerts");
                Set<VdcBllErrors> alerts = new HashSet<VdcBllErrors>(rawAlerts.length);

                for (Object rawAlert : rawAlerts) {
                    Map<String, Object> alert = (Map<String, Object>) rawAlert;
                    Integer alertCode = (Integer) alert.get("code");
                    if (alertCode == null || VdcBllErrors.forValue(alertCode) == null) {
                        log.warnFormat("Unrecognized alert code: {0}.", alertCode);
                        StringBuilder alertStringBuilder = new StringBuilder();
                        XmlRpcObjectDescriptor.toStringBuilder(alert, alertStringBuilder);
                        log.infoFormat("The received alert is: {0}", alertStringBuilder.toString());
                    } else {
                        alerts.add(VdcBllErrors.forValue(alertCode));
                    }
                }

                domain.setAlerts(alerts);
            }
            return domain;
        } catch (RuntimeException ex) {
            log.errorFormat(
                    "vdsBroker::buildStorageDynamicFromXmlRpcStruct::Failed building Storage dynamic, xmlRpcStruct = {0}",
                    xmlRpcStruct.toString());
            VDSErrorException outEx = new VDSErrorException(ex);
            log.error(outEx);
            throw outEx;
        }
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return _result;
    }

    private static final Log log = LogFactory.getLog(GetStorageDomainStatsVDSCommand.class);
}
