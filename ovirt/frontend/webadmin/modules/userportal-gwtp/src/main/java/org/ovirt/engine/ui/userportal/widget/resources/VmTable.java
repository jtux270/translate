package org.ovirt.engine.ui.userportal.widget.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.SubTableResources;
import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.table.ActionCellTable;
import org.ovirt.engine.ui.common.widget.table.column.ImageResourceColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.resources.ResourcesModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.uicommon.model.resources.ResourcesModelProvider;

import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;

public class VmTable extends Composite implements HasEditorDriver<ResourcesModel> {

    private static final VmRowHeaderlessTableResources vmRowResources =
            GWT.create(VmRowHeaderlessTableResources.class);

    private static final DiskRowHeaderlessTableResources diskRowResources =
            GWT.create(DiskRowHeaderlessTableResources.class);

    private HandlerRegistration openHandler = null;

    private HandlerRegistration closeHandler = null;

    private final ResourcesModelProvider modelProvider;

    @UiField(provided = true)
    ActionCellTable<VM> tableHeader;

    @UiField
    Tree vmTree;

    private final ApplicationResources resources;

    private VmSingleSelectionModel vmSelectionModel = new VmSingleSelectionModel();

    private ApplicationConstants constants;

    interface WidgetUiBinder extends UiBinder<Widget, VmTable> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    public VmTable(ResourcesModelProvider modelProvider,
            SubTableResources headerResources,
            ApplicationResources resources, ApplicationConstants constants) {
        this.modelProvider = modelProvider;
        this.resources = resources;
        this.constants = constants;
        tableHeader = new ActionCellTable<VM>(modelProvider, headerResources);
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        initTable();
    }

    private void initTable() {
        tableHeader.addColumn(new EmptyColumn(), constants.virtualMachineSnapshotCreatePopupDescriptionLabel());
        tableHeader.addColumn(new EmptyColumn(), constants.disksVm());
        tableHeader.addColumn(new EmptyColumn(), constants.virtualSizeVm());
        tableHeader.addColumn(new EmptyColumn(), constants.actualSizeVm());
        tableHeader.addColumn(new EmptyColumn(), constants.snapshotsVm());

        setHeaderColumnWidths(Arrays.asList("40%", "10%", "10%", "10%", "30%")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        tableHeader.setRowData(new ArrayList<VM>());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void edit(ResourcesModel model) {
        if (openHandler != null) {
            openHandler.removeHandler();
        }

        if (closeHandler != null) {
            closeHandler.removeHandler();
        }

        clearTreeItems();
        vmTree.clear();

        for (VM vm : (Iterable<VM>) model.getItems()) {
            VmTreeItem vmItem = createVmItem(vm);
            if (vm.getDiskList() != null){
                for (DiskImage disk : vm.getDiskList()) {
                    TreeItem diskItem = createDiskItem(disk);
                    vmItem.addItem(diskItem);
                }
            }
            vmTree.addItem(vmItem);
        }

        updateSelection(model);
        listenOnSelectionChange();
    }

    private void clearTreeItems() {
        int nodeCount = vmTree.getItemCount();
        for (int i = 0; i < nodeCount; i++) {
            clearTreeItems(vmTree.getItem(i));
        }
    }

    private void clearTreeItems(TreeItem node) {
        int nodeCount = node.getChildCount();
        for (int i = 0; i < nodeCount; i++) {
            TreeItem child = node.getChild(i);
            if (child instanceof VmTreeItem) {
                clearTreeItems((VmTreeItem) child);
            }
        }
        if (node instanceof VmTreeItem) {
            ((VmTreeItem) node).reset();
        }
    }

    @Override
    public ResourcesModel flush() {
        return modelProvider.getModel();
    }

    private void setHeaderColumnWidths(List<String> widths) {
        for (int i = 0; i < tableHeader.getColumnCount(); i++) {
            tableHeader.setColumnWidth(tableHeader.getColumn(i), widths.get(i));
        }
    }

    protected void listenOnSelectionChange() {
        openHandler = vmTree.addOpenHandler(new OpenHandler<TreeItem>() {
            @Override
            public void onOpen(OpenEvent<TreeItem> event) {
                setSelectionToModel();
            }

        });

        closeHandler = vmTree.addCloseHandler(new CloseHandler<TreeItem>() {

            @Override
            public void onClose(CloseEvent<TreeItem> event) {
                setSelectionToModel();
            }
        });
    }

    private void setSelectionToModel() {
        List<VM> selectedVMs = new ArrayList<VM>();
        for (int i = 0; i < vmTree.getItemCount(); i++) {
            if (vmTree.getItem(i) instanceof VmTreeItem) {
                if (vmTree.getItem(i).getState()) {
                    selectedVMs.add(((VmTreeItem) vmTree.getItem(i)).getVm());
                }
            }
        }

        modelProvider.setSelectedItems(selectedVMs);
    }

    @SuppressWarnings("unchecked")
    protected void updateSelection(final ResourcesModel model) {
        if (model.getSelectedItems() == null || model.getSelectedItems().size() == 0) {
            return;
        }
        for (int i = 0; i < vmTree.getItemCount(); i++) {
            if (vmTree.getItem(i) instanceof VmTreeItem) {
                ((VmTreeItem) vmTree.getItem(i)).setState(model.getSelectedItems());
            }
        }
    }

    private TreeItem createDiskItem(DiskImage disk) {
        EntityModelCellTable<ListModel> table =
                new EntityModelCellTable<ListModel>(false,
                        diskRowResources,
                        true);

        Column<EntityModel, EntityModel> diskWithMappingColumn =
                new Column<EntityModel, EntityModel>(createDiskImageWithMappingComoisiteCell()) {

                    @Override
                    public EntityModel getValue(EntityModel object) {
                        return object;
                    }
                };

        TextColumn<EntityModel> paddingColumn = new TextColumn<EntityModel>() {

            @Override
            public String getValue(EntityModel entity) {
                return asDisk(entity).getDiskAlias(); //$NON-NLS-1$
            }
        };

        TextColumnWithTooltip<EntityModel> virtualSizeColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return asDisk(entity).getSizeInGigabytes() + "GB"; //$NON-NLS-1$
            }
        };

        TextColumnWithTooltip<EntityModel> actualSizeColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return ((Double) asDisk(entity).getActualDiskWithSnapshotsSize()).intValue() + "GB"; //$NON-NLS-1$
            }
        };

        TextColumnWithTooltip<EntityModel> snapshotsColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return asDisk(entity).getSnapshots().size() + ""; //$NON-NLS-1$
            }
        };

        table.addColumn(diskWithMappingColumn, "", "39%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(paddingColumn, "", "10%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(virtualSizeColumn, "", "10%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(actualSizeColumn, "", "10%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(snapshotsColumn, "", "31%"); //$NON-NLS-1$ //$NON-NLS-2$
        EntityModel entityModel = new EntityModel();
        entityModel.setEntity(disk);

        table.setRowData(Arrays.asList(entityModel));
        return new TreeItem(table);
    }

    private VmTreeItem createVmItem(VM vm) {
        EntityModelCellTable<ListModel> table =
                new EntityModelCellTable<ListModel>(false,
                        vmRowResources,
                        true);

        Column<EntityModel, EntityModel> vmImageWithNameColumn =
                new Column<EntityModel, EntityModel>(createVmImageWithNameCompositeCell()) {

                    @Override
                    public EntityModel getValue(EntityModel object) {
                        return object;
                    }
                };

        TextColumnWithTooltip<EntityModel> diskSizeColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                ArrayList<DiskImage> diskImages = asVm(entity).getDiskList();
                return diskImages != null ? diskImages.size() + "" : "0"; //$NON-NLS-1$ //$NON-NLS-2$
            }
        };

        TextColumnWithTooltip<EntityModel> virtualSizeColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return ((Double) asVm(entity).getDiskSize()).intValue() + "GB"; //$NON-NLS-1$
            }

        };

        TextColumnWithTooltip<EntityModel> actualSizeColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return ((Double) asVm(entity).getActualDiskWithSnapshotsSize()).intValue() + "GB"; //$NON-NLS-1$
            }
        };

        TextColumnWithTooltip<EntityModel> snapshotsColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                ArrayList<DiskImage> diskImages = asVm(entity).getDiskList();
                return diskImages != null ? diskImages.size() > 0 ? diskImages.get(0).getSnapshots().size()
                        + "" : "0" : "0"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        };

        table.addColumn(vmImageWithNameColumn, "", "39%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(diskSizeColumn, "", "10%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(virtualSizeColumn, "", "10%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(actualSizeColumn, "", "10%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.addColumn(snapshotsColumn, "", "31%"); //$NON-NLS-1$ //$NON-NLS-2$
        table.setSelectionModel(vmSelectionModel);

        EntityModel entityModel = new EntityModel();
        entityModel.setEntity(vm);

        table.setRowData(Arrays.asList(entityModel));
        return new VmTreeItem(table, vm);
    }

    private CompositeCell<EntityModel> createDiskImageWithMappingComoisiteCell() {

        final ImageResourceColumn<EntityModel> diskImageColumn = new ImageResourceColumn<EntityModel>() {

            @Override
            public ImageResource getValue(EntityModel object) {
                return resources.vmDiskIcon();
            }

        };

        final TextColumnWithTooltip<EntityModel> driveMappingColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return asDisk(entity).getDiskAlias(); //$NON-NLS-1$
            }
        };

        return new StyledCompositeCell<EntityModel>(
                new ArrayList<HasCell<EntityModel, ?>>(Arrays.asList(diskImageColumn, driveMappingColumn)),
                new StyledCompositeCell.StyledProvider<EntityModel>() {

                    @Override
                    public String styleStringOf(HasCell<EntityModel, ?> cell) {
                        if (cell == diskImageColumn) {
                            return "float: left"; //$NON-NLS-1$
                        } else if (cell == driveMappingColumn) {
                            return "float: left; padding-top: 4px; padding-left: 5px;"; //$NON-NLS-1$
                        }

                        return null;
                    }
                });

    }

    private CompositeCell<EntityModel> createVmImageWithNameCompositeCell() {
        final ImageResourceColumn<EntityModel> vmImageColumn = new ImageResourceColumn<EntityModel>() {

            @Override
            public ImageResource getValue(EntityModel object) {
                return resources.vmIconWithVmTextInside();
            }

        };

        final TextColumnWithTooltip<EntityModel> nameColumn = new TextColumnWithTooltip<EntityModel>() {
            @Override
            public String getValue(EntityModel entity) {
                return asVm(entity).getName();
            }
        };

        return new StyledCompositeCell<EntityModel>(
                new ArrayList<HasCell<EntityModel, ?>>(Arrays.asList(vmImageColumn, nameColumn)),
                new StyledCompositeCell.StyledProvider<EntityModel>() {

                    @Override
                    public String styleStringOf(HasCell<EntityModel, ?> cell) {
                        if (cell == vmImageColumn) {
                            return "float: left"; //$NON-NLS-1$
                        } else if (cell == nameColumn) {
                            return "float: left; padding-top: 4px;"; //$NON-NLS-1$

                        }

                        return null;
                    }
                });
    }

    /**
     * This class guards, that only one row can be selected at a given time and the selection survives the refresh. The
     * single instance of this class should be used for more instances of EntityModelCellTable
     */
    static class VmSingleSelectionModel extends SingleSelectionModel<EntityModel> {

        private VM selectedVM = null;

        @Override
        public void setSelected(EntityModel object, boolean selected) {
            if (object.getEntity() instanceof VM) {
                if (selected) {
                    selectedVM = (VM) object.getEntity();
                    super.setSelected(object, true);
                } else {
                    selectedVM = null;
                    super.setSelected(object, false);
                }
            } else {
                super.setSelected(object, selected);
            }

        }

        @Override
        public boolean isSelected(EntityModel object) {
            if (selectedVM == null || !(object.getEntity() instanceof VM)) {
                return super.isSelected(object);
            }

            VM vm = (VM) object.getEntity();
            if (vm.getId().equals(selectedVM.getId())) {
                return true;
            } else {
                return false;
            }
        }
    }

    private VM asVm(EntityModel entity) {
        return (VM) entity.getEntity();
    }

    private DiskImage asDisk(EntityModel entity) {
        return (DiskImage) entity.getEntity();
    }

    public interface VmRowHeaderlessTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/userportal/css/VmListHeaderlessTable.css" })
        TableStyle cellTableStyle();
    }

    public interface DiskRowHeaderlessTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/userportal/css/DiskListHeaderlessTable.css" })
        TableStyle cellTableStyle();
    }

    /**
     * An empty column - only for the header
     */
    private static class EmptyColumn extends TextColumn<VM> {

        @Override
        public String getValue(VM object) {
            return null;
        }
    }

}

class StyledCompositeCell<T> extends CompositeCell<T> {

    private final List<HasCell<T, ?>> hasCells;
    private final StyledProvider<T> styleProvider;

    public StyledCompositeCell(List<HasCell<T, ?>> hasCells, StyledProvider<T> styleProvider) {
        super(hasCells);
        this.hasCells = hasCells;
        this.styleProvider = styleProvider;
    }

    @Override
    public void render(Context context, T value, SafeHtmlBuilder sb) {
        for (HasCell<T, ?> hasCell : hasCells) {
            String style =
                    styleProvider.styleStringOf(hasCell) == null ? "" : "style=\"" //$NON-NLS-1$ //$NON-NLS-2$
                            + styleProvider.styleStringOf(hasCell) + "\""; //$NON-NLS-1$
            sb.appendHtmlConstant("<div " + style + ">"); //$NON-NLS-1$ //$NON-NLS-2$
            render(context, value, sb, hasCell);
            sb.appendHtmlConstant("</div>"); //$NON-NLS-1$
        }
    }

    @Override
    protected Element getContainerElement(Element parent) {
        return parent.getFirstChildElement();
    }

    interface StyledProvider<T> {
        String styleStringOf(HasCell<T, ?> cell);
    }
}
