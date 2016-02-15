package org.ovirt.engine.core.vdsbroker.vdsbroker;

import java.util.Map;

import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.vdscommands.GetOvaInfoParameters;
import org.ovirt.engine.core.compat.Guid;

public class GetOvaInfoVDSCommand<T extends GetOvaInfoParameters> extends VdsBrokerCommand<T> {
    private OneVmReturnForXmlRpc vmReturn;

    public GetOvaInfoVDSCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeVdsBrokerCommand() {
        vmReturn = getBroker().getExternalVmFromOva(getParameters().getPath());
        proceedProxyReturnValue();
        Map<String, Object> map = vmReturn.vm;
        map.put(VdsProperties.vm_guid, Guid.newGuid().toString());
        map.put(VdsProperties.vm_arch, ArchitectureType.x86_64.toString());
        VM vm = VdsBrokerObjectsBuilder.buildVmsDataFromExternalProvider(map);
        setReturnValue(vm);
    }

    @Override
    protected StatusForXmlRpc getReturnStatus() {
        return vmReturn.status;
    }

    @Override
    protected Object getReturnValueFromBroker() {
        return vmReturn;
    }

    @Override
    protected boolean getIsPrintReturnValue() {
        return false;
    }
}
