package org.ovirt.engine.ui.webadmin.section.main.view.popup.vm;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundWidgetPopupView;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VmSnapshotCreatePopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.SnapshotModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.ApplicationTemplates;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmSnapshotCreatePopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VmSnapshotCreatePopupView extends AbstractModelBoundWidgetPopupView<SnapshotModel> implements VmSnapshotCreatePopupPresenterWidget.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<VmSnapshotCreatePopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public VmSnapshotCreatePopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants,
                                     ApplicationTemplates templates) {
        super(eventBus, resources, new VmSnapshotCreatePopupWidget(constants, templates, resources), "410px", "400px"); //$NON-NLS-1$ //$NON-NLS-2$
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

}
