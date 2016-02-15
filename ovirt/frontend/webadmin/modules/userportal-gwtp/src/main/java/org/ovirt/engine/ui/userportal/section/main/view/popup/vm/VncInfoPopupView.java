package org.ovirt.engine.ui.userportal.section.main.view.popup.vm;

import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundWidgetPopupView;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VncInfoPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.VncInfoModel;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.vm.VncInfoPopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VncInfoPopupView extends AbstractModelBoundWidgetPopupView<VncInfoModel> implements VncInfoPopupPresenterWidget.ViewDef {

    @Inject
    public VncInfoPopupView(EventBus eventBus, ApplicationResources resources) {
        super(eventBus, resources, new VncInfoPopupWidget(), "300px", "250px"); //$NON-NLS-1$ //$NON-NLS-2$
    }

}
