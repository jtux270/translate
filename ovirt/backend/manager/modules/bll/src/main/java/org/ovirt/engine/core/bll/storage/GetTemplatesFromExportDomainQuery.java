package org.ovirt.engine.core.bll.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.bll.OvfHelper;
import org.ovirt.engine.core.bll.context.EngineContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.queries.GetAllFromExportDomainQueryParameters;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;
import org.ovirt.engine.core.utils.ovf.OvfManager;
import org.ovirt.engine.core.utils.ovf.OvfReaderException;

public class GetTemplatesFromExportDomainQuery<P extends GetAllFromExportDomainQueryParameters>
        extends GetAllFromExportDomainQuery<Map<VmTemplate, List<DiskImage>>, P> {

    public GetTemplatesFromExportDomainQuery(P parameters) {
        super(parameters);
    }

    public GetTemplatesFromExportDomainQuery(P parameters, EngineContext engineContext) {
        super(parameters, engineContext);
    }

    @Override
    protected Map<VmTemplate, List<DiskImage>> buildFromOVFs(List<String> ovfList) {
        OvfManager ovfManager = new OvfManager();
        Map<VmTemplate, List<DiskImage>> templateDisksMap = new HashMap<>();
        OvfHelper ovfHelper = new OvfHelper();
        for (String ovf : ovfList) {
            try {
                if (ovfManager.IsOvfTemplate(ovf)) {
                    VmTemplate vmTemplate = ovfHelper.readVmTemplateFromOvf(ovf);
                    List<DiskImage> templateDisks = new ArrayList<>(vmTemplate.getDiskTemplateMap().values());
                    templateDisksMap.put(vmTemplate, templateDisks);
                }
            } catch (OvfReaderException ex) {
                auditLogOvfLoadError(ex.getName());
            }
        }

        return templateDisksMap;
    }

    private void auditLogOvfLoadError(String machineName) {
        AuditLogableBase logable = new AuditLogableBase();
        logable.addCustomValue("Template", machineName);
        AuditLogDirector.log(logable, AuditLogType.IMPORTEXPORT_FAILED_TO_IMPORT_TEMPLATE);

    }

}
