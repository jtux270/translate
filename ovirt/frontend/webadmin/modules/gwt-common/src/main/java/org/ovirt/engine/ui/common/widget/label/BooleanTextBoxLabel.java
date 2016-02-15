package org.ovirt.engine.ui.common.widget.label;

import org.ovirt.engine.ui.common.widget.renderer.BooleanRenderer;

public class BooleanTextBoxLabel extends TextBoxLabelBase<Boolean> {

    public BooleanTextBoxLabel() {
        super(new BooleanRenderer());
    }

    public BooleanTextBoxLabel(String trueText, String falseText) {
        super(new BooleanRenderer(trueText, falseText));
    }
}
