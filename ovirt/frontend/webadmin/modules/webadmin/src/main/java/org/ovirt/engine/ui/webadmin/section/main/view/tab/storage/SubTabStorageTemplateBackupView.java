package org.ovirt.engine.ui.webadmin.section.main.view.tab.storage;

import java.util.Date;

import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.compat.StringFormat;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.storage.StorageListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.TemplateBackupModel;
import org.ovirt.engine.ui.uicompat.UIConstants;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.storage.SubTabStorageTemplateBackupPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractSubTabTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.GeneralDateTimeColumn;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class SubTabStorageTemplateBackupView extends AbstractSubTabTableView<StorageDomain, VmTemplate, StorageListModel, TemplateBackupModel>
        implements SubTabStorageTemplateBackupPresenter.ViewDef {

    private static final UIConstants messageConstants = GWT.create(UIConstants.class);

    @Inject
    public SubTabStorageTemplateBackupView(SearchableDetailModelProvider<VmTemplate, StorageListModel, TemplateBackupModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(final ApplicationConstants constants) {
        getTable().enableColumnResizing();

        TextColumnWithTooltip<VmTemplate> nameColumn =
                new TextColumnWithTooltip<VmTemplate>() {
                    @Override
                    public String getValue(VmTemplate object) {
                        return object.getName();
                    }
                };
        nameColumn.makeSortable();
        getTable().addColumn(nameColumn, constants.nameTemplate(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> versionNameColumn = new TextColumnWithTooltip<VmTemplate>() {
            @Override
            public String getValue(VmTemplate object) {
                if (object.isBaseTemplate()) {
                    return ""; //$NON-NLS-1$
                }

                return StringFormat.format("%s (%s)", //$NON-NLS-1$
                        object.getTemplateVersionName() != null ? object.getTemplateVersionName() : "", //$NON-NLS-1$
                        object.getTemplateVersionNumber());
            }
        };
        versionNameColumn.makeSortable();
        table.addColumn(versionNameColumn, constants.versionTemplate(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> originColumn =
                new TextColumnWithTooltip<VmTemplate>() {
                    @Override
                    public String getValue(VmTemplate object) {
                        return object.getOrigin() == null ? messageConstants.notSpecifiedLabel() : object.getOrigin()
                                .toString();
                    }
                };
        originColumn.makeSortable();
        getTable().addColumn(originColumn, constants.originTemplate(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> memoryColumn =
                new TextColumnWithTooltip<VmTemplate>() {
                    @Override
                    public String getValue(VmTemplate object) {
                        return String.valueOf(object.getMemSizeMb()) + " MB"; //$NON-NLS-1$
                    }
                };
        memoryColumn.makeSortable();
        getTable().addColumn(memoryColumn, constants.memoryTemplate(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> cpuColumn =
                new TextColumnWithTooltip<VmTemplate>() {
                    @Override
                    public String getValue(VmTemplate object) {
                        return String.valueOf(object.getNumOfCpus());
                    }
                };
        cpuColumn.makeSortable();
        getTable().addColumn(cpuColumn, constants.cpusVm(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> archColumn =
                new TextColumnWithTooltip<VmTemplate>() {
                    @Override
                    public String getValue(VmTemplate object) {
                        return String.valueOf(object.getClusterArch());
                    }
                };
        archColumn.makeSortable();
        getTable().addColumn(archColumn, constants.architectureVm(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> diskColumn =
                new TextColumnWithTooltip<VmTemplate>() {
                    @Override
                    public String getValue(VmTemplate object) {
                        return String.valueOf(object.getDiskList().size());
                    }
                };
        diskColumn.makeSortable();
        getTable().addColumn(diskColumn, constants.disksTemplate(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> creationDateColumn =
                new GeneralDateTimeColumn<VmTemplate>() {
                    @Override
                    protected Date getRawValue(VmTemplate object) {
                        return object.getCreationDate();
                    }
                };
        creationDateColumn.makeSortable();
        getTable().addColumn(creationDateColumn, constants.creationDateTemplate(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<VmTemplate> exportDateColumn =
            new GeneralDateTimeColumn<VmTemplate>() {
                @Override
                protected Date getRawValue(VmTemplate object) {
                    return object.getExportDate();
                }
            };
        exportDateColumn.makeSortable();
        getTable().addColumn(exportDateColumn, constants.exportDateTemplate(), "160px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<VmTemplate>(constants.restoreVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRestoreCommand();
            }
        });

        getTable().addActionButton(new WebAdminButtonDefinition<VmTemplate>(constants.removeTemplate()) {
            @Override
            protected UICommand resolveCommand() {
                return getDetailModel().getRemoveCommand();
            }
        });

        getTable().showRefreshButton();
    }

}
