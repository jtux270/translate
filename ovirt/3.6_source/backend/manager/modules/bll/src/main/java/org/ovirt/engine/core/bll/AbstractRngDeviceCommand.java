package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.VirtIoRngValidator;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.RngDeviceParameters;
import org.ovirt.engine.core.common.businessentities.VmBase;
import org.ovirt.engine.core.common.businessentities.VmDevice;
import org.ovirt.engine.core.common.businessentities.VmDeviceGeneralType;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.VmDeviceDao;

/**
 * Base class for crud for random number generator devices
 */
public abstract class AbstractRngDeviceCommand<T extends RngDeviceParameters> extends CommandBase<T>  {

    private VmBase cachedEntity = null;
    private VmEntityType templateType = null;
    private List<VmDevice> cachedRngDevices = null;
    private boolean blankTemplate = false;

    protected AbstractRngDeviceCommand(T parameters, CommandContext context) {
        super(parameters, context);

        if (parameters.getRngDevice() == null || parameters.getRngDevice().getVmId() == null) {
            return;
        }

        Guid vmId = parameters.getRngDevice().getVmId();
        setVmId(vmId);

        if (getParameters().isVm()) {
            cachedEntity = getVmStaticDao().get(vmId);
        } else {
            blankTemplate = VmTemplateHandler.BLANK_VM_TEMPLATE_ID.equals(vmId);

            VmTemplate template = getVmTemplateDao().get(vmId);
            templateType = template.getTemplateType();
            cachedEntity = template;
        }

        if (cachedEntity != null) {
            setVdsGroupId(cachedEntity.getVdsGroupId());
        }

        cachedRngDevices = new ArrayList<>();
        List<VmDevice> rngDevs = getVmDeviceDao().getVmDeviceByVmIdAndType(vmId, VmDeviceGeneralType.RNG);
        if (rngDevs != null) {
            cachedRngDevices.addAll(rngDevs);
        }
    }

    public AbstractRngDeviceCommand(T parameters) {
        this(parameters, null);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<>();
        permissionList.add(new PermissionSubject(getParameters().getRngDevice().getVmId(),
                getParameters().isVm() ? VdcObjectType.VM : VdcObjectType.VmTemplate,
                getActionType().getActionGroup()));
        return permissionList;
    }

    @Override
    protected boolean canDoAction() {
        if (getParameters().getRngDevice().getVmId() == null || cachedEntity == null) {
            return failCanDoAction(getParameters().isVm() ? EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND
                    : EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
        }

        if (getParameters().isVm() && getVm() != null && getVm().isRunningOrPaused()) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_IS_RUNNING);
        }

        return true;
    }

    /**
     * Provides the new instance of VirtIoRngValidator
     * This method is here only to make it possible to mock it in tests
     */
    protected VirtIoRngValidator getVirtioRngValidator() {
        return new VirtIoRngValidator();
    }

    protected List<VmDevice> getRngDevices() {
        return cachedRngDevices;
    }

    protected VmDeviceDao getVmDeviceDao() {
        return getDbFacade().getVmDeviceDao();
    }

    protected VmEntityType getTemplateType() {
        return templateType;
    }

    public boolean isBlankTemplate() {
        return blankTemplate;
    }
}
