package org.ovirt.engine.core.bll.pm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.VdsCommand;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.utils.EngineSSHClient;
import org.ovirt.engine.core.common.action.VdsActionParameters;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.utils.ThreadUtils;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.ovirt.engine.core.vdsbroker.VdsManager;

/**
 * Tries to restart VDSM using SSH connection
 */
@NonTransactiveCommandAttribute
public class SshSoftFencingCommand<T extends VdsActionParameters> extends VdsCommand<T> {
    /**
     * Creates an instance with specified parameters
     *
     * @param parameters
     *            command parameters
     */
    public SshSoftFencingCommand(T parameters) {
        this(parameters, null);
    }

    public SshSoftFencingCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    /**
     * If the VDS is not responding, it executes SSH Soft Fencing.
     */
    @Override
    protected void executeCommand() {
        setVds(null);
        if (getVds() == null) {
            setCommandShouldBeLogged(false);
            log.info("SSH Soft Fencing will not be executed on host '{}' ({}) since it doesn't exist anymore.",
                    getVdsName(),
                    getVdsId());
            getReturnValue().setSucceeded(false);
            return;
        }
        if (new HostFenceActionExecutor(getVds()).isHostPoweredOff()) {
            // do not try to soft-fence if Host is reported as Down via PM
            getReturnValue().setSucceeded(false);
        }
        else {
            if (getVds().shouldVdsBeFenced()) {
                boolean result = executeSshSoftFencingCommand(getVds().getVdsGroupCompatibilityVersion().toString());
                if (result) {
                    // SSH Soft Fencing executed successfully, check if host become Up
                    result = checkIfHostBecomeUp();
                }
                getReturnValue().setSucceeded(result);
            } else {
                setCommandShouldBeLogged(false);
                log.info("SSH Soft Fencing will not be executed on host '{}' ({}) since it's status is ok.",
                        getVdsName(),
                        getVdsId());
                getReturnValue().setSucceeded(false);
            }
        }
    }

    /**
     * Executes SSH Soft Fencing command
     *
     * @param version
     *            cluster compatibility version to acquire correct command to restart VDSM
     * @return {@code true} if command has been executed successfully, {@code false} otherwise
     */
    private boolean executeSshSoftFencingCommand(String version) {
        boolean ret = false;
        try (
            final EngineSSHClient sshClient = new EngineSSHClient();
            final ByteArrayOutputStream cmdOut = new ByteArrayOutputStream();
            final ByteArrayOutputStream cmdErr = new ByteArrayOutputStream();
        ) {
            try {
                log.info("Opening SSH Soft Fencing session on host '{}'", getVds().getHostName());
                sshClient.setVds(getVds());
                sshClient.useDefaultKeyPair();
                sshClient.connect();
                sshClient.authenticate();

                log.info("Executing SSH Soft Fencing command on host '{}'", getVds().getHostName());
                sshClient.executeCommand(
                    Config.<String> getValue(ConfigValues.SshSoftFencingCommand, version),
                    null,
                    cmdOut,
                    cmdErr
                );
                ret = true;
            } catch (Exception ex) {
                log.error("SSH Soft Fencing command failed on host '{}': {}\nStdout: {}\nStderr: {}",
                        getVds().getHostName(),
                        ex.getMessage(),
                        cmdOut,
                        cmdErr);
                log.debug("Exception", ex);
            }
        }
        catch(IOException e) {
            log.error("IOException", e);
        }
        return ret;
    }

    /**
     * Check if host become Up after successful SSH Soft Fencing execution until grace period is over
     *
     * @return {@code true} if host became Up during grace period, otherwise {@code false}
     */
    private boolean checkIfHostBecomeUp() {
        VdsManager vdsManager = getResourceManager().GetVdsManager(getVdsId());
        int sleepInterval = Config.<Integer> getValue(ConfigValues.VdsRefreshRate) * 1000;
        while (vdsManager.isHostInGracePeriod(true)) {
            if (vdsManager.getCopyVds().getStatus() == VDSStatus.Up) {
                // host became Up during grace period
                return true;
            }
            // wait until next host monitoring attempt
            ThreadUtils.sleep(sleepInterval);
        }
        return false;
    }

    public ResourceManager getResourceManager() {
        // TODO: fix when ResourceManager could be injected
        return ResourceManager.getInstance();
    }
}
