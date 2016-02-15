package org.ovirt.engine.ui.webadmin.section.main.view.popup.gluster;

import java.text.ParseException;
import java.util.Date;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Parser;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeTaskStatusForHost;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.editor.generic.StringEntityModelLabelEditor;
import org.ovirt.engine.ui.common.widget.renderer.GlusterRebalanceDateTimeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.gluster.VolumeRebalanceStatusModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.gluster.VolumeRebalanceStatusPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.widget.table.column.HumanReadableTimeColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.RebalanceFileSizeColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class VolumeRebalanceStatusPopupView extends AbstractModelBoundPopupView<VolumeRebalanceStatusModel> implements VolumeRebalanceStatusPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<VolumeRebalanceStatusModel, VolumeRebalanceStatusPopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, VolumeRebalanceStatusPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    interface ViewIdHandler extends ElementIdHandler<VolumeRebalanceStatusPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @UiField
    @Path("volume.entity")
    @WithElementId
    StringEntityModelLabelEditor volumeEditor;

    @UiField
    @Path("cluster.entity")
    @WithElementId
    StringEntityModelLabelEditor clusterEditor;

    @UiField(provided = true)
    @Path("startTime.entity")
    @WithElementId
    EntityModelLabelEditor<Date> startTimeEditor;

    @UiField(provided = true)
    @Path("statusTime.entity")
    @WithElementId
    EntityModelLabelEditor<Date> statusTimeEditor;

    @UiField
    @Ignore
    @WithElementId
    Label status;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> rebalanceHostsTable;

    @UiField
    @Ignore
    Label messageLabel;

    @UiField(provided = true)
    @Path("stopTime.entity")
    @WithElementId
    EntityModelLabelEditor<Date> stopTimeEditor;

    @UiField
    @Ignore
    @WithElementId
    VerticalPanel stopTimePanel;

    ApplicationMessages messages;

    ApplicationConstants constants;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public VolumeRebalanceStatusPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants, ApplicationMessages messages) {
        super(eventBus, resources);
        this.messages = messages;
        this.constants = constants;
        initEditors(constants);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        localize(constants);
        setVisibilities();
        driver.initialize(this);
    }

    private void setVisibilities() {
        status.setVisible(false);
    }

    private void localize(final ApplicationConstants constants) {
        status.setText(constants.rebalanceComplete());
        startTimeEditor.setLabel(constants.rebalanceStartTime());
        volumeEditor.setLabel(constants.rebalanceVolumeName());
        clusterEditor.setLabel(constants.rebalanceClusterVolume());
        statusTimeEditor.setLabel(constants.rebalanceStatusTime());
        stopTimeEditor.setLabel(constants.rebalanceStopTime());
    }

    void initEditors(ApplicationConstants constants) {
        rebalanceHostsTable = new EntityModelCellTable<ListModel>(false, true);

        statusTimeEditor = getInstanceOfDateEditor();

        startTimeEditor = getInstanceOfDateEditor();

        stopTimeEditor = getInstanceOfDateEditor();

        rebalanceHostsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterVolumeTaskStatusForHost>() {
            @Override
            protected String getText(GlusterVolumeTaskStatusForHost entity) {
                return entity.getHostName();
            }
        }, constants.rebalanceSessionHost());

        rebalanceHostsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterVolumeTaskStatusForHost>() {
            @Override
            protected String getText(GlusterVolumeTaskStatusForHost entity) {
                return entity.getFilesMoved() + "";
            }
        }, getColumnHeaderForFilesMoved());

        rebalanceHostsTable.addEntityModelColumn(new RebalanceFileSizeColumn<EntityModel>(messages) {

            @Override
            protected Long getRawValue(EntityModel object) {
                return ((GlusterVolumeTaskStatusForHost)(object.getEntity())).getTotalSizeMoved();
            }
        }, constants.rebalanceSize());

        rebalanceHostsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterVolumeTaskStatusForHost>() {

            @Override
            protected String getText(GlusterVolumeTaskStatusForHost entity) {
                return String.valueOf(entity.getFilesScanned());
            }
        }, constants.rebalanceScannedFileCount());

        rebalanceHostsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterVolumeTaskStatusForHost>() {
            @Override
            protected String getText(GlusterVolumeTaskStatusForHost entity) {
                return String.valueOf(entity.getFilesFailed());
            }
        }, constants.rebalanceFailedFileCount());

        if (isSkippedFileCountNeeded()){
            rebalanceHostsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterVolumeTaskStatusForHost>() {
                @Override
                protected String getText(GlusterVolumeTaskStatusForHost entity) {
                    return String.valueOf(entity.getFilesSkipped());
                }
            }, constants.rebalanceSkippedFileCount());
        }

        rebalanceHostsTable.addEntityModelColumn(new EntityModelTextColumn<GlusterVolumeTaskStatusForHost>() {
            @Override
            protected String getText(GlusterVolumeTaskStatusForHost entity) {
                return entity.getStatus().toString();
            }
        }, constants.rebalanceStatus());

        rebalanceHostsTable.addEntityModelColumn(new HumanReadableTimeColumn<EntityModel>() {

            @Override
            protected Double getRawValue(EntityModel object) {
                return ((GlusterVolumeTaskStatusForHost)(object.getEntity())).getRunTime();
            }
        }, constants.rebalanceRunTime());
    }

    public boolean isSkippedFileCountNeeded(){
        return true;
    }

    public String getColumnHeaderForFilesMoved() {
        return constants.rebalanceFileCount();
    }

    @Override
    public void edit(final VolumeRebalanceStatusModel object) {
        driver.edit(object);

        rebalanceHostsTable.asEditor().edit(object.getRebalanceSessions());

        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs e = (PropertyChangedEventArgs) args;
                if (e.propertyName.equals("STATUS_UPDATED")) {//$NON-NLS-1$
                    status.setVisible(object.isStatusAvailable());
                }
                else if (e.propertyName.equals("STOP_TIME_UPDATED")) {//$NON-NLS-1$
                    stopTimePanel.setVisible(object.isStopTimeVisible());
                }
            }
        });
    }

    private EntityModelLabelEditor<Date> getInstanceOfDateEditor() {
        return new EntityModelLabelEditor<Date>(new AbstractRenderer<Date>(){
            @Override
            public String render(Date entity) {
                if(entity == null) {
                    return constants.unAvailablePropertyLabel();
                }
                return GlusterRebalanceDateTimeRenderer.getLocalizedDateTimeFormat().format(entity);
            }
        }, new Parser<Date>() {
            @Override
            public Date parse(CharSequence text) throws ParseException {
                if(text == null || text.toString().isEmpty()) {
                    return null;
                }
                else {
                    return new Date(Date.parse(text.toString()));
                }
            }
        });
    }
    @Override
    public VolumeRebalanceStatusModel flush() {
        return driver.flush();
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        messageLabel.setText(message);
    }
}
