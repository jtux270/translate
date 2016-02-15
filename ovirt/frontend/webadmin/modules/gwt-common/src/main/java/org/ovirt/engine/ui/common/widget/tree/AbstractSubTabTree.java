package org.ovirt.engine.ui.common.widget.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.label.TextBoxLabel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.ValueLabel;
import com.google.gwt.user.client.ui.Widget;

public abstract class AbstractSubTabTree<M extends SearchableListModel, R, N> extends Composite {

    protected final Tree tree;
    protected Map<Object, Boolean> oldItemStatesMap;
    protected Map<Object, TreeItem> oldRootItemsMap;
    protected ArrayList<Object> selectedItems;
    protected ArrayList<Object> newSelectedItems;

    protected M listModel;

    protected boolean isRootSelectionEnabled;
    protected boolean isNodeSelectionEnabled;
    protected boolean isMultiSelection;
    protected boolean isControlKeyDown;

    protected final CommonApplicationResources resources;
    protected final CommonApplicationConstants constants;
    protected final CommonApplicationTemplates templates;


    protected static final String NODE_HEADER = "nodeHeader"; //$NON-NLS-1$

    public AbstractSubTabTree(CommonApplicationResources resources, CommonApplicationConstants constants, CommonApplicationTemplates templates) {
        this.resources = resources;
        this.constants = constants;
        this.templates = templates;

        tree = new Tree();
        initWidget(tree);

        selectedItems = new ArrayList<Object>();
        newSelectedItems = new ArrayList<Object>();
        oldRootItemsMap = new HashMap<Object, TreeItem>();
        oldItemStatesMap = new HashMap<Object, Boolean>();
        isMultiSelection = true;

        tree.addOpenHandler(treeOpenHandler);
        addSelectionHandler();
    }

    public boolean isMultiSelection() {
        return isMultiSelection;
    }

    public void setMultiSelection(boolean isMultiSelection) {
        this.isMultiSelection = isMultiSelection;
    }

    public boolean isRootSelectionEnabled() {
        return isRootSelectionEnabled;
    }

    public void setRootSelectionEnabled(boolean isRootSelectionEnabled) {
        this.isRootSelectionEnabled = isRootSelectionEnabled;
        selectedItems.clear();
    }

    public boolean isNodeSelectionEnabled() {
        return isNodeSelectionEnabled;
    }

    public void setNodeSelectionEnabled(boolean isNodeSelectionEnabled) {
        this.isNodeSelectionEnabled = isNodeSelectionEnabled;
        selectedItems.clear();
    }

    private IEventListener itemsChangedEventListener = new IEventListener() {
        @Override
        public void eventRaised(Event ev, Object sender, EventArgs args) {
            refreshTree();
        }
    };

    private OpenHandler<TreeItem> treeOpenHandler = new OpenHandler<TreeItem>() {
        @Override
        public void onOpen(OpenEvent<TreeItem> event) {
            TreeItem item = event.getTarget();
            onTreeItemOpen(item);
        }
    };

    public void clearTree() {
        tree.clear();
    }

    private void saveTreeState() {
        oldItemStatesMap.clear();

        for (int i = 0; i < tree.getItemCount(); i++) {
            TreeItem rootItem = tree.getItem(i);
            oldRootItemsMap.put(rootItem.getUserObject(), rootItem);
            saveTreeItemState(rootItem);
        }
    }

    private void saveTreeItemState(TreeItem item) {
        oldItemStatesMap.put(item.getUserObject(), item.getState());

        for (int n = 0; n < item.getChildCount(); n++) {
            saveTreeItemState(item.getChild(n));
        }
    }

    private void updateTreeState() {
        newSelectedItems.clear();

        for (int i = 0; i < tree.getItemCount(); i++) {
            updateItemSelection(tree.getItem(i));
        }

        selectedItems.clear();
        selectedItems.addAll(newSelectedItems);
        onItemsSelection();
    }

    private boolean getItemOldState(Object userObject) {
        for (Map.Entry<Object, Boolean> oldItemStatesEntry : oldItemStatesMap.entrySet()) {
            if (oldItemStatesEntry.getKey() != null && userObject != null && oldItemStatesEntry.getKey().equals(userObject)) {
                return oldItemStatesEntry.getValue();
            }
        }
        return false;
    }

    public void updateTree(M listModel) {
        this.listModel = listModel;

        selectedItems.clear();
        onItemsSelection();

        listModel.getItemsChangedEvent().removeListener(itemsChangedEventListener);
        listModel.getItemsChangedEvent().addListener(itemsChangedEventListener);

        if (listModel.getItems() != null) {
            refreshTree();
        }
    }

    public void refreshTree() {
        List<R> rootItems = (List<R>) listModel.getItems();

        saveTreeState();
        tree.clear();

        if (rootItems == null)
            return;

        for (R root : rootItems) {
            TreeItem rootItem = getRootItem(root);

            TreeItem nodeHeader = getNodeHeader();
            if (nodeHeader != null) {
                rootItem.addItem(nodeHeader);
            }

            if (getNodeObjects(root).isEmpty()) {
                boolean isOpen = getItemOldState(rootItem.getUserObject());
                if (isOpen) {
                    rootItem = oldRootItemsMap.get(rootItem.getUserObject());
                }
                else {
                    if (getEmptyRoot() != null) {
                        rootItem.addItem(getEmptyRoot());
                    }
                }
            } else {
                for (N node : getNodeObjects(root)) {
                    TreeItem nodeItem = getNodeItem(node);

                    addLeaves(nodeItem, node);

                    rootItem.addItem(nodeItem);
                    styleItem(nodeItem, getIsNodeEnabled(node));
                }
            }

            tree.addItem(rootItem);
            styleItem(rootItem, true);
        }

        updateTreeState();
    }

    protected void addLeaves(TreeItem nodeItem, N node) {
        TreeItem leafItem = getLeafItem(node);
        if (leafItem != null) {
            nodeItem.addItem(leafItem);
            styleItem(leafItem, getIsNodeEnabled(node));
        }
    }

    protected abstract TreeItem getRootItem(R rootObject);

    protected abstract TreeItem getNodeItem(N nodeObject);

    protected TreeItem getLeafItem(N nodeObject) {
        return null;
    }

    protected TreeItem getNodeHeader() {
        return null;
    }

    protected TreeItem getEmptyRoot() {
        return null;
    }

    protected abstract ArrayList<N> getNodeObjects(R root);

    protected boolean getIsNodeEnabled(N nodeObject) {
        return true;
    }

    protected String getNodeDisabledTooltip() {
        return null;
    }

    protected void onTreeItemOpen(TreeItem item) {

    }

    public interface TreeHeaderlessTableResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/common/css/TreeHeaderlessTable.css" })
        TableStyle cellTableStyle();
    }

    protected TreeItem createTreeItem(EntityModelCellTable<ListModel> table, ArrayList<EntityModel> list) {
        table.setRowData(list);
        table.setWidth("100%"); //$NON-NLS-1$
        TreeItem item = new TreeItem(table);
        return item;
    }

    protected void styleItem(TreeItem item, boolean enabled) {
        Element tableElm = DOM.getFirstChild(item.getElement());
        tableElm.setAttribute("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$

        Element col = tableElm.getElementsByTagName("td").getItem(0); //$NON-NLS-1$
        col.setAttribute("width", "20px"); //$NON-NLS-1$ //$NON-NLS-2$

        if (!enabled) {
            NodeList<Element> inputs = item.getElement().getElementsByTagName("input"); //$NON-NLS-1$
            for (int i = 0; i < inputs.getLength(); i++) {
                disableElement(inputs.getItem(i));
            }
            NodeList<Element> spans = item.getElement().getElementsByTagName("span"); //$NON-NLS-1$
            for (int i = 0; i < spans.getLength(); i++) {
                disableElement(spans.getItem(i));
            }
        }

        boolean isLeafEmpty = item.getUserObject() != null && item.getUserObject().equals(true);
        if (isLeafEmpty) {
            item.getElement().getElementsByTagName("td").getItem(0).getStyle().setVisibility(Visibility.HIDDEN); //$NON-NLS-1$
        }
    }

    protected void addTextBoxToPanel(HorizontalPanel panel, TextBoxLabel item, String text, String width) {
        item.setText(text);
        addItemToPanel(panel, item, width);
    }

    protected <T> void addValueLabelToPanel(HorizontalPanel panel, ValueLabel<T> item, T value, String width) {
        item.setValue(value);
        addItemToPanel(panel, item, width);
    }

    protected void addItemToPanel(HorizontalPanel panel, Widget item, String width) {
        item.getElement().getStyle().setBackgroundColor("transparent"); //$NON-NLS-1$
        item.getElement().getStyle().setColor("black"); //$NON-NLS-1$

        panel.add(item);
        panel.setCellWidth(item, width);
    }

    protected void disableElement(Element element) {
        element.getStyle().setProperty("disabled", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        element.getStyle().setColor("#999999"); //$NON-NLS-1$

        if (getNodeDisabledTooltip() != null) {
            element.setTitle(getNodeDisabledTooltip());
        }
    }

    public void addSelectionHandler() {
        tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
            @Override
            public void onSelection(SelectionEvent<TreeItem> event) {
                onItemSelection(event.getSelectedItem(), false);
            }
        });

        tree.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                if (event.getNativeEvent().getButton() == NativeEvent.BUTTON_RIGHT) {
                    onItemSelection(findSelectedItem(event.getClientX(), event.getClientY()), true);
                }
            }
        });

        tree.addKeyDownHandler(new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                isControlKeyDown = event.isControlKeyDown();
            }
        });

        tree.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                isControlKeyDown = event.isControlKeyDown();
            }
        });
    }

    private void onItemSelection(TreeItem item, boolean enforceSelection) {
        Object entity = (Object) item.getUserObject();
        boolean isRootItem = item.getParentItem() == null;
        boolean isNodeItem = item.getParentItem() != null;

        if ((isRootItem && !isRootSelectionEnabled) || (isNodeItem && !isNodeSelectionEnabled)) {
            return;
        }

        if (!isControlKeyDown || !isMultiSelection) {
            selectedItems.clear();
        }

        saveTreeState();
        updateTreeState();

        if (!selectedItems.contains(entity)) {
            selectedItems.add(entity);
            onItemsSelection();
        }
        else if (!enforceSelection) {
            selectedItems.remove(entity);
            onItemsSelection();
        }

        updateItemSelection(item);
    }

    private void updateItemSelection(TreeItem item) {
        Object entity = (Object) item.getUserObject();
        boolean isRootItem = item.getParentItem() == null;

        if (entity == null) {
            return;
        }

        // Update selected Items
        boolean selected = false;
        for (Object selectedEntity : selectedItems) {
            if (entity.equals(selectedEntity) && !newSelectedItems.contains(selectedEntity)) {
                selected = true;
                newSelectedItems.add(selectedEntity);
            }
        }

        // Update element's style
        Element element = isRootItem ?
                item.getElement().getElementsByTagName("table").getItem(0) : item.getElement(); //$NON-NLS-1$
        if (!NODE_HEADER.equals(item.getUserObject())) {
            element.getStyle().setBackgroundColor(selected ? "#C3D0E0" : "transparent"); //$NON-NLS-1$ //$NON-NLS-2$
            element.getStyle().setProperty("borderBottom", "1px solid white"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Set item's state
        item.setState(getItemOldState(item.getUserObject()));

        // Recursively update children
        for (int n = 0; n < item.getChildCount(); n++) {
            TreeItem node = item.getChild(n);
            updateItemSelection(node);
        }
    }

    protected Object getEntityId(Object entity) {
        return ((BusinessEntity) entity).getId();
    }

    protected ArrayList<Object> getSelectedEntities() {
        ArrayList<Object> selectedEntities = new ArrayList<Object>();
        for (Object entity : (ArrayList<Object>) listModel.getItems()) {
            if (selectedItems.contains(getEntityId(entity))) {
                selectedEntities.add(entity);
            }
        }
        return selectedEntities;
    }

    protected void onItemsSelection() {
        if (listModel.getItems() == null || (!isRootSelectionEnabled && !isNodeSelectionEnabled)) {
            return;
        }

        ArrayList<Object> selectedEntities = getSelectedEntities();

        listModel.setSelectedItem(selectedEntities.isEmpty() ? null : selectedEntities.get(0));

        if (isMultiSelection) {
            listModel.setSelectedItems(selectedEntities);
        }
    };

    TreeItem findSelectedItem(int clientX, int clientY) {
        return findSelectedTreeItemRecursive(null, clientX, clientY);
    }

    TreeItem findSelectedTreeItemRecursive(TreeItem item, int x, int y) {
        if (null == item) {
            int count = tree.getItemCount();
            for (int i = 0; i < count; i++) {
                TreeItem selected = findSelectedTreeItemRecursive(tree.getItem(i), x, y);
                if (selected != null) {
                    return selected;
                }
            }
            return null;
        }

        int count = item.getChildCount();
        for (int i = 0; i < count; i++) {
            TreeItem selected = findSelectedTreeItemRecursive(item.getChild(i), x, y);
            if (selected != null) {
                return selected;
            }
        }

        if (x >= item.getAbsoluteLeft()
                && x <= item.getAbsoluteLeft() + item.getOffsetWidth()
                && y >= item.getAbsoluteTop()
                && y <= item.getAbsoluteTop() + item.getOffsetHeight()) {
            return item;
        }
        return null;
    }
}
