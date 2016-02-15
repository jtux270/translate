package org.ovirt.engine.ui.webadmin.widget.provider;

import org.ovirt.engine.core.common.businessentities.network.ExternalSubnet.IpVersion;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractModelBoundPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.providers.ExternalSubnetModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.inject.Inject;

public class ExternalSubnetWidget extends AbstractModelBoundPopupWidget<ExternalSubnetModel> {

    interface Driver extends SimpleBeanEditorDriver<ExternalSubnetModel, ExternalSubnetWidget> {
    }

    interface ViewUiBinder extends UiBinder<FlowPanel, ExternalSubnetWidget> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ExternalSubnetWidget> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private static ApplicationConstants constants = GWT.create(ApplicationConstants.class);

    @UiField
    @Path("name.entity")
    @WithElementId("name")
    StringEntityModelTextBoxEditor nameEditor;

    @UiField
    @Path("cidr.entity")
    @WithElementId("cidr")
    StringEntityModelTextBoxEditor cidrEditor;

    @UiField(provided = true)
    @Path("ipVersion.selectedItem")
    @WithElementId("ipVersion")
    ListModelListBoxEditor<IpVersion> ipVersionEditor;

    @UiField
    @Path("gateway.entity")
    @WithElementId("gateway")
    StringEntityModelTextBoxEditor gatewayEditor;

    @UiField
    @Ignore
    public DnsServersWidget dnsServersEditor;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public ExternalSubnetWidget() {
        ipVersionEditor = new ListModelListBoxEditor<IpVersion>(new EnumRenderer<IpVersion>());
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(constants);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        driver.initialize(this);
    }

    private void localize(ApplicationConstants constants) {
        nameEditor.setLabel(constants.nameExternalSubnet());
        cidrEditor.setLabel(constants.cidrExternalSubnet());
        ipVersionEditor.setLabel(constants.ipVersionExternalSubnet());
        gatewayEditor.setLabel(constants.gatewayExternalSubnet());
    }

    @Override
    public void focusInput() {
        nameEditor.setFocus(true);
    }

    @Override
    public void edit(final ExternalSubnetModel subnet) {
        driver.edit(subnet);
        dnsServersEditor.edit(subnet.getDnsServers());
    }

    @Override
    public ExternalSubnetModel flush() {
        dnsServersEditor.flush();
        return driver.flush();
    }
}
