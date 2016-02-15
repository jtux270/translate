package org.ovirt.engine.ui.webadmin.widget.host;

import org.ovirt.engine.ui.common.css.OvirtCss;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

public class HostProxySourceWidget extends AbstractModelBoundPopupWidget<EntityModel<String>>
    implements HasValueChangeHandlers<EntityModel<String>>, HasEnabled {

    interface Driver extends SimpleBeanEditorDriver<EntityModel<String>, HostProxySourceWidget> {
    }

    public interface WidgetUiBinder extends UiBinder<Widget, HostProxySourceWidget> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    private final Driver driver = GWT.create(Driver.class);

    @UiField
    PushButton up;

    @UiField
    PushButton down;

    @UiField
    @Path(value = "entity")
    Label proxyLabel;

    @UiField
    @Ignore
    Label orderLabel;

    EntityModel<String> model;

    public HostProxySourceWidget() {
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        driver.initialize(this);
    }

    @Override
    public void edit(EntityModel<String> object) {
        driver.edit(object);
        this.model = object;
    }

    @Override
    public EntityModel<String> flush() {
        return driver.flush();
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<EntityModel<String>> handler) {
        return addHandler(handler, ValueChangeEvent.getType());
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            orderLabel.removeStyleName(OvirtCss.LABEL_DISABLED);
            proxyLabel.removeStyleName(OvirtCss.LABEL_DISABLED);
        } else {
            orderLabel.addStyleName(OvirtCss.LABEL_DISABLED);
            proxyLabel.addStyleName(OvirtCss.LABEL_DISABLED);
        }
    }

    @Override
    public boolean isEnabled() {
        return up.isEnabled();
    }

    public void setOrder(int order) {
        orderLabel.setText(String.valueOf(order));
    }

    public void enableUpButton(boolean enable) {
        up.setEnabled(enable);
    }

    public void enableDownButton(boolean enable) {
        down.setEnabled(enable);
    }

    public void addUpClickHandler(ClickHandler handler) {
        up.addClickHandler(handler);
    }

    public void addDownClickHandler(ClickHandler handler) {
        down.addClickHandler(handler);
    }

    public EntityModel<String> getModel() {
        return model;
    }
}
