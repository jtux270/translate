package org.ovirt.engine.ui.webadmin.uicommon.model;

import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.RolePermissionsRemoveConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.SearchableTabModelProvider;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.roles_ui.RolePermissionListModel;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class RolePermissionModelProvider extends SearchableTabModelProvider<Permissions, RolePermissionListModel> {

    private final Provider<RolePermissionsRemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider;

    @Inject
    public RolePermissionModelProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            Provider<RolePermissionsRemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider) {
        super(eventBus, defaultConfirmPopupProvider);
        this.removeConfirmPopupProvider = removeConfirmPopupProvider;
    }

    @Override
    public RolePermissionListModel getModel() {
        return (RolePermissionListModel) getCommonModel().getRoleListModel().getDetailModels().get(0);
    }

    @Override
    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(RolePermissionListModel source,
            UICommand lastExecutedCommand) {
        if (lastExecutedCommand == getModel().getRemoveCommand()) {
            return removeConfirmPopupProvider.get();
        } else {
            return super.getConfirmModelPopup(source, lastExecutedCommand);
        }
    }

}
