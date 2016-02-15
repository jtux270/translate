package org.ovirt.engine.ui.webadmin.gin.uicommon;

import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.RemoveConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.RolePermissionsRemoveConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.MainTabModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailTabModelProvider;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaClusterListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaEventListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaPermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaStorageListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaTemplateListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaUserListModel;
import org.ovirt.engine.ui.uicommonweb.models.quota.QuotaVmListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.PermissionsPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.event.EventPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.quota.QuotaPopupPresenterWidget;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class QuotaModule extends AbstractGinModule {

    // Main List Model

    @Provides
    @Singleton
    public MainModelProvider<Quota, QuotaListModel> getQuotaListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<QuotaPopupPresenterWidget> quotaPopupProvider,
            final Provider<RemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider) {
        return new MainTabModelProvider<Quota, QuotaListModel>(eventBus, defaultConfirmPopupProvider, QuotaListModel.class) {
            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(QuotaListModel source,
                    UICommand lastExecutedCommand, Model windowModel) {
                if (lastExecutedCommand.equals(getModel().getCreateCommand())
                        || lastExecutedCommand.equals(getModel().getEditCommand())
                        || lastExecutedCommand.equals(getModel().getCloneCommand())) {
                    return quotaPopupProvider.get();
                } else {
                    return super.getModelPopup(source, lastExecutedCommand, windowModel);
                }
            }

            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(QuotaListModel source,
                    UICommand lastExecutedCommand) {
                if (lastExecutedCommand.equals(getModel().getRemoveCommand())) {
                    return removeConfirmPopupProvider.get();
                } else {
                    return super.getConfirmModelPopup(source, lastExecutedCommand);
                }
            }
        };
    }

    // Form Detail Models

    // Searchable Detail Models

    @Provides
    @Singleton
    public SearchableDetailModelProvider<QuotaVdsGroup, QuotaListModel, QuotaClusterListModel> getQuotaClusterListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        return new SearchableDetailTabModelProvider<QuotaVdsGroup, QuotaListModel, QuotaClusterListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaClusterListModel.class);
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<QuotaStorage, QuotaListModel, QuotaStorageListModel> getQuotaStorageListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        return new SearchableDetailTabModelProvider<QuotaStorage, QuotaListModel, QuotaStorageListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaStorageListModel.class);
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<VM, QuotaListModel, QuotaVmListModel> getQuotaVmListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        return new SearchableDetailTabModelProvider<VM, QuotaListModel, QuotaVmListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaVmListModel.class);
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<VmTemplate, QuotaListModel, QuotaTemplateListModel> getQuotaTemplateListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider) {
        return new SearchableDetailTabModelProvider<VmTemplate, QuotaListModel, QuotaTemplateListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaTemplateListModel.class);
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<Permissions, QuotaListModel, QuotaUserListModel> getQuotaUserListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<PermissionsPopupPresenterWidget> popupProvider,
            final Provider<RemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider) {
        return new SearchableDetailTabModelProvider<Permissions, QuotaListModel, QuotaUserListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaUserListModel.class) {
            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(QuotaUserListModel source,
                    UICommand lastExecutedCommand, Model windowModel) {
                if (lastExecutedCommand == getModel().getAddCommand()) {
                    return popupProvider.get();
                } else {
                    return super.getModelPopup(source, lastExecutedCommand, windowModel);
                }
            }

            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(QuotaUserListModel source,
                    UICommand lastExecutedCommand) {
                if (lastExecutedCommand == getModel().getRemoveCommand()) {
                    return removeConfirmPopupProvider.get();
                } else {
                    return super.getConfirmModelPopup(source, lastExecutedCommand);
                }
            }
        };
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<Permissions, QuotaListModel, QuotaPermissionListModel> getQuotaPermissionListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<PermissionsPopupPresenterWidget> popupProvider,
            final Provider<RolePermissionsRemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider) {
        return new SearchableDetailTabModelProvider<Permissions, QuotaListModel, QuotaPermissionListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaPermissionListModel.class) {
            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(QuotaPermissionListModel source,
                    UICommand lastExecutedCommand,
                    Model windowModel) {
                if (lastExecutedCommand == getModel().getAddCommand()) {
                    return popupProvider.get();
                } else {
                    return super.getModelPopup(source, lastExecutedCommand, windowModel);
                }
            }

            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(QuotaPermissionListModel source,
                    UICommand lastExecutedCommand) {
                if (lastExecutedCommand == getModel().getRemoveCommand()) {
                    return removeConfirmPopupProvider.get();
                } else {
                    return super.getConfirmModelPopup(source, lastExecutedCommand);
                }
            }
        };
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<AuditLog, QuotaListModel, QuotaEventListModel> getQuotaEventListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<EventPopupPresenterWidget> eventPopupProvider) {
        return new SearchableDetailTabModelProvider<AuditLog, QuotaListModel, QuotaEventListModel>(
                eventBus, defaultConfirmPopupProvider,
                QuotaListModel.class,
                QuotaEventListModel.class) {
            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(QuotaEventListModel source,
                    UICommand lastExecutedCommand,
                    Model windowModel) {
                if (lastExecutedCommand.equals(getModel().getDetailsCommand())) {
                    return eventPopupProvider.get();
                } else {
                    return super.getModelPopup(source, lastExecutedCommand, windowModel);
                }
            }
        };
    }

    @Override
    protected void configure() {
    }

}
