package org.ovirt.engine.ui.common.view.popup;

import java.util.Map.Entry;

import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.CommonApplicationMessages;
import org.ovirt.engine.ui.common.CommonApplicationResources;
import org.ovirt.engine.ui.common.widget.Align;
import org.ovirt.engine.ui.common.widget.EntityModelCheckBoxWidget;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;

import com.google.gwt.event.shared.EventBus;

public class AbstractVmRemoveConfimationPopup extends RemoveConfirmationPopupView {
    public AbstractVmRemoveConfimationPopup(EventBus eventBus,
            CommonApplicationResources resources,
            CommonApplicationMessages messages,
            CommonApplicationConstants constants) {
        super(eventBus, resources, messages, constants);
        itemPanel.setHeight("80%"); //$NON-NLS-1$
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addItemText(Object item) {
        Entry<Guid, EntityModel<Boolean>> entry = (Entry<Guid, EntityModel<Boolean>>) item;
        EntityModelCheckBoxWidget cb =
                new EntityModelCheckBoxWidget(Align.RIGHT,
                        "- " + entry.getValue().getMessage(), entry.getValue().getTitle()); //$NON-NLS-1$

        cb.edit(entry.getValue());
        itemPanel.add(cb);
    }
}
