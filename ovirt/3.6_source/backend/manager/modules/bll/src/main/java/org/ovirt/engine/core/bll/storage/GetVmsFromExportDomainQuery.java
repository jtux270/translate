package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.OvfHelper;
import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;

public class GetVmsFromExportDomainQuery<P extends GetAllFromExportDomainQueryParameters>
        extends GetAllFromExportDomainQuery<List<VM>, P> {

    public GetVmsFromExportDomainQuery(P parameters) {
        this(parameters, null);
    }

    public GetVmsFromExportDomainQuery(P parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }

    @Override
    protected List<VM> buildFromOVFs(List<String> ovfList) {
        OvfHelper ovfHelper = new OvfHelper();
        List<VM> vms = new ArrayList<>();

        for (String ovf : ovfList) {
            try {
                if (!ovfHelper.isOvfTemplate(ovf)) {
                    vms.add(ovfHelper.readVmFromOvf(ovf));
                }
            } catch (OvfReaderException ex) {
                auditLogOvfLoadError(ex.getName(), ex.getMessage());
            }
        }

        return vms;
    }

    private void auditLogOvfLoadError(String machineName, String errorMessage) {
        AuditLogableBase logable = new AuditLogableBase();
        logable.addCustomValue("ImportedVmName", machineName);
        logable.addCustomValue("ErrorMessage", errorMessage);
        auditLogDirector.log(logable, AuditLogType.IMPORTEXPORT_FAILED_TO_IMPORT_VM);
    }
}
