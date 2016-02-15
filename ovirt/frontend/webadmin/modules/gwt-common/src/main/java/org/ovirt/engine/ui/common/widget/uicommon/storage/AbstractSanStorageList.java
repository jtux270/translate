package org.ovirt.engine.ui.common.widget.uicommon.storage;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import org.ovirt.engine.core.common.businessentities.LUNs;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.LunModel;
import org.ovirt.engine.ui.uicommonweb.models.storage.SanStorageModelBase;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractSanStorageList<M extends EntityModel, L extends ListModel> extends Composite {

    @SuppressWarnings("rawtypes")
    interface WidgetUiBinder extends UiBinder<Widget, AbstractSanStorageList> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    @UiField
    SimplePanel treeHeader;

    @UiField
    ScrollPanel treeContainer;

    SanStorageModelBase model;

    Tree tree;

    boolean hideLeaf;
    boolean multiSelection;

    protected static final CommonApplicationConstants constants = GWT.create(CommonApplicationConstants.class);
    protected static final CommonApplicationMessages messages = GWT.create(CommonApplicationMessages.class);
    protected static final CommonApplicationResources resources = GWT.create(CommonApplicationResources.class);

    public AbstractSanStorageList(SanStorageModelBase model) {
        this(model, false, false);
    }

    public AbstractSanStorageList(SanStorageModelBase model, boolean hideLeaf, boolean multiSelection) {
        this.model = model;
        this.hideLeaf = hideLeaf;
        this.multiSelection = multiSelection;
        model.setMultiSelection(multiSelection);
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        createHeaderWidget();
        createSanStorageListWidget();
    }

    public void activateItemsUpdate() {
        disableItemsUpdate();

        model.getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                updateItems();
            }
        });
        updateItems();
    }

    public void disableItemsUpdate() {
        model.getItemsChangedEvent().getListeners().clear();
    }

    @SuppressWarnings("unchecked")
    protected void updateItems() {
        List<M> items = (List<M>) model.getItems();
        tree.clear();

        if (items != null) {
            for (M rootModel : items) {
                addRootNode(createRootNode(rootModel), createLeafNode(getLeafModel(rootModel)));
            }
        }
    }

    protected void addRootNode(final TreeItem rootItem, final TreeItem leafItem) {
        rootItem.getElement().getStyle().setBackgroundColor("#eff3ff"); //$NON-NLS-1$
        rootItem.getElement().getStyle().setMarginBottom(1, Unit.PX);
        rootItem.getElement().getStyle().setPadding(0, Unit.PX);

        if (leafItem != null) {
            rootItem.addItem(leafItem);

            // Defer styling in order to override padding done in:
            // com.google.gwt.user.client.ui.Tree -> showLeafImage
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    leafItem.getElement().getStyle().setBackgroundColor("#ffffff"); //$NON-NLS-1$
                    leafItem.getElement().getStyle().setMarginLeft(20, Unit.PX);
                    leafItem.getElement().getStyle().setPadding(0, Unit.PX);

                    Boolean isLeafEmpty = (Boolean) leafItem.getUserObject();
                    if (isLeafEmpty != null && isLeafEmpty.equals(Boolean.TRUE)) {
                        rootItem.getElement().getElementsByTagName("td").getItem(0).getStyle().setVisibility(Visibility.HIDDEN); //$NON-NLS-1$
                    }
                    rootItem.getElement().getElementsByTagName("td").getItem(1).getStyle().setWidth(100, Unit.PCT); //$NON-NLS-1$
                }
            });
        }

        tree.addItem(rootItem);
    }

    protected void grayOutItem(ArrayList<String> grayOutReasons,
            EntityModel model,
            EntityModelCellTable<ListModel> table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (table.getVisibleItem(row).equals(model)) {
                TableRowElement tableRowElement = table.getRowElement(row);
                Element input = tableRowElement.getElementsByTagName("input").getItem(0); //$NON-NLS-1$
                input.setPropertyBoolean("disabled", true); //$NON-NLS-1$
                updateInputTitle(grayOutReasons, input);
            }
        }
    }

    protected void updateInputTitle(ArrayList<String> grayOutReasons, Element input) {
        StringBuilder title = new StringBuilder(constants.empty());
        for (String reason : grayOutReasons) {
            title.append(reason).append(constants.space());
        }
        input.setTitle(title.toString());
    }

    protected void updateSelectedLunWarning(LunModel lunModel) {
        LUNs lun = (LUNs) lunModel.getEntity();
        String warning = constants.empty();

        // Adding 'GrayedOutReasons'
        if (lun.getStorageDomainId() != null) {
            warning = messages.lunAlreadyPartOfStorageDomainWarning(lun.getStorageDomainName());
        }
        else if (lun.getDiskId() != null) {
            warning = messages.lunUsedByDiskWarning(lun.getDiskAlias());
        }

        model.setSelectedLunWarning(warning);
    }

    protected void createSanStorageListWidget() {
        tree = new Tree();
        treeContainer.add(tree);
    }

    public void setTreeContainerStyleName(String styleName) {
        treeContainer.setStyleName(styleName);
    }

    public void setTreeContainerHeight(double height) {
        treeContainer.getElement().getStyle().setHeight(height, Unit.PX);
    }

    public ScrollPanel getTreeContainer() {
        return treeContainer;
    }

    protected abstract void createHeaderWidget();

    protected abstract L getLeafModel(M rootModel);

    protected abstract TreeItem createRootNode(M rootModel);

    protected abstract TreeItem createLeafNode(L leafModel);

    public interface SanStorageListHeaderResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/SanStorageListHeader.css" })
        TableStyle cellTableStyle();
    }

    public interface SanStorageListTargetRootResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/SanStorageListTargetRoot.css" })
        TableStyle cellTableStyle();
    }

    public interface SanStorageListLunTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/SanStorageListLunTable.css" })
        TableStyle cellTableStyle();
    }

    public interface SanStorageListLunRootResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/SanStorageListLunRoot.css" })
        TableStyle cellTableStyle();
    }

    public interface SanStorageListTargetTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/SanStorageListTargetTable.css" })
        TableStyle cellTableStyle();
    }

}
