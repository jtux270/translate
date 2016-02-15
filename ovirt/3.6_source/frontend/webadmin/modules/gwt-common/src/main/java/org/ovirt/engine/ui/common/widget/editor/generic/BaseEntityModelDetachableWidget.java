package org.ovirt.engine.ui.common.widget.editor.generic;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.gin.AssetProvider;
import org.ovirt.engine.ui.common.widget.HasDetachable;
import org.ovirt.engine.ui.common.widget.tooltip.WidgetTooltip;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.UIObject;

public abstract class BaseEntityModelDetachableWidget extends Composite implements HasDetachable {

    private final static CommonApplicationResources resources = AssetProvider.getResources();
    private final static CommonApplicationConstants constants = AssetProvider.getConstants();

    @UiField
    WidgetTooltip tooltip;

    Image attachedSeparatedImage;

    interface BaseStyle extends CssResource {
        String contentWidgetWithDetachable();

        String contentWidgetWithoutDetachable();
    }

    private BaseStyle style;

    private UIObject decoratedWidget;

    // can not be as a constructor because needs to be called after the widgets get bind properly in children
    protected void initialize(UIObject decoratedWidget, Image attachedSeparatedImage, BaseStyle style) {
        this.decoratedWidget = decoratedWidget;
        this.attachedSeparatedImage = attachedSeparatedImage;
        this.tooltip.setWidget(attachedSeparatedImage);
        this.tooltip.reconfigure();
        this.style = style;

        setAttached(true);

        // by default it is a regular widget - needs to be made detachable explicitly
        setDetachableIconVisible(false);
    }

    @Override
    public void setDetachableIconVisible(boolean visible) {
        if (visible) {
            attachedSeparatedImage.getElement().getStyle().setDisplay(Display.INLINE);
        } else {
            attachedSeparatedImage.getElement().getStyle().setDisplay(Display.NONE);
        }

        changeWidgetStyle(decoratedWidget, visible);
    }

    protected void changeWidgetStyle(UIObject widget, boolean detachableIconVisible) {
        if (detachableIconVisible) {
            widget.removeStyleName(style.contentWidgetWithoutDetachable());
            widget.addStyleName(style.contentWidgetWithDetachable());
        } else {
            widget.removeStyleName(style.contentWidgetWithDetachable());
            widget.addStyleName(style.contentWidgetWithoutDetachable());
        }
    }

    @Override
    public void setAttached(boolean attached) {
        attachedSeparatedImage.setResource(attached ? resources.joinedIcon() : resources.separatedIcon());

        tooltip.setText(attached ? constants.attachedToInstanceType() : constants.detachedFromInstanceType());
        tooltip.reconfigure();
    }
}
