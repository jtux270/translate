package org.ovirt.engine.core.bll.storage;

import java.util.List;

import org.ovirt.engine.core.common.action.StorageDomainManagementParameter;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageServerConnections;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public class AddStorageDomainCommon<T extends StorageDomainManagementParameter> extends AddStorageDomainCommand<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     * @param commandId
     */
    protected AddStorageDomainCommon(Guid commandId) {
        super(commandId);
    }

    public AddStorageDomainCommon(T parameters) {
        super(parameters);
    }

    protected boolean checkStorageConnection(String storageDomainConnection) {
        List<StorageDomain> domains = null;
        StorageServerConnections connection = getStorageServerConnectionDAO().get(storageDomainConnection);
        if (connection == null) {
            return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_CONNECTION_NOT_EXIST);
        }
        if (connection.getstorage_type().isFileDomain()) {
            domains = getStorageDomainsByConnId(connection.getid());
            if (domains.size() > 0) {
                String domainNames = createDomainNamesListFromStorageDomains(domains);
                return prepareFailureMessageForDomains(domainNames);
            }
        }
        return true;
    }

    protected String createDomainNamesListFromStorageDomains(List<StorageDomain> domains) {
        // Build domain names list to display in the error
        StringBuilder domainNames = new StringBuilder();
        for (StorageDomain domain : domains) {
            domainNames.append(domain.getStorageName());
            domainNames.append(",");
        }
        // Remove the last "," after the last domain
        domainNames.deleteCharAt(domainNames.length() - 1);
        return domainNames.toString();
    }

    protected List<StorageDomain> getStorageDomainsByConnId(String connectionId) {
        return getStorageDomainDAO().getAllByConnectionId(Guid.createGuidFromString(connectionId));
    }

    protected boolean prepareFailureMessageForDomains(String domainNames) {
        addCanDoActionMessageVariable("domainNames", domainNames);
        return failCanDoAction(VdcBllMessages.ACTION_TYPE_FAILED_STORAGE_CONNECTION_BELONGS_TO_SEVERAL_STORAGE_DOMAINS);
    }

    @Override
    protected boolean canAddDomain() {
        return checkStorageConnection(getStorageDomain().getStorage());
    }

    @Override
    protected String getStorageArgs() {
        return DbFacade.getInstance()
                .getStorageServerConnectionDao()
                .get(getStorageDomain().getStorage())
                .getconnection();
    }

}
