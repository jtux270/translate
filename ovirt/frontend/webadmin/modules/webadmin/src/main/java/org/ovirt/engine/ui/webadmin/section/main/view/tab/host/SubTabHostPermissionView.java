package org.ovirt.engine.ui.webadmin.section.main.view.tab.host;

import javax.inject.Inject;

import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.system.ClientStorage;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.hosts.HostListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.host.SubTabHostPermissionPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.tab.AbstractSubTabPermissionsView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;

public class SubTabHostPermissionView extends AbstractSubTabPermissionsView<VDS, HostListModel>
        implements SubTabHostPermissionPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<SubTabHostPermissionView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    @Inject
    public SubTabHostPermissionView(SearchableDetailModelProvider<Permissions, HostListModel, PermissionListModel> modelProvider,
            EventBus eventBus,
            ClientStorage clientStorage, ApplicationConstants constants) {
        super(modelProvider, eventBus, clientStorage, constants);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

}
