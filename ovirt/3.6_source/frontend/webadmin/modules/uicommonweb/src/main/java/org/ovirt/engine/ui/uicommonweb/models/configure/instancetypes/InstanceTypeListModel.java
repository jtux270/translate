package org.ovirt.engine.ui.uicommonweb.models.configure.instancetypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.ovirt.engine.core.common.action.AddVmTemplateParameters;
import org.ovirt.engine.core.common.action.UpdateVmTemplateParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmTemplateParametersBase;
import org.ovirt.engine.core.common.businessentities.InstanceType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.VmWatchdogType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.utils.ObjectUtils;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.vm.HwOnlyCoreUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.MigrationOptionsUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.NameUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.UnitToGraphicsDeviceParamsBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.UsbPolicyUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.HasEntity;
import org.ovirt.engine.ui.uicommonweb.models.ListWithSimpleDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.BaseInterfaceCreatingManager;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModelNetworkAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmBasedWidgetSwitchModeCommand;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmModelBehaviorBase;
import org.ovirt.engine.ui.uicommonweb.models.vms.instancetypes.ExistingNonClusterModelBehavior;
import org.ovirt.engine.ui.uicommonweb.models.vms.instancetypes.InstanceTypeInterfaceCreatingManager;
import org.ovirt.engine.ui.uicommonweb.models.vms.instancetypes.NewInstanceTypeModelBehavior;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import com.google.inject.Inject;

public class InstanceTypeListModel extends ListWithSimpleDetailsModel<Void, InstanceType> {

    private final UICommand newInstanceTypeCommand;

    private final UICommand editInstanceTypeCommand;

    private final UICommand deleteInstanceTypeCommand;

    private final InstanceTypeInterfaceCreatingManager addInstanceTypeNetworkManager =
            new InstanceTypeInterfaceCreatingManager(new BaseInterfaceCreatingManager.PostVnicCreatedCallback() {
                @Override
                public void vnicCreated(Guid vmId, UnitVmModel model) {
                    getWindow().stopProgress();
                    cancel();
                    updateActionAvailability();
                }

                @Override
                public void queryFailed() {
                    getWindow().stopProgress();
                    cancel();
                }
            });

    @Inject
    public InstanceTypeListModel(final InstanceTypeGeneralModel instanceTypeGeneralModel) {
        setDetailList(instanceTypeGeneralModel);
        setDefaultSearchString("Instancetypes:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        this.newInstanceTypeCommand = new UICommand("NewInstanceType", this); //$NON-NLS-1$
        this.editInstanceTypeCommand = new UICommand("EditInstanceType", this); //$NON-NLS-1$
        this.deleteInstanceTypeCommand = new UICommand("DeleteInstanceType", this); //$NON-NLS-1$

        setSearchPageSize(1000);

        updateActionAvailability();
    }

    private void setDetailList(final InstanceTypeGeneralModel instanceTypeGeneralModel) {
        List<HasEntity<InstanceType>> list = new ArrayList<>();
        list.add(instanceTypeGeneralModel);
        setDetailModels(list);
    }

    @Override
    protected void syncSearch() {
        SearchParameters params = new SearchParameters(getSearchString(), SearchType.InstanceType, isCaseSensitiveSearch());
        params.setMaxCount(getSearchPageSize());
        super.syncSearch(VdcQueryType.Search, params);
    }

    private void newInstanceType() {
        createWindow(
                new NewInstanceTypeModelBehavior(),
                "new_instance_type", //$NON-NLS-1$
                "OnNewInstanceType", //$NON-NLS-1$
                true,
                ConstantsManager.getInstance().getConstants().newInstanceTypeTitle(),
                HelpTag.new_instance_type
        );
    }

    private void editInstanceType() {
        createWindow(
                new ExistingNonClusterModelBehavior((VmTemplate) getSelectedItem()),
                "edit_instance_type", //$NON-NLS-1$
                "OnEditInstanceType", //$NON-NLS-1$
                false,
                ConstantsManager.getInstance().getConstants().editInstanceTypeTitle(),
                HelpTag.edit_instance_type
        );
    }

    private void deleteInstanceType() {
        if (getWindow() != null) {
            return;
        }

        final ConfirmationModel window = new ConfirmationModel();
        setConfirmWindow(window);
        window.startProgress(null);

        window.setHelpTag(HelpTag.remove_instance_type);
        window.setHashName("remove_instance_type"); //$NON-NLS-1$

        final Guid instanceTypeId = getSelectedItem().getId();
        Frontend.getInstance().runQuery(VdcQueryType.GetVmsByInstanceTypeId,
                new IdQueryParameters(instanceTypeId), new AsyncQuery(this, new INewAsyncCallback() {
            @Override
            public void onSuccess(Object parentModel, Object returnValue) {
                List<VM> vmsAttachedToInstanceType = ((VdcQueryReturnValue) returnValue).getReturnValue();
                if (vmsAttachedToInstanceType == null || vmsAttachedToInstanceType.size() == 0) {
                    window.setTitle(ConstantsManager.getInstance().getConstants().removeInstanceTypeTitle());
                    window.setItems(Arrays.asList(getSelectedItem().getName()));
                } else {
                    List<String> attachedVmsNames = new ArrayList<String>();
                    for (VM vm : vmsAttachedToInstanceType) {
                        attachedVmsNames.add(vm.getName());
                    }

                    Collections.sort(attachedVmsNames);

                    window.setItems(attachedVmsNames);

                    window.getLatch().setIsAvailable(true);
                    window.getLatch().setIsChangeable(true);
                    window.setNote(ConstantsManager.getInstance().getConstants().vmsAttachedToInstanceTypeNote());

                    window.setMessage(ConstantsManager.getInstance().getConstants().vmsAttachedToInstanceTypeWarningMessage());
                }

                window.stopProgress();
            }
        }));



        UICommand tempVar = UICommand.createDefaultOkUiCommand("OnDeleteInstanceType", this); //$NON-NLS-1$
        window.getCommands().add(tempVar);
        UICommand tempVar2 = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        window.getCommands().add(tempVar2);
    }

    private void onNewInstanceType() {
        if (!((UnitVmModel) getWindow()).validateInstanceTypePart()) {
            return;
        }

        AsyncDataProvider.getInstance().isTemplateNameUnique(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        boolean isNameUnique = (Boolean) returnValue;
                        if (isNameUnique) {
                            postInstanceTypeNameUniqueCheck();
                        } else {
                            UnitVmModel VmModel = (UnitVmModel) getWindow();
                            VmModel.getInvalidityReasons().clear();
                            VmModel.getName()
                                    .getInvalidityReasons()
                                    .add(ConstantsManager.getInstance()
                                            .getConstants()
                                            .nameMustBeUniqueInvalidReason());
                            VmModel.getName().setIsValid(false);
                            VmModel.setIsValid(false);
                        }
                    }
                }), ((UnitVmModel) getWindow()).getName().getEntity(), null);
    }

    private void buildVmStatic(VmBase vmBase) {
        UnitVmModel model = (UnitVmModel) getWindow();
        BuilderExecutor.build(model, vmBase,
                new HwOnlyCoreUnitToVmBaseBuilder(),
                new NameUnitToVmBaseBuilder(),
                new UsbPolicyUnitToVmBaseBuilder(),
                new MigrationOptionsUnitToVmBaseBuilder()
        );

        // from CommonUnitToVmBaseBuilder
        vmBase.setAutoStartup(model.getIsHighlyAvailable().getEntity());
        vmBase.setPriority(model.getPriority().getSelectedItem().getEntity());
    }

    private void postInstanceTypeNameUniqueCheck() {
        UnitVmModel model = (UnitVmModel) getWindow();
        VM vm = new VM();
        buildVmStatic(vm.getStaticData());
        vm.setVmDescription(model.getDescription().getEntity());


        AddVmTemplateParameters addInstanceTypeParameters =
                new AddVmTemplateParameters(vm, model.getName().getEntity(), model.getDescription().getEntity());
        addInstanceTypeParameters.setTemplateType(VmEntityType.INSTANCE_TYPE);
        addInstanceTypeParameters.setVmTemplateId(null);
        addInstanceTypeParameters.setPublicUse(true);

        addInstanceTypeParameters.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
        addInstanceTypeParameters.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
        addInstanceTypeParameters.setBalloonEnabled(model.getMemoryBalloonDeviceEnabled().getEntity());
        addInstanceTypeParameters.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());

        setVmWatchdogToParams(model, addInstanceTypeParameters);
        setRngDeviceToParams(model, addInstanceTypeParameters);
        BuilderExecutor.build(model, addInstanceTypeParameters, new UnitToGraphicsDeviceParamsBuilder());

        getWindow().startProgress(null);

        Frontend.getInstance().runAction(
                VdcActionType.AddVmTemplate,
                addInstanceTypeParameters,
                new UnitVmModelNetworkAsyncCallback(model, addInstanceTypeNetworkManager),
                this
        );
    }

    private void onEditInstanceType() {
        UnitVmModel model = (UnitVmModel) getWindow();
        if (!model.validateInstanceTypePart()) {
            return;
        }

        VmTemplate instanceType = (VmTemplate) Cloner.clone(selectedItem);
        instanceType.setTemplateType(VmEntityType.INSTANCE_TYPE);
        buildVmStatic(instanceType);
        instanceType.setDescription(model.getDescription().getEntity());

        UpdateVmTemplateParameters updateInstanceTypeParameters =
                new UpdateVmTemplateParameters(instanceType);

        updateInstanceTypeParameters.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
        updateInstanceTypeParameters.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
        updateInstanceTypeParameters.setBalloonEnabled(model.getMemoryBalloonDeviceEnabled().getEntity());
        updateInstanceTypeParameters.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());

        setVmWatchdogToParams(model, updateInstanceTypeParameters);
        setRngDeviceToParams(model, updateInstanceTypeParameters);
        BuilderExecutor.build(model, updateInstanceTypeParameters, new UnitToGraphicsDeviceParamsBuilder());

        getWindow().startProgress(null);

        Frontend.getInstance().runAction(
                VdcActionType.UpdateVmTemplate,
                updateInstanceTypeParameters,
                new UnitVmModelNetworkAsyncCallback(model, addInstanceTypeNetworkManager, instanceType.getId()),
                this
        );
    }

    private void onDeleteInstanceType() {
        final ConfirmationModel model = (ConfirmationModel) getConfirmWindow();

        boolean latchChecked = !model.validate();

        if (model.getProgress() != null || latchChecked) {
            return;
        }

        model.startProgress(null);

        Guid instanceTypeId = getSelectedItem().getId();

        Frontend.getInstance().runAction(VdcActionType.RemoveVmTemplate, new VmTemplateParametersBase(instanceTypeId),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        model.stopProgress();
                        cancel();
                    }
                }, this);
    }

    private void createWindow(VmModelBehaviorBase<UnitVmModel> behavior, String hashName, String onOkAction, boolean isNew, String title, HelpTag helpTag) {
        if (getWindow() != null) {
            return;
        }

        UnitVmModel model = new UnitVmModel(behavior, this);
        model.setIsAdvancedModeLocalStorageKey("instance_type_dialog"); //$NON-NLS-1$
        setWindow(model);

        model.setTitle(title);
        model.setHelpTag(helpTag);
        model.setHashName(hashName); //$NON-NLS-1$
        model.setIsNew(isNew);

        model.initialize(null);

        VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
        switchModeCommand.init(model);
        model.getCommands().add(switchModeCommand);

        UICommand newTemplate = UICommand.createDefaultOkUiCommand(onOkAction, this);
        model.getCommands().add(newTemplate);

        UICommand cancel = UICommand.createCancelUiCommand("Cancel", this); //$NON-NLS-1$
        model.getCommands().add(cancel);
    }

    private void cancel() {
        setWindow(null);
        setConfirmWindow(null);
    }

    protected void updateActionAvailability() {
        int numOfSelectedItems = getSelectedItems() != null ? getSelectedItems().size() : 0;

        getEditInstanceTypeCommand().setIsExecutionAllowed(numOfSelectedItems == 1);
        getDeleteInstanceTypeCommand().setIsExecutionAllowed(numOfSelectedItems == 1);
    }

    @Override
    protected void selectedItemsChanged() {
        super.selectedItemsChanged();

        updateActionAvailability();
    }

    @Override
    protected String getListName() {
        return "InstanceTypeListModel"; //$NON-NLS-1$
    }

    @Override
    public void executeCommand(UICommand command) {
        super.executeCommand(command);

        if (command == getNewInstanceTypeCommand()) {
            newInstanceType();
        } else if (ObjectUtils.objectsEqual(command.getName(), "OnNewInstanceType")) { //$NON-NLS-1$
            onNewInstanceType();
        } else if (command == getEditInstanceTypeCommand()) {
            editInstanceType();
        } else if (ObjectUtils.objectsEqual(command.getName(), "OnEditInstanceType")) { //$NON-NLS-1$
            onEditInstanceType();
        } else if (command == getDeleteInstanceTypeCommand()) {
            deleteInstanceType();
        } else if (ObjectUtils.objectsEqual(command.getName(), "OnDeleteInstanceType")) { //$NON-NLS-1$
            onDeleteInstanceType();
        } else if (ObjectUtils.objectsEqual(command.getName(), "Cancel")) { //$NON-NLS-1$
            cancel();
        }
    }

    private void setVmWatchdogToParams(final UnitVmModel model, VmTemplateParametersBase updateVmParams) {
        VmWatchdogType wdModel = model.getWatchdogModel().getSelectedItem();
        updateVmParams.setUpdateWatchdog(true);
        if (wdModel != null) {
            VmWatchdog vmWatchdog = new VmWatchdog();
            vmWatchdog.setAction(model.getWatchdogAction().getSelectedItem());
            vmWatchdog.setModel(wdModel);
            updateVmParams.setWatchdog(vmWatchdog);
        }
    }

    private void setRngDeviceToParams(UnitVmModel model, VmTemplateParametersBase parameters) {
        parameters.setUpdateRngDevice(true);
        parameters.setRngDevice(model.getIsRngEnabled().getEntity() ? model.generateRngDevice() : null);
    }

    @Override
    public UICommand getEditCommand() {
        return getEditInstanceTypeCommand();
    }

    public UICommand getNewInstanceTypeCommand() {
        return newInstanceTypeCommand;
    }

    public UICommand getEditInstanceTypeCommand() {
        return editInstanceTypeCommand;
    }

    public UICommand getDeleteInstanceTypeCommand() {
        return deleteInstanceTypeCommand;
    }

}
