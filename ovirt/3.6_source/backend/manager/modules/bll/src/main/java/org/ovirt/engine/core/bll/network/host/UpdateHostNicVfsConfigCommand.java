package org.ovirt.engine.core.bll.network.host;

import java.util.Collections;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.action.UpdateHostNicVfsConfigParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.network.HostNicVfsConfig;
import org.ovirt.engine.core.common.errors.EngineError;
import org.ovirt.engine.core.common.errors.EngineException;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.vdscommands.HostDevChangeNumVfsVDSParameters;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.utils.transaction.TransactionMethod;
import org.ovirt.engine.core.utils.transaction.TransactionSupport;

@NonTransactiveCommandAttribute
public class UpdateHostNicVfsConfigCommand extends VfsConfigCommandBase<UpdateHostNicVfsConfigParameters> {

    @Inject
    private NetworkDeviceHelper networkDeviceHelper;

    private HostNicVfsConfig hostNicVfsConfig;

    public UpdateHostNicVfsConfigCommand(UpdateHostNicVfsConfigParameters parameters) {
        this(parameters, null);
    }

    public UpdateHostNicVfsConfigCommand(UpdateHostNicVfsConfigParameters parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected void executeCommand() {
        boolean result = true;
        HostNicVfsConfig oldVfsConfig = getVfsConfig();
        boolean allNetworksAllowedChanged = isAllNetworksAllowed() != oldVfsConfig.isAllNetworksAllowed();

        super.executeCommand();

        // Check if 'allNetworksAllowed' has changed
        if (allNetworksAllowedChanged) {

            oldVfsConfig.setAllNetworksAllowed(isAllNetworksAllowed());

            if (isAllNetworksAllowed()) {
                oldVfsConfig.setNetworks(Collections.<Guid> emptySet());
                oldVfsConfig.setNetworkLabels(Collections.<String> emptySet());
            }
        }

        boolean shouldRefreshHost = false;
        if (wasNumOfVfsChanged()) {
            shouldRefreshHost = true;
            String deviceName = networkDeviceHelper.getPciDeviceNameByNic(getNic());
            VDSReturnValue returnValue = null;
            try {
                returnValue = runVdsCommand(VDSCommandType.HostDevChangeNumVfs,
                        new HostDevChangeNumVfsVDSParameters(getVdsId(), deviceName, getNumOfVfs()));
                result = returnValue.getSucceeded();
            } catch (EngineException e) {
                throw new EngineException(EngineError.UPDATE_NUM_VFS_FAILURE);
            }
        }

        if (result) {
            setSucceeded(saveChangesToDb(shouldRefreshHost, oldVfsConfig, allNetworksAllowedChanged));
        }
    }

    private boolean saveChangesToDb(final boolean shouldRefreshHost,
            final HostNicVfsConfig oldVfsConfig,
            final boolean allNetworksAllowedChanged) {
        return TransactionSupport.executeInNewTransaction(new TransactionMethod<Boolean>() {

            @Override
            public Boolean runInTransaction() {
                boolean result = true;

                if (shouldRefreshHost) {
                    result = refreshHost();
                }

                if (result && allNetworksAllowedChanged) {
                    getVfsConfigDao().update(oldVfsConfig);
                }

                return result;
            }
        });
    }

    private boolean refreshHost() {
            VdsActionParameters vdsActionParams = new VdsActionParameters(getVdsId());
            return runInternalAction(VdcActionType.RefreshHost, vdsActionParams).getSucceeded();
    }

    @Override
    protected boolean canDoAction() {
        boolean isValid = super.canDoAction();
        if (isValid && wasNumOfVfsChanged()) {
            isValid = validate(getVfsConfigValidator().allVfsAreFree(networkDeviceHelper))
                    && validate(getVfsConfigValidator().numOfVfsInValidRange(getNumOfVfs()));
        }

        return isValid;
    }

    boolean wasNumOfVfsChanged() {
        return getVfsConfig().getNumOfVfs() != getNumOfVfs();
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.UPDATE_HOST_NIC_VFS_CONFIG
                : AuditLogType.UPDATE_HOST_NIC_VFS_CONFIG_FAILED;
    }

    @Override
    protected void setActionMessageParameters() {
        addCanDoActionMessage(EngineMessage.VAR__ACTION__UPDATE);
        addCanDoActionMessage(EngineMessage.VAR__TYPE__HOST_NIC_VFS_CONFIG);
    }

    private int getNumOfVfs() {
        return getParameters().getNumOfVfs();
    }

    private boolean isAllNetworksAllowed() {
        return getParameters().getAllNetworksAllowed();
    }

    @Override
    public HostNicVfsConfig getVfsConfig() {
        HostNicVfsConfig tmpVfsConfig = super.getVfsConfig();
        if (hostNicVfsConfig == null) {
            networkDeviceHelper.updateHostNicVfsConfigWithNumVfsData(tmpVfsConfig);
        }

        hostNicVfsConfig = tmpVfsConfig;

        return hostNicVfsConfig;

    }
}
