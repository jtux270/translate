package org.ovirt.engine.ui.common.widget.editor.generic;

import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;
import org.ovirt.engine.ui.common.widget.VisibilityRenderer;
import org.ovirt.engine.ui.common.widget.editor.AbstractValueBoxWithLabelEditor;

/**
 * Composite Editor that uses {@link EntityModelTextBox}.
 */
public class EntityModelTextBoxEditor<T> extends AbstractValueBoxWithLabelEditor<T, EntityModelTextBox<T>> {

    public EntityModelTextBoxEditor(EntityModelTextBox<T> contentWidget, VisibilityRenderer visibilityRenderer) {
        super(contentWidget, visibilityRenderer);
    }

    public EntityModelTextBoxEditor(Renderer<T> renderer, Parser<T> parser) {
        super(new EntityModelTextBox<T>(renderer, parser));
    }

    public EntityModelTextBoxEditor(Renderer<T> renderer, Parser<T> parser, VisibilityRenderer visibilityRenderer) {
        super(new EntityModelTextBox<T>(renderer, parser), visibilityRenderer);
    }
}
