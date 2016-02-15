package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaVdsDependent;
import org.ovirt.engine.core.bll.quota.QuotaVdsGroupConsumptionParameter;
import org.ovirt.engine.core.bll.validator.LocalizedVmStatus;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.HotSetNumberOfCpusParameters;
import org.ovirt.engine.core.common.action.PlugAction;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.errors.EngineFault;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.vdsbroker.SetNumberOfCpusVDSCommand;

/**
 * Set the number of CPU of a running VM also called hot plug or hot unplug, hot add, hot remove.
 * This command behaviour varies between OS implementation. To that matter, the parameter of the desired
 * number of CPUs will manifest as a hot plug or unplug, depending on the current cpu count at the Guest level.
 *
 * The execute will never throw an exception. it will rather wrap a return value in case of failure.
 */
@NonTransactiveCommandAttribute
public class HotSetNumberOfCpusCommand<T extends HotSetNumberOfCpusParameters> extends VmManagementCommandBase<T> implements QuotaVdsDependent {

    public static final String LOGABLE_FIELD_NUMBER_OF_CPUS = "numberOfCpus";
    public static final String LOGABLE_FIELD_PREVIOUS_NUMBER_OF_CPUS = "previousNumberOfCpus";
    public static final String LOGABLE_FIELD_ERROR_MESSAGE = "ErrorMessage";

    public HotSetNumberOfCpusCommand(T parameters) {
        this(parameters, null);
    }

    public HotSetNumberOfCpusCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }


    @Override
    protected boolean canDoAction() {
        boolean canDo = true;
        if (getVm() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_NOT_FOUND);
        }
        if (getVm().getStatus() != VMStatus.Up) {
            canDo = failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_VM_STATUS_ILLEGAL,
                    LocalizedVmStatus.from(getVm().getStatus()));
        }
        if (getParameters().getVm().getCpuPerSocket() >
                Config.<Integer>getValue(
                        ConfigValues.MaxNumOfCpuPerSocket,
                        getVm().getVdsGroupCompatibilityVersion().getValue())) {
            canDo = failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_MAX_CPU_PER_SOCKET);
        }
        if (getParameters().getVm().getNumOfSockets() >
                Config.<Integer>getValue(
                        ConfigValues.MaxNumOfVmSockets,
                        getVm().getVdsGroupCompatibilityVersion().getValue())) {
            canDo = failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_MAX_NUM_SOCKETS);
        }
        if (getParameters().getPlugAction() == PlugAction.PLUG) {
            if (!FeatureSupported.hotPlugCpu(getVm().getVdsGroupCompatibilityVersion(), getVm().getClusterArch())) {
                canDo = failCanDoAction(EngineMessage.HOT_PLUG_CPU_IS_NOT_SUPPORTED);
            }
        } else if (!FeatureSupported.hotUnplugCpu(getVm().getVdsGroupCompatibilityVersion(), getVm().getClusterArch())) {
            canDo = failCanDoAction(EngineMessage.HOT_UNPLUG_CPU_IS_NOT_SUPPORTED);
        }

        return canDo;
    }

    /**
     * Execution shall perform a call to VDSM to set the number of CPUs.
     * The guest OS will plug/unplug CPUs if the current guest configuration is lower/higher than
     * the requested number respectively.
     */
    @Override
    protected void executeCommand() {
        VDSReturnValue vdsReturnValue = runVdsCommand(VDSCommandType.SetNumberOfCpus,
                new SetNumberOfCpusVDSCommand.Params(
                        getVm().getRunOnVds(),
                        getVm().getId(),
                        getParameters().getVm().getNumOfCpus()));

        if (vdsReturnValue.getSucceeded()) {
            setSucceeded(true);
        } else {
            EngineFault fault = new EngineFault();
            fault.setError(vdsReturnValue.getVdsError().getCode());
            fault.setMessage(vdsReturnValue.getVdsError().getMessage());
            getReturnValue().setFault(fault);
        }

    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        addCustomValue(LOGABLE_FIELD_NUMBER_OF_CPUS, String.valueOf(getParameters().getVm().getNumOfCpus()));
        addCustomValue(LOGABLE_FIELD_PREVIOUS_NUMBER_OF_CPUS, String.valueOf(getVm().getNumOfCpus()));

        if (getSucceeded()) {
            return AuditLogType.HOT_SET_NUMBER_OF_CPUS;
        } else {
            addCustomValue(LOGABLE_FIELD_ERROR_MESSAGE, getReturnValue().getFault().getMessage());
            return AuditLogType.FAILED_HOT_SET_NUMBER_OF_CPUS;
        }

    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__HOT_SET_CPUS);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__VM);
        addCanDoActionMessageVariable("clusterVersion", getVm().getVdsGroupCompatibilityVersion());
        addCanDoActionMessageVariable("architecture", getVm().getClusterArch());
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaVdsConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();

        // Calculate the change in CPU consumption, result above Zero means we add CPUs to
        // the VM
        // result bellow Zero means we subtracted CPUs from the VM
        int cpuToConsume =
                getParameters().getVm().getNumOfCpus() - getVm().getNumOfCpus();

        if (cpuToConsume > 0) {
            // Consume CPU quota
            list.add(new QuotaVdsGroupConsumptionParameter(getVm().getQuotaId(),
                    null,
                    QuotaConsumptionParameter.QuotaAction.CONSUME,
                    getVm().getVdsGroupId(),
                    getVm().getCpuPerSocket() * cpuToConsume,
                    0));

        } else if (cpuToConsume < 0) {
            // Release CPU quota
            list.add(new QuotaVdsGroupConsumptionParameter(getVm().getQuotaId(),
                    null,
                    QuotaConsumptionParameter.QuotaAction.RELEASE,
                    getVm().getVdsGroupId(),
                    getVm().getCpuPerSocket() * Math.abs(cpuToConsume),
                    0));
        }
        return list;
    }
}
