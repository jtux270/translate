package org.ovirt.engine.core.bll;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.utils.EngineSSHClient;
import org.ovirt.engine.core.common.action.FenceVdsActionParameters;
import org.ovirt.engine.core.common.action.VdsPowerDownParameters;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.FenceActionType;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;

/**
 * Tries to shutdown a host using SSH connection. The host has to be in maintenance mode.
 */
@NonTransactiveCommandAttribute
public class VdsPowerDownCommand<T extends VdsPowerDownParameters> extends VdsCommand<T> {
    public VdsPowerDownCommand(T parameters) {
        this(parameters, null);
    }

    public VdsPowerDownCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected boolean canDoAction() {
        return getVds().getStatus() == VDSStatus.Maintenance && super.canDoAction();
    }

    /**
     * Try to shut down the host using a clean ssh poweroff method
     */
    @Override
    protected void executeCommand() {
        setVds(null);
        if (getVds() == null) {
            handleError("SSH power down will not be executed on host {0} ({1}) since it doesn't exist anymore.");
            return;
        }

        /* Try this only when the Host is in maintenance state */
        if (getVds().getStatus() != VDSStatus.Maintenance) {
            handleError("SSH power down will not be executed on host {0} ({1}) since it is not in Maintenance.");
            return;
        }

        boolean result = executeSshPowerdown(getVds().getVdsGroupCompatibilityVersion().toString());
        if (result) {
            // SSH powerdown executed without errors set the status to down
            getVds().setStatus(VDSStatus.Down);

            // clear the automatic PM flag unless instructed otherwise
            if (!getParameters().getKeepPolicyPMEnabled()) {
                getVds().setPowerManagementControlledByPolicy(false);
                getDbFacade().getVdsDynamicDao().updateVdsDynamicPowerManagementPolicyFlag(
                        getVdsId(),
                        getVds().getDynamicData().isPowerManagementControlledByPolicy());
            }

        } else if (getParameters().getFallbackToPowerManagement() && getVds().getpm_enabled()) {
            FenceVdsActionParameters parameters = new FenceVdsActionParameters(getVds().getId(), FenceActionType.Stop);
            parameters.setKeepPolicyPMEnabled(getParameters().getKeepPolicyPMEnabled());
            runInternalAction(VdcActionType.StopVds,
                    parameters,
                    ExecutionHandler.createInternalJobContext());
        }

        getReturnValue().setSucceeded(result);
    }

    private void handleError(final String errorMessage) {
        setCommandShouldBeLogged(false);
        log.infoFormat(errorMessage,
                getVdsName(),
                getVdsId());
        getReturnValue().setSucceeded(false);
    }

    /**
     * Executes SSH shutdown command
     *
     * @returns {@code true} if command has been executed successfully, {@code false} otherwise
     */
    private boolean executeSshPowerdown(String version) {
        boolean ret = false;
        try (
                final EngineSSHClient sshClient = new EngineSSHClient();
                final ByteArrayOutputStream cmdOut = new ByteArrayOutputStream();
                final ByteArrayOutputStream cmdErr = new ByteArrayOutputStream();
        ) {
            try {
                log.infoFormat("Opening SSH power down session on host {0}", getVds().getHostName());
                sshClient.setVds(getVds());
                sshClient.useDefaultKeyPair();
                sshClient.connect();
                sshClient.authenticate();

                log.infoFormat("Executing SSH power down command on host {0}", getVds().getHostName());
                sshClient.executeCommand(
                        Config.<String> getValue(ConfigValues.SshVdsPowerdownCommand, version),
                        null,
                        cmdOut,
                        cmdErr
                );
                ret = true;
            } catch (Exception ex) {
                log.errorFormat("SSH power down command failed on host {0}: {1}\nStdout: {2}\nStderr: {3}\nStacktrace: {4}",
                        getVds().getHostName(), ex.getMessage(), cmdOut.toString(), cmdErr.toString(), ex);
            }
        }
        catch(IOException e) {
            log.error("IOException", e);
        }
        return ret;
    }
}
