package org.ovirt.engine.ui.userportal.section.main.view.tab.extended.vm;

import org.ovirt.engine.core.common.businessentities.Disk;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.view.AbstractSubTabTableWidgetView;
import org.ovirt.engine.ui.common.widget.uicommon.vm.PoolDiskListModelTable;
import org.ovirt.engine.ui.uicommonweb.models.pools.PoolDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalItemModel;
import org.ovirt.engine.ui.uicommonweb.models.userportal.UserPortalListModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.section.main.presenter.tab.extended.vm.SubTabExtendedPoolVirtualDiskPresenter;
import org.ovirt.engine.ui.userportal.uicommon.model.vm.PoolDiskListModelProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SubTabExtendedPoolVirtualDiskView extends AbstractSubTabTableWidgetView<UserPortalItemModel, Disk, UserPortalListModel, PoolDiskListModel>
        implements SubTabExtendedPoolVirtualDiskPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabExtendedPoolVirtualDiskView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabExtendedPoolVirtualDiskView(PoolDiskListModelProvider modelProvider,
            EventBus eventBus, ClientStorage clientStorage, ApplicationConstants constants) {
        super(new PoolDiskListModelTable(modelProvider, eventBus, clientStorage));
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getModelBoundTableWidget());
    }

}
