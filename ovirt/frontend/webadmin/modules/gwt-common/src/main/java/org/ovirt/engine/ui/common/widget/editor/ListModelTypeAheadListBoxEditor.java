package org.ovirt.engine.ui.common.widget.editor;

import com.google.gwt.core.client.GWT;
import org.ovirt.engine.ui.common.CommonApplicationTemplates;
import org.ovirt.engine.ui.common.widget.AbstractValidatedWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.VisibilityRenderer;

import com.google.gwt.editor.client.IsEditor;

/**
 * Composite Editor that uses {@link ListModelTypeAheadListBox}.
 * @param <T>
 *            SuggestBox item type.
 */
public class ListModelTypeAheadListBoxEditor<T> extends AbstractValidatedWidgetWithLabel<T, ListModelTypeAheadListBox<T>>
        implements IsEditor<WidgetWithLabelEditor<T, ListModelTypeAheadListBoxEditor<T>>> {

    private final WidgetWithLabelEditor<T, ListModelTypeAheadListBoxEditor<T>> editor;

    public ListModelTypeAheadListBoxEditor(SuggestBoxRenderer<T> renderer) {
        this(renderer, true);
    }

    public ListModelTypeAheadListBoxEditor(SuggestBoxRenderer<T> renderer, boolean autoAddToValidValues) {
        this(renderer, autoAddToValidValues, new VisibilityRenderer.SimpleVisibilityRenderer());
    }

    public ListModelTypeAheadListBoxEditor(SuggestBoxRenderer<T> renderer, VisibilityRenderer visibilityRenderer) {
        this(renderer, true, visibilityRenderer);
    }

    public ListModelTypeAheadListBoxEditor(SuggestBoxRenderer<T> renderer,
            boolean autoAddToValidValues,
            VisibilityRenderer visibilityRenderer) {
        super(new ListModelTypeAheadListBox<T>(renderer, autoAddToValidValues), visibilityRenderer);
        this.editor = WidgetWithLabelEditor.of(getContentWidget().asEditor(), this);
    }

    @Override
    public WidgetWithLabelEditor<T, ListModelTypeAheadListBoxEditor<T>> asEditor() {
        return editor;
    }

    /**
     * A renderer for the suggest box. Receives an instance of the EntityModel and returns two kinds of the rendering.
     */
    public static interface SuggestBoxRenderer<T> {
        /**
         * Returns the string that will be shown in the text box (not the suggestions list). Can be only clean string.
         * <p>
         * The following has to be true for each item from the underlying list model:
         * data1 != data2 => getReplacementString(data1) != getReplacementString(data2)
         */
        String getReplacementString(T data);

        /**
         * The string which is displayed as a suggestion. Can be rich - can contain html. There are no invariants - can
         * return anything.
         */
        String getDisplayString(T data);

    }

    @Override
    public ListModelTypeAheadListBox<T> asWidget() {
        return getContentWidget();
    }

    public static abstract class NullSafeSuggestBoxRenderer<T> implements SuggestBoxRenderer<T> {

        private static final CommonApplicationTemplates templates = GWT.create(CommonApplicationTemplates.class);

        @Override
        public String getReplacementString(T data) {
            return emptyOr(data == null ? "" : getReplacementStringNullSafe(data));
        }

        @Override
        public String getDisplayString(T data) {
            return emptyOr(data == null ? templates.typeAheadEmptyContent().asString() : getDisplayStringNullSafe(data));
        }

        private String emptyOr(String string) {
            return string == null ? "" : string;
        }

        public abstract String getReplacementStringNullSafe(T data);

        public abstract String getDisplayStringNullSafe(T data);

    }
}
