package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import java.util.ArrayList;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.InfoIcon;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.uicommonweb.models.gluster.RemoveBrickModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.RemoveBrickPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class RemoveBrickPopupView extends AbstractModelBoundPopupView<RemoveBrickModel> implements RemoveBrickPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<RemoveBrickModel, RemoveBrickPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, RemoveBrickPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<RemoveBrickPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    WidgetStyle style;

    @UiField
    @Ignore
    Label messageLabel;

    @UiField
    VerticalPanel itemsPanel;

    @UiField
    FlowPanel migratePanel;

    @UiField(provided = true)
    @Path("migrateData.entity")
    EntityModelCheckBoxEditor migrateEditor;

    @UiField(provided = true)
    InfoIcon migrateInfoIcon;

    @UiField
    @Ignore
    Label warningLabel;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public RemoveBrickPopupView(EventBus eventBus,
            ApplicationResources resources,
            ApplicationConstants constants,
            ApplicationTemplates templates) {
        super(eventBus, resources);
        initEditors(constants, resources, templates);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        addStyles();
        driver.initialize(this);
    }

    private void initEditors(ApplicationConstants constants,
            ApplicationResources resources,
            ApplicationTemplates templates) {
        migrateEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        migrateInfoIcon = new InfoIcon(templates.italicText(constants.removeBricksMigrateDataInfo()), resources);
    }

    protected void addStyles() {
        migrateEditor.addContentWidgetStyleName(style.migrateOption());
    }

    private void localize(ApplicationConstants constants) {
        warningLabel.setText(constants.removeBricksWarning());
        migrateEditor.setLabel(constants.removeBricksMigrateData());
    }

    @Override
    public void edit(final RemoveBrickModel object) {
        driver.edit(object);

        object.getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                ArrayList<String> items = (ArrayList<String>) object.getItems();

                for (String item : items) {
                    itemsPanel.add(new Label(getItemTextFormatted(item)));
                }
            }
        });

        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                String propName = ((PropertyChangedEventArgs) args).propertyName;

                if ("IsMigrationSupported".equals(propName)) { //$NON-NLS-1$
                    migratePanel.setVisible(object.isMigrationSupported());
                }
            }
        });

        object.getMigrateData().getEntityChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                warningLabel.setVisible(!(Boolean) object.getMigrateData().getEntity());
            }
        });
    }

    private String getItemTextFormatted(String itemText) {
        return "- " + itemText; //$NON-NLS-1$
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    @Override
    public RemoveBrickModel flush() {
        return driver.flush();
    }

    interface WidgetStyle extends CssResource {

        String migrateOption();
    }

}
