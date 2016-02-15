package org.ovirt.engine.ui.webadmin.widget.footer;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.core.common.job.Job;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTabModelProvider;
import org.ovirt.engine.ui.common.widget.action.CommandLocation;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.AuditLogSeverityColumn;
import org.ovirt.engine.ui.common.widget.table.column.FullDateTimeColumn;
import org.ovirt.engine.ui.common.widget.table.column.ImageButtonCell;
import org.ovirt.engine.ui.common.widget.table.column.ImageResourceColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.AlertFirstRowModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.AlertModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.AlertModelProvider.AlertCountChangeHandler;
import org.ovirt.engine.ui.webadmin.uicommon.model.EventFirstRowModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.EventModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.TaskFirstRowModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.TaskModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.TaskModelProvider.TaskHandler;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.TaskStatusColumn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

public class AlertsEventsFooterView extends Composite implements AlertCountChangeHandler, TaskHandler {

    interface WidgetUiBinder extends UiBinder<Widget, AlertsEventsFooterView> {
        WidgetUiBinder uiBinder = GWT.create(WidgetUiBinder.class);
    }

    @UiField
    Style style;

    @UiField
    SimplePanel tablePanel;

    @UiField
    SimplePanel widgetPanel;

    @UiField
    SimplePanel firstRowTablePanel;

    @UiField
    HorizontalPanel widgetInnerPanel;

    @UiField
    ToggleButton alertButton;

    @UiField
    ToggleButton eventButton;

    @UiField
    ToggleButton taskButton;

    @UiField
    PushButton expandButton;

    @UiField
    PushButton collapseButton;

    @UiField
    Label message;

    SimpleActionTable<AuditLog> alertsTable;
    SimpleActionTable<AuditLog> eventsTable;
    TasksTree tasksTree;
    SimpleActionTable<AuditLog> _alertsTable;
    SimpleActionTable<AuditLog> _eventsTable;
    SimpleActionTable<Job> _tasksTable;

    String buttonUpStart;
    String buttonUpStretch;
    String buttonUpEnd;
    String buttonDownStart;
    String buttonDownStretch;
    String buttonDownEnd;

    private final ApplicationTemplates templates;
    private final ApplicationResources resources;
    private final ApplicationConstants constants;
    private final SafeHtml alertImage;

    private TaskModelProvider taskModelProvider;

    public AlertsEventsFooterView(AlertModelProvider alertModelProvider,
            AlertFirstRowModelProvider alertFirstRowModelProvider,
            EventModelProvider eventModelProvider,
            EventFirstRowModelProvider eventFirstRowModelProvider,
            TaskModelProvider taskModelProvider,
            TaskFirstRowModelProvider taskFirstRowModelProvider,
            ApplicationResources resources,
            ApplicationTemplates templates,
            EventBus eventBus,
            ClientStorage clientStorage,
            ApplicationConstants constants) {
        this.resources = resources;
        this.templates = templates;
        this.constants = constants;
        this.taskModelProvider = taskModelProvider;
        initWidget(WidgetUiBinder.uiBinder.createAndBindUi(this));
        initButtonHandlers();
        alertModelProvider.setAlertCountChangeHandler(this);
        taskModelProvider.setTaskHandler(this);

        alertsTable = createActionTable(alertModelProvider);
        alertsTable.setBarStyle(style.barStyle());
        initAlertTable(alertsTable, alertModelProvider);

        _alertsTable = createActionTable(alertFirstRowModelProvider);
        _alertsTable.setBarStyle(style.barStyle());
        _alertsTable.getElement().getStyle().setOverflowY(Overflow.HIDDEN);
        initTable(_alertsTable);

        eventsTable = createActionTable(eventModelProvider);
        eventsTable.setBarStyle(style.barStyle());
        initTable(eventsTable);

        _eventsTable = createActionTable(eventFirstRowModelProvider);
        _eventsTable.setBarStyle(style.barStyle());
        _eventsTable.getElement().getStyle().setOverflowY(Overflow.HIDDEN);
        initTable(_eventsTable);

        tasksTree = new TasksTree(resources, constants, templates);
        tasksTree.updateTree(taskModelProvider.getModel());

        _tasksTable =
                new SimpleActionTable<Job>(taskFirstRowModelProvider, getTableResources(), eventBus, clientStorage);
        _tasksTable.setBarStyle(style.barStyle());
        _tasksTable.getElement().getStyle().setOverflowY(Overflow.HIDDEN);
        initTaskTable(_tasksTable);

        taskButton.setValue(false);
        alertButton.setValue(false);
        eventButton.setValue(true);
        message.setText(constants.lastMsgEventFooter());
        collapseButton.setVisible(false);

        tablePanel.clear();
        firstRowTablePanel.clear();
        tablePanel.add(eventsTable);
        firstRowTablePanel.add(_eventsTable);
        widgetInnerPanel.setCellWidth(firstRowTablePanel, "100%"); //$NON-NLS-1$

        String image = AbstractImagePrototype.create(resources.alertConfigureImage()).getHTML();
        alertImage = SafeHtmlUtils.fromTrustedString(image);

        // no body is invoking the alert search (timer)
        alertModelProvider.getModel().search();

        // no body is invoking the alert search (timer)
        taskModelProvider.getModel().search();

        updateButtonResources();
        updateEventsButton();
        updateTaskButton(0);
        setAlertCount(0);
    }

    SimpleActionTable<AuditLog> createActionTable(SearchableTabModelProvider<AuditLog, ?> modelProvider) {
        return new SimpleActionTable<AuditLog>(modelProvider, getTableResources(),
                ClientGinjectorProvider.getEventBus(),
                ClientGinjectorProvider.getClientStorage());
    }

    AlertsEventsFooterResources getTableResources() {
        return GWT.<AlertsEventsFooterResources> create(AlertsEventsFooterResources.class);
    }

    @Override
    public void onAlertCountChange(int count) {
        setAlertCount(count);
    }

    @Override
    public void onRunningTasksCountChange(int count) {
        updateTaskButton(count);
    }

    void setAlertCount(int count) {
        if (alertImage == null) {
            return;
        }

        String countStr = constants.alertsEventFooter() + " (" + count + ")"; //$NON-NLS-1$  //$NON-NLS-2$

        SafeHtml up = templates.alertEventButton(alertImage, countStr,
                buttonUpStart, buttonUpStretch, buttonUpEnd, style.alertButtonUpStyle());
        SafeHtml down = templates.alertEventButton(alertImage, countStr,
                buttonDownStart, buttonDownStretch, buttonDownEnd, style.alertButtonDownStyle());

        alertButton.getUpFace().setHTML(up);
        alertButton.getDownFace().setHTML(down);
    }

    private void updateTaskButton(int count) {
        String tasksGrayImageSrc = AbstractImagePrototype.create(resources.iconTask()).getHTML();
        SafeHtml tasksGrayImage = SafeHtmlUtils.fromTrustedString(tasksGrayImageSrc);

        SafeHtml up = templates.alertEventButton(tasksGrayImage, constants.tasksEventFooter() + " (" + count + ")",  //$NON-NLS-1$  //$NON-NLS-2$
                buttonUpStart, buttonUpStretch, buttonUpEnd, style.taskButtonUpStyle());
        SafeHtml down = templates.alertEventButton(tasksGrayImage, constants.tasksEventFooter() + " (" + count + ")",  //$NON-NLS-1$  //$NON-NLS-2$
                buttonDownStart, buttonDownStretch, buttonDownEnd, style.taskButtonDownStyle());

        taskButton.getUpFace().setHTML(up);
        taskButton.getDownFace().setHTML(down);
    }

    void updateEventsButton() {
        String eventsGrayImageSrc = AbstractImagePrototype.create(resources.eventsGrayImage()).getHTML();
        SafeHtml eventsGrayImage = SafeHtmlUtils.fromTrustedString(eventsGrayImageSrc);

        SafeHtml up = templates.alertEventButton(eventsGrayImage, constants.eventsEventFooter(),
                buttonUpStart, buttonUpStretch, buttonUpEnd, style.eventButtonUpStyle());
        SafeHtml down = templates.alertEventButton(eventsGrayImage, constants.eventsEventFooter(),
                buttonDownStart, buttonDownStretch, buttonDownEnd, style.eventButtonDownStyle());

        eventButton.getUpFace().setHTML(up);
        eventButton.getDownFace().setHTML(down);
    }

    void updateButtonResources() {
        buttonUpStart = resources.footerButtonUpStart().getURL();
        buttonUpStretch = resources.footerButtonUpStretch().getURL();
        buttonUpEnd = resources.footerButtonUpEnd().getURL();
        buttonDownStart = resources.footerButtonDownStart().getURL();
        buttonDownStretch = resources.footerButtonDownStretch().getURL();
        buttonDownEnd = resources.footerButtonDownEnd().getURL();
    }

    void initButtonHandlers() {
        alertButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (alertButton.getValue()) {
                    eventButton.setValue(false);
                    taskButton.setValue(false);
                    tablePanel.clear();
                    tablePanel.add(alertsTable);

                    firstRowTablePanel.clear();
                    firstRowTablePanel.add(_alertsTable);

                    message.setText(constants.lastMsgEventFooter());
                    collapseButton.setVisible(false);
                }
                else {
                    alertButton.setValue(true);
                }
            }
        });

        eventButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (eventButton.getValue()) {
                    alertButton.setValue(false);
                    taskButton.setValue(false);
                    tablePanel.clear();
                    tablePanel.add(eventsTable);

                    firstRowTablePanel.clear();
                    firstRowTablePanel.add(_eventsTable);

                    message.setText(constants.lastMsgEventFooter());
                    collapseButton.setVisible(false);
                }
                else {
                    eventButton.setValue(true);
                }
            }
        });

        taskButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (taskButton.getValue()) {
                    alertButton.setValue(false);
                    eventButton.setValue(false);
                    tablePanel.clear();
                    tablePanel.add(tasksTree);

                    firstRowTablePanel.clear();
                    firstRowTablePanel.add(_tasksTable);

                    message.setText(constants.lastTaskEventFooter());
                    collapseButton.setVisible(true);
                }
                else {
                    taskButton.setValue(true);
                }
            }
        });

        expandButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                String height = widgetPanel.getElement().getParentElement().getParentElement().getStyle().getHeight();
                int offset = 26;
                if (height.equals("26px")) { //$NON-NLS-1$
                    offset = 162;
                }
                widgetPanel.getElement().getParentElement().getParentElement().getStyle().setHeight(offset, Unit.PX);
                widgetPanel.getElement().getParentElement().getParentElement().getStyle().setBottom(0, Unit.PX);
                Element e =
                        (Element) widgetPanel.getElement()
                                .getParentElement()
                                .getParentElement()
                                .getParentElement()
                                .getChild(2);
                e.getStyle().setBottom(offset, Unit.PX);
                e =
                        (Element) widgetPanel.getElement()
                                .getParentElement()
                                .getParentElement()
                                .getParentElement()
                                .getChild(3);
                e.getStyle().setBottom(offset + 4, Unit.PX);
            }
        });

        collapseButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                tasksTree.collapseAllTasks();
            }
        });
    }

    void initTable(SimpleActionTable<AuditLog> table) {
        table.addColumn(new AuditLogSeverityColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<AuditLog> logTimeColumn = new FullDateTimeColumn<AuditLog>() {
            @Override
            protected Date getRawValue(AuditLog object) {
                return object.getlog_time();
            }
        };
        table.addColumn(logTimeColumn, constants.timeEvent(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<AuditLog> messageColumn = new TextColumnWithTooltip<AuditLog>() {
            @Override
            public String getValue(AuditLog object) {
                return object.getmessage();
            }
        };
        table.addColumn(messageColumn, constants.messageEvent());
    }

    void initAlertTable(final SimpleActionTable<AuditLog> table, final AlertModelProvider alertModelProvider) {
        table.addColumn(new AuditLogSeverityColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<AuditLog> logTimeColumn = new FullDateTimeColumn<AuditLog>() {
            @Override
            protected Date getRawValue(AuditLog object) {
                return object.getlog_time();
            }
        };
        table.addColumn(logTimeColumn, constants.timeEvent(), "160px"); //$NON-NLS-1$

        table.addColumn(new DismissColumn(alertModelProvider), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<AuditLog> messageColumn = new TextColumnWithTooltip<AuditLog>() {
            @Override
            public String getValue(AuditLog object) {
                return object.getmessage();
            }
        };
        table.addColumn(messageColumn, constants.messageEvent());


        table.getSelectionModel().setMultiSelectEnabled(false);

        table.addActionButton(new WebAdminButtonDefinition<AuditLog>(constants.dismissAlert(), CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return alertModelProvider.getModel().getDismissCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<AuditLog>(constants.clearAllDismissedAlerts(), CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return alertModelProvider.getModel().getClearAllCommand();
            }
        });

        table.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                List<AuditLog> selectedItems = table.getSelectionModel().getSelectedList();
                AuditLog selectedItem = selectedItems != null && selectedItems.size() > 0 ? selectedItems.get(0) : null;
                alertModelProvider.getModel().setSelectedItem(selectedItem);
            }
        });
    }

    void initTaskTable(SimpleActionTable<Job> taskTable) {
        ImageResourceColumn<Job> taskStatusColumn = new ImageResourceColumn<Job>() {
            @Override
            public ImageResource getValue(Job object) {
                EntityModel entityModel = new EntityModel();
                entityModel.setEntity(object);
                return new TaskStatusColumn().getValue(entityModel);
            }
        };

        taskTable.addColumn(taskStatusColumn, constants.statusTask(), "30px"); //$NON-NLS-1$

        FullDateTimeColumn<Job> timeColumn = new FullDateTimeColumn<Job>() {
            @Override
            protected Date getRawValue(Job object) {
                return object.getEndTime() == null ? object.getStartTime() : object.getEndTime();
            }
        };
        taskTable.addColumn(timeColumn, constants.timeTask(), "160px"); //$NON-NLS-1$

        TextColumnWithTooltip<Job> descriptionColumn = new TextColumnWithTooltip<Job>() {
            @Override
            public String getValue(Job object) {
                return object.getDescription();
            }
        };
        taskTable.addColumn(descriptionColumn, constants.descriptionTask());
    }

    public interface AlertsEventsFooterResources extends CellTable.Resources {
        interface TableStyle extends CellTable.Style {
        }

        @Override
        @Source({ CellTable.Style.DEFAULT_CSS, "org/ovirt/engine/ui/webadmin/css/FooterHeaderlessTable.css" })
        TableStyle cellTableStyle();
    }

    interface Style extends CssResource {

        String barStyle();

        String alertButtonUpStyle();

        String alertButtonDownStyle();

        String eventButtonUpStyle();

        String eventButtonDownStyle();

        String taskButtonUpStyle();

        String taskButtonDownStyle();
    }

    @Override
    public void onTaskCountChange(int count) {
        return;
    }

    @Override
    public void updateTree() {
        tasksTree.updateTree(taskModelProvider.getModel());
    }

    class DismissColumn extends Column<AuditLog, AuditLog> {

        DismissColumn(AlertModelProvider alertModelProvider) {
            super(new DismissAuditLogImageButtonCell(alertModelProvider));
        }

        @Override
        public AuditLog getValue(AuditLog object) {
            return object;
        }
    }

    class DismissAuditLogImageButtonCell extends ImageButtonCell<AuditLog> {

        AlertModelProvider alertModelProvider;

        public DismissAuditLogImageButtonCell(AlertModelProvider alertModelProvider) {
            super(resources.dialogIconClose());
            this.alertModelProvider = alertModelProvider;
        }

        @Override
        protected String getTitle(AuditLog value) {
            return constants.dismissAlert();
        }

        @Override
        protected UICommand resolveCommand(AuditLog value) {
            alertModelProvider.getModel().setSelectedItem(value);
            return alertModelProvider.getModel().getDismissCommand();
        }
    }

}
