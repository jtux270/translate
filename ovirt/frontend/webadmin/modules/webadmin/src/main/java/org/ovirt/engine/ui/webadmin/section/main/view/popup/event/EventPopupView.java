package org.ovirt.engine.ui.webadmin.section.main.view.popup.event;

import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextAreaLabelEditor;
import org.ovirt.engine.ui.common.widget.renderer.FullDateTimeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.events.EventModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.event.EventPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.inject.Inject;

public class EventPopupView extends AbstractModelBoundPopupView<EventModel> implements EventPopupPresenterWidget.ViewDef {

    private final Driver driver;

    interface Driver extends SimpleBeanEditorDriver<EventModel, EventPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, EventPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    protected interface Style extends CssResource {
        String editorContent();
    }

    @UiField
    protected Style style;

    @UiField
    @Ignore
    StringEntityModelLabelEditor eventIdLabel;

    @UiField
    @Ignore
    StringEntityModelLabelEditor eventTimeLabel;

    @UiField
    @Ignore
    StringEntityModelTextAreaLabelEditor eventMessageTextArea;

    @Inject
    public EventPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        localize(constants);
        applyStyles();

        driver = GWT.create(Driver.class);
        driver.initialize(this);
    }

    private void localize(ApplicationConstants constants) {
        eventIdLabel.setLabel(constants.idEvent());
        eventTimeLabel.setLabel(constants.timeEvent());
        eventMessageTextArea.setLabel(constants.messageEvent());
    }

    private void applyStyles() {
        eventIdLabel.addContentWidgetStyleName(style.editorContent());
        eventTimeLabel.addContentWidgetStyleName(style.editorContent());
        eventMessageTextArea.addContentWidgetStyleName(style.editorContent());
    }

    @Override
    public void edit(final EventModel eventModel) {
        driver.edit(eventModel);

        AuditLog event = eventModel.getEvent();
        eventIdLabel.asValueBox().setValue(String.valueOf(event.getlog_typeValue()));
        eventTimeLabel.asValueBox().setValue(new FullDateTimeRenderer().render(event.getlog_time()));
        eventMessageTextArea.asValueBox().setValue(event.getmessage());
    }

    @Override
    public EventModel flush() {
        return driver.flush();
    }
}
