package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.datacenter;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.datacenters.IscsiBondModel;

public class IscsiBondPopupPresenterWidget
        extends AbstractModelBoundPopupPresenterWidget<IscsiBondModel, IscsiBondPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<IscsiBondModel> {
    }

    @Inject
    public IscsiBondPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }
}
