package org.ovirt.engine.core.bll.network.vm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.ValidationResult;
import org.ovirt.engine.core.bll.network.ExternalNetworkManager;
import org.ovirt.engine.core.bll.network.MacPoolManager;
import org.ovirt.engine.core.bll.network.cluster.NetworkHelper;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.VmNicValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddVmInterfaceParameters;
import org.ovirt.engine.core.common.action.PlugAction;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.validation.group.UpdateVmNic;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VmNicDeviceVDSParameters;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.utils.linq.LinqUtils;
import org.ovirt.engine.core.utils.linq.Predicate;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute(forceCompensation = true)
public class UpdateVmInterfaceCommand<T extends AddVmInterfaceParameters> extends AbstractVmInterfaceCommand<T> {

    private VmNic oldIface;
    private VmDevice oldVmDevice;
    private boolean macShouldBeChanged;
    private RequiredAction requiredAction = null;

    public UpdateVmInterfaceCommand(T parameters) {
        super(parameters);
        setVmId(parameters.getVmId());
    }

    private RequiredAction getRequiredAction() {
        if (requiredAction == null) {
            if (!oldVmDevice.getIsPlugged() && getInterface().isPlugged()) {
                requiredAction = RequiredAction.PLUG;
            } else if (oldVmDevice.getIsPlugged() && !getInterface().isPlugged()) {
                requiredAction = RequiredAction.UNPLUG;
            } else if (liveActionRequired() && propertiesRequiringVmUpdateDeviceWereUpdated()) {
                requiredAction = RequiredAction.UPDATE_VM_DEVICE;
            }
        }

        return requiredAction;
    }

    private boolean liveActionRequired() {
        return oldVmDevice.getIsPlugged() && getInterface().isPlugged() && getVm().getStatus() == VMStatus.Up;
    }

    @Override
    protected void executeVmCommand() {
        addCustomValue("InterfaceType",
                (VmInterfaceType.forValue(getInterface().getType()).getDescription()).toString());

        boolean succeeded = false;
        boolean macAddedToPool = false;
        try {
            if (isVnicProfileChanged(oldIface, getInterface())) {
                Network newNetwork = NetworkHelper.getNetworkByVnicProfileId(getInterface().getVnicProfileId());
                Network oldNetwork = NetworkHelper.getNetworkByVnicProfileId(oldIface.getVnicProfileId());
                if (ObjectUtils.notEqual(oldNetwork, newNetwork)) {
                    new ExternalNetworkManager(oldIface).deallocateIfExternal();
                }
            }

            if (macShouldBeChanged) {
                macAddedToPool = addMacToPool(getMacAddress());
            }

            if (mustChangeAddress(oldIface.getType(), getInterface().getType())) {
                getVmDeviceDao().clearDeviceAddress(getInterface().getId());
            }

            getInterface().setSpeed(VmInterfaceType.forValue(getInterface().getType()).getSpeed());

            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    getCompensationContext().snapshotEntity(oldIface);
                    getVmNicDao().update(getInterface());
                    getCompensationContext().stateChanged();
                    return null;
                }
            });

            succeeded = updateHost();
        } finally {
            setSucceeded(succeeded);
            if (macAddedToPool) {
                if (succeeded) {
                    MacPoolManager.getInstance().freeMac(oldIface.getMacAddress());
                } else {
                    MacPoolManager.getInstance().freeMac(getMacAddress());
                }
            }
        }
    }

    private boolean updateHost() {
        if (getVm().getStatus() == VMStatus.Up) {
            setVdsId(getVm().getRunOnVds());
        }

        if (getRequiredAction() != null){
            switch (getRequiredAction()) {
            case PLUG: {
                return activateOrDeactivateExistingNic(getInterface(), PlugAction.PLUG);
            }
            case UNPLUG: {
                return activateOrDeactivateExistingNic(oldIface, PlugAction.UNPLUG);
            }
            case UPDATE_VM_DEVICE: {
                runVdsCommand(VDSCommandType.UpdateVmInterface,
                        new VmNicDeviceVDSParameters(getVdsId(),
                                getVm(),
                                getVmNicDao().get(getInterface().getId()),
                                oldVmDevice));
                break;
            }
            }
        }
        return true;
    }

    private boolean propertiesRequiringVmUpdateDeviceWereUpdated() {
        return !ObjectUtils.equals(oldIface.getVnicProfileId(), getInterface().getVnicProfileId())
                || oldIface.isLinked() != getInterface().isLinked();
    }

    @Override
    protected boolean canDoAction() {
        if (getVm() == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_FOUND);
            return false;
        }

        if (getVm() == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST);
            return false;
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (!validate(vmStatusLegal(getVm().getStatus()))) {
            return false;
        }

        oldVmDevice = getVmDeviceDao().get(new VmDeviceId(getInterface().getId(), getVmId()));
        List<VmNic> interfaces = getVmNicDao().getAllForVm(getVmId());
        oldIface = LinqUtils.firstOrNull(interfaces, new Predicate<VmNic>() {
            @Override
            public boolean eval(VmNic i) {
                return i.getId().equals(getInterface().getId());
            }
        });

        if (oldIface == null || oldVmDevice == null) {
            addCanDoActionMessage(VdcBllMessages.VM_INTERFACE_NOT_EXIST);
            return false;
        }

        if (!updateVnicForBackwardCompatibility(oldIface)) {
            return false;
        }

        if (!StringUtils.equals(oldIface.getName(), getInterfaceName()) && !uniqueInterfaceName(interfaces)) {
            return false;
        }

        // check that not exceeded PCI and IDE limit
        List<VmNic> allInterfaces = new ArrayList<>(interfaces);
        allInterfaces.remove(oldIface);
        allInterfaces.add(getInterface());

        if (!pciAndIdeWithinLimit(getVm(), allInterfaces)) {
            return false;
        }

        if (!validate(vmTemplateEmpty())) {
            return false;
        }

        UpdateVmNicValidator nicValidator =
                new UpdateVmNicValidator(getInterface(), getVm().getVdsGroupCompatibilityVersion(), getVm().getOs());
        if (!validate(nicValidator.unplugPlugNotRequired())
                || !validate(nicValidator.linkedCorrectly())
                || !validate(nicValidator.isCompatibleWithOs())
                || !validate(nicValidator.emptyNetworkValid())
                || !validate(nicValidator.hotUpdatePossible())
                || !validate(nicValidator.profileValid(getVm().getVdsGroupId()))
                || !validate(nicValidator.canVnicWithExternalNetworkBePlugged())) {
            return false;
        }

        Network network = NetworkHelper.getNetworkByVnicProfileId(getInterface().getVnicProfileId());
        if (getRequiredAction() == RequiredAction.UPDATE_VM_DEVICE) {
            Network oldNetwork = NetworkHelper.getNetworkByVnicProfileId(oldIface.getVnicProfileId());
            if (!validate(nicValidator.hotUpdateDoneWithInternalNetwork(oldNetwork, network))
                    || !validate(nicValidator.networkExistsOnHost(network))) {
                return false;
            }
        }

        macShouldBeChanged = !StringUtils.equals(oldIface.getMacAddress(), getMacAddress());
        if (macShouldBeChanged && !validate(macAvailable())) {
            return false;
        }

        return true;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(UpdateVmNic.class);
        return super.getValidationGroups();
    }

    /**
     * Set the parameters for bll messages, such as type and action,
     */
    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__UPDATE);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (getSucceeded()) {
            if (oldIface.isLinked() != getInterface().isLinked()) {
                AuditLogType customValue =
                        getInterface().isLinked() ? AuditLogType.NETWORK_UPDATE_VM_INTERFACE_LINK_UP
                                : AuditLogType.NETWORK_UPDATE_VM_INTERFACE_LINK_DOWN;
                addCustomValue("LinkState", AuditLogDirector.getMessage(customValue));
            } else {
                addCustomValue("LinkState", " ");
            }
            return AuditLogType.NETWORK_UPDATE_VM_INTERFACE;
        }

        return AuditLogType.NETWORK_UPDATE_VM_INTERFACE_FAILED;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = super.getPermissionCheckSubjects();

        if (getInterface() != null && getInterface().getVnicProfileId() != null && getVm() != null) {
            VmNic oldNic = getVmNicDao().get(getInterface().getId());
            if (oldNic == null || isVnicProfileChanged(oldNic, getInterface())) {
                permissionList.add(new PermissionSubject(getInterface().getVnicProfileId(),
                        VdcObjectType.VnicProfile,
                        getActionType().getActionGroup()));
            }
        }

        return permissionList;
    }

    /**
     * Check if address must be changed after change NIC type
     * @param oldType - Old nic type
     * @param newType - New nic type
     * @return
     */
    private boolean mustChangeAddress (int oldType, int newType) {
        int spaprVlanType = VmInterfaceType.spaprVlan.getValue();
        return oldType == spaprVlanType ^ newType == spaprVlanType;
    }

    private boolean isVnicProfileChanged(VmNic oldNic, VmNic newNic) {
        return !ObjectUtils.equals(oldNic.getVnicProfileId(), newNic.getVnicProfileId());
    }

    private enum RequiredAction {
        PLUG,
        UNPLUG,
        UPDATE_VM_DEVICE
    }

    /**
     * Internal validator that adds checks specific to this class, but uses info from the {@link VmNicValidator}.
     */
    private class UpdateVmNicValidator extends VmNicValidator {

        public UpdateVmNicValidator(VmNic nic, Version version, int osId) {
            super(nic, version, osId);
        }

        public ValidationResult networkExistsOnHost(Network network) {
            if (network == null) {
                return ValidationResult.VALID;
            }

            Guid vdsId = getVmDynamicDao().get(nic.getVmId()).getRunOnVds();
            List<VdsNetworkInterface> hostNics = getDbFacade().getInterfaceDao().getAllInterfacesForVds(vdsId);
            for (VdsNetworkInterface hostNic : hostNics) {
                if (network.getName().equals(hostNic.getNetworkName())) {
                    return ValidationResult.VALID;
                }
            }

            return new ValidationResult(VdcBllMessages.ACTIVATE_DEACTIVATE_NETWORK_NOT_IN_VDS);
        }

        /**
         * @return An error if hot updated is needed, and either network linking is not supported or the NIC has port
         *         mirroring set.
         */
        public ValidationResult hotUpdatePossible() {
            if (getRequiredAction() == RequiredAction.UPDATE_VM_DEVICE) {
                if (!FeatureSupported.networkLinking(version)) {
                    return new ValidationResult(VdcBllMessages.HOT_VM_INTERFACE_UPDATE_IS_NOT_SUPPORTED,
                            clusterVersion());
                } else if (portMirroringEnabled(getInterface().getVnicProfileId())
                        || portMirroringEnabled(oldIface.getVnicProfileId())) {
                    return new ValidationResult(VdcBllMessages.CANNOT_PERFORM_HOT_UPDATE_WITH_PORT_MIRRORING);
                }
            }

            return ValidationResult.VALID;
        }

        private boolean portMirroringEnabled(Guid profileId) {
            VnicProfile vnicProfile = profileId == null ? null : getVnicProfileDao().get(profileId);
            return vnicProfile != null && vnicProfile.isPortMirroring();
        }

        /**
         * @return An error if live action is required and the properties requiring the NIC to be unplugged and then
         *         plugged again have changed.
         */
        public ValidationResult unplugPlugNotRequired() {
            return liveActionRequired() && propertiesRequiringUnplugPlugWereUpdated()
                    ? new ValidationResult(VdcBllMessages.CANNOT_PERFORM_HOT_UPDATE) : ValidationResult.VALID;
        }

        private boolean propertiesRequiringUnplugPlugWereUpdated() {
            return (!oldIface.getType().equals(getInterface().getType()))
                    || (!oldIface.getMacAddress().equals(getMacAddress()));
        }

        /**
         * @param oldNetwork
         *            The old network (can be <code>null</code>).
         * @param newNetwork
         *            The new network (can be <code>null</code>).
         * @return An error if either the old or new network is an external network, otherwise hot update is allowed.
         */
        public ValidationResult hotUpdateDoneWithInternalNetwork(Network oldNetwork, Network newNetwork) {
            return (oldNetwork == null || !oldNetwork.isExternal())
                    && (newNetwork == null || !newNetwork.isExternal())
                    ? ValidationResult.VALID
                            : new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_EXTERNAL_NETWORK_CANNOT_BE_REWIRED);
        }

        public ValidationResult canVnicWithExternalNetworkBePlugged() {
            return ValidationResult.failWith(VdcBllMessages.PLUGGED_UNLINKED_VM_INTERFACE_WITH_EXTERNAL_NETWORK_IS_NOT_SUPPORTED)
                    .when(RequiredAction.PLUG == getRequiredAction()
                          && !nic.isLinked()
                          && isVnicAttachedToExternalNetwork());
        }

        private boolean isVnicAttachedToExternalNetwork() {
            final Network network = NetworkHelper.getNetworkByVnicProfileId(nic.getVnicProfileId());
            return (network != null && network.isExternal());
        }
    }
}
