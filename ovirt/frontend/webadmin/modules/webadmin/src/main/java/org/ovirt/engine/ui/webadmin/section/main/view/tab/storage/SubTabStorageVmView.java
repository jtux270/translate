package org.ovirt.engine.ui.webadmin.section.main.view.tab.storage;

import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.EmptyColumn;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageVmListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.storage.SubTabStorageVmPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTreeView;
import org.ovirt.engine.ui.webadmin.widget.storage.VMsTree;

import com.google.inject.Inject;

public class SubTabStorageVmView extends AbstractSubTabTreeView<VMsTree<StorageVmListModel>, StorageDomain, VM, StorageListModel, StorageVmListModel>
        implements SubTabStorageVmPresenter.ViewDef {

    @Inject
    public SubTabStorageVmView(SearchableDetailModelProvider<VM, StorageListModel, StorageVmListModel> modelProvider,
            ApplicationConstants constants, ApplicationTemplates templates, ApplicationResources resources) {
        super(modelProvider, constants, templates, resources);
    }

    @Override
    protected void initHeader(ApplicationConstants constants) {
        table.addColumn(new EmptyColumn(), constants.aliasVm());
        table.addColumn(new EmptyColumn(), constants.disksVm(), "80px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.templateVm(), "160px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.vSizeVm(), "110px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.actualSizeVm(), "110px"); //$NON-NLS-1$
        table.addColumn(new EmptyColumn(), constants.creationDateVm(), "170px"); //$NON-NLS-1$
    }

    @Override
    protected VMsTree getTree() {
        return new VMsTree(resources, constants, templates);
    }

}
