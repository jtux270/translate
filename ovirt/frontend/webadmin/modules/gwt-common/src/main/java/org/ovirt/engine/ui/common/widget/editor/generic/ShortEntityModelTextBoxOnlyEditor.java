package org.ovirt.engine.ui.common.widget.editor.generic;

import org.ovirt.engine.ui.common.widget.parser.generic.ToShortEntityModelParser;

public class ShortEntityModelTextBoxOnlyEditor extends EntityModelTextBoxOnlyEditor<Short> {
    public ShortEntityModelTextBoxOnlyEditor() {
        super(new ToStringEntityModelRenderer<Short>(), new ToShortEntityModelParser());
    }
}
