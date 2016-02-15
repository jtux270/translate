package org.ovirt.engine.ui.webadmin.section.main.view.popup.datacenter;

import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelEnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;

public class AbstractFindStoragePopupView extends AbstractModelBoundPopupView<ListModel> {

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, AbstractFindStoragePopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    EntityModelCellTable<ListModel> table;

    @UiField
    Label messageLabel;

    public AbstractFindStoragePopupView(EventBus eventBus, ApplicationResources resources, boolean multiSelection, ApplicationConstants constants) {
        super(eventBus, resources);
        table = new EntityModelCellTable<ListModel>(multiSelection);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        // Table Entity Columns
        table.addEntityModelColumn(new EntityModelTextColumn<StorageDomain>() {
            @Override
            public String getText(StorageDomain storage) {
                return storage.getStorageName();
            }
        }, constants.nameStorage());

        table.addEntityModelColumn(new EntityModelEnumColumn<StorageDomain, StorageDomainType>() {

            @Override
            protected StorageDomainType getEnum(StorageDomain storage) {
                return storage.getStorageDomainType();
            }
        }, constants.typeStorage());

        table.addEntityModelColumn(new EntityModelTextColumn<StorageDomain>() {

            @Override
            public String getText(StorageDomain storage) {
                if (storage.getAvailableDiskSize() == null || storage.getAvailableDiskSize() < 1) {
                    return "< 1 GB"; //$NON-NLS-1$
                }
                return storage.getAvailableDiskSize() + " GB"; //$NON-NLS-1$
            }
        }, constants.freeSpaceStorage());

    }

    @Override
    public void edit(ListModel object) {
        table.asEditor().edit(object);
    }

    @Override
    public void setMessage(String message) {
        super.setMessage(message);
        // Hide table in case of message
        if (message != null && message.length() > 0) {
            table.setVisible(false);
        }
        messageLabel.setText(message);
    }

    @Override
    public ListModel flush() {
        return table.asEditor().flush();
    }

}
