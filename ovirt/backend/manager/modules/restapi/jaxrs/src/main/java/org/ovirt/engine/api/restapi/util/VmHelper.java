package org.ovirt.engine.api.restapi.util;

import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;

import java.util.List;

public class VmHelper extends ResourceHelper {

    private final static VmHelper instance = new VmHelper();

    private VmHelper() {
        // empty block
    }

    public static VmHelper getInstance() {
        return instance;
    }

    public List<String> getVirtioScsiControllersForEntity(Guid id) {
        return getEntity(List.class,
                VdcQueryType.GetVirtioScsiControllers,
                new IdQueryParameters(id),
                "GetVirtioScsiControllers", true);
    }

    public List<String> getSoundDevicesForEntity(Guid id) {
        return getEntity(List.class,
                VdcQueryType.GetSoundDevices,
                new IdQueryParameters(id),
                "GetSoundDevices", true);
    }

    public Boolean isMemoryBalloonEnabledForEntity(Guid id) {
        return getEntity(Boolean.class,
                VdcQueryType.IsBalloonEnabled,
                new IdQueryParameters(id),
                "IsBalloonEnabled", true);
    }

    public List<VmRngDevice> getRngDevicesForEntity(Guid id) {
        return getEntity(List.class,
                VdcQueryType.GetRngDevice,
                new IdQueryParameters(id),
                "GetRngDevice", true);
    }
}
