package org.ovirt.engine.ui.webadmin.widget.provider;

import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties.BrokerType;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.EntityModelWidgetWithInfo;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabel;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxOnlyEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.providers.NeutronAgentModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;

public class NeutronAgentWidget extends AbstractModelBoundPopupWidget<NeutronAgentModel> {

    interface Driver extends SimpleBeanEditorDriver<NeutronAgentModel, NeutronAgentWidget> {
    }

    private final Driver driver = GWT.create(Driver.class);

    interface ViewUiBinder extends UiBinder<FlowPanel, NeutronAgentWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<NeutronAgentWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private static ApplicationConstants constants = GWT.create(ApplicationConstants.class);
    private static ApplicationTemplates templates = GWT.create(ApplicationTemplates.class);

    @UiField(provided = true)
    EntityModelWidgetWithInfo<String> mappings;

    @Path(value = "interfaceMappingsLabel.entity")
    @WithElementId("interfaceMappingsLabel")
    StringEntityModelLabel mappingsLabel;

    @Path(value = "interfaceMappings.entity")
    @WithElementId("interfaceMappings")
    StringEntityModelTextBoxOnlyEditor interfaceMappings;

    @UiField(provided = true)
    @Path("brokerType.selectedItem")
    @WithElementId("brokerType")
    ListModelListBoxEditor<BrokerType> brokerTypeEditor;

    @UiField
    @Path(value = "messagingServer.entity")
    @WithElementId("messagingServer")
    StringEntityModelTextBoxEditor messagingServer;

    @UiField
    @Path(value = "messagingServerPort.entity")
    @WithElementId("messagingServerPort")
    StringEntityModelTextBoxEditor messagingServerPort;

    @UiField
    @Path(value = "messagingServerUsername.entity")
    @WithElementId("messagingServerUsername")
    StringEntityModelTextBoxEditor messagingServerUsername;

    @UiField
    @Path(value = "messagingServerPassword.entity")
    @WithElementId("messagingServerPassword")
    StringEntityModelPasswordBoxEditor messagingServerPassword;

    @Inject
    public NeutronAgentWidget() {
        brokerTypeEditor = new ListModelListBoxEditor<BrokerType>(new EnumRenderer<BrokerType>());
        mappingsLabel = new StringEntityModelLabel();
        interfaceMappings = new StringEntityModelTextBoxOnlyEditor();
        mappings = new EntityModelWidgetWithInfo<String>(mappingsLabel, interfaceMappings);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);

        brokerTypeEditor.setLabel(constants.messagingBrokerType());
        messagingServer.setLabel(constants.messagingServer());
        messagingServerPort.setLabel(constants.messagingServerPort());
        messagingServerUsername.setLabel(constants.messagingServerUsername());
        messagingServerPassword.setLabel(constants.messagingServerPassword());

        driver.initialize(this);
    }

    @Override
    public void edit(final NeutronAgentModel model) {
        driver.edit(model);
        mappings.setExplanation(templates.italicText(model.getInterfaceMappingsExplanation().getEntity()));
        model.getInterfaceMappingsExplanation().getEntityChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                mappings.setExplanation(templates.italicText(model.getInterfaceMappingsExplanation()
                        .getEntity()));
            }
        });
    }

    @Override
    public NeutronAgentModel flush() {
        return driver.flush();
    }

}
