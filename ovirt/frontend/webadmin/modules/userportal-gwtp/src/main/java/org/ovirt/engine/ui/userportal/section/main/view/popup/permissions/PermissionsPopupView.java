package org.ovirt.engine.ui.userportal.section.main.view.popup.permissions;

import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.view.popup.permissions.AbstractPermissionsPopupView;
import org.ovirt.engine.ui.uicommonweb.models.users.UserPortalAdElementListModel;
import org.ovirt.engine.ui.userportal.ApplicationConstants;
import org.ovirt.engine.ui.userportal.ApplicationResources;
import org.ovirt.engine.ui.userportal.section.main.presenter.popup.permissions.PermissionsPopupPresenterWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.SimpleBeanEditorDriver;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public class PermissionsPopupView extends AbstractPermissionsPopupView<UserPortalAdElementListModel> implements PermissionsPopupPresenterWidget.ViewDef {

    interface Driver extends SimpleBeanEditorDriver<UserPortalAdElementListModel, PermissionsPopupView> {
    }

    interface ViewIdHandler extends ElementIdHandler<PermissionsPopupView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final Driver driver = GWT.create(Driver.class);

    @Inject
    public PermissionsPopupView(EventBus eventBus, ApplicationResources resources, ApplicationConstants constants) {
        super(eventBus, resources, constants);
        driver.initialize(this);
    }

    @Override
    protected void generateIds() {
        ViewIdHandler.idHandler.generateAndSetIds(this);
    }

    @Override
    public void edit(UserPortalAdElementListModel object) {
        super.edit(object);
        driver.edit(object);
    }

    @Override
    protected UserPortalAdElementListModel doFlush() {
        return driver.flush();
    }

}
