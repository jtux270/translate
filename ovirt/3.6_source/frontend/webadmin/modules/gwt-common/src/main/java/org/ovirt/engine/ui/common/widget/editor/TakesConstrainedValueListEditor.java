package org.ovirt.engine.ui.common.widget.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.TakesValue;
import com.google.gwt.user.client.ui.HasConstrainedValue;

public class TakesConstrainedValueListEditor<T> extends TakesConstrainedValueEditor<List<T>> {

    public static <T> TakesConstrainedValueListEditor<T> ofList(TakesListValue<T> peer,
            HasConstrainedValue<List<T>> peerWithConstraints,
            HasValueChangeHandlers<List<T>> peerWithValueChangeHandlers) {
        return new TakesConstrainedValueListEditor<T>(peer, peerWithConstraints, peerWithValueChangeHandlers);
    }

    TakesValue<List<T>> peer;

    protected TakesConstrainedValueListEditor(TakesValue<List<T>> peer,
            HasConstrainedValue<List<T>> peerWithConstraints,
            HasValueChangeHandlers<List<T>> peerWithValueChangeHandlers) {
        super(peer, peerWithConstraints, peerWithValueChangeHandlers);
        this.peer = peer;
    }

    public void setAcceptableListValues(Collection<T> values) {
        List<List<T>> acceptableItems = createListOfLists(values);
        super.setAcceptableValues(acceptableItems);
    }

    private List<List<T>> createListOfLists(Collection<T> items) {
        List<List<T>> acceptableItems = new ArrayList<>();
        for (T item: items) {
            acceptableItems.add(Arrays.asList(item));
        }
        return acceptableItems;
    }

    public void setListValue(List<T> value) {
        //Make sure to send a copy so we don't accidentally wipe out the original values.
        List<T> listValue = value == null ? Collections.<T>emptyList() : new ArrayList<>(value);
        ((TakesListValue<T>)peer).setListValue(listValue);
    }
}
