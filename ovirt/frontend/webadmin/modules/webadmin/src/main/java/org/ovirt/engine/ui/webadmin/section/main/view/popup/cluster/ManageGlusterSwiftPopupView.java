package org.ovirt.engine.ui.webadmin.section.main.view.popup.cluster;

import com.google.gwt.text.shared.Parser;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServiceStatus;
import org.ovirt.engine.core.common.businessentities.gluster.ServiceType;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelCheckBoxEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.RadioboxCell;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.GlusterSwiftServiceModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ManageGlusterSwiftModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.cluster.ManageGlusterSwiftPopupPresenterWidget;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import java.text.ParseException;

public class ManageGlusterSwiftPopupView extends AbstractModelBoundPopupView<ManageGlusterSwiftModel> implements ManageGlusterSwiftPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<ManageGlusterSwiftModel, ManageGlusterSwiftPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, ManageGlusterSwiftPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<ManageGlusterSwiftPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final Driver driver = GWT.create(Driver.class);

    @UiField
    WidgetStyle style;

    @UiField(provided = true)
    @Path("swiftStatus.entity")
    EntityModelLabelEditor<GlusterServiceStatus> swiftStatusEditor;

    @UiField(provided = true)
    @Path("startSwift.entity")
    EntityModelRadioButtonEditor startSwift;

    @UiField(provided = true)
    @Path("stopSwift.entity")
    EntityModelRadioButtonEditor stopSwift;

    @UiField(provided = true)
    @Path("restartSwift.entity")
    EntityModelRadioButtonEditor restartSwift;

    @UiField(provided = true)
    @Path("isManageServerLevel.entity")
    EntityModelCheckBoxEditor manageSwiftServerLevel;

    @UiField(provided = true)
    @Ignore
    EntityModelCellTable<ListModel> hostServicesTable;

    @UiField
    @Ignore
    Label messageLabel;

    @Inject
    public ManageGlusterSwiftPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources);
        initEditors(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        applyStyles();
        driver.initialize(this);
    }

    private void initEditors(ApplicationConstants constants) {
        swiftStatusEditor = new EntityModelLabelEditor<GlusterServiceStatus>(new EnumRenderer<GlusterServiceStatus>(), new Parser<GlusterServiceStatus>() {
            @Override
            public GlusterServiceStatus parse(CharSequence text) throws ParseException {
                if (StringHelper.isNullOrEmpty(text.toString())) {
                    return null;
                }
                return GlusterServiceStatus.valueOf(text.toString().toUpperCase());
            }
        });
        startSwift = new EntityModelRadioButtonEditor("swift_action", Align.RIGHT); //$NON-NLS-1$
        stopSwift = new EntityModelRadioButtonEditor("swift_action", Align.RIGHT); //$NON-NLS-1$
        restartSwift = new EntityModelRadioButtonEditor("swift_action", Align.RIGHT); //$NON-NLS-1$
        manageSwiftServerLevel = new EntityModelCheckBoxEditor(Align.RIGHT);

        hostServicesTable = new EntityModelCellTable<ListModel>(false, true);
        hostServicesTable.addEntityModelColumn(new EntityModelTextColumn<GlusterServerService>() {
            @Override
            public String getText(GlusterServerService entity) {
                return entity.getHostName();
            }
        }, constants.hostGlusterSwift());

        hostServicesTable.addColumn(new EnumColumn<EntityModel, ServiceType>() {
            @Override
            protected ServiceType getRawValue(EntityModel object) {
                return ((GlusterSwiftServiceModel) object).getEntity().getServiceType();
            }
        }, constants.serviceNameGlusterSwift());

        hostServicesTable.addColumn(new EnumColumn<EntityModel, GlusterServiceStatus>() {
            @Override
            protected GlusterServiceStatus getRawValue(EntityModel object) {
                return ((GlusterSwiftServiceModel) object).getEntity().getStatus();
            }
        }, constants.serviceStatusGlusterSwift());

        Column<EntityModel, Boolean> startSwiftColumn =
                new Column<EntityModel, Boolean>(new RadioboxCell(false, true)) {
                    @Override
                    public Boolean getValue(EntityModel object) {
                        GlusterSwiftServiceModel swiftServiceModel = (GlusterSwiftServiceModel) object;
                        return swiftServiceModel.getStartSwift().getEntity();
                    }

                    @Override
                    public void render(Context context, EntityModel object, SafeHtmlBuilder sb) {
                        GlusterSwiftServiceModel swiftServiceModel = (GlusterSwiftServiceModel) object;
                        if (swiftServiceModel.getStartSwift().getIsChangable()) {
                            super.render(context, object, sb);
                        }
                    }
                };
        startSwiftColumn.setFieldUpdater(new FieldUpdater<EntityModel, Boolean>() {
            @Override
            public void update(int index, EntityModel object, Boolean value) {
                GlusterSwiftServiceModel swiftModel = (GlusterSwiftServiceModel) object;
                swiftModel.getStartSwift().setEntity(value);
                if (value) {
                    swiftModel.getStopSwift().setEntity(false);
                    swiftModel.getRestartSwift().setEntity(false);
                    hostServicesTable.redraw();
                }
            }
        });
        hostServicesTable.addEntityModelColumn(startSwiftColumn, constants.startGlusterSwift());

        Column<EntityModel, Boolean> stopSwiftColumn = new Column<EntityModel, Boolean>(new RadioboxCell(false, true)) {
            @Override
            public Boolean getValue(EntityModel object) {
                GlusterSwiftServiceModel swiftServiceModel = (GlusterSwiftServiceModel) object;
                return swiftServiceModel.getStopSwift().getEntity();
            }

            @Override
            public void render(Context context, EntityModel object, SafeHtmlBuilder sb) {
                GlusterSwiftServiceModel swiftServiceModel = (GlusterSwiftServiceModel) object;
                if (swiftServiceModel.getStopSwift().getIsChangable()) {
                    super.render(context, object, sb);
                }
            }
        };
        stopSwiftColumn.setFieldUpdater(new FieldUpdater<EntityModel, Boolean>() {
            @Override
            public void update(int index, EntityModel object, Boolean value) {
                GlusterSwiftServiceModel swiftModel = (GlusterSwiftServiceModel) object;
                if (swiftModel.getStopSwift().getIsChangable()) {
                    swiftModel.getStopSwift().setEntity(value);
                    if (value) {
                        swiftModel.getStartSwift().setEntity(false);
                        swiftModel.getRestartSwift().setEntity(false);
                        hostServicesTable.redraw();
                    }
                }
            }
        });
        hostServicesTable.addEntityModelColumn(stopSwiftColumn, constants.stopGlusterSwift());

        Column<EntityModel, Boolean> restartSwiftColumn =
                new Column<EntityModel, Boolean>(new RadioboxCell(false, true)) {
                    @Override
                    public Boolean getValue(EntityModel object) {
                        GlusterSwiftServiceModel swiftServiceModel = (GlusterSwiftServiceModel) object;
                        return swiftServiceModel.getRestartSwift().getEntity();
                    }

                    @Override
                    public void render(Context context, EntityModel object, SafeHtmlBuilder sb) {
                        GlusterSwiftServiceModel swiftServiceModel = (GlusterSwiftServiceModel) object;
                        if (swiftServiceModel.getRestartSwift().getIsChangable()) {
                            super.render(context, object, sb);
                        }
                    }
                };
        restartSwiftColumn.setFieldUpdater(new FieldUpdater<EntityModel, Boolean>() {
            @Override
            public void update(int index, EntityModel object, Boolean value) {
                GlusterSwiftServiceModel swiftModel = (GlusterSwiftServiceModel) object;
                swiftModel.getRestartSwift().setEntity(value);
                if (value) {
                    swiftModel.getStartSwift().setEntity(false);
                    swiftModel.getStopSwift().setEntity(false);
                    hostServicesTable.redraw();
                }
            }
        });
        hostServicesTable.addEntityModelColumn(restartSwiftColumn, constants.restartGlusterSwift());
    }

    private void localize(ApplicationConstants constants) {
        swiftStatusEditor.setLabel(constants.clusterGlusterSwiftLabel());
        startSwift.setLabel(constants.startGlusterSwift());
        stopSwift.setLabel(constants.stopGlusterSwift());
        restartSwift.setLabel(constants.restartGlusterSwift());
        manageSwiftServerLevel.setLabel(constants.manageServerLevelGlusterSwift());
    }

    private void applyStyles() {
        swiftStatusEditor.addContentWidgetStyleName(style.swiftStatusWidget());
    }

    @Override
    public void edit(ManageGlusterSwiftModel object) {
        driver.edit(object);
        hostServicesTable.asEditor().edit(object.getHostServicesList());
    }

    @Override
    public ManageGlusterSwiftModel flush() {
        hostServicesTable.flush();
        return driver.flush();
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }

    interface WidgetStyle extends CssResource {

        String swiftStatusWidget();
    }
}
