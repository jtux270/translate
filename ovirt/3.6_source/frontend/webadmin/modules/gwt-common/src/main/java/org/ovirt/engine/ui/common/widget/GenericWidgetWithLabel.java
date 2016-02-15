package org.ovirt.engine.ui.common.widget;

import java.util.Iterator;

import org.ovirt.engine.ui.common.widget.label.EnableableFormLabel;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

public class GenericWidgetWithLabel extends Composite implements HasWidgets, HasEnabled {

    interface WidgetUiBinder extends UiBinder<Widget, GenericWidgetWithLabel> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    public GenericWidgetWithLabel() {
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        setEnabled(true);
    }

    @UiField
    FlowPanel childrenPanel;

    @UiField
    EnableableFormLabel label;

    @Override
    public void add(Widget w) {
        childrenPanel.add(w);
    }

    @Override
    public void clear() {
        childrenPanel.clear();
    }

    @Override
    public Iterator<Widget> iterator() {
        return childrenPanel.iterator();
    }

    @Override
    public boolean remove(Widget w) {
        return childrenPanel.remove(w);
    }

    @Override
    public boolean isEnabled() {
        return label.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        label.setEnabled(enabled);
        applySetEnabledRecursively(enabled, childrenPanel);
    }

    public void setLabel(String labelText) {
        label.setText(labelText);
    }

    private void applySetEnabledRecursively(boolean enabled, HasWidgets panel) {
        for (Widget widget : panel) {
            if (widget instanceof HasEnabled) {
                ((HasEnabled) widget).setEnabled(enabled);
            } else if (widget instanceof HasWidgets) {
                final HasWidgets widgetContainer = (HasWidgets) widget;
                applySetEnabledRecursively(enabled, widgetContainer);
            }
        }
    }
}
