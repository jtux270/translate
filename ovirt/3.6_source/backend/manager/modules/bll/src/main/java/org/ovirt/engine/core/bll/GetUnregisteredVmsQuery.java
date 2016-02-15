package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.OvfEntityData;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;

public class GetUnregisteredVmsQuery<P extends IdQueryParameters> extends GetUnregisteredEntitiesQuery<P> {
    public GetUnregisteredVmsQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<OvfEntityData> entityList = getOvfEntityList(VmEntityType.VM);
        List<VM> vmList = new ArrayList<>();
        OvfHelper ovfHelper = getOvfHelper();
        for (OvfEntityData ovf : entityList) {
            try {
                VM vm = ovfHelper.readVmFromOvf(ovf.getOvfData());

                // Setting the rest of the VM attributes which are not in the OVF.
                vm.setVdsGroupCompatibilityVersion(ovf.getLowestCompVersion());
                vm.setClusterArch(ovf.getArchitecture());
                vmList.add(vm);
            } catch (OvfReaderException e) {
                log.debug("failed to parse a given ovf configuration: \n" + ovf.getOvfData(), e);
                getQueryReturnValue().setExceptionString("failed to parse a given ovf configuration "
                        + e.getMessage());
            }
        }
        getQueryReturnValue().setSucceeded(true);
        getQueryReturnValue().setReturnValue(vmList);
    }
}
