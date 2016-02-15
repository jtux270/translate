package org.ovirt.engine.ui.webadmin.section.main.view.popup.instancetypes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.ovirt.engine.core.common.businessentities.InstanceType;
import org.ovirt.engine.ui.common.MainTableHeaderlessResources;
import org.ovirt.engine.ui.common.MainTableResources;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.common.widget.uicommon.popup.instancetypes.InstanceTypeGeneralModelForm;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.uicommon.model.InstanceTypeGeneralModelProvider;
import org.ovirt.engine.ui.webadmin.uicommon.model.InstanceTypeModelProvider;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;

public class InstanceTypesView extends Composite {

    interface ViewUiBinder extends UiBinder<FlowPanel, InstanceTypesView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    SimplePanel instanceTypesTabContent;

    private SimpleActionTable<InstanceType> table;
    private InstanceTypeGeneralModelForm detailTable;
    private SplitLayoutPanel splitLayoutPanel;

    private final InstanceTypeModelProvider instanceTypeModelProvider;
    private final InstanceTypeGeneralModelProvider instanceTypeGeneralModelProvider;

    private final EventBus eventBus;

    private final ClientStorage clientStorage;

    @Inject
    public InstanceTypesView(ApplicationConstants constants,
            InstanceTypeModelProvider instanceTypeModelProvider,
            EventBus eventBus, ClientStorage clientStorage,
            InstanceTypeGeneralModelProvider instanceTypeGeneralModelProvider) {
        this.instanceTypeModelProvider = instanceTypeModelProvider;
        this.eventBus = eventBus;
        this.clientStorage = clientStorage;
        this.instanceTypeGeneralModelProvider = instanceTypeGeneralModelProvider;
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        initSplitLayoutPanel();

        initMainTable(constants);
        initSubtabTable(constants);
    }

    private void initSplitLayoutPanel() {
        splitLayoutPanel = new SplitLayoutPanel();
        splitLayoutPanel.setHeight("100%"); //$NON-NLS-1$
        splitLayoutPanel.setWidth("100%"); //$NON-NLS-1$
        instanceTypesTabContent.add(splitLayoutPanel);
    }

    public void setSubTabVisibility(boolean visible) {
        splitLayoutPanel.clear();
        if (visible) {
            splitLayoutPanel.addSouth(detailTable, 150);
        }
        splitLayoutPanel.add(table);
    }

    private void initMainTable(ApplicationConstants constants) {
        table = new SimpleActionTable<InstanceType>(instanceTypeModelProvider,
                getTableHeaderlessResources(), getTableResources(), eventBus, clientStorage);

        TextColumnWithTooltip<InstanceType> nameColumn = new TextColumnWithTooltip<InstanceType>() {
            @Override
            public String getValue(InstanceType object) {
                return object.getName();
            }
        };
        table.addColumn(nameColumn, constants.instanceTypeName(), "100px"); //$NON-NLS-1$


        table.addActionButton(new WebAdminButtonDefinition<InstanceType>(constants.newInstanceType()) {
            @Override
            protected UICommand resolveCommand() {
                return instanceTypeModelProvider.getModel().getNewInstanceTypeCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<InstanceType>(constants.editInstanceType()) {
            @Override
            protected UICommand resolveCommand() {
                return instanceTypeModelProvider.getModel().getEditInstanceTypeCommand();
            }
        });

        table.addActionButton(new WebAdminButtonDefinition<InstanceType>(constants.removeInstanceType()) {
            @Override
            protected UICommand resolveCommand() {
                return instanceTypeModelProvider.getModel().getDeleteInstanceTypeCommand();
            }
        });

        splitLayoutPanel.add(table);

        table.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                instanceTypeModelProvider.setSelectedItems(table.getSelectionModel().getSelectedList());
                if (table.getSelectionModel().getSelectedList().size() > 0) {
                    setSubTabVisibility(true);
                    detailTable.update();
                } else {
                    setSubTabVisibility(false);
                }
            }
        });

    }

    private void initSubtabTable(ApplicationConstants constants) {
        detailTable = new InstanceTypeGeneralModelForm(instanceTypeGeneralModelProvider, constants);
    }

    protected Resources getTableHeaderlessResources() {
        return GWT.create(MainTableHeaderlessResources.class);
    }

    protected Resources getTableResources() {
        return GWT.create(MainTableResources.class);
    }

}
