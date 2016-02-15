package org.ovirt.engine.ui.uicommonweb.models.vms;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VnicProfileView;
import org.ovirt.engine.core.common.queries.ConfigurationValues;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.ui.frontend.AsyncQuery;
import org.ovirt.engine.ui.frontend.Frontend;
import org.ovirt.engine.ui.frontend.INewAsyncCallback;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.dataprovider.AsyncDataProvider;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicommonweb.models.ListModel;
import org.ovirt.engine.ui.uicommonweb.models.Model;
import org.ovirt.engine.ui.uicommonweb.validation.IValidation;
import org.ovirt.engine.ui.uicommonweb.validation.MacAddressValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NoSpecialCharactersWithDotValidation;
import org.ovirt.engine.ui.uicommonweb.validation.NotEmptyValidation;
import org.ovirt.engine.ui.uicompat.ConstantsManager;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.FrontendActionAsyncResult;
import org.ovirt.engine.ui.uicompat.IFrontendActionAsyncCallback;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

@SuppressWarnings("unused")
public abstract class VmInterfaceModel extends Model
{
    protected static final String ON_SAVE_COMMAND = "OnSave"; //$NON-NLS-1$
    protected static String ENGINE_NETWORK_NAME;

    private EntityModel<String> privateName;
    private ListModel<VnicProfileView> privateProfile;
    private EntityModel<Boolean> linked;
    private EntityModel<Boolean> linked_IsSelected;
    private EntityModel<Boolean> unlinked_IsSelected;
    private ListModel<VmInterfaceType> privateNicType;
    private EntityModel<String> privateMAC;
    private EntityModel<Boolean> enableMac;
    private EntityModel<Boolean> plugged;
    private EntityModel<Boolean> plugged_IsSelected;
    private EntityModel<Boolean> unplugged_IsSelected;

    protected final boolean hotPlugSupported;
    protected final boolean hotUpdateSupported;
    private final VmBase vm;
    private final ArrayList<VmNetworkInterface> vmNicList;
    private final VMStatus vmStatus;

    private UICommand okCommand;

    private final EntityModel sourceModel;
    private final Version clusterCompatibilityVersion;

    private ProfileBehavior profileBehavior;

    private Guid dcId;

    protected VmInterfaceModel(VmBase vm,
            VMStatus vmStatus,
            Guid dcId,
            Version clusterCompatibilityVersion,
            ArrayList<VmNetworkInterface> vmNicList,
            EntityModel sourceModel,
            ProfileBehavior profileBehavior)
    {
        this.dcId = dcId;
        this.profileBehavior = profileBehavior;
        // get management network name
        ENGINE_NETWORK_NAME =
                (String) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.ManagementNetwork);

        this.vm = vm;
        this.vmNicList = vmNicList;
        this.vmStatus = vmStatus;
        this.sourceModel = sourceModel;
        this.clusterCompatibilityVersion = clusterCompatibilityVersion;

        hotPlugSupported =
                AsyncDataProvider.getNicHotplugSupport(vm.getOsId(),
                        clusterCompatibilityVersion);

        hotUpdateSupported =
                (Boolean) AsyncDataProvider.getConfigValuePreConverted(ConfigurationValues.NetworkLinkingSupported,
                        clusterCompatibilityVersion.toString());

        setName(new EntityModel<String>());
        setProfile(new ListModel<VnicProfileView>() {
            @Override
            public void setSelectedItem(VnicProfileView value) {
                super.setSelectedItem(value);
                updateLinkChangability();
            }
        });
        setNicType(new ListModel<VmInterfaceType>());
        setMAC(new EntityModel<String>());
        setEnableMac(new EntityModel<Boolean>() {
            @Override
            public void setEntity(Boolean enableManualMac) {
                super.setEntity(enableManualMac);
                getMAC().setIsChangable(enableManualMac);
            }
        });
        getEnableMac().setEntity(false);
        getMAC().getPropertyChangedEvent().addListener(this);

        setLinked(new EntityModel<Boolean>());
        getLinked().getPropertyChangedEvent().addListener(this);

        setLinked_IsSelected(new EntityModel<Boolean>());
        getLinked_IsSelected().getEntityChangedEvent().addListener(this);

        setUnlinked_IsSelected(new EntityModel<Boolean>());
        getUnlinked_IsSelected().getEntityChangedEvent().addListener(this);

        setPlugged(new EntityModel<Boolean>());
        getPlugged().getPropertyChangedEvent().addListener(this);

        setPlugged_IsSelected(new EntityModel<Boolean>());
        getPlugged_IsSelected().getEntityChangedEvent().addListener(this);

        setUnplugged_IsSelected(new EntityModel<Boolean>());
        getUnplugged_IsSelected().getEntityChangedEvent().addListener(this);

    }

    protected abstract void init();

    public EntityModel getSourceModel() {
        return sourceModel;
    }

    public VmBase getVm() {
        return vm;
    }

    public ArrayList<VmNetworkInterface> getVmNicList() {
        return vmNicList;
    }

    public VMStatus getVmStatus() {
        return vmStatus;
    }

    /**
     * The user may also plug and unplug interfaces when the VM is down (regardless of hotplug support)
     * or create an unplugged NIC in a running VM when there isn't support for hotplug.
     * @return an boolean.
     */
    public boolean allowPlug() {
        return hotPlugSupported || getVmStatus().equals(VMStatus.Down);
    }

    public Version getClusterCompatibilityVersion() {
        return clusterCompatibilityVersion;
    }

    public EntityModel<String> getName()
    {
        return privateName;
    }

    private void setName(EntityModel<String> value)
    {
        privateName = value;
    }

    public ListModel<VnicProfileView> getProfile()
    {
        return privateProfile;
    }

    private void setProfile(ListModel<VnicProfileView> value)
    {
        privateProfile = value;
    }

    public EntityModel<Boolean> getLinked()
    {
        return linked;
    }

    private void setLinked(EntityModel<Boolean> value)
    {
        linked = value;
    }

    public EntityModel<Boolean> getLinked_IsSelected()
    {
        return linked_IsSelected;
    }

    public void setLinked_IsSelected(EntityModel<Boolean> value)
    {
        linked_IsSelected = value;
    }

    public EntityModel<Boolean> getUnlinked_IsSelected()
    {
        return unlinked_IsSelected;
    }

    public void setUnlinked_IsSelected(EntityModel<Boolean> value)
    {
        unlinked_IsSelected = value;
    }

    public ListModel<VmInterfaceType> getNicType()
    {
        return privateNicType;
    }

    private void setNicType(ListModel<VmInterfaceType> value)
    {
        privateNicType = value;
    }

    public EntityModel<String> getMAC()
    {
        return privateMAC;
    }

    private void setMAC(EntityModel<String> value)
    {
        privateMAC = value;
    }

    public EntityModel<Boolean> getEnableMac()
    {
        return enableMac;
    }

    private void setEnableMac(EntityModel<Boolean> value)
    {
        enableMac = value;
    }

    public EntityModel<Boolean> getPlugged()
    {
        return plugged;
    }

    private void setPlugged(EntityModel<Boolean> value)
    {
        plugged = value;
    }

    public EntityModel<Boolean> getPlugged_IsSelected()
    {
        return plugged_IsSelected;
    }

    public void setPlugged_IsSelected(EntityModel<Boolean> value)
    {
        plugged_IsSelected = value;
    }

    public EntityModel<Boolean> getUnplugged_IsSelected()
    {
        return unplugged_IsSelected;
    }

    public void setUnplugged_IsSelected(EntityModel<Boolean> value)
    {
        unplugged_IsSelected = value;
    }

    @Override
    public void eventRaised(Event ev, Object sender, EventArgs args)
    {
        super.eventRaised(ev, sender, args);

        if (sender == getMAC())
        {
            mAC_PropertyChanged((PropertyChangedEventArgs) args);
        }

        else if (sender == getPlugged())
        {
            PropertyChangedEventArgs propArgs = (PropertyChangedEventArgs) args;
            if (propArgs.propertyName.equals("Entity")) { //$NON-NLS-1$
                boolean plugged = getPlugged().getEntity();
                getPlugged_IsSelected().setEntity(plugged);
                getUnplugged_IsSelected().setEntity(!plugged);
            } else if (propArgs.propertyName.equals("IsChangable")) { //$NON-NLS-1$

                boolean isPlugChangeable = getPlugged().getIsChangable();

                getPlugged_IsSelected().setChangeProhibitionReason(getLinked().getChangeProhibitionReason());
                getPlugged_IsSelected().setIsChangable(isPlugChangeable);

                getUnplugged_IsSelected().setChangeProhibitionReason(getLinked().getChangeProhibitionReason());
                getUnplugged_IsSelected().setIsChangable(isPlugChangeable);
            } else if (propArgs.propertyName.equals("IsAvailable")) { //$NON-NLS-1$
                boolean isPlugAvailable = getPlugged().getIsAvailable();
                getPlugged_IsSelected().setIsAvailable(isPlugAvailable);
                getUnplugged_IsSelected().setIsAvailable(isPlugAvailable);
            }
        }
        else if (sender == getPlugged_IsSelected())
        {
            if (getPlugged_IsSelected().getEntity()) {
                getPlugged().setEntity(true);
            }
        }
        else if (sender == getUnplugged_IsSelected())
        {
            if (getUnplugged_IsSelected().getEntity()) {
                getPlugged().setEntity(false);
            }
        }

        else if (sender == getLinked())
        {
            PropertyChangedEventArgs propArgs = (PropertyChangedEventArgs) args;
            if (propArgs.propertyName.equals("Entity")) { //$NON-NLS-1$
                boolean linked = getLinked().getEntity();
                getLinked_IsSelected().setEntity(linked);
                getUnlinked_IsSelected().setEntity(!linked);
            } else if (propArgs.propertyName.equals("IsChangable")) { //$NON-NLS-1$
                boolean isLinkedChangeable = getLinked().getIsChangable();

                getLinked_IsSelected().setChangeProhibitionReason(getChangeProhibitionReason());
                getLinked_IsSelected().setIsChangable(isLinkedChangeable);

                getUnlinked_IsSelected().setChangeProhibitionReason(getLinked().getChangeProhibitionReason());
                getUnlinked_IsSelected().setIsChangable(isLinkedChangeable);
            } else if (propArgs.propertyName.equals("IsAvailable")) { //$NON-NLS-1$
                boolean isLinkedAvailable = getLinked().getIsAvailable();
                getLinked_IsSelected().setIsAvailable(isLinkedAvailable);
                getUnlinked_IsSelected().setIsAvailable(isLinkedAvailable);
            }
        }
        else if (sender == getLinked_IsSelected())
        {
            if (getLinked_IsSelected().getEntity()) {
                getLinked().setEntity(true);
            }
        }
        else if (sender == getUnlinked_IsSelected())
        {
            if (getUnlinked_IsSelected().getEntity()) {
                getLinked().setEntity(false);
            }
        }
    }

    private void mAC_PropertyChanged(PropertyChangedEventArgs e)
    {
        if (e.propertyName.equals("IsChangeAllowed") && !getMAC().getIsChangable()) //$NON-NLS-1$
        {
            getMAC().setIsValid(true);
        }
    }

    public boolean validate()
    {
        getName().validateEntity(new IValidation[] { new NotEmptyValidation(), new NoSpecialCharactersWithDotValidation() });

        getNicType().validateSelectedItem(new IValidation[] { new NotEmptyValidation() });

        getMAC().setIsValid(true);
        if (getMAC().getIsChangable())
        {
            getMAC().validateEntity(new IValidation[] { new NotEmptyValidation(), new MacAddressValidation() });
        }

        return getName().getIsValid() && getNicType().getIsValid()
                && getMAC().getIsValid();
    }

    protected abstract VmNetworkInterface createBaseNic();

    protected void onSave()
    {
        VmNetworkInterface nic = createBaseNic();

        if (getProgress() != null)
        {
            return;
        }

        if (!validate())
        {
            return;
        }

        // Save changes.
        nic.setName(getName().getEntity());
        VnicProfileView profile = getProfile().getSelectedItem();
        nic.setVnicProfileId(profile.getId());
        nic.setNetworkName(profile.getNetworkName());
        nic.setLinked(getLinked().getEntity());
        if (getNicType().getSelectedItem() == null)
        {
            nic.setType(null);
        }
        else
        {
            nic.setType(getNicType().getSelectedItem().getValue());
        }
        onSaveMAC(nic);

        nic.setPlugged(getPlugged().getEntity());

        startProgress(null);

        Frontend.getInstance().runAction(getVdcActionType(),
                createVdcActionParameters(nic),
                new IFrontendActionAsyncCallback() {
                    @Override
                    public void executed(FrontendActionAsyncResult result) {
                        VdcReturnValueBase returnValue = result.getReturnValue();
                        stopProgress();

                        if (returnValue != null && returnValue.getSucceeded())
                        {
                            cancel();
                            postOnSave();
                        }
                    }
                },
                this);
    }

    protected void postOnSave()
    {
        // Do nothing
    }

    protected void cancel()
    {
        sourceModel.setWindow(null);
    }

    protected abstract String getDefaultMacAddress();

    protected abstract VdcActionType getVdcActionType();

    protected void initProfiles() {
        AsyncQuery _asyncQuery = new AsyncQuery();
        _asyncQuery.asyncCallback = new INewAsyncCallback() {
            @Override
            public void onSuccess(Object model1, Object result1)
            {
                getProfile().setItems((List<VnicProfileView>) result1);
                profileBehavior.initSelectedProfile(getProfile(), getNic());
                updateProfileChangability();

                // fetch completed
                okCommand.setIsExecutionAllowed(true);
            }
        };

        profileBehavior.initProfiles(hotUpdateSupported, getVm().getVdsGroupId(), dcId, _asyncQuery);
    }

    protected void initCommands() {
        okCommand = new UICommand(ON_SAVE_COMMAND, this);
        okCommand.setTitle(ConstantsManager.getInstance().getConstants().ok());
        okCommand.setIsDefault(true);
        // wait for data to fetch
        okCommand.setIsExecutionAllowed(false);
        getCommands().add(okCommand);
        UICommand cancelCommand = new UICommand(CANCEL_COMMAND, this);
        cancelCommand.setTitle(ConstantsManager.getInstance().getConstants().cancel());
        cancelCommand.setIsCancel(true);
        getCommands().add(cancelCommand);
    }

    protected abstract VmNetworkInterface getNic();

    protected abstract void initSelectedType();

    @Override
    public void executeCommand(UICommand command)
    {
        super.executeCommand(command);

        if (ON_SAVE_COMMAND.equals(command.getName()))
        {
            onSave();
        }
        else if (CANCEL_COMMAND.equals(command.getName()))
        {
            cancel();
        }
    }

    protected abstract void initMAC();

    protected abstract void initLinked();

    protected void onSaveMAC(VmNetworkInterface nicToSave) {
        nicToSave.setMacAddress(getMAC().getIsChangable() ? (getMAC().getEntity() == null ? null
                : getMAC().getEntity().toLowerCase()) : getDefaultMacAddress());
    }

    protected abstract VdcActionParametersBase createVdcActionParameters(VmNetworkInterface nicToSave);

    protected void updateLinkChangability() {
        boolean isNullProfileSelected = getProfile().getSelectedItem() == null;

        if (isNullProfileSelected) {
            getLinked().setIsChangable(false);
            return;
        }
        if (!hotUpdateSupported) {
            getLinked().setIsChangable(false);
            return;
        }
        getLinked().setIsChangable(true);
    }

    protected void updateProfileChangability() {
        getProfile().setIsChangable(true);
    }

    protected boolean selectedNetworkExternal() {

        VnicProfileView profile = getProfile().getSelectedItem();
        Network network = null;

        if (profile != null && profile.getId() != null) {
            network = getProfileBehavior().findNetworkById(profile.getId());
        }
        return network != null && network.isExternal();
    }

    public ProfileBehavior getProfileBehavior() {
        return profileBehavior;
    }
}
