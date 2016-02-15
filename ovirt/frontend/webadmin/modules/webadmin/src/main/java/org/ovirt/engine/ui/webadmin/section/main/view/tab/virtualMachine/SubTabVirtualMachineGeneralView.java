package org.ovirt.engine.ui.webadmin.section.main.view.tab.virtualMachine;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.view.AbstractSubTabFormView;
import org.ovirt.engine.ui.common.widget.uicommon.vm.VmGeneralModelForm;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.virtualMachine.SubTabVirtualMachineGeneralPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class SubTabVirtualMachineGeneralView extends AbstractSubTabFormView<VM, VmListModel, VmGeneralModel> implements SubTabVirtualMachineGeneralPresenter.ViewDef, Editor<VmGeneralModel> {

    interface ViewUiBinder extends UiBinder<Widget, SubTabVirtualMachineGeneralView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    // We need this in order to find the icon for alert messages:
    private final ApplicationResources resources;

    @UiField(provided = true)
    VmGeneralModelForm form;

    // This is the panel containing the alerts label and the
    // potential alert, this way we can hide the panel
    // completely (including the label) if there is no alert
    // to present:
    @UiField
    HTMLPanel alertsPanel;

    // This is the list of action items inside the panel, so that we
    // can clear and add elements inside without affecting the panel:
    @UiField
    FlowPanel alertsList;

    @Inject
    public SubTabVirtualMachineGeneralView(DetailModelProvider<VmListModel, VmGeneralModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        this.form = new VmGeneralModelForm(modelProvider, constants);

        // Inject a reference to the resources:
        resources = ClientGinjectorProvider.getApplicationResources();

        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        clearAlerts();
    }

    @Override
    public void setMainTabSelectedItem(VM selectedItem) {
        form.update();
    }

    @Override
    public void clearAlerts() {
        // Remove all the alert widgets and make the panel invisible:
        alertsList.clear();
        alertsPanel.setVisible(false);
    }

    @Override
    public void addAlert(Widget alertWidget) {
        // Create a composite panel that contains the alert icon and the widget provided
        // by the caller, both rendered horizontally:
        FlowPanel alertPanel = new FlowPanel();
        Image alertIcon = new Image(resources.alertImage());
        alertIcon.getElement().getStyle().setProperty("display", "inline"); //$NON-NLS-1$ //$NON-NLS-2$
        alertWidget.getElement().getStyle().setProperty("display", "inline"); //$NON-NLS-1$ //$NON-NLS-2$
        alertPanel.add(alertIcon);
        alertPanel.add(alertWidget);

        // Add the composite panel to the alerts panel:
        alertsList.add(alertPanel);

        // Make the panel visible if it wasn't:
        if (!alertsPanel.isVisible()) {
            alertsPanel.setVisible(true);
        }
    }

}
