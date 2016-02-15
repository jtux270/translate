package org.ovirt.engine.ui.uicommonweb.models.storage;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportTemplateData;
import org.ovirt.engine.ui.uicompat.ConstantsManager;

public class StorageRegisterTemplateListModel extends StorageRegisterEntityListModel {

    public StorageRegisterTemplateListModel() {
        setTitle(ConstantsManager.getInstance().getConstants().templateImportTitle());
        setHelpTag(HelpTag.template_register);
        setHashName("template_register"); //$NON-NLS-1$
    }

    @Override
    RegisterEntityModel createRegisterEntityModel() {
        RegisterTemplateModel model = new RegisterTemplateModel();
        model.setTitle(ConstantsManager.getInstance().getConstants().importTemplatesTitle());
        model.setHelpTag(HelpTag.register_template);
        model.setHashName("register_template"); //$NON-NLS-1$

        return model;
    }

    @Override
    ImportEntityData createImportEntityData(Object entity) {
        return new ImportTemplateData((VmTemplate) entity);
    }

    @Override
    protected void syncSearch() {
        syncSearch(VdcQueryType.GetUnregisteredVmTemplates, new Linq.VmTemplateComparator());
    }

    @Override
    protected String getListName() {
        return "StorageRegisterTemplateListModel"; //$NON-NLS-1$
    }
}
