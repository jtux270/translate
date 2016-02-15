package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.storage.RegisterEntityModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportEntityData;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportTemplateData;

import com.google.gwt.user.client.ui.ScrollPanel;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.storage.backup.ImportTemplateGeneralSubTabView;

public class RegisterTemplateInfoPanel extends RegisterEntityInfoPanel {

    private ImportTemplateGeneralSubTabView generalView;
    private TemplateGeneralModel templateGeneralModel;

    public RegisterTemplateInfoPanel(RegisterEntityModel model) {
        super(model);
    }

    @Override
    protected void init() {
        // Initialize Tables
        initGeneralForm();
        initDisksTable();
        initNicsTable();

        // Add Tabs
        add(new ScrollPanel(generalView.asWidget()), constants.generalLabel());
        add(new ScrollPanel(disksTable), constants.disksLabel());
        add(new ScrollPanel(nicsTable), constants.nicsLabel());
    }

    @Override
    public void updateTabsData(ImportEntityData importEntityData) {
        VmTemplate vmTemplate = ((ImportTemplateData) importEntityData).getTemplate();

        templateGeneralModel.setEntity(vmTemplate);
        generalView.setMainTabSelectedItem(vmTemplate);

        disksTable.setRowData((List) Arrays.asList(vmTemplate.getDiskTemplateMap().values().toArray()));
        nicsTable.setRowData((List) Arrays.asList(vmTemplate.getInterfaces().toArray()));
    }

    private void initGeneralForm() {
        DetailModelProvider<TemplateListModel, TemplateGeneralModel> modelProvider =
            new DetailModelProvider<TemplateListModel, TemplateGeneralModel>() {
                @Override
                public TemplateGeneralModel getModel() {
                    return getTemplateGeneralModel();
                }

                @Override
                public void onSubTabSelected() {
                }
            };
        generalView = new ImportTemplateGeneralSubTabView(modelProvider, constants);
    }

    public TemplateGeneralModel getTemplateGeneralModel() {
        if (templateGeneralModel == null) {
            templateGeneralModel = new TemplateGeneralModel();
        }
        return templateGeneralModel;
    }
}
