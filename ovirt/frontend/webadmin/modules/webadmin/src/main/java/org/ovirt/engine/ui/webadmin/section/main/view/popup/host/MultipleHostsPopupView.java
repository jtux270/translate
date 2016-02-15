package org.ovirt.engine.ui.webadmin.section.main.view.popup.host;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable.SelectionMode;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelPasswordBoxEditor;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.common.widget.table.column.PasswordTextInputCell;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostDetailModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.MultipleHostsModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.host.MultipleHostsPopupPresenterWidget;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class MultipleHostsPopupView extends AbstractModelBoundPopupView<MultipleHostsModel> implements MultipleHostsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<MultipleHostsModel, MultipleHostsPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, MultipleHostsPopupView> {

        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<MultipleHostsPopupView> {

        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel<EntityModel<HostDetailModel>>> hostsTable;

    @UiField(provided = true)
    @Path(value = "useCommonPassword.entity")
    @WithElementId
    EntityModelCheckBoxEditor useCommonPasswordEditor;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCheckBoxEditor configureFirewallEditor;

    @UiField
    @Path(value = "commonPassword.entity")
    @WithElementId
    StringEntityModelPasswordBoxEditor commonPasswordEditor;

    @UiField
    @WithElementId
    UiCommandButton applyPasswordButton;

    @UiField
    @Ignore
    Label messageLabel;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public MultipleHostsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        initTableColumns(constants);
        initButtons();

        driver.initialize(this);
    }

    private void initEditors() {
        hostsTable = new EntityModelCellTable<ListModel<EntityModel<HostDetailModel>>>(SelectionMode.SINGLE, true);
        useCommonPasswordEditor = new EntityModelCheckBoxEditor(Align.RIGHT);
        configureFirewallEditor = new EntityModelCheckBoxEditor(Align.LEFT);
        configureFirewallEditor.setAccessible(true);
    }

    private void initTableColumns(ApplicationConstants constants) {

        Column<EntityModel, String> nameColumn = new Column<EntityModel, String>(new TextInputCell()) {

            @Override
            public String getValue(EntityModel object) {
                return ((HostDetailModel) object.getEntity()).getName();
            }
        };
        hostsTable.addColumn(nameColumn, constants.nameHost(), "50px"); //$NON-NLS-1$

        nameColumn.setFieldUpdater(new FieldUpdater<EntityModel, String>() {

            @Override
            public void update(int index, EntityModel object, String value) {
                ((HostDetailModel) object.getEntity()).setName(value);
            }
        });

        hostsTable.addColumn(new EntityModelTextColumn<HostDetailModel>() {
            @Override
            public String getText(HostDetailModel hostModel) {
                return hostModel.getAddress();
            }
        }, constants.ipHost(), "100px"); //$NON-NLS-1$

        Column<EntityModel, String> passwordColumn = new Column<EntityModel, String>(new PasswordTextInputCell()) {

            @Override
            public String getValue(EntityModel object) {
                return ((HostDetailModel) object.getEntity()).getPassword();
            }
        };
        hostsTable.addColumn(passwordColumn, constants.hostPopupPasswordLabel(), "100px"); //$NON-NLS-1$
        passwordColumn.setFieldUpdater(new FieldUpdater<EntityModel, String>() {

            @Override
            public void update(int index, EntityModel object, String value) {
                ((HostDetailModel) object.getEntity()).setPassword(value);
            }
        });

        hostsTable.addColumn(new EntityModelTextColumn<HostDetailModel>() {
            @Override
            public String getText(HostDetailModel hostModel) {
                return hostModel.getFingerprint();
            }
        }, constants.hostsPopupFingerprint(), "300px"); //$NON-NLS-1$

    }

    private void initButtons() {
        applyPasswordButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                applyPasswordButton.getCommand().execute();
            }
        });
    }

    private void localize(ApplicationConstants constants) {
        useCommonPasswordEditor.setLabel(constants.hostsPopupUseCommonPassword());
        commonPasswordEditor.setLabel(constants.hostsPopupRootPassword());
        applyPasswordButton.setLabel(constants.hostsPopupApply());
        configureFirewallEditor.setLabel(constants.configureFirewallForAllHostsOfThisCluster());
    }

    @Override
    public void edit(final MultipleHostsModel object) {
        hostsTable.asEditor().edit(object.getHosts());
        driver.edit(object);
        applyPasswordButton.setCommand(object.getApplyPasswordCommand());
        configureFirewallEditor.asCheckBox().setChecked(true);
        configureFirewallEditor.asCheckBox().addClickListener(new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                object.setConfigureFirewall(configureFirewallEditor.asCheckBox().isChecked());
            }
        });
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    @Override
    public MultipleHostsModel flush() {
        hostsTable.flush();
        return driver.flush();
    }

}
