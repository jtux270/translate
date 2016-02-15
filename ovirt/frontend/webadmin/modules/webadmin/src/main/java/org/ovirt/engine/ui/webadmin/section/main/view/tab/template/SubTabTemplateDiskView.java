package org.ovirt.engine.ui.webadmin.section.main.view.tab.template;

import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.action.SubTabTreeActionPanel;
import org.ovirt.engine.ui.common.widget.action.UiCommandButtonDefinition;
import org.ovirt.engine.ui.common.widget.table.column.EmptyColumn;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.templates.TemplateListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.DiskModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.template.SubTabTemplateDiskPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTreeView;
import org.ovirt.engine.ui.webadmin.widget.template.DisksTree;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SubTabTemplateDiskView extends AbstractSubTabTreeView<DisksTree, VmTemplate, DiskModel, TemplateListModel, TemplateDiskListModel> implements SubTabTemplateDiskPresenter.ViewDef {

    @Inject
    public SubTabTemplateDiskView(final SearchableDetailModelProvider<DiskModel, TemplateListModel, TemplateDiskListModel> modelProvider,
            EventBus eventBus,
            ApplicationConstants constants,
            ApplicationTemplates templates,
            ApplicationResources resources) {
        super(modelProvider, constants, templates, resources);

        actionPanel.addActionButton(new UiCommandButtonDefinition<DiskModel>(eventBus, constants.copyDisk()) {
            @Override
            protected UICommand resolveCommand() {
                return modelProvider.getModel().getCopyCommand();
            }
        });

        actionPanel.addActionButton(new UiCommandButtonDefinition<DiskModel>(eventBus, constants.assignQuota()) {
            @Override
            protected UICommand resolveCommand() {
                return modelProvider.getModel().getChangeQuotaCommand();
            }
        });

        setIsActionTree(true);
    }

    @Override
    protected void initHeader(ApplicationConstants constants) {
        table.addColumn(new EmptyColumn(), constants.aliasDisk(), ""); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.provisionedSizeDisk(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.sizeDisk(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.statusDisk(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.allocationDisk(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.interfaceDisk(), "120px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.creationDateDisk(), "120px"); //$NON-NLS-1$
    }

    @Override
    protected DisksTree getTree() {
        return new DisksTree(resources, constants, templates);
    }

    @Override
    protected SubTabTreeActionPanel createActionPanel(SearchableDetailModelProvider modelProvider) {
        return new SubTabTreeActionPanel<DiskModel>(modelProvider, ClientGinjectorProvider.getEventBus());
    }
}
