package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.GraphicsParameters;
import org.ovirt.engine.core.common.businessentities.GraphicsDevice;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.dao.VmDeviceDao;

public abstract class AbstractGraphicsDeviceCommand<T extends GraphicsParameters> extends CommandBase<T> {

    protected AbstractGraphicsDeviceCommand(T parameters) {
        super(parameters);
        if (parameters.isVm()) {
            setVmId(parameters.getDev().getVmId());
        } else {
            setVmTemplateId(parameters.getDev().getVmId());
        }
    }

    @Override
    protected boolean canDoAction() {
        GraphicsDevice dev = getParameters().getDev();

        if (dev == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_DEVICE_MUST_BE_SPECIFIED);
        }

        if (getParameters().isVm() && getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }

        if (!getParameters().isVm() && getVmTemplate() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST);
        }

        if (dev.getGraphicsType() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_GRAPHIC_TYPE_MUST_BE_SPECIFIED);
        }

        return true;
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<>();
        permissionList.add(new PermissionSubject(getParameters().getDev().getVmId(),
                getParameters().isVm() ? VdcObjectType.VM : VdcObjectType.VmTemplate,
                getActionType().getActionGroup()));
        return permissionList;
    }

    protected VmDeviceDao getVmDeviceDao() {
        return getDbFacade().getVmDeviceDao();
    }

}
