package org.ovirt.engine.ui.common.widget.editor;

import org.ovirt.engine.ui.common.widget.AbstractValidatedWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.VisibilityRenderer;

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;

/**
 * Base class for composite Editors that use text input widget with a label.
 *
 * @param <T>
 *            The type being edited.
 * @param <W>
 *            Text input widget type.
 */
public abstract class AbstractValueBoxWithLabelEditor<T, W extends EditorWidget<T, ValueBoxEditor<T>>> extends AbstractValidatedWidgetWithLabel<T, W>
        implements IsEditor<WidgetWithLabelEditor<T, AbstractValueBoxWithLabelEditor<T, W>>> {

    private final WidgetWithLabelEditor<T, AbstractValueBoxWithLabelEditor<T, W>> editor;

    public AbstractValueBoxWithLabelEditor(W contentWidget, VisibilityRenderer visibilityRenderer) {
        super(contentWidget, visibilityRenderer);
        this.editor = WidgetWithLabelEditor.of(contentWidget.asEditor(), this);
    }

    public AbstractValueBoxWithLabelEditor(W contentWidget) {
        this(contentWidget, new VisibilityRenderer.SimpleVisibilityRenderer());
    }

    public W asValueBox() {
        return getContentWidget();
    }

    @Override
    public WidgetWithLabelEditor<T, AbstractValueBoxWithLabelEditor<T, W>> asEditor() {
        return editor;
    }

    public void setAutoComplete(String value) {
        asValueBox().asWidget().getElement().setAttribute("autocomplete", "off"); //$NON-NLS-1$ //$NON-NLS-2$
    }

}
