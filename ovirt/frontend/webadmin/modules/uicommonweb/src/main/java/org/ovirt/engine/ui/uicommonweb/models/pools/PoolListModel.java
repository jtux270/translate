package org.ovirt.engine.ui.uicommonweb.models.pools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ovirt.engine.core.common.VdcActionUtils;
import org.ovirt.engine.core.common.action.AddVmPoolWithVmsParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VmPoolParametersBase;
import org.ovirt.engine.core.common.businessentities.StoragePool;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmPool;
import org.ovirt.engine.core.common.businessentities.VmPoolType;
import org.ovirt.engine.core.common.businessentities.VmType;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.mode.ApplicationMode;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.SearchParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.StringHelper;
import org.ovirt.engine.core.searchbackend.SearchObjects;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.Cloner;
import org.ovirt.engine.ui.uicommonweb.Linq;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.builders.BuilderExecutor;
import org.ovirt.engine.ui.uicommonweb.builders.vm.CpuProfileUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.VmSpecificUnitToVmBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.CoreUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.DedicatedVmForVdsUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.KernelParamsUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.MigrationOptionsUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.NameUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.builders.vm.UsbPolicyUnitToVmBaseBuilder;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.help.HelpTag;
import org.ovirt.engine.ui.uicommonweb.models.ConfirmationModel;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ISupportSystemTreeContext;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.SystemTreeItemModel;
import org.ovirt.engine.ui.uicommonweb.models.configure.PermissionListModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.ExistingPoolModelBehavior;
import org.ovirt.engine.ui.uicommonweb.models.vms.NewPoolModelBehavior;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmBasedWidgetSwitchModeCommand;
import org.ovirt.engine.ui.uicommonweb.place.WebAdminApplicationPlaces;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendMultipleActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.IFrontendMultipleActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.ObservableCollection;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;
import org.ovirt.engine.ui.uicompat.UIConstants;

public class PoolListModel extends ListWithDetailsModel implements ISupportSystemTreeContext
{
    private final UIConstants constants = ConstantsManager.getInstance().getConstants();

    private UICommand privateNewCommand;

    public UICommand getNewCommand()
    {
        return privateNewCommand;
    }

    private void setNewCommand(UICommand value)
    {
        privateNewCommand = value;
    }

    private UICommand privateEditCommand;

    @Override
    public UICommand getEditCommand()
    {
        return privateEditCommand;
    }

    private void setEditCommand(UICommand value)
    {
        privateEditCommand = value;
    }

    private UICommand privateRemoveCommand;

    public UICommand getRemoveCommand()
    {
        return privateRemoveCommand;
    }

    private void setRemoveCommand(UICommand value)
    {
        privateRemoveCommand = value;
    }

    private SystemTreeItemModel systemTreeSelectedItem;

    @Override
    public SystemTreeItemModel getSystemTreeSelectedItem() {
        return systemTreeSelectedItem;
    }

    @Override
    public void setSystemTreeSelectedItem(SystemTreeItemModel value) {
        systemTreeSelectedItem = value;
    }

    protected Object[] getSelectedKeys()
    {
        if (getSelectedItems() == null)
        {
            return new Object[0];
        }
        else
        {
            Object[] keys = new Object[getSelectedItems().size()];
            for (int i = 0; i < getSelectedItems().size(); i++)
            {
                keys[i] = ((VmPool) getSelectedItems().get(i)).getVmPoolId();
            }
            return keys;
        }
    }

    public PoolListModel()
    {
        setTitle(ConstantsManager.getInstance().getConstants().poolsTitle());
        setApplicationPlace(WebAdminApplicationPlaces.poolMainTabPlace);

        setDefaultSearchString("Pools:"); //$NON-NLS-1$
        setSearchString(getDefaultSearchString());
        setSearchObjects(new String[] { SearchObjects.VDC_POOL_OBJ_NAME, SearchObjects.VDC_POOL_PLU_OBJ_NAME });
        setAvailableInModes(ApplicationMode.VirtOnly);

        setNewCommand(new UICommand("New", this)); //$NON-NLS-1$
        setEditCommand(new UICommand("Edit", this)); //$NON-NLS-1$
        setRemoveCommand(new UICommand("Remove", this)); //$NON-NLS-1$

        updateActionAvailability();

        getSearchNextPageCommand().setIsAvailable(true);
        getSearchPreviousPageCommand().setIsAvailable(true);
    }

    @Override
    protected void initDetailModels()
    {
        super.initDetailModels();

        ObservableCollection<EntityModel> list = new ObservableCollection<EntityModel>();
        list.add(new PoolGeneralModel());
        list.add(new PoolVmListModel());
        list.add(new PermissionListModel());
        setDetailModels(list);
    }

    @Override
    public boolean isSearchStringMatch(String searchString)
    {
        return searchString.trim().toLowerCase().startsWith("pool"); //$NON-NLS-1$
    }

    @Override
    protected void syncSearch()
    {
        SearchParameters tempVar = new SearchParameters(applySortOptions(getSearchString()), SearchType.VmPools, isCaseSensitiveSearch());
        tempVar.setMaxCount(getSearchPageSize());
        super.syncSearch(VdcQueryType.Search, tempVar);
    }

    @Override
    public boolean supportsServerSideSorting() {
        return true;
    }

    @Override
    public void search()
    {
        super.search();
    }

    public void newEntity()
    {
        if (getWindow() != null)
        {
            return;
        }

        PoolModel model = new PoolModel(new NewPoolModelBehavior());
        model.setIsNew(true);
        model.setCustomPropertiesKeysList(AsyncDataProvider.getCustomPropertiesList());
        model.setIsAdvancedModeLocalStorageKey("wa_pool_dialog");  //$NON-NLS-1$
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().newPoolTitle());
        model.setHelpTag(HelpTag.new_pool);
        model.setHashName("new_pool"); //$NON-NLS-1$
        model.getVmType().setSelectedItem(VmType.Desktop);
        model.initialize(getSystemTreeSelectedItem());

        VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
        switchModeCommand.init(model);
        model.getCommands().add(switchModeCommand);

        UICommand command = new UICommand("OnSave", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
        command.setIsDefault(true);
        model.getCommands().add(command);

        command = new UICommand("Cancel", this); //$NON-NLS-1$
        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        command.setIsCancel(true);
        model.getCommands().add(command);
    }

    public void edit()
    {
        final VmPool pool = (VmPool) getSelectedItem();

        if (getWindow() != null)
        {
            return;
        }

        final PoolListModel poolListModel = this;

        Frontend.getInstance().runQuery(VdcQueryType.GetVmDataByPoolId,
                new IdQueryParameters(pool.getVmPoolId()),

                new AsyncQuery(this, new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object modell, Object result) {
                        final VM vm = (VM) ((VdcQueryReturnValue) result).getReturnValue();

                        final ExistingPoolModelBehavior behavior = new ExistingPoolModelBehavior(vm);
                        behavior.getPoolModelBehaviorInitializedEvent().addListener(new IEventListener() {
                            @Override
                            public void eventRaised(Event ev, Object sender, EventArgs args) {
                                final PoolModel model = behavior.getModel();

                                for (EntityModel<VmPoolType> item : model.getPoolType().getItems())
                                {
                                    if (item.getEntity() == pool.getVmPoolType())
                                    {
                                        model.getPoolType().setSelectedItem(item);
                                        break;
                                    }
                                }
                                String cdImage = null;

                                if (vm != null) {
                                    model.getDataCenterWithClustersList().setSelectedItem(null);
                                    model.getDataCenterWithClustersList().setSelectedItem(Linq.firstOrDefault(model.getDataCenterWithClustersList()
                                            .getItems(),
                                            new Linq.DataCenterWithClusterPredicate(vm.getStoragePoolId(), vm.getVdsGroupId())));

                                    model.getTemplate().setIsChangable(false);
                                    cdImage = vm.getIsoPath();
                                    model.getVmType().setSelectedItem(vm.getVmType());
                                }
                                else
                                {
                                    model.getDataCenterWithClustersList()
                                            .setSelectedItem(Linq.firstOrDefault(model.getDataCenterWithClustersList().getItems()));
                                }

                                model.getDataCenterWithClustersList().setIsChangable(vm == null);

                                boolean hasCd = !StringHelper.isNullOrEmpty(cdImage);
                                model.getCdImage().setIsChangable(hasCd);
                                model.getCdAttached().setEntity(hasCd);
                                if (hasCd) {
                                    model.getCdImage().setSelectedItem(cdImage);
                                }

                                model.getProvisioning().setIsChangable(false);
                                model.getStorageDomain().setIsChangable(false);
                            }
                        });

                        PoolModel model = new PoolModel(behavior);
                        model.setCustomPropertiesKeysList(AsyncDataProvider.getCustomPropertiesList());
                        model.startProgress("");
                        model.setIsAdvancedModeLocalStorageKey("wa_pool_dialog");  //$NON-NLS-1$
                        setWindow(model);

                        VmBasedWidgetSwitchModeCommand switchModeCommand = new VmBasedWidgetSwitchModeCommand();
                        switchModeCommand.init(model);
                        model.getCommands().add(switchModeCommand);

                        UICommand command = new UICommand("OnSave", poolListModel); //$NON-NLS-1$
                        command.setTitle(ConstantsManager.getInstance().getConstants().ok());
                        command.setIsDefault(true);
                        model.getCommands().add(command);

                        command = new UICommand("Cancel", poolListModel); //$NON-NLS-1$
                        command.setTitle(ConstantsManager.getInstance().getConstants().cancel());
                        command.setIsCancel(true);
                        model.getCommands().add(command);

                        model.setTitle(ConstantsManager.getInstance().getConstants().editPoolTitle());
                        model.setHelpTag(HelpTag.edit_pool);
                        model.setHashName("edit_pool"); //$NON-NLS-1$
                        model.initialize(getSystemTreeSelectedItem());
                        model.getName().setEntity(pool.getName());
                        model.getDescription().setEntity(pool.getVmPoolDescription());
                        model.getComment().setEntity(pool.getComment());
                        model.getAssignedVms().setEntity(pool.getAssignedVmsCount());
                        model.getPrestartedVms().setEntity(pool.getPrestartedVms());
                        model.setPrestartedVmsHint("0-" + pool.getAssignedVmsCount()); //$NON-NLS-1$
                        model.getMaxAssignedVmsPerUser().setEntity(pool.getMaxAssignedVmsPerUser());

                    }
                }));
    }

    private List<StoragePool> asList(Object returnValue) {
        if (returnValue instanceof ArrayList) {
            return (ArrayList<StoragePool>) returnValue;
        }

        if (returnValue instanceof StoragePool) {
            List<StoragePool> res = new ArrayList<StoragePool>();
            res.add((StoragePool) returnValue);
            return res;
        }

        throw new IllegalArgumentException("Expected ArrayList of storage_pools or a storage_pool. Given " + returnValue.getClass().getName()); //$NON-NLS-1$
    }

    public void remove()
    {
        if (getWindow() != null)
        {
            return;
        }

        ConfirmationModel model = new ConfirmationModel();
        setWindow(model);
        model.setTitle(ConstantsManager.getInstance().getConstants().removePoolsTitle());
        model.setHelpTag(HelpTag.remove_pool);
        model.setHashName("remove_pool"); //$NON-NLS-1$

        ArrayList<String> list = new ArrayList<String>();
        for (VmPool item : Linq.<VmPool> cast(getSelectedItems()))
        {
            list.add(item.getName());
        }
        model.setItems(list);

        UICommand tempVar = new UICommand("OnRemove", this); //$NON-NLS-1$
        tempVar.setTitle(ConstantsManager.getInstance().getConstants().ok());
        tempVar.setIsDefault(true);
        model.getCommands().add(tempVar);
        UICommand tempVar2 = new UICommand("Cancel", this); //$NON-NLS-1$
        tempVar2.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        tempVar2.setIsCancel(true);
        model.getCommands().add(tempVar2);
    }

    public void onRemove()
    {
        ConfirmationModel model = (ConfirmationModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        ArrayList<VdcActionParametersBase> list = new ArrayList<VdcActionParametersBase>();
        for (Object item : getSelectedItems())
        {
            VmPool pool = (VmPool) item;
            list.add(new VmPoolParametersBase(pool.getVmPoolId()));
        }

        model.startProgress(null);

        Frontend.getInstance().runMultipleAction(VdcActionType.RemoveVmPool, list,
                new IFrontendMultipleActionAsyncCallback() {
                    @Override
                    public void executed(FrontendMultipleActionAsyncResult result) {

                        ConfirmationModel localModel = (ConfirmationModel) result.getState();
                        localModel.stopProgress();
                        cancel();

                    }
                }, model);
    }

    public void onSave()
    {
        final PoolModel model = (PoolModel) getWindow();

        if (model.getProgress() != null)
        {
            return;
        }

        if (!model.getIsNew() && getSelectedItem() == null)
        {
            cancel();
            return;
        }

        if (!model.validate())
        {
            return;
        }

        final VmPool pool = model.getIsNew() ? new VmPool() : (VmPool) Cloner.clone(getSelectedItem());

        final String name = model.getName().getEntity();

        // Check name unicitate.
        AsyncDataProvider.isPoolNameUnique(new AsyncQuery(this,
                new INewAsyncCallback() {
                    @Override
                    public void onSuccess(Object target, Object returnValue) {
                        Boolean isUnique = (Boolean) returnValue;

                        if ((model.getIsNew() && !isUnique)
                                || (!model.getIsNew() && !isUnique && name.compareToIgnoreCase(pool.getName()) != 0)) {
                            model.getName()
                                    .getInvalidityReasons()
                                    .add(ConstantsManager.getInstance().getConstants().nameMustBeUniqueInvalidReason());
                            model.getName().setIsValid(false);
                            model.setIsGeneralTabValid(false);
                            return;
                        }

                        // Save changes.
                        pool.setName(model.getName().getEntity());
                        pool.setVmPoolDescription(model.getDescription().getEntity());
                        pool.setVdsGroupId(model.getSelectedCluster().getId());
                        pool.setComment(model.getComment().getEntity());
                        pool.setPrestartedVms(model.getPrestartedVms().getEntity());
                        pool.setMaxAssignedVmsPerUser(model.getMaxAssignedVmsPerUser().getEntity());

                        EntityModel<VmPoolType> poolTypeSelectedItem = model.getPoolType().getSelectedItem();
                        pool.setVmPoolType(poolTypeSelectedItem.getEntity());

                        if (model.getSpiceProxyEnabled().getEntity()) {
                            pool.setSpiceProxy(model.getSpiceProxy().getEntity());
                        }


                        VM vm = buildVmOnSave(model);
                        vm.setVmInit(model.getVmInitModel().buildCloudInitParameters(model));
                        vm.setBalloonEnabled(model.getMemoryBalloonDeviceEnabled().getEntity());

                        vm.setUseLatestVersion(constants.latestTemplateVersionName().equals(model.getTemplate().getSelectedItem().getTemplateVersionName()));
                        vm.setStateless(false);
                        vm.setInstanceTypeId(model.getInstanceTypes().getSelectedItem().getId());

                        AddVmPoolWithVmsParameters param =
                                new AddVmPoolWithVmsParameters(pool, vm, model.getNumOfDesktops().getEntity(), 0);

                        param.setStorageDomainId(Guid.Empty);
                        param.setDiskInfoDestinationMap(model.getDisksAllocationModel()
                                                                .getImageToDestinationDomainMap());
                        param.setConsoleEnabled(model.getIsConsoleDeviceEnabled().getEntity());
                        param.setVirtioScsiEnabled(model.getIsVirtioScsiEnabled().getEntity());

                        param.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
                        param.setRngDevice(model.getIsRngEnabled().getEntity() ? model.generateRngDevice() : null);

                        param.setSoundDeviceEnabled(model.getIsSoundcardEnabled().getEntity());
                        param.setBalloonEnabled(model.getMemoryBalloonDeviceEnabled().getEntity());

                        if (model.getQuota().getSelectedItem() != null) {
                            vm.setQuotaId(model.getQuota().getSelectedItem().getId());
                        }

                        model.startProgress(null);

                        if (model.getIsNew())
                        {
                            Frontend.getInstance().runMultipleAction(VdcActionType.AddVmPoolWithVms,
                                    new ArrayList<VdcActionParametersBase>(Arrays.asList(new VdcActionParametersBase[] { param })),
                                    new IFrontendMultipleActionAsyncCallback() {
                                        @Override
                                        public void executed(FrontendMultipleActionAsyncResult result) {
                                            cancel();
                                            stopProgress();
                                        }
                                    },
                                    this);
                        }
                        else
                        {
                            Frontend.getInstance().runMultipleAction(VdcActionType.UpdateVmPoolWithVms,
                                    new ArrayList<VdcActionParametersBase>(Arrays.asList(new VdcActionParametersBase[] { param })),
                                    new IFrontendMultipleActionAsyncCallback() {
                                        @Override
                                        public void executed(FrontendMultipleActionAsyncResult result) {
                                            cancel();
                                            stopProgress();
                                        }
                                    },
                                    this);
                        }

                    }
                }),
                name);
    }

    protected static VM buildVmOnSave(PoolModel model) {
        VM vm = new VM();
        BuilderExecutor.build(model, vm.getStaticData(),
                              new NameUnitToVmBaseBuilder(),
                              new CoreUnitToVmBaseBuilder(),
                              new KernelParamsUnitToVmBaseBuilder(),
                              new MigrationOptionsUnitToVmBaseBuilder(),
                              new DedicatedVmForVdsUnitToVmBaseBuilder(),
                              new UsbPolicyUnitToVmBaseBuilder(),
                              new CpuProfileUnitToVmBaseBuilder());
        BuilderExecutor.build(model, vm, new VmSpecificUnitToVmBuilder());
        return vm;
    }

    public void cancel()
    {
        setWindow(null);
    }

    @Override
    protected void onSelectedItemChanged()
    {
        super.onSelectedItemChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemsChanged()
    {
        super.selectedItemsChanged();
        updateActionAvailability();
    }

    @Override
    protected void selectedItemPropertyChanged(Object sender, PropertyChangedEventArgs e)
    {
        super.selectedItemPropertyChanged(sender, e);
        updateActionAvailability();
    }

    private void updateActionAvailability()
    {
        getEditCommand().setIsExecutionAllowed(getSelectedItem() != null && getSelectedItems() != null
                && getSelectedItems().size() == 1 && hasVms(getSelectedItem()));

        getRemoveCommand().setIsExecutionAllowed(getSelectedItems() != null && getSelectedItems().size() > 0
                && VdcActionUtils.canExecute(getSelectedItems(), VmPool.class, VdcActionType.RemoveVmPool));
    }

    private boolean hasVms(Object selectedItem) {
        if (selectedItem instanceof VmPool) {
            return ((VmPool) selectedItem).getAssignedVmsCount() != 0;
        }
        return false;
    }

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (command == getNewCommand())
        {
            newEntity();
        }
        if (command == getEditCommand())
        {
            edit();
        }
        if (command == getRemoveCommand())
        {
            remove();
        }
        if ("Cancel".equals(command.getName())) //$NON-NLS-1$
        {
            cancel();
        }
        if ("OnSave".equals(command.getName())) //$NON-NLS-1$
        {
            onSave();
        }
        if ("OnRemove".equals(command.getName())) //$NON-NLS-1$
        {
            onRemove();
        }
    }

    @Override
    protected String getListName() {
        return "PoolListModel"; //$NON-NLS-1$
    }

}
