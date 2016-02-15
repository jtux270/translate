package org.ovirt.engine.core.bll;

import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmPayload;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.VmDeviceDAO;


public class GetVmPayloadQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {

    public GetVmPayloadQuery(P parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }
    public GetVmPayloadQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        if (MultiLevelAdministrationHandler.isAdminUser(getUser())) {
            VmDeviceDAO dao = getDbFacade().getVmDeviceDao();
            List<VmDevice> disks = dao.getVmDeviceByVmIdAndType(getParameters().getId(), VmDeviceGeneralType.DISK);

            for (VmDevice disk : disks) {
                if (VmPayload.isPayload(disk.getSpecParams())) {
                    VmPayload payload = new VmPayload(disk);
                    for (Map.Entry<String, String> entry : payload.getFiles().entrySet()) {
                        entry.setValue(new String(Base64.decodeBase64(entry.getValue())));
                    }

                    getQueryReturnValue().setReturnValue(payload);
                }
            }
        }
    }
}
