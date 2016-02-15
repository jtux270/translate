package org.ovirt.engine.ui.webadmin.section.main.view.popup.vm;

import com.google.gwt.user.client.ui.Panel;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.AdvancedParametersExpander;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.generic.EntityModelRadioButtonEditor;
import org.ovirt.engine.ui.common.widget.editor.ListModelListBoxEditor;
import org.ovirt.engine.ui.common.widget.renderer.NullSafeRenderer;
import org.ovirt.engine.ui.uicommonweb.models.vms.MigrateModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmMigratePopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class VmMigratePopupView extends AbstractModelBoundPopupView<MigrateModel>
        implements VmMigratePopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<MigrateModel, VmMigratePopupView> {
    }

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, VmMigratePopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    @Path(value = "selectHostAutomatically_IsSelected.entity")
    EntityModelRadioButtonEditor selectHostAutomaticallyEditor;

    @UiField(provided = true)
    @Path(value = "selectDestinationHost_IsSelected.entity")
    EntityModelRadioButtonEditor selectDestinationHostEditor;

    @UiField(provided = true)
    @Path(value = "hosts.selectedItem")
    ListModelListBoxEditor<VDS> hostsListEditor;

    @UiField
    @Ignore
    Label message1;

    @UiField
    @Ignore
    Label message2;

    @UiField
    @Ignore
    Label message3;

    @UiField
    @Ignore
    AdvancedParametersExpander advancedOptionsExpander;

    @UiField
    @Ignore
    Panel advancedOptionsExpanderContent;

    @UiField(provided = true)
    @Path(value = "clusters.selectedItem")
    @WithElementId("clusters")
    public ListModelListBoxEditor<VDSGroup> clustersEditor;

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public VmMigratePopupView(EventBus eventBus,
            ApplicationResources resources,
            ApplicationConstants constants,
            ApplicationMessages messages) {
        super(eventBus, resources);
        initEditors();
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        advancedOptionsExpander.initWithContent(advancedOptionsExpanderContent.getElement());

        localize(constants, messages);
        driver.initialize(this);
    }

    void initEditors() {
        selectHostAutomaticallyEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$
        selectDestinationHostEditor = new EntityModelRadioButtonEditor("1"); //$NON-NLS-1$

        hostsListEditor = new ListModelListBoxEditor<VDS>(new NullSafeRenderer<VDS>() {
            @Override
            public String renderNullSafe(VDS vds) {
                return vds.getName();
            }
        });

        clustersEditor = new ListModelListBoxEditor<VDSGroup>(new NullSafeRenderer<VDSGroup>() {
            @Override
            protected String renderNullSafe(VDSGroup cluster) {
                return cluster.getName();
            }
        });
    }

    void localize(ApplicationConstants constants, ApplicationMessages messages) {
        selectHostAutomaticallyEditor.setLabel(constants.vmMigratePopupSelectHostAutomaticallyLabel());
        selectDestinationHostEditor.setLabel(constants.vmMigratePopupSelectDestinationHostLabel());
        hostsListEditor.setLabel(constants.vmMigratePopupHostsListLabel());
        clustersEditor.setLabel(constants.hostClusterVmPopup());
        message1.setText(messages.migrateHostDisabledVMsInServerClusters());
        message2.setText(messages.migrateSomeVmsAlreadyRunningOnHost());
        message3.setText(messages.migrateNoAvailableHost());
    }

    private void updateMessages(MigrateModel object) {
        message1.setVisible(!object.getVmsOnSameCluster());
        message2.setVisible(object.getIsSameVdsMessageVisible());
        message3.setVisible(object.getNoSelAvailable());
    }

    @Override
    public void edit(final MigrateModel object) {
        driver.edit(object);

        updateMessages(object);

        // Listen for changes in the properties of the model in order
        // to update the alerts panel:
        object.getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                if (args instanceof PropertyChangedEventArgs) {
                    updateMessages(object);
                }
            }
        });
    }

    @Override
    public MigrateModel flush() {
        return driver.flush();
    }

}
