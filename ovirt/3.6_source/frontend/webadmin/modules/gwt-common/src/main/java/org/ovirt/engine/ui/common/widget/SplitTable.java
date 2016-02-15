package org.ovirt.engine.ui.common.widget;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import org.ovirt.engine.ui.common.widget.dialog.ShapedButton;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

public abstract class SplitTable<M extends ListModel<T>, T> extends Composite {

    private final MultiSelectionModel<T> excludedSelectionModel;
    private final MultiSelectionModel<T> includedSelectionModel;

    private final IEventListener<EventArgs> excludedItemsChangedListener;
    private final IEventListener<EventArgs> includedItemsChangedListener;

    private UICommand onIncludeButtonPressed;
    private UICommand onExcludeButtonPressed;

    @UiField(provided = true)
    protected ShapedButton includeButton;

    @UiField(provided = true)
    protected ShapedButton excludeButton;

    @UiField(provided = true)
    protected EntityModelCellTable<M> excludedTable;

    @UiField(provided = true)
    protected EntityModelCellTable<M> includedTable;

    @UiField(provided = true)
    protected Label excludedTitle;

    @UiField(provided = true)
    protected Label includedTitle;

    @SuppressWarnings("unchecked")
    public SplitTable(EntityModelCellTable<M> excludedTable,
            EntityModelCellTable<M> includedTable,
            String excludedTitle,
            String includedTitle) {

        this.excludedTable = excludedTable;
        this.includedTable = includedTable;
        this.excludedTitle = new Label(excludedTitle);
        this.includedTitle = new Label(includedTitle);

        includeButton = createIncludeButton();
        excludeButton = createExcludeButton();

        includeButton.setEnabled(false);
        excludeButton.setEnabled(false);

        initWidget();

        excludedSelectionModel = (MultiSelectionModel) excludedTable.getSelectionModel();
        includedSelectionModel = (MultiSelectionModel) includedTable.getSelectionModel();

        excludedItemsChangedListener = new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                excludedSelectionModel.clear();
            }
        };
        includedItemsChangedListener = new IEventListener<EventArgs>() {
            @Override
            public void eventRaised(Event<? extends EventArgs> ev, Object sender, EventArgs args) {
                includedSelectionModel.clear();
            }
        };

        addSelectionHandler(true);
        addSelectionHandler(false);
        addClickHandler(true);
        addClickHandler(false);
    }

    protected abstract ShapedButton createIncludeButton();

    protected abstract ShapedButton createExcludeButton();

    protected abstract void initWidget();

    private void addSelectionHandler(boolean excludedTable) {
        final MultiSelectionModel<T> selectionModel = getSelectionModel(excludedTable);
        final ShapedButton button = getButton(excludedTable);
        selectionModel.addSelectionChangeHandler(new Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                button.setEnabled(!selectionModel.getSelectedSet().isEmpty());
            }
        });
    }

    private void addClickHandler(final boolean excludedTableIsSource) {
        getButton(excludedTableIsSource).addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                MultiSelectionModel<T> sourceSelectionModel = getSelectionModel(excludedTableIsSource);
                UICommand command = excludedTableIsSource ? onIncludeButtonPressed : onExcludeButtonPressed;

                if (command != null) {
                    command.execute();
                }

                Set<T> selectedItems = sourceSelectionModel.getSelectedSet();

                if (excludedTableIsSource) {
                    includeItems(selectedItems, true);
                } else {
                    excludeItems(selectedItems, true);
                }
            }
        });
    }

    /**
     * Allows items to be moved between {@code excludedTable} and {@code includedTable}
     * by double-clicking them. Moving items this way retains selection in both tables
     * (excluding the item that was moved).
     */
    public void enableDoubleClickItemMoving() {
        excludedTable.addSimulatedDoubleClickHandler(new CellPreviewEvent.Handler<EntityModel>() {
            @Override
            public void onCellPreview(CellPreviewEvent<EntityModel> event) {
                T clickedItem = (T) event.getDisplay().getVisibleItem(event.getIndex());
                includeItems(Collections.singletonList(clickedItem), false);
            }
        });
        includedTable.addSimulatedDoubleClickHandler(new CellPreviewEvent.Handler<EntityModel>() {
            @Override
            public void onCellPreview(CellPreviewEvent<EntityModel> event) {
                T clickedItem = (T) event.getDisplay().getVisibleItem(event.getIndex());
                excludeItems(Collections.singletonList(clickedItem), false);
            }
        });
    }

    private void includeItems(Collection<T> itemsToInclude, boolean clearSelectionModels) {
        M excludedTableModel = excludedTable.asEditor().flush();
        M includedTableModel = includedTable.asEditor().flush();

        if (excludedTableModel.getItems() != null && excludedTableModel.getItems().containsAll(itemsToInclude)) {
            excludedTableModel.getItems().removeAll(itemsToInclude);
            getListModelItems(includedTableModel).addAll(itemsToInclude);

            for (T item : itemsToInclude) {
                excludedSelectionModel.setSelected(item, false);
            }

            refresh(clearSelectionModels);
        }
    }

    private void excludeItems(Collection<T> itemsToExclude, boolean clearSelectionModels) {
        M excludedTableModel = excludedTable.asEditor().flush();
        M includedTableModel = includedTable.asEditor().flush();

        if (includedTableModel.getItems() != null && includedTableModel.getItems().containsAll(itemsToExclude)) {
            includedTableModel.getItems().removeAll(itemsToExclude);
            getListModelItems(excludedTableModel).addAll(itemsToExclude);

            for (T item : itemsToExclude) {
                includedSelectionModel.setSelected(item, false);
            }

            refresh(clearSelectionModels);
        }
    }

    private Collection<T> getListModelItems(M model) {
        Collection<T> items = model.getItems();
        if (items == null) {
            items = new LinkedList<>();
            model.setItems(items);
        }
        return items;
    }

    private MultiSelectionModel<T> getSelectionModel(boolean excluded) {
        return excluded ? excludedSelectionModel : includedSelectionModel;
    }

    private ShapedButton getButton(boolean include) {
        return include ? includeButton : excludeButton;
    }

    private EntityModelCellTable<M> getTable(boolean excluded) {
        return excluded ? excludedTable : includedTable;
    }

    private void refresh(boolean clearSelectionModels) {
        if (clearSelectionModels) {
            excludedSelectionModel.clear();
            includedSelectionModel.clear();
        }

        excludedTable.asEditor().edit(excludedTable.asEditor().flush());
        includedTable.asEditor().edit(includedTable.asEditor().flush());
    }

    private void edit(M model, final boolean excludedTableIsEdited) {
        EntityModelCellTable<M> table = getTable(excludedTableIsEdited);
        M oldModel = table.asEditor().flush();
        IEventListener<EventArgs> listener = excludedTableIsEdited ? excludedItemsChangedListener : includedItemsChangedListener;
        if (oldModel != null) {
            oldModel.getItemsChangedEvent().removeListener(listener);
        }
        model.getItemsChangedEvent().addListener(listener);
        table.asEditor().edit(model);
    }

    public void edit(M excludedListModel, M includedListModel) {
        edit(excludedListModel, true);
        edit(includedListModel, false);
    }

    public void edit(M excludedListModel,
            M includedListModel,
            UICommand onIncludeButtonPressed,
            UICommand onExcludeButtonPressed) {
        edit(excludedListModel, includedListModel);
        this.onIncludeButtonPressed = onIncludeButtonPressed;
        this.onExcludeButtonPressed = onExcludeButtonPressed;
    }
}
