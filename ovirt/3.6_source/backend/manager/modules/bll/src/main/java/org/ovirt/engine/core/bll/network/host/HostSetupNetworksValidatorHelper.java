package org.ovirt.engine.core.bll.network.host;

import java.util.Objects;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.BusinessEntityMap;
import org.ovirt.engine.core.common.businessentities.Nameable;
import org.ovirt.engine.core.common.businessentities.network.Bond;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;

public class HostSetupNetworksValidatorHelper {

    @Inject
    public HostSetupNetworksValidatorHelper() {
    }

    public <T extends BusinessEntity<Guid> & Nameable> ValidationResult validateCoherentIdentification(String violatingEntityId,
            Guid referringId,
            String referringName,
            EngineMessage message,
            BusinessEntityMap<T> map) {

        boolean bothIdentificationSet = referringId != null && referringName != null;
        String[] replacements =
                createIncoherentIdentificationErrorReplacements(violatingEntityId, referringId, referringName);
        return ValidationResult
                .failWith(message, replacements)
                .when(bothIdentificationSet && isNameAndIdIncoherent(referringId, referringName, map));
    }

    private String[] createIncoherentIdentificationErrorReplacements(String violatingEntityId,
            Guid referringId,
            String referringName) {
        return new String[] {
                String.format("$referrerId %s", violatingEntityId),
                String.format("$referringId %s", referringId),
                String.format("$referringName %s", referringName)
        };
    }

    private <T extends BusinessEntity<Guid> & Nameable> boolean isNameAndIdIncoherent(Guid id,
            String name,
            BusinessEntityMap<T> map) {
        T entityById = map.get(id);
        T entityByName = map.get(name);
        return !Objects.equals(entityById, entityByName);
    }

    public boolean isNicActuallyExistsOrReferencesNewBond(BusinessEntityMap<VdsNetworkInterface> existingInterfacesMap,
            BusinessEntityMap<Bond> bondsMap,
            String nicName,
            Guid nicId) {
        boolean nicExists = existingInterfacesMap.get(nicId, nicName) != null;
        if (nicExists) {
            return true;
        }

        boolean nicIsNewBond = nicName != null && bondsMap.get(nicName) != null;
        if (nicIsNewBond) {
            return true;
        }

        return false;
    }
}
