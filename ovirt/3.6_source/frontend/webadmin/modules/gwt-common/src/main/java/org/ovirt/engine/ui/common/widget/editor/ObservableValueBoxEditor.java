package org.ovirt.engine.ui.common.widget.editor;

import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ValueBoxBase;

/**
 * A {@link ValueBoxEditor} that adapts to {@link HasValueChangeHandlers} interface.
 * @deprecated use org.ovirt.engine.ui.common.widget.editor.generic.ObservableValueBoxEditor
 */
@Deprecated
public class ObservableValueBoxEditor extends ValueBoxEditor<Object> implements HasValueChangeHandlers<Object> {
    private final ValueBoxBase<Object> peer;

    public ObservableValueBoxEditor(ValueBoxBase<Object> peer) {
        super(peer);
        this.peer = peer;
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        peer.fireEvent(event);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Object> handler) {
        return peer.addValueChangeHandler(handler);
    }

}
