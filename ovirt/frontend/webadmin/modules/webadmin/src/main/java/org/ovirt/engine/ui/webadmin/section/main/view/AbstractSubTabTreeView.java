package org.ovirt.engine.ui.webadmin.section.main.view;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.widget.action.SubTabTreeActionPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.label.NoItemsLabel;
import org.ovirt.engine.ui.common.widget.tree.AbstractSubTabTree;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractSubTabTreeView<E extends AbstractSubTabTree, I, T, M extends ListWithDetailsModel, D extends SearchableListModel> extends AbstractSubTabTableView<I, T, M, D> {

    interface ViewUiBinder extends UiBinder<Widget, AbstractSubTabTreeView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    protected SimplePanel headerTableContainer;

    @UiField
    protected SimplePanel treeContainer;

    @UiField
    protected SimplePanel actionPanelContainer;

    protected SubTabTreeActionPanel actionPanel;

    protected EntityModelCellTable<ListModel> table;

    protected E tree;

    boolean isActionTree;

    protected final ApplicationConstants constants;
    protected final ApplicationTemplates templates;
    protected final ApplicationResources resources;

    public AbstractSubTabTreeView(SearchableDetailModelProvider modelProvider,
            ApplicationConstants constants, ApplicationTemplates templates, ApplicationResources resources) {
        super(modelProvider);

        this.constants = constants;
        this.templates = templates;
        this.resources = resources;

        table = new EntityModelCellTable<ListModel>(false, true);
        tree = getTree();

        initHeader(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        headerTableContainer.add(table);
        treeContainer.add(tree);

        actionPanel = createActionPanel(modelProvider);
        if (actionPanel != null) {
            actionPanelContainer.add(actionPanel);
            actionPanel.addContextMenuHandler(tree);
        }

        updateStyles();
    }

    private void updateStyles() {
        treeContainer.addStyleName(isActionTree ? style.actionTreeContainer() : style.treeContainer());
    }

    public void setIsActionTree(boolean isActionTree) {
        this.isActionTree = isActionTree;

        updateStyles();
    }

    private final IEventListener itemsChangedListener = new IEventListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void eventRaised(Event ev, Object sender, EventArgs args) {
            table.setRowData(new ArrayList<EntityModel>());
            // Since tree views don't have an 'emptyTreeWidget to display, we will
            // use the fact that we are using a table to display the 'header' to have
            // it display the no items to display message.
            if (sender instanceof ListModel) {
                ListModel model = (ListModel) sender;
                Iterable<M> items = model.getItems();
                if (model.getItems() == null || (items instanceof List && ((List<M>) items).isEmpty())) {
                    table.setEmptyTableWidget(new NoItemsLabel());
                } else {
                    table.setEmptyTableWidget(null);
                }
            }
        }
    };

    @Override
    public void setMainTabSelectedItem(I selectedItem) {
        table.setEmptyTableWidget(null);
        if (getDetailModel().getItems() == null) {
            table.setLoadingState(LoadingState.LOADING);
        }
        if (!getDetailModel().getItemsChangedEvent().getListeners().contains(itemsChangedListener)) {
            getDetailModel().getItemsChangedEvent().addListener(itemsChangedListener);
        }

        tree.clearTree();
        tree.updateTree(getDetailModel());
    }

    protected abstract void initHeader(ApplicationConstants constants);

    protected abstract E getTree();

    protected SubTabTreeActionPanel createActionPanel(SearchableDetailModelProvider modelProvider) {
        return null;
    }

    interface WidgetStyle extends CssResource {
        String treeContainer();

        String actionTreeContainer();
    }
}
