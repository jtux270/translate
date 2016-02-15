package org.ovirt.engine.ui.common.widget.uicommon.popup.quota;

import java.util.ArrayList;

import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.PopupSimpleTableResources;
import org.ovirt.engine.ui.common.idhandler.HasElementId;
import org.ovirt.engine.ui.common.widget.HasEditorDriver;
import org.ovirt.engine.ui.common.widget.editor.EntityModelCellTable;
import org.ovirt.engine.ui.common.widget.table.column.EmptyColumn;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.ChangeQuotaItemModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.ChangeQuotaModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class ChangeQuotaView extends Composite implements HasEditorDriver<ChangeQuotaModel>, HasElementId {

    interface Driver extends SimpleBeanEditorDriver<ChangeQuotaModel, ChangeQuotaView> {
    }

    interface ViewUiBinder extends UiBinder<Widget, ChangeQuotaView> {
        ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);
    }

    @UiField
    FlowPanel listPanel;

    @UiField
    SimplePanel listHeaderPanel;

    @Ignore
    EntityModelCellTable<ListModel> listHeader;

    private final Driver driver = GWT.create(Driver.class);

    private final CommonApplicationConstants constants;

    public ChangeQuotaView(CommonApplicationConstants constants) {
        this.constants = constants;
        initWidget(ViewUiBinder.uiBinder.createAndBindUi(this));
        driver.initialize(this);
    }

    void updateListHeader(ChangeQuotaModel model) {
        String width = "100px"; //$NON-NLS-1$
        listHeader = new EntityModelCellTable(false, (Resources) GWT.create(
                PopupSimpleTableResources.class), true);
        listHeader.addColumn(new EmptyColumn(), constants.elementName(), width);
        listHeader.addColumn(new EmptyColumn(), constants.storageDomainDisk(), width);
        listHeader.addColumn(new EmptyColumn(), constants.currentQuota(), width);

        listHeader.addColumn(new EmptyColumn(), constants.quotaDisk(), width);

        listHeader.setRowData(new ArrayList());
        listHeader.setWidth("100%", true); //$NON-NLS-1$

        listHeaderPanel.setWidget(listHeader);
    }

    @Override
    public void edit(ChangeQuotaModel model) {
        driver.edit(model);
        initListerners(model);
        updateListHeader(model);
    }

    private void initListerners(final ChangeQuotaModel model) {
        model.getItemsChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                addItems(model);
            }
        });
    }

    void addItems(ChangeQuotaModel model) {
        listPanel.clear();

        for (final ChangeQuotaItemModel itemModel : (ArrayList<ChangeQuotaItemModel>) model.getItems()) {
            ChangeQuotaItemView itemView = new ChangeQuotaItemView(constants);
            itemView.edit(itemModel);
            listPanel.add(itemView);
        }
    }

    @Override
    public ChangeQuotaModel flush() {
        return driver.flush();
    }

    @Override
    public void setElementId(String elementId) {
    }

}
