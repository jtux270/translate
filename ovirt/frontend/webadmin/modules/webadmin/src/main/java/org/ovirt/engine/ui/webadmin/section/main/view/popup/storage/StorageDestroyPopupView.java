package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.ui.common.view.popup.ForceRemoveConfirmationPopupView;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationMessages;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.StorageDestroyPopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class StorageDestroyPopupView extends ForceRemoveConfirmationPopupView
        implements StorageDestroyPopupPresenterWidget.ViewDef {

    @Inject
    public StorageDestroyPopupView(EventBus eventBus,
            ApplicationResources resources,
            ApplicationConstants constants,
            ApplicationMessages messages) {
        super(eventBus, resources, constants, messages);
    }

    @Override
    protected String getWarning() {
        return ((ApplicationConstants) constants).storageDestroyPopupWarningLabel();
    }

    @Override
    protected String getFormattedMessage(String itemName) {
        return ((ApplicationMessages) messages).storageDestroyPopupMessageLabel(itemName);
    }

}
