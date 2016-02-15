package org.ovirt.engine.core.bll.network.vm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.network.MacPoolManager;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.VmNicValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddVmInterfaceParameters;
import org.ovirt.engine.core.common.action.PlugAction;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.network.VmInterfaceType;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute(forceCompensation = true)
public class AddVmInterfaceCommand<T extends AddVmInterfaceParameters> extends AbstractVmInterfaceCommand<T> {

    public AddVmInterfaceCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected void executeVmCommand() {
        addCustomValue("InterfaceType",
                (VmInterfaceType.forValue(getInterface().getType()).getDescription()).toString());
        this.setVmName(getVmStaticDAO().get(getParameters().getVmId()).getName());

        boolean succeeded = false;
        boolean macAddedToPool = false;

        try {
            if (StringUtils.isEmpty(getMacAddress())) {
                getInterface().setMacAddress(MacPoolManager.getInstance().allocateNewMac());
                macAddedToPool = true;
            } else {
                macAddedToPool = addMacToPool(getMacAddress());
            }

            getInterface().setSpeed(VmInterfaceType.forValue(getInterface().getType()).getSpeed());
            getInterface().setId(Guid.newGuid());
            getInterface().setVmId(getParameters().getVmId());

            TransactionSupport.executeInNewTransaction(new TransactionMethod<Void>() {
                @Override
                public Void runInTransaction() {
                    addInterfaceToDb(getInterface());
                    addInterfaceDeviceToDb();
                    getCompensationContext().stateChanged();
                    return null;
                }
            });

            if (getInterface().isPlugged()) {
                succeeded = activateOrDeactivateNewNic(getInterface(), PlugAction.PLUG);
            } else {
                succeeded = true;
            }
        } finally {
            setSucceeded(succeeded);
            if (macAddedToPool && !succeeded) {
                MacPoolManager.getInstance().freeMac(getMacAddress());
            }
        }
    }

    private void addInterfaceDeviceToDb() {
        VmDevice vmDevice = VmDeviceUtils.addNetworkInterfaceDevice(
                new VmDeviceId(getInterface().getId(), getParameters().getVmId()),
                getInterface().isPlugged());
        getCompensationContext().snapshotNewEntity(vmDevice);
    }

    private void addInterfaceToDb(VmNic vmNetworkInterface) {
        getVmNicDao().save(vmNetworkInterface);
        getCompensationContext().snapshotNewEntity(vmNetworkInterface);

        getDbFacade().getVmNetworkStatisticsDao().save(vmNetworkInterface.getStatistics());
        getCompensationContext().snapshotNewEntity(vmNetworkInterface.getStatistics());
    }

    @Override
    protected boolean canDoAction() {
        VmStatic vm = getVm().getStaticData();
        if (vm == null) {
            addCanDoActionMessage(VdcBllMessages.ACTION_TYPE_FAILED_VM_NOT_EXIST);
            return false;
        }

        if (!canRunActionOnNonManagedVm()) {
            return false;
        }

        if (!updateVnicForBackwardCompatibility()) {
            return false;
        }

        if (!validate(vmStatusLegal(getVmDynamicDao().get(getParameters().getVmId()).getStatus()))) {
            return false;
        }

        List<VmNic> interfaces = getVmNicDao().getAllForVm(getParameters().getVmId());

        if (!uniqueInterfaceName(interfaces)) {
            return false;
        }

        if (!validate(vmTemplateEmpty())) {
            return false;
        }

        // check that not exceeded PCI and IDE limit
        List<VmNic> allInterfaces = new ArrayList<>(interfaces);
        allInterfaces.add(getInterface());

        if (!pciAndIdeWithinLimit(getVm(), allInterfaces)) {
            return false;
        }

        Version compatibilityVersion = getVm().getVdsGroupCompatibilityVersion();
        VmNicValidator nicValidator = new VmNicValidator(getInterface(), compatibilityVersion, getVm().getOs());

        if (!validate(nicValidator.linkedCorrectly())
                || !validate(nicValidator.isCompatibleWithOs())
                || !validate(nicValidator.emptyNetworkValid())
                || !validate(nicValidator.profileValid(vm.getVdsGroupId()))) {
            return false;
        }

        if (StringUtils.isNotEmpty(getMacAddress())) {
            if (!validate(macAvailable())) {
                return false;
            }
        } else if (MacPoolManager.getInstance().getAvailableMacsCount() <= 0) {
            addCanDoActionMessage(VdcBllMessages.MAC_POOL_NOT_ENOUGH_MAC_ADDRESSES);
            return false;
        }

        return true;
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

    /**
     * Set the parameters for bll messages, such as type and action,
     */
    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(VdcBllMessages.VAR__ACTION__ADD);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.NETWORK_ADD_VM_INTERFACE : AuditLogType.NETWORK_ADD_VM_INTERFACE_FAILED;
    }

    /**
     * The permissions list contains the vm and the vnic profile id used by the vnic.<br>
     */
    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = super.getPermissionCheckSubjects();

        if (getInterface() != null && getInterface().getVnicProfileId() != null && getVm() != null) {
            permissionList.add(new PermissionSubject(getInterface().getVnicProfileId(),
                    VdcObjectType.VnicProfile,
                    getActionType().getActionGroup()));
        }

        return permissionList;
    }
}
