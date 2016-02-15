package org.ovirt.engine.ui.webadmin.section.main.presenter.popup.provider;

import java.util.List;

import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.uicommonweb.models.networks.ImportNetworksModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class ImportNetworksPopupPresenterWidget extends AbstractModelBoundPopupPresenterWidget<ImportNetworksModel, ImportNetworksPopupPresenterWidget.ViewDef> {

    public interface ViewDef extends AbstractModelBoundPopupPresenterWidget.ViewDef<ImportNetworksModel> {
        void validateImportedNetworks(List<String> errors);
    }

    @Inject
    public ImportNetworksPopupPresenterWidget(EventBus eventBus, ViewDef view) {
        super(eventBus, view);
    }

    @Override
    public void init(final ImportNetworksModel model) {
        super.init(model);
        model.getErrors().getItemsChangedEvent().addListener(new IEventListener() {

            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                getView().validateImportedNetworks((List<String>) model.getErrors().getItems());
            }
        });
    }

}
