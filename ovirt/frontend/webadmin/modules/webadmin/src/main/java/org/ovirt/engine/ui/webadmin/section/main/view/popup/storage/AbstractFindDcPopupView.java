package org.ovirt.engine.ui.webadmin.section.main.view.popup.storage;

import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.ui.common.view.popup.AbstractModelBoundPopupView;
import org.ovirt.engine.ui.common.widget.dialog.SimpleDialogPanel;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.table.column.EntityModelTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;

public class AbstractFindDcPopupView extends AbstractModelBoundPopupView<ListModel> {

    interface ViewUiBinder extends UiBinder<SimpleDialogPanel, AbstractFindDcPopupView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField(provided = true)
    EntityModelCellTable<ListModel> table;

    @UiField
    Label messageLabel;

    public AbstractFindDcPopupView(EventBus eventBus, ApplicationResources resources, boolean multiSelection, final ApplicationConstants constants) {
        super(eventBus, resources);
        table = new EntityModelCellTable<ListModel>(multiSelection);
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));

        // Table Entity Columns
        table.addEntityModelColumn(new EntityModelTextColumn<StoragePool>() {
            @Override
            public String getText(StoragePool storage) {
                return storage.getName();
            }
        }, constants.nameDc());

        table.addEntityModelColumn(new EntityModelTextColumn<StoragePool>() {
            @Override
            protected String getText(StoragePool entity) {
                return entity.isLocal() ? constants.storageTypeLocal() : constants.storageTypeShared();
            }
        }
        , constants.storgeTypeDc());
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
