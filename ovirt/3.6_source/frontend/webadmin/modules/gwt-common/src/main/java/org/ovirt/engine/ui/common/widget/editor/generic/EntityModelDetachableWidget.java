package org.ovirt.engine.ui.common.widget.editor.generic;

import static com.google.gwt.dom.client.Style.Unit;

import java.util.List;

import org.ovirt.engine.ui.common.widget.AbstractValidatedWidgetWithLabel;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.HasValidation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Takes a AbstractValidatedWidgetWithLabel, decorates it with the detachable icon but does not render it's label
 */
public class EntityModelDetachableWidget extends BaseEntityModelDetachableWidget implements HasEnabled, HasValidation {

    interface WidgetUiBinder extends UiBinder<Widget, EntityModelDetachableWidget> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    interface Style extends BaseStyle {
        String contentWrapperImageOnRight();
        String contentWrapper();
    }

    @UiField
    Image attachedSeparatedImage;

    @UiField(provided = true)
    SimplePanel contentWidgetContainer;

    private AbstractValidatedWidgetWithLabel decorated;

    @UiField
    FlowPanel contentWrapper;

    @UiField
    Style style;

    public EntityModelDetachableWidget(AbstractValidatedWidgetWithLabel decorated, Align attachedImageAlign) {
        this.decorated = decorated;
        this.contentWidgetContainer = decorated.getContentWidgetContainer();

        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));

        setupAlign(attachedImageAlign);

        initialize(contentWidgetContainer, attachedSeparatedImage, style);
    }

    public void setupAlign(Align attachedImageAlign) {
        if (attachedImageAlign == Align.IGNORE) {
            return;
        }

        setupContentWidgetContainerLayout(attachedImageAlign);
        setupContentWrapper(attachedImageAlign);
    }

    public void setupContentWidgetContainerLayout(Align attachedImageAlign) {
        if (attachedImageAlign == Align.LEFT) {
            contentWidgetContainer.getElement().getStyle().setFloat(Float.RIGHT);
        } else if (attachedImageAlign == Align.RIGHT) {
            contentWidgetContainer.getElement().getStyle().setFloat(Float.LEFT);
            contentWidgetContainer.getElement().getStyle().setWidth(AbstractValidatedWidgetWithLabel.CONTENT_WIDTH_LEGACY, Unit.PX);
        }
    }

    public void setupContentWrapper(Align attachedImageAlign) {
        if (attachedImageAlign == Align.RIGHT) {
            contentWrapper.removeStyleName(style.contentWrapper());
            contentWrapper.addStyleName(style.contentWrapperImageOnRight());
        }
    }

    public EntityModelDetachableWidget(AbstractValidatedWidgetWithLabel decorated) {
        this(decorated, Align.LEFT);
    }

    @Override
    public boolean isEnabled() {
        return decorated.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        decorated.setEnabled(enabled);
    }

    public void setContentWidgetContainerStyle(String style) {
        contentWidgetContainer.addStyleName(style);
    }

    public void setContentWrapperStypeName(String styleName) {
        contentWrapper.addStyleName(styleName);
    }

    @Override
    public void markAsValid() {
        decorated.markAsValid();
    }

    @Override
    public void markAsInvalid(List<String> validationHints) {
        decorated.markAsInvalid(validationHints);
    }

    @Override
    public boolean isValid() {
        return decorated.isValid();
    }
}
