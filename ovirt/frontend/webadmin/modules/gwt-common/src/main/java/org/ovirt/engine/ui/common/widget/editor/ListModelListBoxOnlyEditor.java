package org.ovirt.engine.ui.common.widget.editor;

import org.ovirt.engine.ui.common.widget.AbstractValidatedWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.VisibilityRenderer;
import org.ovirt.engine.ui.common.widget.renderer.StringRenderer;

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

/*
 * Represents a Model bound editor containing only a ListBox with no label.
 */
public class ListModelListBoxOnlyEditor<T> extends AbstractValidatedWidgetWithLabel<T, ListModelListBox<T>>
    implements IsEditor<WidgetWithLabelEditor<T, ListModelListBoxOnlyEditor<T>>> {

    private final WidgetWithLabelEditor<T, ListModelListBoxOnlyEditor<T>> editor;

    public ListModelListBoxOnlyEditor() {
        this(new StringRenderer<T>());
    }

    public ListModelListBoxOnlyEditor(Renderer<T> renderer) {
        this(renderer, new VisibilityRenderer.SimpleVisibilityRenderer());
    }

    public ListModelListBoxOnlyEditor(Renderer<T> renderer, VisibilityRenderer visibilityRenderer) {
        super(new ListModelListBox<T>(renderer), visibilityRenderer);
        this.editor = WidgetWithLabelEditor.of(getContentWidget().asEditor(), this);
    }

    public ListBox asListBox() {
        return getContentWidget().asListBox();
    }

    @Override
    public WidgetWithLabelEditor<T, ListModelListBoxOnlyEditor<T>> asEditor() {
        return editor;
    }

    @Override
    protected void initWidget(Widget wrapperWidget) {
        super.initWidget(wrapperWidget);

        // Fix styles.
        com.google.gwt.dom.client.Style labelStyle = getLabelElement().getStyle();
        labelStyle.setDisplay(com.google.gwt.dom.client.Style.Display.NONE);

        //        Style textBoxStyle = getContentWidgetElement().getStyle();
        //        textBoxStyle.setWidth(240, Style.Unit.PX);

        com.google.gwt.dom.client.Style panelStyle = getContentWidgetContainer().getElement().getStyle();
        panelStyle.setFloat(com.google.gwt.dom.client.Style.Float.NONE);
    }
}
