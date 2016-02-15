package org.ovirt.engine.core.bll;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.SetNonOperationalVdsParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.action.VdsGroupOperationParameters;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.MigrateOnErrorOptions;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.ServerCpu;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.errors.VdcBllMessages;
import org.ovirt.engine.core.compat.TransactionScopeOption;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBase;

@NonTransactiveCommandAttribute
public class HandleVdsCpuFlagsOrClusterChangedCommand<T extends VdsActionParameters> extends VdsCommand<T> {

    private boolean _hasFlags = true;
    private boolean architectureMatch = true;
    private boolean foundCPU = true;

    public HandleVdsCpuFlagsOrClusterChangedCommand(T parameters) {
        super(parameters);
    }

    @Override
    protected boolean canDoAction() {
        boolean result = true;
        if (getVds() == null) {
            addCanDoActionMessage(VdcBllMessages.VDS_INVALID_SERVER_ID);
            result = false;
        }
        return result;
    }

    @Override
    protected void executeCommand() {
        String vdsGroupCpuName = getVds().getVdsGroupCpuName();

        VDSGroup grp = DbFacade.getInstance().getVdsGroupDao().get(getVds().getVdsGroupId());

        ServerCpu sc = CpuFlagsManagerHandler.FindMaxServerCpuByFlags(getVds().getCpuFlags(), getVds()
                .getVdsGroupCompatibilityVersion());

        if (sc == null) {
            // if there are flags and no cpu found, mark to be non
            // operational
            if (!StringUtils.isEmpty(getVds().getCpuFlags())) {
                foundCPU = false;
            } else {
                _hasFlags = false;
            }
            log.errorFormat("Could not find server cpu for server {0}:{1}, flags: {2}", getVdsId(), getVds()
                    .getName(), getVds().getCpuFlags());
        }

        // Checks whether the host and the cluster have the same architecture
        if (_hasFlags && foundCPU) {
            if (grp.getArchitecture() != ArchitectureType.undefined &&
                    sc.getArchitecture() != grp.getArchitecture()) {
                architectureMatch = false;

                addCustomValue("VdsArchitecture", sc.getArchitecture().name());
                addCustomValue("VdsGroupArchitecture", grp.getArchitecture().name());

                SetNonOperationalVdsParameters tempVar = new SetNonOperationalVdsParameters(getVdsId(),
                        NonOperationalReason.ARCHITECTURE_INCOMPATIBLE_WITH_CLUSTER);

                runInternalAction(VdcActionType.SetNonOperationalVds,
                        tempVar,
                        ExecutionHandler.createInternalJobContext(getContext()));
            } else {
                // if cluster doesn't have cpu then get the cpu from the vds
                if (StringUtils.isEmpty(vdsGroupCpuName)) {
                    // update group with the cpu name

                    grp.setcpu_name(sc.getCpuName());
                    grp.setArchitecture(sc.getArchitecture());

                    updateMigrateOnError(grp);

                    // use suppress in order to update group even if action fails
                    // (out of the transaction)
                    VdsGroupOperationParameters tempVar = new VdsGroupOperationParameters(grp);
                    tempVar.setTransactionScopeOption(TransactionScopeOption.Suppress);
                    tempVar.setIsInternalCommand(true);
                    runInternalAction(VdcActionType.UpdateVdsGroup, tempVar);

                    vdsGroupCpuName = sc.getCpuName();
                }
            }
        }

        // If the host CPU name is not found by the CpuFlagsManagerHandler class, report an error
        if (architectureMatch) {
            List<String> missingFlags = CpuFlagsManagerHandler.missingServerCpuFlags(vdsGroupCpuName, getVds()
                    .getCpuFlags(), getVds().getVdsGroupCompatibilityVersion());
            if (!StringUtils.isEmpty(getVds().getCpuFlags())
                    && (!foundCPU || missingFlags != null)) {
                if (missingFlags != null) {
                    addCustomValue("CpuFlags", StringUtils.join(missingFlags, ", "));
                    if (missingFlags.contains("nx")) {
                        AuditLogableBase logable = new AuditLogableBase(getVds().getId());
                        AuditLogDirector.log(logable, AuditLogType.CPU_FLAGS_NX_IS_MISSING);
                    }
                }

                SetNonOperationalVdsParameters tempVar2 = new SetNonOperationalVdsParameters(getVdsId(),
                        NonOperationalReason.CPU_TYPE_INCOMPATIBLE_WITH_CLUSTER);
                runInternalAction(VdcActionType.SetNonOperationalVds,
                        tempVar2,
                        ExecutionHandler.createInternalJobContext(getContext()));
            } else {
                // if no need to change to non operational then don't log the command
                setCommandShouldBeLogged(false);
            }
        }
        setSucceeded(true);
    }

    private void updateMigrateOnError(VDSGroup group) {
        ArchitectureType arch = getArchitecture(group);

        boolean isMigrationSupported = FeatureSupported.isMigrationSupported(arch, group.getcompatibility_version());

        if (!isMigrationSupported) {
            group.setMigrateOnError(MigrateOnErrorOptions.NO);
        }
    }

    protected ArchitectureType getArchitecture(VDSGroup group) {
        if (StringUtils.isNotEmpty(group.getcpu_name())) {
            return CpuFlagsManagerHandler.getArchitectureByCpuName(group.getcpu_name(),
                    group.getcompatibility_version());
        }

        return group.getArchitecture();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        if (!foundCPU) {
            return AuditLogType.CPU_TYPE_UNSUPPORTED_IN_THIS_CLUSTER_VERSION;
        } else if (!architectureMatch) {
            return AuditLogType.VDS_ARCHITECTURE_NOT_SUPPORTED_FOR_CLUSTER;
        } else if (!_hasFlags) {
            return AuditLogType.VDS_CPU_RETRIEVE_FAILED;
        } else {
            return AuditLogType.VDS_CPU_LOWER_THAN_CLUSTER;
        }
    }
}

