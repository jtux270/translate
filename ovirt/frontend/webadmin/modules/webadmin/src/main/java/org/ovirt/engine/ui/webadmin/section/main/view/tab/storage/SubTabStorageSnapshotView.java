package org.ovirt.engine.ui.webadmin.section.main.view.tab.storage;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.uicommon.disks.DisksViewColumns;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageSnapshotListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.storage.SubTabStorageSnapshotPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class SubTabStorageSnapshotView extends AbstractSubTabTableView<StorageDomain, Disk, StorageListModel, StorageSnapshotListModel>
        implements SubTabStorageSnapshotPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabStorageSnapshotView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabStorageSnapshotView(SearchableDetailModelProvider<Disk, StorageListModel,
            StorageSnapshotListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().ensureColumnPresent(
                DisksViewColumns.getSnapshotSizeColumn(null), constants.diskSnapshotSize(), true, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.getDateCreatedColumn(null), constants.creationDateDisk(), true, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.getAliasColumn(null), constants.diskSnapshotAlias(), true, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.getSnapshotDescriptionColumn(null), constants.diskSnapshotDescription(), true, "160px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.diskContainersColumn, constants.attachedToDisk(), true, "130px"); //$NON-NLS-1$

        getTable().ensureColumnPresent(
                DisksViewColumns.getStatusColumn(null), constants.statusDisk(), true, "80px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<Disk>(constants.removeDisk()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });
    }
}
