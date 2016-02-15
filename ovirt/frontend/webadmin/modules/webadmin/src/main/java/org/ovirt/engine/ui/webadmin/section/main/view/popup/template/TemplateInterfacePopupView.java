package org.ovirt.engine.ui.webadmin.section.main.view.popup.template;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundWidgetPopupView;
import org.ovirt.engine.ui.common.widget.uicommon.popup.template.TemplateNetworkInterfacePopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmInterfaceModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.template.TemplateInterfacePopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class TemplateInterfacePopupView extends AbstractModelBoundWidgetPopupView<VmInterfaceModel> implements TemplateInterfacePopupPresenterWidget.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<TemplateInterfacePopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public TemplateInterfacePopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus,
                resources,
                new TemplateNetworkInterfacePopupWidget(eventBus, constants), "490px", //$NON-NLS-1$
                "320px"); //$NON-NLS-1$
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

}
