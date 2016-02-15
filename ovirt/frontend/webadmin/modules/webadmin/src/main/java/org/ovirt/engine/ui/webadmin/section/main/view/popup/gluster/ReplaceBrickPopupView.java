package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelTextBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.gluster.ReplaceBrickModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.ReplaceBrickPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.inject.Inject;

public class ReplaceBrickPopupView extends AbstractModelBoundPopupView<ReplaceBrickModel> implements ReplaceBrickPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ReplaceBrickModel, ReplaceBrickPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ReplaceBrickPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ReplaceBrickPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField(provided = true)
    @Path(value = "servers.selectedItem")
    @WithElementId
    ListModelListBoxEditor<VDS> serverEditor;

    @UiField
    @Path(value = "brickDirectory.entity")
    @WithElementId
    StringEntityModelTextBoxEditor brickDirEditor;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public ReplaceBrickPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initListBoxEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        driver.initialize(this);
    }

    private void initListBoxEditors() {
        serverEditor = new ListModelListBoxEditor<VDS>(new NullSafeRenderer<VDS>() {
            @Override
            public String renderNullSafe(VDS vds) {
                return vds.getHostName();
            }
        });

    }

    private void localize(ApplicationConstants constants) {
        serverEditor.setLabel(constants.serverBricks());
        brickDirEditor.setLabel(constants.brickDirectoryBricks());
    }

    @Override
    public void edit(ReplaceBrickModel object) {
        driver.edit(object);
    }

    @Override
    public ReplaceBrickModel flush() {
        return driver.flush();
    }

}
