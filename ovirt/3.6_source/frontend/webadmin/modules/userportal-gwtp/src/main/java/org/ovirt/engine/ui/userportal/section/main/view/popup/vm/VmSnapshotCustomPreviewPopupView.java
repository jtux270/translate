package org.ovirt.engine.ui.userportal.section.main.view.popup.vm;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundWidgetPopupView;
import org.ovirt.engine.ui.common.widget.uicommon.popup.vm.VmSnapshotCustomPreviewPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.vms.PreviewSnapshotModel;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.vm.VmSnapshotCustomPreviewPopupPresenterWidget;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class VmSnapshotCustomPreviewPopupView extends AbstractModelBoundWidgetPopupView<PreviewSnapshotModel> implements VmSnapshotCustomPreviewPopupPresenterWidget.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<VmSnapshotCustomPreviewPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public VmSnapshotCustomPreviewPopupView(EventBus eventBus) {
        super(eventBus, new VmSnapshotCustomPreviewPopupWidget(), "900px", "600px"); //$NON-NLS-1$ //$NON-NLS-2$
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }
}
