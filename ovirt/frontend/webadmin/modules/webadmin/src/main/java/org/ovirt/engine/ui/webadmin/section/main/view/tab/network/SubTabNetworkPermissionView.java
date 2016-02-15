package org.ovirt.engine.ui.webadmin.section.main.view.tab.network;

import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.network.NetworkView;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.networks.NetworkListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.network.SubTabNetworkPermissionPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.tab.AbstractSubTabPermissionsView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class SubTabNetworkPermissionView extends AbstractSubTabPermissionsView<NetworkView, NetworkListModel>
        implements SubTabNetworkPermissionPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabNetworkPermissionView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabNetworkPermissionView(SearchableDetailModelProvider<Permissions, NetworkListModel, PermissionListModel> modelProvider,
            EventBus eventBus,
            ClientStorage clientStorage, ApplicationConstants constants) {
        super(modelProvider, eventBus, clientStorage, constants);
        getTable().enableColumnResizing();
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

}

