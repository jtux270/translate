package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.searchbackend.VdcUserConditionFieldAutoCompleter;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.frontend.utils.FormatUtils;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.users.UserListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
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

    @Inject
    public MainTabUserView(MainModelProvider<DbUser, UserListModel> modelProvider, ApplicationConstants constants) {
        super(modelProvider);
        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(constants);
        initWidget(getTable());
    }

    void initTable(ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().addColumn(new UserStatusColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<DbUser> firstNameColumn = new TextColumnWithTooltip<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getFirstName();
            }
        };
        firstNameColumn.makeSortable(VdcUserConditionFieldAutoCompleter.FIRST_NAME);
        getTable().addColumn(firstNameColumn, constants.firstnameUser(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<DbUser> lastNameColumn = new TextColumnWithTooltip<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getLastName();
            }
        };
        lastNameColumn.makeSortable(VdcUserConditionFieldAutoCompleter.LAST_NAME);
        getTable().addColumn(lastNameColumn, constants.lastNameUser(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<DbUser> userNameColumn = new TextColumnWithTooltip<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return FormatUtils.getFullLoginName(object);
            }
        };
        userNameColumn.makeSortable(VdcUserConditionFieldAutoCompleter.USER_NAME);
        getTable().addColumn(userNameColumn, constants.userNameUser(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<DbUser> authzColumn = new TextColumnWithTooltip<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getDomain();
            }
        };
        authzColumn.makeSortable();
        getTable().addColumn(authzColumn, constants.authz(), "150px"); //$NON-NLS-1$

        TextColumnWithTooltip<DbUser> namespaceColumn = new TextColumnWithTooltip<DbUser>() {
            @Override
            public String getValue(DbUser object) {
                return object.getNamespace();
            }
        };
        namespaceColumn.makeSortable();
        getTable().addColumn(namespaceColumn, constants.namespace(), "150px"); //$NON-NLS-1$

        getTable().addColumn(new TextColumnWithTooltip<DbUser>() {
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
