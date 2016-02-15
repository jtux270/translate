package org.ovirt.engine.ui.webadmin.section.main.view.tab.cluster;


import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServerService;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterServiceStatus;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabFormView;
import org.ovirt.engine.ui.common.widget.UiCommandButton;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.EnumRenderer;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.clusters.ClusterServiceModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.cluster.SubTabClusterServicePresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

public class SubTabClusterServiceView extends AbstractSubTabFormView<VDSGroup, ClusterListModel, ClusterServiceModel>
        implements SubTabClusterServicePresenter.ViewDef, Editor<ClusterServiceModel> {

    interface Driver extends SimpleBeanEditorDriver<ClusterServiceModel, SubTabClusterServiceView> {
    }

    interface ViewUiBinder extends UiBinder<Widget, SubTabClusterServiceView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    @Path(value = "hostList.selectedItem")
    @WithElementId("hostList")
    ListModelListBoxEditor<Object> hostEditor;

    @UiField(provided = true)
    @Path(value = "serviceTypeList.selectedItem")
    @WithElementId
    ListModelListBoxEditor<Object> serviceTypeEditor;

    @UiField
    @WithElementId
    UiCommandButton filterButton;

    @UiField
    @WithElementId
    UiCommandButton clearButton;

    @UiField(provided = true)
    @Ignore
    @WithElementId
    EntityModelCellTable<ListModel> servicesTable;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public SubTabClusterServiceView(final DetailModelProvider<ClusterListModel, ClusterServiceModel> modelProvider,
            ApplicationConstants constants) {
        super(modelProvider);
        servicesTable = new EntityModelCellTable<ListModel>(false, true);
        initListBoxEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        localize(constants);
        initTableColumns(constants);
        initButtons();
        driver.initialize(this);
    }

    private void initListBoxEditors() {
        hostEditor = new ListModelListBoxEditor<Object>(new NullSafeRenderer<Object>() {
            @Override
            public String renderNullSafe(Object object) {
                if (object != null) {
                    return ((VDS) object).getHostName();
                }
                else {
                    return ""; //$NON-NLS-1$
                }
            }
        });
        serviceTypeEditor = new ListModelListBoxEditor<Object>(new EnumRenderer() {
            @Override
            public String render(Enum object) {
                if (object != null) {
                    return super.render(object);
                }
                else {
                    return ""; //$NON-NLS-1$
                }
            }
        });
    }

    private void localize(ApplicationConstants constants) {
        hostEditor.setLabel(constants.hostService());
        serviceTypeEditor.setLabel(constants.nameService());
        filterButton.setLabel(constants.filterService());
        clearButton.setLabel(constants.showAllService());
    }

    protected void initTableColumns(final ApplicationConstants constants) {
        // Table Entity Columns
        servicesTable.addEntityModelColumn(new EntityModelTextColumn<GlusterServerService>() {
            @Override
            public String getText(GlusterServerService entity) {
                return entity.getHostName();
            }
        }, constants.hostService());

        servicesTable.addEntityModelColumn(new EntityModelTextColumn<GlusterServerService>() {
            @Override
            public String getText(GlusterServerService entity) {
                return entity.getServiceType().name();
            }
        }, constants.nameService());

        servicesTable.addColumn(new EnumColumn<EntityModel, GlusterServiceStatus>() {
            @Override
            protected GlusterServiceStatus getRawValue(EntityModel object) {
                return ((GlusterServerService)object.getEntity()).getStatus();
            }
        }, constants.statusService());

        servicesTable.addEntityModelColumn(new EntityModelTextColumn<GlusterServerService>() {
            @Override
            public String getText(GlusterServerService entity) {
                if (entity.getPort() != null && entity.getPort() > 0) {
                    return String.valueOf(entity.getPort());
                }
                else {
                    return constants.notAvailableLabel();
                }
            }
        }, constants.portService());

        servicesTable.addEntityModelColumn(new EntityModelTextColumn<GlusterServerService>() {
            @Override
            public String getText(GlusterServerService entity) {
                return String.valueOf(entity.getPid());
            }
        }, constants.pidService());
    }

    private void initButtons() {
        filterButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                getDetailModel().executeCommand(getDetailModel().getFilterServicesCommand());
            }
        });

        clearButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                getDetailModel().executeCommand(getDetailModel().getClearFilterServicesCommand());
            }
        });
    }

    @Override
    public void setMainTabSelectedItem(VDSGroup selectedItem) {
        servicesTable.asEditor().edit(getDetailModel().getServiceList());
        driver.edit(getDetailModel());
    }

}
