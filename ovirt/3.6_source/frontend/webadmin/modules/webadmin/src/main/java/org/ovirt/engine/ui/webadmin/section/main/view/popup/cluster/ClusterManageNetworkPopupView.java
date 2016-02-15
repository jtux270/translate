package org.ovirt.engine.ui.webadmin.section.main.view.popup.cluster;

import java.util.ArrayList;
import java.util.Collections;

import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable.SelectionMode;
import org.ovirt.engine.ui.common.widget.table.column.AbstractCheckboxColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractSafeHtmlColumn;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.common.widget.table.header.AbstractCheckboxHeader;
import org.ovirt.engine.ui.common.widget.table.header.SafeHtmlHeader;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkManageModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterNetworkModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.cluster.ClusterManageNetworkPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.widget.table.column.MultiImageColumnHelper;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.inject.Inject;

public class ClusterManageNetworkPopupView extends AbstractModelBoundPopupView<ClusterNetworkManageModel>
        implements ClusterManageNetworkPopupPresenterWidget.ViewDef {

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ClusterManageNetworkPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    EntityModelCellTable<ClusterNetworkManageModel> networks;

    private final static ApplicationTemplates templates = AssetProvider.getTemplates();
    private final static ApplicationResources resources = AssetProvider.getResources();
    private final static ApplicationConstants constants = AssetProvider.getConstants();

    private final SafeHtml vmImage;
    private final SafeHtml emptyImage;

    @Inject
    public ClusterManageNetworkPopupView(EventBus eventBus) {
        super(eventBus);

        this.networks = new EntityModelCellTable<>(SelectionMode.NONE, true);
        vmImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.networkVm()).getHTML());
        emptyImage = SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(resources.networkEmpty()).getHTML());
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
    }

    Iterable<ClusterNetworkModel> getNetworksTableItems() {
        ClusterNetworkManageModel tableModel = networks.asEditor().flush();
        return tableModel != null ? tableModel.getItems() : new ArrayList<ClusterNetworkModel>();
    }

    void refreshNetworksTable() {
        networks.asEditor().edit(networks.asEditor().flush());
    }

    void initEntityModelCellTable() {
        networks.enableColumnResizing();
        boolean multiCluster = networks.asEditor().flush().isMultiCluster();

        networks.addColumn(new NetworkNameTextColumnWithTooltip(), constants.nameNetwork(), "85px"); //$NON-NLS-1$

        networks.addColumn(
                new AttachedIndicatorCheckboxColumn(new AttachedIndicatorFieldUpdater()),
                new AttachedIndicatorCheckboxHeader(), "90px"); //$NON-NLS-1$

        networks.addColumn(
                new RequiredIndicatorCheckboxColumn(new RequiredIndicatorFieldUpdater()),
                new RequiredAllCheckboxHeader(), "110px"); //$NON-NLS-1$

        networks.addColumn(
                new VmNetworkImageSafeHtmlWithSafeHtmlTooltipColumn(),
                constants.vmNetwork(), "80px"); //$NON-NLS-1$

        networks.addColumn(
                new ManagementNetworkIndicatorCheckboxColumn(multiCluster, new ManagementNetworkIndicatorFieldUpdater()),
                constants.managementItemInfo(), "80px"); //$NON-NLS-1$

        networks.addColumn(
                new DisplayNetworkIndicatorCheckboxColumn(multiCluster,
                        new DisplayNetworkIndicatorFieldUpdater()),
                new SafeHtmlHeader(SafeHtmlUtils.fromSafeConstant(constants.displayNetwork()),
                        SafeHtmlUtils.fromSafeConstant(constants.changeDisplayNetworkWarning())),
                "100px"); //$NON-NLS-1$

        networks.addColumn(
                new MigrationNetworkIndicatorCheckboxColumn(multiCluster,
                        new MigrationNetworkIndicatorFieldUpdater()),
                constants.migrationNetwork(), "105px"); //$NON-NLS-1$

        networks.addColumn(
                new GlusterNetworkIndicatorCheckboxColumn(multiCluster,
                        new GlusterNetworkIndicatorFieldUpdater()),
                constants.glusterNetwork(), "100px"); //$NON-NLS-1$
    }

    @Override
    public void edit(ClusterNetworkManageModel clusterNetworkManageModel) {
        networks.asEditor().edit(clusterNetworkManageModel);
        initEntityModelCellTable();
    }

    @Override
    public ClusterNetworkManageModel flush() {
        return networks.asEditor().flush();
    }

    private void changeIsAttached(ClusterNetworkModel clusterNetworkModel, Boolean value) {
        clusterNetworkModel.setAttached(value);
        if (!value) {
            if (clusterNetworkModel.isDisplayNetwork()) {
                updateDisplayNetwork(clusterNetworkModel, false);
            }
            if (clusterNetworkModel.isMigrationNetwork()) {
                updateMigrationNetwork(clusterNetworkModel, false);
            }
            if (clusterNetworkModel.isGlusterNetwork()) {
                updateGlusterNetwork(clusterNetworkModel, false);
            }
            if (clusterNetworkModel.isRequired()) {
                clusterNetworkModel.setRequired(false);
            }
        }
    }

    private boolean canEditAssign(ClusterNetworkModel clusterNetworkModel) {
        return !clusterNetworkModel.isManagement();
    }

    private boolean canEditRequired(ClusterNetworkModel clusterNetworkModel) {
        return clusterNetworkModel.isAttached() && !clusterNetworkModel.isManagement()
                && !clusterNetworkModel.isExternal();
    }

    private static final class NetworkNameTextColumnWithTooltip extends AbstractTextColumn<ClusterNetworkModel> {
        @Override
        public String getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.getDisplayedName();
        }
    }

    private final class VmNetworkImageSafeHtmlWithSafeHtmlTooltipColumn
            extends AbstractSafeHtmlColumn<ClusterNetworkModel> {
        private final ApplicationConstants constants = AssetProvider.getConstants();

        private VmNetworkImageSafeHtmlWithSafeHtmlTooltipColumn() {
            }

        @Override
        public SafeHtml getValue(ClusterNetworkModel clusterNetworkModel) {
            return MultiImageColumnHelper.getValue(Collections.singletonList(clusterNetworkModel.isVmNetwork() ?
                    vmImage : emptyImage));
        }

        @Override
        public SafeHtml getTooltip(ClusterNetworkModel clusterNetworkModel) {
            return MultiImageColumnHelper.getTooltip(clusterNetworkModel.isVmNetwork() ?
                    Collections.singletonMap(vmImage, constants.vmItemInfo())
                    : Collections.<SafeHtml, String>emptyMap());
        }
    }

    private final class RequiredIndicatorCheckboxColumn extends AbstractCheckboxColumn<ClusterNetworkModel> {

        private RequiredIndicatorCheckboxColumn(RequiredIndicatorFieldUpdater requiredIndicatorFieldUpdater) {
            super(requiredIndicatorFieldUpdater);
        }

        @Override
        public Boolean getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isRequired();
        }

        @Override
        protected boolean canEdit(ClusterNetworkModel clusterNetworkModel) {
            return canEditRequired(clusterNetworkModel);
        }

        @Override
        public void render(Context context, ClusterNetworkModel object, SafeHtmlBuilder sb) {
            super.render(context, object, sb);
            sb.append(templates.textForCheckBox(constants.required()));
        }
    }

    private final class RequiredIndicatorFieldUpdater implements FieldUpdater<ClusterNetworkModel, Boolean> {
        @Override
        public void update(int index, ClusterNetworkModel clusterNetworkModel, Boolean value) {
            clusterNetworkModel.setRequired(value);
            refreshNetworksTable();
        }
    }

    private final class RequiredAllCheckboxHeader extends AbstractCheckboxHeader {

        @Override
        protected void selectionChanged(Boolean value) {
            for (ClusterNetworkModel clusterNetworkModel : getNetworksTableItems()) {
                if (canEditRequired(clusterNetworkModel)) {
                    clusterNetworkModel.setRequired(value);
                }
                refreshNetworksTable();
            }
        }

        @Override
        public Boolean getValue() {
            boolean allEntriesDisabled = !isEnabled();
            for (ClusterNetworkModel clusterNetworkModel : getNetworksTableItems()) {
                if (allEntriesDisabled || canEditRequired(clusterNetworkModel)) {
                    if (!clusterNetworkModel.isRequired()) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean isEnabled() {
            for (ClusterNetworkModel clusterNetworkModel : getNetworksTableItems()) {
                if (canEditRequired(clusterNetworkModel)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getLabel() {
            return constants.requiredAll();
        }
    }

    private final class AttachedIndicatorCheckboxColumn extends AbstractCheckboxColumn<ClusterNetworkModel> {
        private AttachedIndicatorCheckboxColumn(AttachedIndicatorFieldUpdater attachedIndicatorFieldUpdater) {
            super(attachedIndicatorFieldUpdater);
        }

        @Override
        public Boolean getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isAttached();
        }

        @Override
        protected boolean canEdit(ClusterNetworkModel clusterNetworkModel) {
            return canEditAssign(clusterNetworkModel);
        }

        @Override
        public void render(Context context, ClusterNetworkModel object, SafeHtmlBuilder sb) {
            super.render(context, object, sb);
            sb.append(templates.textForCheckBox(constants.assign()));
        }
    }

    private final class AttachedIndicatorFieldUpdater implements FieldUpdater<ClusterNetworkModel, Boolean> {
        @Override
        public void update(int index, ClusterNetworkModel clusterNetworkModel, Boolean value) {
            changeIsAttached(clusterNetworkModel, value);
            refreshNetworksTable();
        }
    }

    private final class AttachedIndicatorCheckboxHeader extends AbstractCheckboxHeader {

        @Override
        protected void selectionChanged(Boolean value) {
            for (ClusterNetworkModel clusterNetworkModel : getNetworksTableItems()) {
                if (canEditAssign(clusterNetworkModel)) {
                    changeIsAttached(clusterNetworkModel, value);
                }
            }
            refreshNetworksTable();
        }

        @Override
        public Boolean getValue() {
            boolean allEntriesDisabled = !isEnabled();
            for (ClusterNetworkModel clusterNetworkModel : getNetworksTableItems()) {
                if (allEntriesDisabled || canEditAssign(clusterNetworkModel)) {
                    if (!clusterNetworkModel.isAttached()) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean isEnabled() {
            for (ClusterNetworkModel clusterNetworkModel : getNetworksTableItems()) {
                if (canEditAssign(clusterNetworkModel)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getLabel() {
            return constants.assignAll();
        }

    }

    private static final class MigrationNetworkIndicatorCheckboxColumn extends AbstractCheckboxColumn<ClusterNetworkModel> {
        private MigrationNetworkIndicatorCheckboxColumn(boolean multipleSelectionAllowed,
                MigrationNetworkIndicatorFieldUpdater migrationNetworkIndicatorFieldUpdater) {
            super(multipleSelectionAllowed, migrationNetworkIndicatorFieldUpdater);
        }

        @Override
        public Boolean getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isMigrationNetwork();
        }

        @Override
        protected boolean canEdit(ClusterNetworkModel clusterNetworkModel) {
            Boolean migrationNetworkEnabled =
                    (Boolean) AsyncDataProvider.getInstance().getConfigValuePreConverted(ConfigurationValues.MigrationNetworkEnabled,
                            clusterNetworkModel.getCluster().getCompatibilityVersion().toString());
            return migrationNetworkEnabled && clusterNetworkModel.isAttached() && !clusterNetworkModel.isExternal();
        }
    }

    private final class MigrationNetworkIndicatorFieldUpdater implements FieldUpdater<ClusterNetworkModel, Boolean> {
        @Override
        public void update(int index, ClusterNetworkModel clusterNetworkModel, Boolean value) {
            updateMigrationNetwork(clusterNetworkModel, value);
            refreshNetworksTable();
        }
    }

    private void updateMigrationNetwork(ClusterNetworkModel clusterNetworkModel, boolean value) {
        networks.asEditor().flush().setMigrationNetwork(clusterNetworkModel, value);
    }

    private static final class ManagementNetworkIndicatorCheckboxColumn extends AbstractCheckboxColumn<ClusterNetworkModel> {
        private final boolean multiCluster;

        private ManagementNetworkIndicatorCheckboxColumn(boolean multiCluster,
                ManagementNetworkIndicatorFieldUpdater managementNetworkIndicatorFieldUpdater) {
            super(multiCluster, managementNetworkIndicatorFieldUpdater);
            this.multiCluster = multiCluster;
        }

        @Override
        public Boolean getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isManagement();
        }

        @Override
        protected boolean canEdit(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isAttached() && clusterNetworkModel.isRequired() && !clusterNetworkModel.isExternal() &&
                    !(multiCluster && isManagementOriginally(clusterNetworkModel));
        }

        private boolean isManagementOriginally(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.getOriginalNetworkCluster() != null
                    && clusterNetworkModel.getOriginalNetworkCluster().isManagement();
        }
    }

    private final class ManagementNetworkIndicatorFieldUpdater implements FieldUpdater<ClusterNetworkModel, Boolean> {
        @Override
        public void update(int index, ClusterNetworkModel clusterNetworkModel, Boolean value) {
            updateManagementNetwork(clusterNetworkModel, value);
            refreshNetworksTable();
        }
    }

    private void updateManagementNetwork(ClusterNetworkModel clusterNetworkModel, boolean value) {
        networks.asEditor().flush().setManagementNetwork(clusterNetworkModel, value);
    }

    private static final class GlusterNetworkIndicatorCheckboxColumn extends AbstractCheckboxColumn<ClusterNetworkModel> {
        private GlusterNetworkIndicatorCheckboxColumn(boolean multipleSelectionAllowed,
                GlusterNetworkIndicatorFieldUpdater glusterNetworkIndicatorFieldUpdater) {
            super(multipleSelectionAllowed, glusterNetworkIndicatorFieldUpdater);
        }

        @Override
        public Boolean getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isGlusterNetwork();
        }

        @Override
        protected boolean canEdit(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isAttached() && !clusterNetworkModel.isExternal();
        }

    }

    private final class GlusterNetworkIndicatorFieldUpdater implements FieldUpdater<ClusterNetworkModel, Boolean> {
        @Override
        public void update(int index, ClusterNetworkModel clusterNetworkModel, Boolean value) {
            updateGlusterNetwork(clusterNetworkModel, value);
            refreshNetworksTable();
        }
    }

    private void updateGlusterNetwork(ClusterNetworkModel clusterNetworkModel, boolean value) {
        networks.asEditor().flush().setGlusterNetwork(clusterNetworkModel, value);
    }

    private final class DisplayNetworkIndicatorFieldUpdater implements FieldUpdater<ClusterNetworkModel, Boolean> {
        @Override
        public void update(int index, ClusterNetworkModel clusterNetworkModel, Boolean value) {
            updateDisplayNetwork(clusterNetworkModel, value);
            refreshNetworksTable();
        }
    }

    private void updateDisplayNetwork(ClusterNetworkModel clusterNetworkModel, boolean value) {
        networks.asEditor().flush().setDisplayNetwork(clusterNetworkModel, value);
    }

    private final static class DisplayNetworkIndicatorCheckboxColumn extends AbstractCheckboxColumn<ClusterNetworkModel> {
        private DisplayNetworkIndicatorCheckboxColumn(boolean multipleSelectionAllowed,
                DisplayNetworkIndicatorFieldUpdater displayNetworkIndicatorFieldUpdater) {
            super(multipleSelectionAllowed, displayNetworkIndicatorFieldUpdater);
        }

        @Override
        public Boolean getValue(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isDisplayNetwork();
        }

        @Override
        protected boolean canEdit(ClusterNetworkModel clusterNetworkModel) {
            return clusterNetworkModel.isAttached() && !clusterNetworkModel.isExternal();
        }
    }
}
