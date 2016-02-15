package org.ovirt.engine.core.bll.validator;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.network.macpoolmanager.MacPoolManagerStrategy;
import org.ovirt.engine.core.common.errors.EngineMessage;

public class MacAddressValidator {
    private final MacPoolManagerStrategy macPool;
    private final String macAddress;

    public MacAddressValidator(MacPoolManagerStrategy macPool, String macAddress) {
        this.macPool = macPool;
        this.macAddress = macAddress;
    }

    public ValidationResult isMacAssignableValidator() {
        Boolean allowDupMacs = macPool.isDuplicateMacAddressesAllowed();
        boolean macIsAlreadyUsed = macPool.isMacInUse(macAddress);
        boolean illegalDuplicateMacUsage = macIsAlreadyUsed && !allowDupMacs;
        return ValidationResult.failWith(EngineMessage.NETWORK_MAC_ADDRESS_IN_USE).when(illegalDuplicateMacUsage);
    }
}
