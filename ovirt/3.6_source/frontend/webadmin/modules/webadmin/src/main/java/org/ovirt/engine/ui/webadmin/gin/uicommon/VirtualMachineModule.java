package org.ovirt.engine.ui.webadmin.gin.uicommon;

import org.ovirt.engine.core.common.businessentities.AuditLog;
import org.ovirt.engine.core.common.businessentities.Erratum;
import org.ovirt.engine.core.common.businessentities.HostDeviceView;
import org.ovirt.engine.core.common.businessentities.Permission;
import org.ovirt.engine.core.common.businessentities.Snapshot;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.storage.Disk;
import org.ovirt.engine.core.common.scheduling.AffinityGroup;
import org.ovirt.engine.ui.common.presenter.AbstractModelBoundPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.ModelBoundPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.ConsolePopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.DefaultConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.presenter.popup.RemoveConfirmationPopupPresenterWidget;
import org.ovirt.engine.ui.common.uicommon.model.DetailModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.DetailTabModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.MainTabModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.SearchableDetailTabModelProvider;
import org.ovirt.engine.ui.uicommonweb.ReportCommand;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.CommonModel;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.models.VmErrataCountModel;
import org.ovirt.engine.ui.uicommonweb.models.VmErrataListModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.scheduling.affinity_groups.list.VmAffinityGroupListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.AttachDiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.EditDiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmFromExportDomainModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmFromExternalProviderModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ImportVmsModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.NewDiskModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmAppListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmDevicesListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmDiskListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmEventListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmGuestInfoModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmImportGeneralModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmInterfaceListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmNextRunConfigurationModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmSnapshotListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VncInfoModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.hostdev.VmHostDeviceListModel;
import org.ovirt.engine.ui.webadmin.section.main.presenter.ReportPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.AssignTagsPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.ImportVmsPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.VmErrataListWithDetailsPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.event.EventPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.guide.GuidePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.hostdev.AddVmHostDevicePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.hostdev.VmRepinHostPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.quota.ChangeQuotaPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.scheduling.AffinityGroupPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.DisksAllocationPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.storage.backup.ImportVmFromExportDomainPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.CloneVmPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.ImportVmFromExternalProviderPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.SingleSelectionVmDiskAttachPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmChangeCDPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmClonePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmDiskAttachPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmDiskPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmDiskRemovePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmExportPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmInterfacePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmMakeTemplatePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmMigratePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmNextRunConfigurationPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmRunOncePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmSnapshotCreatePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmSnapshotCustomPreviewPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VmSnapshotPreviewPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.presenter.popup.vm.VncInfoPopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.vm.VmRemovePopupPresenterWidget;
import org.ovirt.engine.ui.webadmin.uicommon.model.PermissionModelProvider;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class VirtualMachineModule extends AbstractGinModule {

    // Main List Model

    @Provides
    @Singleton
    public MainModelProvider<VM, VmListModel<Void>> getVmListProvider(EventBus eventBus,
            final Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<AssignTagsPopupPresenterWidget> assignTagsPopupProvider,
            final Provider<VmMakeTemplatePopupPresenterWidget> makeTemplatePopupProvider,
            final Provider<VmRunOncePopupPresenterWidget> runOncePopupProvider,
            final Provider<VmChangeCDPopupPresenterWidget> changeCDPopupProvider,
            final Provider<VmExportPopupPresenterWidget> exportPopupProvider,
            final Provider<VmSnapshotCreatePopupPresenterWidget> createSnapshotPopupProvider,
            final Provider<VmMigratePopupPresenterWidget> migratePopupProvider,
            final Provider<VmPopupPresenterWidget> newVmPopupProvider,
            final Provider<GuidePopupPresenterWidget> guidePopupProvider,
            final Provider<RemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider,
            final Provider<VmRemovePopupPresenterWidget> vmRemoveConfirmPopupProvider,
            final Provider<ReportPresenterWidget> reportWindowProvider,
            final Provider<ConsolePopupPresenterWidget> consolePopupProvider,
            final Provider<VncInfoPopupPresenterWidget> vncWindoProvider,
            final Provider<VmNextRunConfigurationPresenterWidget> nextRunProvider,
            final Provider<ImportVmsPopupPresenterWidget> importVmsProvider,
            final Provider<CloneVmPopupPresenterWidget> cloneVmProvider,
            final Provider<ImportVmFromExportDomainPopupPresenterWidget> importVmFromExportDomainPopupProvider,
            final Provider<ImportVmFromExternalProviderPopupPresenterWidget> importVmFromExternalProviderProvider,
            final Provider<VmListModel<Void>> modelProvider,
            final Provider<CommonModel> commonModelProvider,
            final Provider<VmDiskPopupPresenterWidget> newDiskPopupProvider,
            final Provider<SingleSelectionVmDiskAttachPopupPresenterWidget> attachDiskPopupProvider,
            final Provider<VmDiskRemovePopupPresenterWidget> removeDiskConfirmPopupProvider) {
        MainTabModelProvider<VM, VmListModel<Void>> result =
                new MainTabModelProvider<VM, VmListModel<Void>>(eventBus, defaultConfirmPopupProvider, commonModelProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmListModel<Void> source,
                            UICommand lastExecutedCommand, Model windowModel) {
                        if (lastExecutedCommand == getModel().getAssignTagsCommand()) {
                            return assignTagsPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getNewTemplateCommand()) {
                            return makeTemplatePopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getRunOnceCommand()) {
                            return runOncePopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getChangeCdCommand()) {
                            return changeCDPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getExportCommand()) {
                            return exportPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getCreateSnapshotCommand()) {
                            return createSnapshotPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getMigrateCommand()) {
                            return migratePopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getEditCommand() || lastExecutedCommand == getModel().getNewVmCommand() || "OnSave".equals(lastExecutedCommand.getName())) { //$NON-NLS-1$
                            if (windowModel instanceof AttachDiskModel) {
                                return attachDiskPopupProvider.get();
                            } else if ((windowModel instanceof NewDiskModel) || (windowModel instanceof EditDiskModel)) {
                                return newDiskPopupProvider.get();
                            } else {
                                return newVmPopupProvider.get();
                            }
                        } else if (lastExecutedCommand == getModel().getGuideCommand()) {
                            return guidePopupProvider.get();
                        } else if (windowModel instanceof VncInfoModel) {
                            return vncWindoProvider.get();
                        } else if (lastExecutedCommand == getModel().getEditConsoleCommand()) {
                            return consolePopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getCloneVmCommand()) {
                            return cloneVmProvider.get();
                        } else if (lastExecutedCommand == getModel().getImportVmCommand()
                                || windowModel instanceof ImportVmsModel) {
                            return importVmsProvider.get();
                        } else if (windowModel instanceof ImportVmFromExportDomainModel) {
                            return importVmFromExportDomainPopupProvider.get();
                        } else if (windowModel instanceof ImportVmFromExternalProviderModel) {
                            return importVmFromExternalProviderProvider.get();
                        } else {
                            return super.getModelPopup(source, lastExecutedCommand, windowModel);
                        }
                    }

                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(VmListModel<Void> source,
                            UICommand lastExecutedCommand) {
                        if (lastExecutedCommand == getModel().getRemoveCommand()) {
                            return vmRemoveConfirmPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getStopCommand() ||
                                lastExecutedCommand == getModel().getShutdownCommand()) {
                            return removeConfirmPopupProvider.get();
                        } else if (source.getConfirmWindow() instanceof VmNextRunConfigurationModel) {
                            return nextRunProvider.get();
                        } else if ("OnSave".equals(lastExecutedCommand.getName())) { //$NON-NLS-1$
                            return defaultConfirmPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getEditCommand()) {
                            return removeDiskConfirmPopupProvider.get();
                        } else {
                            return super.getConfirmModelPopup(source, lastExecutedCommand);
                        }
                    }

                    @Override
                    protected ModelBoundPresenterWidget<? extends Model> getModelBoundWidget(UICommand lastExecutedCommand) {
                        if (lastExecutedCommand instanceof ReportCommand) {
                            return reportWindowProvider.get();
                        } else {
                            return super.getModelBoundWidget(lastExecutedCommand);
                        }
                    }
                };
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<Disk, VmListModel<Void>, VmDiskListModel> getVmDiskListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<VmDiskPopupPresenterWidget> popupProvider,
            final Provider<VmDiskAttachPopupPresenterWidget> attachPopupProvider,
            final Provider<VmDiskRemovePopupPresenterWidget> removeConfirmPopupProvider,
            final Provider<DisksAllocationPopupPresenterWidget> movePopupProvider,
            final Provider<ChangeQuotaPopupPresenterWidget> changeQutoaPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmDiskListModel> modelProvider) {
        SearchableDetailTabModelProvider<Disk, VmListModel<Void>, VmDiskListModel> result =
                new SearchableDetailTabModelProvider<Disk, VmListModel<Void>, VmDiskListModel>(
                        eventBus, defaultConfirmPopupProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmDiskListModel source,
                            UICommand lastExecutedCommand, Model windowModel) {
                        VmDiskListModel model = getModel();

                        if (lastExecutedCommand == model.getNewCommand()
                                || lastExecutedCommand == model.getEditCommand()) {
                            return popupProvider.get();
                        } else if (lastExecutedCommand == getModel().getAttachCommand()) {
                            return attachPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getMoveCommand()) {
                            return movePopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getChangeQuotaCommand()) {
                            return changeQutoaPopupProvider.get();
                        } else {
                            return super.getModelPopup(source, lastExecutedCommand, windowModel);
                        }
                    }

                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(VmDiskListModel source,
                            UICommand lastExecutedCommand) {
                        if (lastExecutedCommand == getModel().getRemoveCommand()) {
                            return removeConfirmPopupProvider.get();
                        } else {
                            return super.getConfirmModelPopup(source, lastExecutedCommand);
                        }
                    }
                };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<VmNetworkInterface, VmListModel<Void>, VmInterfaceListModel> getVmInterfaceListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<VmInterfacePopupPresenterWidget> popupProvider,
            final Provider<RemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmInterfaceListModel> modelProvider) {
        SearchableDetailTabModelProvider<VmNetworkInterface, VmListModel<Void>, VmInterfaceListModel> result =
                new SearchableDetailTabModelProvider<VmNetworkInterface, VmListModel<Void>, VmInterfaceListModel>(
                        eventBus, defaultConfirmPopupProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmInterfaceListModel source,
                            UICommand lastExecutedCommand,
                            Model windowModel) {
                        VmInterfaceListModel model = getModel();

                        if (lastExecutedCommand == model.getNewCommand()
                                || lastExecutedCommand == model.getEditCommand()) {
                            return popupProvider.get();
                        } else {
                            return super.getModelPopup(source, lastExecutedCommand, windowModel);
                        }
                    }

                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(VmInterfaceListModel source,
                            UICommand lastExecutedCommand) {
                        if (lastExecutedCommand == getModel().getRemoveCommand()) {
                            return removeConfirmPopupProvider.get();
                        } else {
                            return super.getConfirmModelPopup(source, lastExecutedCommand);
                        }
                    }
                };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<AuditLog, VmListModel<Void>, VmEventListModel> getVmEventListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<EventPopupPresenterWidget> eventPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmEventListModel> modelProvider) {
        SearchableDetailTabModelProvider<AuditLog, VmListModel<Void>, VmEventListModel> result =
                new SearchableDetailTabModelProvider<AuditLog, VmListModel<Void>, VmEventListModel>(
                        eventBus, defaultConfirmPopupProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmEventListModel source,
                            UICommand lastExecutedCommand,
                            Model windowModel) {
                        if (lastExecutedCommand.equals(getModel().getDetailsCommand())) {
                            return eventPopupProvider.get();
                        } else {
                            return super.getModelPopup(source, lastExecutedCommand, windowModel);
                        }
                    }
                };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<Snapshot, VmListModel<Void>, VmSnapshotListModel> getVmSnapshotListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<VmSnapshotCreatePopupPresenterWidget> createPopupProvider,
            final Provider<VmClonePopupPresenterWidget> cloneVmPopupProvider,
            final Provider<VmMakeTemplatePopupPresenterWidget> cloneTemplatePopupProvider,
            final Provider<VmSnapshotPreviewPopupPresenterWidget> previewPopupProvider,
            final Provider<VmSnapshotCustomPreviewPopupPresenterWidget> customPreviewPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmSnapshotListModel> modelProvider) {
        SearchableDetailTabModelProvider<Snapshot, VmListModel<Void>, VmSnapshotListModel> result =
                new SearchableDetailTabModelProvider<Snapshot, VmListModel<Void>, VmSnapshotListModel>(
                        eventBus, defaultConfirmPopupProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmSnapshotListModel source,
                            UICommand lastExecutedCommand, Model windowModel) {
                        if (lastExecutedCommand == getModel().getNewCommand()) {
                            return createPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getCloneVmCommand()) {
                            getModel().setSystemTreeSelectedItem(this.getMainModel().getSystemTreeSelectedItem());
                            return cloneVmPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getPreviewCommand()) {
                            return previewPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getCustomPreviewCommand()) {
                            return customPreviewPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getCloneTemplateCommand()) {
                            return cloneTemplatePopupProvider.get();
                        } else {
                            return super.getModelPopup(source, lastExecutedCommand, windowModel);
                        }
                    }
                };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<AffinityGroup, VmListModel<Void>, VmAffinityGroupListModel> getAffinityGroupListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<AffinityGroupPopupPresenterWidget> popupProvider,
            final Provider<RemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmAffinityGroupListModel> modelProvider) {
        SearchableDetailTabModelProvider<AffinityGroup, VmListModel<Void>, VmAffinityGroupListModel> result =
                new SearchableDetailTabModelProvider<AffinityGroup, VmListModel<Void>, VmAffinityGroupListModel>(
                        eventBus, defaultConfirmPopupProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmAffinityGroupListModel source,
                            UICommand lastExecutedCommand,
                            Model windowModel) {
                        if (lastExecutedCommand == getModel().getNewCommand()
                                || lastExecutedCommand == getModel().getEditCommand()) {
                            return popupProvider.get();
                        } else {
                            return super.getModelPopup(source, lastExecutedCommand, windowModel);
                        }
                    }

                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(VmAffinityGroupListModel source,
                            UICommand lastExecutedCommand) {
                        if (lastExecutedCommand == getModel().getRemoveCommand()) {
                            return removeConfirmPopupProvider.get();
                        } else {
                            return super.getConfirmModelPopup(source, lastExecutedCommand);
                        }
                    }
                };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<HostDeviceView, VmListModel<Void>, VmHostDeviceListModel> getVmHostDeviceListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<AddVmHostDevicePopupPresenterWidget> addPopupProvider,
            final Provider<VmRepinHostPopupPresenterWidget> repinPopupProvider,
            final Provider<RemoveConfirmationPopupPresenterWidget> removeConfirmPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmHostDeviceListModel> modelProvider) {
        SearchableDetailTabModelProvider<HostDeviceView, VmListModel<Void>, VmHostDeviceListModel> result =
                new SearchableDetailTabModelProvider<HostDeviceView, VmListModel<Void>, VmHostDeviceListModel>(eventBus, defaultConfirmPopupProvider) {
                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmHostDeviceListModel source, UICommand lastExecutedCommand, Model windowModel) {
                        if (lastExecutedCommand == getModel().getAddCommand()) {
                            return addPopupProvider.get();
                        } else if (lastExecutedCommand == getModel().getRepinHostCommand()) {
                            return repinPopupProvider.get();
                        }
                        return super.getModelPopup(source, lastExecutedCommand, windowModel);
                    }

                    @Override
                    public AbstractModelBoundPopupPresenterWidget<? extends ConfirmationModel, ?> getConfirmModelPopup(VmHostDeviceListModel source, UICommand lastExecutedCommand) {
                        if (lastExecutedCommand == getModel().getRemoveCommand()) {
                            return removeConfirmPopupProvider.get();
                        }
                        return super.getConfirmModelPopup(source, lastExecutedCommand);
                    }
                };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);
        return result;
    }

    @Provides
    @Singleton
    public SearchableDetailModelProvider<Erratum, VmListModel<Void>, VmErrataListModel> getVmErrataListProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmErrataListModel> modelProvider) {

        SearchableDetailTabModelProvider<Erratum, VmListModel<Void>, VmErrataListModel> result =
                new SearchableDetailTabModelProvider<Erratum, VmListModel<Void>, VmErrataListModel>(
                        eventBus, defaultConfirmPopupProvider);
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);

        return result;
    }

    @Provides
    @Singleton
    public DetailTabModelProvider<VmListModel<Void>, VmErrataCountModel> getVmErrataCountProvider(EventBus eventBus,
            Provider<DefaultConfirmationPopupPresenterWidget> defaultConfirmPopupProvider,
            final Provider<VmListModel<Void>> mainModelProvider,
            final Provider<VmErrataListWithDetailsPopupPresenterWidget> errataPopupProvider,
            final Provider<VmErrataCountModel> modelProvider) {

        DetailTabModelProvider<VmListModel<Void>, VmErrataCountModel> result =
                new DetailTabModelProvider<VmListModel<Void>,
                VmErrataCountModel>(eventBus, defaultConfirmPopupProvider) {
            @Override
            public AbstractModelBoundPopupPresenterWidget<? extends Model, ?> getModelPopup(VmErrataCountModel source,
                    UICommand lastExecutedCommand,
                    Model windowModel) {

                return errataPopupProvider.get();
            }
        };
        result.setMainModelProvider(mainModelProvider);
        result.setModelProvider(modelProvider);

        return result;
    }


    @Override
    protected void configure() {
        bind(new TypeLiteral<VmListModel<Void>>() {}).in(Singleton.class);
        bind(VmGeneralModel.class).in(Singleton.class);
        bind(VmImportGeneralModel.class).in(Singleton.class);
        bind(VmGuestInfoModel.class).in(Singleton.class);
        bind(VmDiskListModel.class).in(Singleton.class);
        bind(VmInterfaceListModel.class).in(Singleton.class);
        bind(VmEventListModel.class).in(Singleton.class);
        bind(VmSnapshotListModel.class).in(Singleton.class);
        bind(VmAffinityGroupListModel.class).in(Singleton.class);
        bind(new TypeLiteral<VmAppListModel<VM>>() {}).in(Singleton.class);
        bind(VmHostDeviceListModel.class).in(Singleton.class);
        bind(new TypeLiteral<PermissionListModel<VM>>(){}).in(Singleton.class);
        bind(new TypeLiteral<VmDevicesListModel<VM>>() {}).in(Singleton.class);
        bind(VmErrataCountModel.class).in(Singleton.class);
        bind(VmErrataListModel.class).in(Singleton.class);



        // Form Detail Models
        bind(new TypeLiteral<DetailModelProvider<VmListModel<Void>, VmGeneralModel>>(){})
            .to(new TypeLiteral<DetailTabModelProvider<VmListModel<Void>, VmGeneralModel>>(){}).in(Singleton.class);
        bind(new TypeLiteral<DetailModelProvider<ImportVmsModel, VmImportGeneralModel>>(){})
            .to(new TypeLiteral<DetailTabModelProvider<ImportVmsModel, VmImportGeneralModel>>(){}).in(Singleton.class);
        bind(new TypeLiteral<DetailModelProvider<VmListModel<Void>, VmGuestInfoModel>>(){})
           .to(new TypeLiteral<DetailTabModelProvider<VmListModel<Void>, VmGuestInfoModel>>(){}).in(Singleton.class);
        // Search-able Detail Models
        bind(new TypeLiteral<SearchableDetailModelProvider<String, VmListModel<Void>, VmAppListModel<VM>>>(){})
           .to(new TypeLiteral<SearchableDetailTabModelProvider<String, VmListModel<Void>, VmAppListModel<VM>>>(){})
           .in(Singleton.class);
        // Permission Detail Model
        bind(new TypeLiteral<SearchableDetailModelProvider<Permission, VmListModel<Void>, PermissionListModel<VM>>>(){})
           .to(new TypeLiteral<PermissionModelProvider<VM, VmListModel<Void>>>() {}).in(Singleton.class);
        bind(new TypeLiteral<SearchableDetailModelProvider<VmDevice, VmListModel<Void>, VmDevicesListModel<VM>>>(){})
                .to(new TypeLiteral<SearchableDetailTabModelProvider<VmDevice, VmListModel<Void>, VmDevicesListModel<VM>>>(){})
                .in(Singleton.class);
    }

}
