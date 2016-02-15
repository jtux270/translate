package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ovirt.engine.core.bll.QueriesCommandBase;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatic;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.StoragePoolStatus;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.errors.VdcBLLException;
import org.ovirt.engine.core.common.interfaces.VDSBrokerFrontend;
import org.ovirt.engine.core.common.queries.StorageDomainsAndStoragePoolIdQueryParameters;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.HSMGetStorageDomainInfoVDSCommandParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSParametersBase;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;

public class GetStorageDomainsWithAttachedStoragePoolGuidQuery<P extends StorageDomainsAndStoragePoolIdQueryParameters> extends QueriesCommandBase<P> {

    private Guid vdsId;

    public GetStorageDomainsWithAttachedStoragePoolGuidQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        List<StorageDomainStatic> storageDomainsWithAttachedStoragePoolId = new ArrayList<>();
        if ((getVdsForConnectStorage() != null) && isDataCenterValidForAttachedStorageDomains()) {
            storageDomainsWithAttachedStoragePoolId = filterAttachedStorageDomains();
        }
        getQueryReturnValue().setReturnValue(storageDomainsWithAttachedStoragePoolId);
    }

    private Guid getVdsForConnectStorage() {
        vdsId = getParameters().getVdsId();
        if (vdsId == null) {
            // Get a Host which is at UP state to connect to the Storage Domain.
            List<VDS> hosts =
                    getDbFacade().getVdsDao().getAllForStoragePoolAndStatus(getParameters().getId(), VDSStatus.Up);
            if (!hosts.isEmpty()) {
                vdsId = hosts.get(new Random().nextInt(hosts.size())).getId();
                log.infoFormat("vds id {0} was chosen to fetch the Storage domain info", vdsId);
            } else {
                log.warn("There is no available vds in UP state to fetch the Storage domain info from VDSM");
            }
        }
        return vdsId;
    }

    private boolean isDataCenterValidForAttachedStorageDomains() {
        if (getParameters().isCheckStoragePoolStatus()) {
            StoragePool storagePool = getDbFacade().getStoragePoolDao().get(getParameters().getId());
            if ((storagePool == null) || (storagePool.getStatus() != StoragePoolStatus.Up)) {
                log.info("The Data Center is not in UP status.");
                return false;
            }
        }
        return true;
    }

    protected List<StorageDomainStatic> filterAttachedStorageDomains() {
        List<StorageDomain> connectedStorageDomainsToVds = new ArrayList<>();
        for (StorageDomain storageDomain : getParameters().getStorageDomainList()) {
            if (!connectStorageDomain(storageDomain)) {
                logErrorMessage(storageDomain);
            } else {
                connectedStorageDomainsToVds.add(storageDomain);
            }
        }

        List<StorageDomainStatic> storageDomainsWithAttachedStoragePoolId =
                getAttachedStorageDomains(connectedStorageDomainsToVds);
        for (StorageDomain storageDomain : connectedStorageDomainsToVds) {
            if (!disconnectStorageDomain(storageDomain)) {
                log.warnFormat("Could not disconnect Storage Domain {0} from VDS {1}. ", storageDomain.getName(), getVdsId());
            }
        }
        return storageDomainsWithAttachedStoragePoolId;
    }

    public Guid getVdsId() {
        return vdsId;
    }

    protected List<StorageDomainStatic> getAttachedStorageDomains(List<StorageDomain> storageDomains) {
        VDSReturnValue vdsReturnValue = null;
        List<StorageDomainStatic> storageDomainsWithAttachedStoragePoolId = new ArrayList<>();

        // Go over the list of Storage Domains and try to get the Storage Domain info to check if it is attached to
        // another Storage Pool
        for (StorageDomain storageDomain : storageDomains) {
            try {
                vdsReturnValue =
                        runVdsCommand(VDSCommandType.HSMGetStorageDomainInfo,
                                new HSMGetStorageDomainInfoVDSCommandParameters(getVdsId(), storageDomain.getId()));
            } catch (RuntimeException e) {
                logErrorMessage(storageDomain);
                continue;
            }
            if (!vdsReturnValue.getSucceeded()) {
                logErrorMessage(storageDomain);
                continue;
            }
            Pair<StorageDomainStatic, Guid> domainFromIrs =
                    (Pair<StorageDomainStatic, Guid>) vdsReturnValue.getReturnValue();
            if (domainFromIrs.getSecond() != null) {
                storageDomainsWithAttachedStoragePoolId.add(domainFromIrs.getFirst());
            }
        }
        return storageDomainsWithAttachedStoragePoolId;
    }

    protected VDSBrokerFrontend getVdsBroker() {
        return getBackend().getResourceManager();
    }

    protected VDSReturnValue runVdsCommand(VDSCommandType commandType, VDSParametersBase parameters)
            throws VdcBLLException {
        return getVdsBroker().RunVdsCommand(commandType, parameters);
    }

    protected boolean connectStorageDomain(StorageDomain storageDomain) {
        return StorageHelperDirector.getInstance()
                .getItem(storageDomain.getStorageType())
                .connectStorageToDomainByVdsId(storageDomain, getVdsId());
    }

    protected boolean disconnectStorageDomain(StorageDomain storageDomain) {
        return StorageHelperDirector.getInstance()
                .getItem(storageDomain.getStorageType())
                .disconnectStorageFromDomainByVdsId(storageDomain, getVdsId());
    }

    protected void logErrorMessage(StorageDomain storageDomain) {
        if (storageDomain != null) {
            log.errorFormat("Could not get Storage Domain info for Storage Domain (name:{0}, id:{1}) with VDS {2}. ",
                    storageDomain.getName(),
                    storageDomain.getId(),
                    getVdsId());
        } else {
            log.errorFormat("Could not get Storage Domain info with VDS {0}. ", getVdsId());
        }
    }
}
