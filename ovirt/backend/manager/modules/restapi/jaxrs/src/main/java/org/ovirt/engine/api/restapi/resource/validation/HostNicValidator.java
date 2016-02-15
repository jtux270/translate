package org.ovirt.engine.api.restapi.resource.validation;

import org.ovirt.engine.api.model.BootProtocol;
import org.ovirt.engine.api.model.HostNIC;
import static org.ovirt.engine.api.common.util.EnumValidator.validateEnum;

@ValidatedClass(clazz = HostNIC.class)
public class HostNicValidator implements Validator<HostNIC> {

    @Override
    public void validateEnums(HostNIC hostNic) {
        if (hostNic.isSetBootProtocol()) {
            validateEnum(BootProtocol.class, hostNic.getBootProtocol(), true);
        }
    }
}
