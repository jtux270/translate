package org.ovirt.engine.ui.common.widget.table.header;

import java.util.ArrayList;

import org.ovirt.engine.ui.uicommonweb.models.ListModel;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.view.client.SelectionModel;

public abstract class SelectAllCheckBoxHeader<T> extends Header<Boolean> {

    public SelectAllCheckBoxHeader() {
        super(new CheckboxCell(true, true));
        setUpdater(new ValueUpdater<Boolean>() {
            @Override
            public void update(Boolean value) {
                selectionChanged(value);
            }
        });
    }

    protected abstract void selectionChanged(Boolean value);

    public void handleSelection(Boolean value, ListModel listModel, SelectionModel selectionModel) {
        if (!listModel.getItems().iterator().hasNext()) {
            return;
        }
        ArrayList<T> selectedItems = new ArrayList<T>();
        for (T entity : (Iterable<T>) listModel.getItems()) {
            if (value) {
                selectedItems.add(entity);
            }
            selectionModel.setSelected(entity, value);
        }
        listModel.setSelectedItems(selectedItems);
    }

    public boolean getCheckValue(Iterable<T> items, SelectionModel selectionModel) {
        if (!items.iterator().hasNext()) {
            return false;
        }

        boolean allSelected = true;
        for (T entity : items) {
            if (!selectionModel.isSelected(entity)) {
                return false;
            }
        }
        return true;
    }
}
