package org.ovirt.engine.ui.webadmin.section.main.view.popup.networkQoS;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.NetworkQoSModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.NetworkQoSPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.inject.Inject;

public class NetworkQoSPopupView extends AbstractModelBoundPopupView<NetworkQoSModel>
        implements NetworkQoSPopupPresenterWidget.ViewDef {

    @UiField(provided = true)
    @Path(value = "dataCenters.selectedItem")
    @WithElementId
    ListModelListBoxEditor<StoragePool> dataCenterEditor;

    @UiField
    @Path(value = "name.entity")
    @WithElementId
    StringEntityModelTextBoxEditor nameEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    NetworkQosWidget qosWidget;

    interface Driver extends SimpleBeanEditorDriver<NetworkQoSModel, NetworkQoSPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, NetworkQoSPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<NetworkQoSPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public NetworkQoSPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors();
        qosWidget = new NetworkQosWidget(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);

        localize(constants);
        driver.initialize(this);
    }

    private void initListBoxEditors() {
        dataCenterEditor = new ListModelListBoxEditor<StoragePool>(new NullSafeRenderer<StoragePool>() {
            @Override
            public String renderNullSafe(StoragePool dataCenter) {
                return dataCenter.getName();
            }
        });
    }

    private void localize(ApplicationConstants constants) {
        nameEditor.setLabel(constants.networkQoSName());
        dataCenterEditor.setLabel(constants.dataCenterNetworkQoSPopup());
    }

    @Override
    public void edit(NetworkQoSModel object) {
        qosWidget.edit(object);
        driver.edit(object);
    }

    @Override
    public NetworkQoSModel flush() {
        qosWidget.flush();
        return driver.flush();
    }

}
