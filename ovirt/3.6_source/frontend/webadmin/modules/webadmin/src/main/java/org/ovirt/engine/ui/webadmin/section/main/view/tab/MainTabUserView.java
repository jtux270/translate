package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.searchbackend.VdcUserConditionFieldAutoCompleter;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.frontend.utils.FormatUtils;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.users.UserListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.gin.AssetProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabUserPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.UserStatusColumn;
import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class MainTabUserView extends AbstractMainTabWithDetailsTableView<DbUser, UserListModel> implements MainTabUserPresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainTabUserView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final static ApplicationConstants constants = AssetProvider.getConstants();

    @Inject
    public MainTabUserView(MainModelProvider<DbUser, UserListModel> modelProvider) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable();
        initWidget(getTable());
    }

    void initTable() {
        getTable().enableColumnResizing();

        getTable().addColumn(new UserStatusColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        AbstractTextColumn<DbUser> firstNameColumn = new AbstractTextColumn<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getFirstName();
            }
        };
        firstNameColumn.makeSortable(VdcUserConditionFieldAutoCompleter.FIRST_NAME);
        getTable().addColumn(firstNameColumn, constants.firstnameUser(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<DbUser> lastNameColumn = new AbstractTextColumn<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getLastName();
            }
        };
        lastNameColumn.makeSortable(VdcUserConditionFieldAutoCompleter.LAST_NAME);
        getTable().addColumn(lastNameColumn, constants.lastNameUser(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<DbUser> userNameColumn = new AbstractTextColumn<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return FormatUtils.getFullLoginName(object);
            }
        };
        userNameColumn.makeSortable(VdcUserConditionFieldAutoCompleter.USER_NAME);
        getTable().addColumn(userNameColumn, constants.userNameUser(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<DbUser> authzColumn = new AbstractTextColumn<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getDomain();
            }
        };
        authzColumn.makeSortable(VdcUserConditionFieldAutoCompleter.DIRECTORY);
        getTable().addColumn(authzColumn, constants.authz(), "150px"); //$NON-NLS-1$

        AbstractTextColumn<DbUser> namespaceColumn = new AbstractTextColumn<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getNamespace();
            }
        };
        namespaceColumn.makeSortable();
        getTable().addColumn(namespaceColumn, constants.namespace(), "150px"); //$NON-NLS-1$

        getTable().addColumn(new AbstractTextColumn<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getEmail();
            }
        }, constants.emailUser());

        getTable().addActionButton(new WebAdminButtonDefinition<DbUser>(constants.addUser()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getAddCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<DbUser>(constants.removeUser()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<DbUser>(constants.assignTagsUser()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getAssignTagsCommand();
            }
        });
    }

}
