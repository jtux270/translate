package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.StorageDomainValidator;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.RngDeviceParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.action.VmTemplateParametersBase;
import org.ovirt.engine.core.common.action.WatchdogParameters;
import org.ovirt.engine.core.common.businessentities.DiskImage;
import org.ovirt.engine.core.common.businessentities.ImageStatus;
import org.ovirt.engine.core.common.businessentities.VmDeviceId;
import org.ovirt.engine.core.common.businessentities.VmEntityType;
import org.ovirt.engine.core.common.businessentities.VmRngDevice;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.VmTemplateStatus;
import org.ovirt.engine.core.common.businessentities.VmWatchdog;
import org.ovirt.engine.core.common.businessentities.network.VmNic;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.VdcQueryReturnValue;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NotImplementedException;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;

public abstract class VmTemplateCommand<T extends VmTemplateParametersBase> extends CommandBase<T> {

    /**
     * Constructor for command creation when compensation is applied on startup
     *
     * @param commandId
     */
    protected VmTemplateCommand(Guid commandId) {
        super(commandId);
    }

    protected VmTemplateCommand(T parameters) {
        this(parameters, null);
    }

    protected VmTemplateCommand() {
    }

    protected VmTemplateCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
        setVmTemplateId(parameters.getVmTemplateId());
    }

    @Override
    protected void executeCommand() {
        throw new NotImplementedException();
    }

    public static boolean isVmTemlateWithSameNameExist(String name) {
        return DbFacade.getInstance().getVmTemplateDao().getByName(name, null, false) != null;
    }

    public static boolean isVmTemplateImagesReady(VmTemplate vmTemplate,
            Guid storageDomainId,
            List<String> reasons,
            boolean checkImagesExists,
            boolean checkLocked,
            boolean checkIllegal,
            boolean checkStorageDomain, List<DiskImage> providedVmtImages) {
        boolean returnValue = true;
        List<DiskImage> vmtImages = providedVmtImages;
        if (checkStorageDomain) {
            StorageDomainValidator storageDomainValidator =
                    new StorageDomainValidator(DbFacade.getInstance().getStorageDomainDao().getForStoragePool(
                            storageDomainId, vmTemplate.getStoragePoolId()));
            ValidationResult res = storageDomainValidator.isDomainExistAndActive();
            returnValue = res.isValid();
            if (!returnValue) {
                reasons.add(res.getMessage().toString());
            }
        }
        if (returnValue && checkImagesExists) {
            if (vmtImages == null) {
                vmtImages =
                        ImagesHandler.filterImageDisks(DbFacade.getInstance()
                                .getDiskDao()
                                .getAllForVm(vmTemplate.getId()), false, false, true);
            }
            if (vmtImages.size() > 0
                    && !ImagesHandler.isImagesExists(vmtImages, vmtImages.get(0).getStoragePoolId())) {
                reasons.add(VdcBllMessages.TEMPLATE_IMAGE_NOT_EXIST.toString());
                returnValue = false;
            }
        }
        if (returnValue && checkLocked) {
            if (vmTemplate.getStatus() == VmTemplateStatus.Locked) {
                returnValue = false;
            } else {
                if (vmtImages != null) {
                    for (DiskImage image : vmtImages) {
                        if (image.getImageStatus() == ImageStatus.LOCKED) {
                            returnValue = false;
                            break;
                        }
                    }
                }
            }
            if (!returnValue) {
                reasons.add(VdcBllMessages.VM_TEMPLATE_IMAGE_IS_LOCKED.toString());
            }
        }
        if (returnValue && checkIllegal && (vmTemplate.getStatus() == VmTemplateStatus.Illegal)) {
            returnValue = false;
            reasons.add(VdcBllMessages.VM_TEMPLATE_IMAGE_IS_ILLEGAL.toString());
        }
        return returnValue;
    }

    /**
     * Determines whether [is domain legal] [the specified domain name].
     *
     * @param domainName
     *            Name of the domain.
     * @param reasons
     *            The reasons.
     * @return <c>true</c> if [is domain legal] [the specified domain name];
     *         otherwise, <c>false</c>.
     */
    public static boolean isDomainLegal(String domainName, ArrayList<String> reasons) {
        boolean result = true;
        char[] illegalChars = new char[] { '&' };
        if (StringUtils.isNotEmpty(domainName)) {
            for (char c : illegalChars) {
                if (domainName.contains((Character.toString(c)))) {
                    result = false;
                    reasons.add(VdcBllMessages.ACTION_TYPE_FAILED_ILLEGAL_DOMAIN_NAME.toString());
                    reasons.add(String.format("$Domain %1$s", domainName));
                    reasons.add(String.format("$Char %1$s", c));
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Determines whether [is high availability value legal] [the specified
     * value].
     *
     * @param value
     *            The value.
     * @param reasons
     *            The reasons.
     * @return <c>true</c> if [is vm priority value legal] [the specified
     *         value]; otherwise, <c>false</c>.
     */
    public static boolean isVmPriorityValueLegal(int value, List<String> reasons) {
        boolean res = false;
        if (value >= 0 && value <= Config.<Integer> getValue(ConfigValues.VmPriorityMaxValue)) {
            res = true;
        } else {
            reasons.add(VdcBllMessages.VM_OR_TEMPLATE_ILLEGAL_PRIORITY_VALUE.toString());
            reasons.add(String.format("$MaxValue %1$s", Config.<Integer> getValue(ConfigValues.VmPriorityMaxValue)));
        }
        return res;
    }

    protected ValidationResult templateExists() {
        return getVmTemplate() == null ? new ValidationResult(VdcBllMessages.ACTION_TYPE_FAILED_TEMPLATE_DOES_NOT_EXIST)
                : ValidationResult.VALID;
    }

    @Override
    protected String getDescription() {
        return getVmTemplateName();
    }

    protected void removeNetwork() {
        List<VmNic> list = getVmNicDao().getAllForTemplate(getVmTemplateId());
        for (VmNic iface : list) {
            DbFacade.getInstance().getVmDeviceDao().remove(new VmDeviceId(iface.getId(), getVmTemplateId()));
            getVmNicDao().remove(iface.getId());
        }
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionList = new ArrayList<PermissionSubject>();
        permissionList.add(new PermissionSubject(getVmTemplateId(),
                VdcObjectType.VmTemplate,
                getActionType().getActionGroup()));
        return permissionList;
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = new HashMap<String, String>();
            jobProperties.put(VdcObjectType.StoragePool.name().toLowerCase(), getStoragePoolName());
            jobProperties.put(VdcObjectType.VmTemplate.name().toLowerCase(), getVmTemplateName());
        }
        return jobProperties;
    }

    protected void updateWatchdog(Guid templateId) {
        // do not update if this flag is not set
        if (getParameters().isUpdateWatchdog()) {
            VdcQueryReturnValue query =
                    runInternalQuery(VdcQueryType.GetWatchdog, new IdQueryParameters(templateId));
            List<VmWatchdog> watchdogs = query.getReturnValue();
            if (watchdogs.isEmpty()) {
                if (getParameters().getWatchdog() != null) {
                    WatchdogParameters parameters = new WatchdogParameters();
                    parameters.setVm(false);
                    parameters.setInstanceType(getVmTemplate().getTemplateType() == VmEntityType.INSTANCE_TYPE);

                    parameters.setId(templateId);
                    parameters.setAction(getParameters().getWatchdog().getAction());
                    parameters.setModel(getParameters().getWatchdog().getModel());
                    runInternalAction(VdcActionType.AddWatchdog, parameters, cloneContextAndDetachFromParent());
                }
            } else {
                WatchdogParameters watchdogParameters = new WatchdogParameters();
                watchdogParameters.setVm(false);
                watchdogParameters.setInstanceType(getVmTemplate().getTemplateType() == VmEntityType.INSTANCE_TYPE);

                watchdogParameters.setId(templateId);
                if (getParameters().getWatchdog() == null) {
                    // there is a watchdog in the vm, there should not be any, so let's delete
                    runInternalAction(VdcActionType.RemoveWatchdog, watchdogParameters, cloneContextAndDetachFromParent());
                } else {
                    // there is a watchdog in the vm, we have to update.
                    watchdogParameters.setAction(getParameters().getWatchdog().getAction());
                    watchdogParameters.setModel(getParameters().getWatchdog().getModel());
                    runInternalAction(VdcActionType.UpdateWatchdog, watchdogParameters, cloneContextAndDetachFromParent());
                }
            }
        }
    }

    protected void updateRngDevice(Guid templateId) {
        // do not update if this flag is not set
        if (getParameters().isUpdateRngDevice()) {
            VdcQueryReturnValue query =
                    runInternalQuery(VdcQueryType.GetRngDevice, new IdQueryParameters(templateId));

            List<VmRngDevice> rngDevs = query.getReturnValue();

            if (getParameters().getRngDevice() != null) {
                getParameters().getRngDevice().setVmId(templateId);
            }

            VdcReturnValueBase rngCommandResult = null;
            if (rngDevs.isEmpty()) {
                if (getParameters().getRngDevice() != null) {
                    RngDeviceParameters params = new RngDeviceParameters(getParameters().getRngDevice(), false);
                    rngCommandResult = runInternalAction(VdcActionType.AddRngDevice, params, cloneContextAndDetachFromParent());
                }
            } else {
                if (getParameters().getRngDevice() == null) {
                    RngDeviceParameters params = new RngDeviceParameters(rngDevs.get(0), false);
                    rngCommandResult = runInternalAction(VdcActionType.RemoveRngDevice, params, cloneContextAndDetachFromParent());
                } else {
                    RngDeviceParameters params = new RngDeviceParameters(getParameters().getRngDevice(), false);
                    params.getRngDevice().setDeviceId(rngDevs.get(0).getDeviceId());
                    rngCommandResult = runInternalAction(VdcActionType.UpdateRngDevice, params, cloneContextAndDetachFromParent());
                }
            }

            if (rngCommandResult != null && !rngCommandResult.getSucceeded()) {
                getReturnValue().setSucceeded(false);
            }
        }
    }

}
