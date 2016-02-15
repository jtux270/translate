package org.ovirt.engine.ui.webadmin.uicommon.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gwtplatform.dispatch.annotation.GenEvent;

import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.DataBoundTabModelProvider;
import org.ovirt.engine.ui.common.widget.tree.TreeModelWithElementId;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.widget.tree.SystemTreeItemCell;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SystemTreeModelProvider extends DataBoundTabModelProvider<SystemTreeItemModel, SystemTreeModel>
        implements SearchableTreeModelProvider<SystemTreeItemModel, SystemTreeModel>, TreeModelWithElementId {

    @GenEvent
    public class SystemTreeSelectionChange {

        SystemTreeItemModel selectedItem;

    }

    private final DefaultSelectionEventManager<SystemTreeItemModel> selectionManager =
            DefaultSelectionEventManager.createDefaultManager();
    private final SingleSelectionModel<SystemTreeItemModel> selectionModel;

    private final SystemTreeItemCell cell;

    private CellTree display;

    @Inject
    public SystemTreeModelProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            ApplicationResources applicationResources, ApplicationTemplates applicationTemplates) {
        super(eventBus, defaultConfirmPopupProvider);
        this.cell = new SystemTreeItemCell(applicationResources, applicationTemplates);

        // Create selection model
        selectionModel = new SingleSelectionModel<SystemTreeItemModel>();
        selectionModel.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                SystemTreeModelProvider.this.setSelectedItems(Arrays.asList(selectionModel.getSelectedObject()));
            }
        });
    }

    @Override
    protected void initializeModelHandlers() {
        super.initializeModelHandlers();

        // Add model reset handler
        getModel().getResetRequestedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                ArrayList<SystemTreeItemModel> items = getModel().getItems();
                if (items != null && !items.isEmpty()) {
                    // Select first (root) tree item
                    selectionModel.setSelected(items.get(0), true);
                }
            }
        });
    }

    @Override
    protected void updateDataProvider(List<SystemTreeItemModel> items) {
        // Update data provider only for non-empty data
        if (!items.isEmpty()) {
            super.updateDataProvider(items);
            selectionModel.setSelected(getModel().getSelectedItem(), true);
        }
    }

    @Override
    public SystemTreeModel getModel() {
        return getCommonModel().getSystemTree();
    }

    public SingleSelectionModel<SystemTreeItemModel> getSelectionModel() {
        return selectionModel;
    }

    // TODO-GWT: https://code.google.com/p/google-web-toolkit/issues/detail?id=6310
    public void setSelectedItem(Guid id) {
        display.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED); // open small GWT workaround
        selectionModel.setSelected(getModel().getItemById(id), true);
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                display.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.BOUND_TO_SELECTION); // close small GWT workaround
            }
        });
    }

    @Override
    public void setSelectedItems(List<SystemTreeItemModel> items) {
        getModel().setSelectedItem(items.size() > 0 ? items.get(0) : null);

        if (items.size() > 0) {
            SystemTreeSelectionChangeEvent.fire(this, items.get(0));
        }
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T parent) {
        if (parent != null) {
            // Not a root node
            SystemTreeItemModel parentModel = (SystemTreeItemModel) parent;
            List<SystemTreeItemModel> children = parentModel.getChildren();
            return new DefaultNodeInfo<SystemTreeItemModel>(new ListDataProvider<SystemTreeItemModel>(children),
                    cell, selectionModel, selectionManager, null);
        } else {
            // This is the root node
            return new DefaultNodeInfo<SystemTreeItemModel>(getDataProvider(),
                    cell, selectionModel, selectionManager, null);
        }
    }

    @Override
    public boolean isLeaf(Object value) {
        if (value != null) {
            SystemTreeItemModel itemModel = (SystemTreeItemModel) value;
            List<SystemTreeItemModel> children = itemModel.getChildren();

            if (children != null) {
                return children.isEmpty();
            }
        }

        return false;
    }

    @Override
    public void setElementIdPrefix(String elementIdPrefix) {
        cell.setElementIdPrefix(elementIdPrefix);
    }

    public void setDataDisplay(CellTree display) {
        this.display = display;
    }

}
