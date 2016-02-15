package org.ovirt.engine.ui.common.widget;

import com.google.gwt.user.client.ui.HasEnabled;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelLabel;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelDetachableWidget;

public class EntityModelDetachableWidgetWithInfo<T> extends EntityModelWidgetWithInfo implements HasDetachable, HasEnabled {

    public EntityModelDetachableWidgetWithInfo(EntityModelLabel<T> label, AbstractValidatedWidgetWithLabel contentWidget) {
        super(label, new EntityModelDetachableWidget(contentWidget));
    }

    @Override
    public void setDetachableIconVisible(boolean visible) {
        ((HasDetachable) contentWidget).setDetachableIconVisible(visible);
    }

    @Override
    public void setAttached(boolean attached) {
        ((HasDetachable) contentWidget).setAttached(attached);
    }

    @Override
    public void setEnabled(boolean enabled) {
        ((HasEnabled) contentWidget).setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return ((HasEnabled) contentWidget).isEnabled();
    }

    public EntityModelDetachableWidget getContentWidget() {
        return (EntityModelDetachableWidget) contentWidget;
    }

}
