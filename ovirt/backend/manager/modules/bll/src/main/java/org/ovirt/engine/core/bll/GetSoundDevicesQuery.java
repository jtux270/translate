package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.VmDeviceDAO;

public class GetSoundDevicesQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {

    public GetSoundDevicesQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<VmDevice> soundDevList =
                getVmDeviceDAO().getVmDeviceByVmIdAndType(getParameters().getId(), VmDeviceGeneralType.SOUND);

        List<String> result = new ArrayList<String>(soundDevList.size());
        for (VmDevice v : soundDevList) {
            result.add(v.getDevice());
        }
        getQueryReturnValue().setReturnValue(result);
    }

    protected VmDeviceDAO getVmDeviceDAO() {
        return getDbFacade().getVmDeviceDao();
    }
}
