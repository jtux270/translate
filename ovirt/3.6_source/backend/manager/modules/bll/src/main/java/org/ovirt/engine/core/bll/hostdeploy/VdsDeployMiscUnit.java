package org.ovirt.engine.core.bll.hostdeploy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.crypt.EngineEncryptionUtils;
import org.ovirt.otopi.constants.NetEnv;
import org.ovirt.otopi.constants.SysEnv;
import org.ovirt.otopi.dialog.Event;
import org.ovirt.ovirt_host_deploy.constants.GlusterEnv;
import org.ovirt.ovirt_host_deploy.constants.TuneEnv;
import org.ovirt.ovirt_host_deploy.constants.VdsmEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VdsDeployMiscUnit implements VdsDeployUnit {

    private static final Logger log = LoggerFactory.getLogger(VdsDeployMiscUnit.class);

    private static final String COND_HOST_REBOOT = "HOST_REBOOT";

    private final List<Callable<Boolean>> CUSTOMIZATION_DIALOG = Arrays.asList(
        new Callable<Boolean>() { public Boolean call() throws Exception {
            _deploy.getParser().cliEnvironmentSet(
                SysEnv.CLOCK_SET,
                true
            );
            return true;
        }},
        new Callable<Boolean>() { public Boolean call() throws Exception {
            _deploy.getParser().cliEnvironmentSet(
                NetEnv.SSH_ENABLE,
                true
            );
            return true;
        }},
        new Callable<Boolean>() { public Boolean call() throws Exception {
            _deploy.getParser().cliEnvironmentSet(
                NetEnv.SSH_USER,
                _deploy.getVds().getSshUsername()
            );
            return true;
        }},
        new Callable<Boolean>() { public Boolean call() throws Exception {
            _deploy.getParser().cliEnvironmentSet(
                NetEnv.SSH_KEY,
                EngineEncryptionUtils.getEngineSSHPublicKey().replace("\n", "")
            );
            return true;
        }},
        new Callable<Boolean>() { public Boolean call() throws Exception {
            VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(
                _deploy.getVds().getVdsGroupId()
            );
            String tunedProfile = vdsGroup.supportsGlusterService() ? vdsGroup.getGlusterTunedProfile() : null;
            if (tunedProfile == null || tunedProfile.isEmpty()) {
                _deploy.getParser().cliNoop();
            } else {
                _deploy.getParser().cliEnvironmentSet(TuneEnv.TUNED_PROFILE, tunedProfile);
            }
            return true;
        }},
        new Callable<Boolean>() { public Boolean call() throws Exception {
            /**
             * Legacy logic
             * Force reboot only if not node.
             */
            if (
                (Boolean)_deploy.getParser().cliEnvironmentGet(
                    VdsmEnv.OVIRT_NODE
                )
            ) {
                _deploy.removeCustomizationCondition(COND_HOST_REBOOT);
            }
            return true;
        }},
        new Callable<Boolean>() {@VdsDeployUnit.CallWhen(COND_HOST_REBOOT)
        public Boolean call() throws Exception {
            _deploy.userVisibleLog(
                Level.INFO,
                "Enforcing host reboot"
            );
            _deploy.getParser().cliEnvironmentSet(
                org.ovirt.ovirt_host_deploy.constants.CoreEnv.FORCE_REBOOT,
                true
            );
            return true;
        }},
        new Callable<Boolean>() { public Boolean call() throws Exception {
            VDSGroup vdsGroup = DbFacade.getInstance().getVdsGroupDao().get(
                _deploy.getVds().getVdsGroupId()
            );
            _deploy.getParser().cliEnvironmentSet(
                GlusterEnv.ENABLE,
                vdsGroup.supportsGlusterService()
            );
            return true;
        }}
    );

    private VdsDeployBase _deploy;
    private boolean _reboot = false;

    public VdsDeployMiscUnit(boolean reboot) {
        _reboot = reboot;
    }

    // VdsDeployUnit interface

    @Override
    public void setVdsDeploy(VdsDeployBase deploy) {
        _deploy = deploy;
    }

    @Override
    public void init() {
        _deploy.addCustomizationDialog(CUSTOMIZATION_DIALOG);
        if (_reboot) {
            _deploy.addCustomizationCondition(COND_HOST_REBOOT);
        }
    }

    @Override
    public boolean processEvent(Event.Base bevent) throws IOException {
        return true;
    }
}
