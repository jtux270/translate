package org.ovirt.engine.ui.webadmin.section.main.view.popup.vm;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundWidgetPopupView;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VmDiskPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.AbstractDiskModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmDiskPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VmDiskPopupView extends AbstractModelBoundWidgetPopupView<AbstractDiskModel> implements VmDiskPopupPresenterWidget.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<VmDiskPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public VmDiskPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants, ApplicationTemplates templates) {
        super(eventBus, resources, new VmDiskPopupWidget(constants, resources, templates, true), "815px", "615px"); //$NON-NLS-1$ //$NON-NLS-2$
        ViewIdHandler.idHandler.generateAndSetIds(this);
        asWidget().enableResizeSupport(true);
    }

    @Override
    public boolean handleEnterKeyDisabled() {
        VmDiskPopupWidget vmDiskPopupWidget = (VmDiskPopupWidget) asWidget().getContent();
        return vmDiskPopupWidget.handleEnterKeyDisabled();
    }

}
