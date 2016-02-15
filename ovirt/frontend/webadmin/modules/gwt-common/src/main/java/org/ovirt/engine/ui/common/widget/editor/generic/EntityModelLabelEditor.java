package org.ovirt.engine.ui.common.widget.editor.generic;

import java.text.ParseException;

import org.ovirt.engine.ui.common.widget.editor.AbstractValueBoxWithLabelEditor;

import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;

public class EntityModelLabelEditor<T> extends AbstractValueBoxWithLabelEditor<T, EntityModelLabel<T>> {

    /**
     * A ValueBoxWithLabelEditor that has a Label as the widget
     */
    public EntityModelLabelEditor(Renderer<T> renderer, Parser<T> parser) {
        this(new EntityModelLabel<T>(renderer, parser));
    }

    /**
     * A ValueBoxWithLabelEditor that should be readonly, so the parser is not needed.
     * @param renderer The renderer.
     */
    public EntityModelLabelEditor(Renderer<T> renderer) {
        this(new EntityModelLabel<T>(renderer, new Parser<T>() {
            @Override
            public T parse(CharSequence text) throws ParseException {
                //Parser is not needed as its a read only field and value is not used.
                return null;
            }
        }));
    }

    public EntityModelLabelEditor(EntityModelLabel<T> widget) {
        super(widget);
    }
}
