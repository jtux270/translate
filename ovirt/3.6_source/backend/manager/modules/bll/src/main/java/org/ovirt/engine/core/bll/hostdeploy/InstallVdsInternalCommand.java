package org.ovirt.engine.core.bll.hostdeploy;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.bll.LockMessagesMatchUtil;
import org.ovirt.engine.core.bll.NonTransactiveCommandAttribute;
import org.ovirt.engine.core.bll.VdsCommand;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.NetworkConfigurator;
import org.ovirt.engine.core.bll.network.cluster.ManagementNetworkUtil;
import org.ovirt.engine.core.bll.transport.ProtocolDetector;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.action.LockProperties;
import org.ovirt.engine.core.common.action.LockProperties.Scope;
import org.ovirt.engine.core.common.action.hostdeploy.InstallVdsParameters;
import org.ovirt.engine.core.common.businessentities.OpenstackNetworkProviderProperties;
import org.ovirt.engine.core.common.businessentities.Provider;
import org.ovirt.engine.core.common.businessentities.ProviderType;
import org.ovirt.engine.core.common.businessentities.VDS;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsProtocol;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.locks.LockingGroup;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.vdsbroker.ResourceManager;
import org.ovirt.engine.core.vdsbroker.vdsbroker.VDSNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonTransactiveCommandAttribute
public class InstallVdsInternalCommand<T extends InstallVdsParameters> extends VdsCommand<T> {

    private static Logger log = LoggerFactory.getLogger(InstallVdsInternalCommand.class);
    private VDSStatus vdsInitialStatus;

    @Inject
    private ManagementNetworkUtil managementNetworkUtil;

    @Inject
    private ResourceManager resourceManager;

    public InstallVdsInternalCommand(T parameters) {
        this(parameters, null);
    }

    public InstallVdsInternalCommand(T parameters, CommandContext commandContext) {
        super(parameters, commandContext);
    }

    @Override
    protected LockProperties applyLockProperties(LockProperties lockProperties) {
        return lockProperties.withScope(Scope.Execution);
    }

    @Override
    protected boolean canDoAction() {
        if (Guid.isNullOrEmpty(getVdsId())) {
            return failCanDoAction(EngineMessage.VDS_INVALID_SERVER_ID);
        }
        if (getVds() == null) {
            return failCanDoAction(EngineMessage.ACTION_TYPE_FAILED_HOST_NOT_EXIST);
        }
        return true;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        AuditLogType result = null;
        if (getSucceeded()) {
            result = AuditLogType.VDS_INSTALL;
        } else {
            // In case of failure - add to audit log the error as achieved from
            // the host
            addCustomValue("FailedInstallMessage", getErrorMessage(_failureMessage));
            result = AuditLogType.VDS_INSTALL_FAILED;
        }
        return result;
    }

    @Override
    protected void executeCommand() {
        if (getVds() == null) {
            return;
        }

        vdsInitialStatus = getVds().getStatus();
        installHost();
    }

    private void installHost() {
        try (final VdsDeploy deploy = new VdsDeploy("ovirt-host-deploy", getVds(), true)) {
            log.info(
                "Before Installation host {}, {}",
                getVds().getId(),
                getVds().getName()
            );

            T parameters = getParameters();
            deploy.setCorrelationId(getCorrelationId());
            boolean configureNetworkUsingHostDeploy = !FeatureSupported.setupManagementNetwork(
                getVds().getVdsGroupCompatibilityVersion()
            );

            deploy.addUnit(
                new VdsDeployMiscUnit(
                    (
                        parameters.isRebootAfterInstallation() &&
                        configureNetworkUsingHostDeploy
                    )
                ),
                new VdsDeployVdsmUnit(
                    (
                        configureNetworkUsingHostDeploy ?
                        managementNetworkUtil.getManagementNetwork(getVdsGroupId()).getName() :
                        null
                    )
                ),
                new VdsDeployPKIUnit(),
                new VdsDeployKdumpUnit()
            );

            if (parameters.getNetworkProviderId() != null) {
                Provider<?> provider = getDbFacade().getProviderDao().get(parameters.getNetworkProviderId());
                if (provider.getType() == ProviderType.OPENSTACK_NETWORK) {
                    OpenstackNetworkProviderProperties agentProperties =
                            (OpenstackNetworkProviderProperties) provider.getAdditionalProperties();
                    if (StringUtils.isNotBlank(parameters.getNetworkMappings())) {
                        agentProperties.getAgentConfiguration().setNetworkMappings(
                            parameters.getNetworkMappings()
                        );
                    }
                    deploy.addUnit(new VdsDeployOpenStackUnit(agentProperties));
                }
            }

            if (parameters.getOverrideFirewall()) {
                switch (getVds().getVdsType()) {
                    case VDS:
                        deploy.addUnit(new VdsDeployIptablesUnit());
                    break;
                    case oVirtNode:
                        log.warn(
                            "Installation of Host {} will ignore Firewall Override option, since it is not supported for Host type {}",
                            getVds().getName(),
                            getVds().getVdsType().name()
                        );
                    break;
                    default:
                        throw new IllegalArgumentException(
                            String.format(
                                "Not handled VDS type: %1$s",
                                getVds().getVdsType()
                            )
                        );
                }
            }

            if (parameters.getEnableSerialConsole()) {
                /* in 3.6.0 we always enable serial console without user intervention. */
                if (FeatureSupported.virtioSerialConsole(getVds().getVdsGroupCompatibilityVersion())) {
                    deploy.addUnit(new VdsDeployVmconsoleUnit());
                } else {
                    log.warn(
                            "Installation of Host {} will skip Virtio Serial Console, because it is not supported for clusterLevel {}",
                            getVds().getName(),
                            getVds().getVdsGroupCompatibilityVersion()
                    );
                }
            }

            switch (getParameters().getAuthMethod()) {
                case Password:
                    deploy.setPassword(parameters.getPassword());
                break;
                case PublicKey:
                    deploy.useDefaultKeyPair();
                break;
                default:
                    throw new Exception("Invalid authentication method value was sent to InstallVdsInternalCommand");
            }

            setVdsStatus(VDSStatus.Installing);
            deploy.execute();

            switch (deploy.getDeployStatus()) {
                case Failed:
                    throw new VdsInstallException(VDSStatus.InstallFailed, StringUtils.EMPTY);
                case Incomplete:
                    throw new VdsInstallException(VDSStatus.InstallFailed, "Partial installation");
                case Reboot:
                    setVdsStatus(VDSStatus.Reboot);
                    runSleepOnReboot(getStatusOnReboot());
                break;
                case Complete:
                    if (checkProtocolTofallback(getVds())) {
                        // we need to check whether we are connecting to vdsm which supports xmlrpc only
                        ProtocolDetector detector = new ProtocolDetector(getVds(), resourceManager);
                        if (!detector.attemptConnection()) {
                            detector.stopConnection();
                            if (detector.attemptFallbackProtocol()) {
                                detector.setFallbackProtocol();
                            } else {
                                throw new VdsInstallException(VDSStatus.InstallFailed, "Host not reachable");
                            }
                        }
                    }
                    if (!configureNetworkUsingHostDeploy) {
                        configureManagementNetwork();
                    }
                    if (!getParameters().getActivateHost() && VDSStatus.Maintenance.equals(vdsInitialStatus)) {
                        setVdsStatus(VDSStatus.Maintenance);
                    } else {
                        setVdsStatus(VDSStatus.Initializing);
                    }
                break;
            }

            log.info(
                "After Installation host {}, {}",
                getVds().getName(),
                getVds().getVdsType().name()
            );
            setSucceeded(true);
        } catch (VdsInstallException e) {
            handleError(e, e.getStatus());
        } catch (Exception e) {
            handleError(e, VDSStatus.InstallFailed);
        }
    }

    private boolean checkProtocolTofallback(VDS vds) {
        return VdsProtocol.STOMP.equals(vds.getProtocol());
    }

    private void configureManagementNetwork() {
        final NetworkConfigurator networkConfigurator = new NetworkConfigurator(getVds(), getContext());
        if (!networkConfigurator.awaitVdsmResponse()) {
            throw new VdsInstallException(
                VDSStatus.NonResponsive,
                "Network error during communication with the host"
            );
        }

        try {
            networkConfigurator.refreshNetworkConfiguration();
            networkConfigurator.createManagementNetworkIfRequired();
        } catch (VDSNetworkException e) {
            throw new VdsInstallException(
                VDSStatus.NonResponsive,
                "Network error during communication with the host",
                e
            );
        } catch (Exception e) {
            throw new VdsInstallException(
                VDSStatus.NonOperational,
                "Failed to configure management network on the host",
                e
            );
        }
    }

    @Override
    protected Map<String, Pair<String, String>> getExclusiveLocks() {
        return Collections.singletonMap(
                getParameters().getVdsId().toString(),
                LockMessagesMatchUtil.makeLockingPair(
                        LockingGroup.VDS,
                        EngineMessage.ACTION_TYPE_FAILED_OBJECT_LOCKED
                )
        );
    }

    private VDSStatus getStatusOnReboot() {
        if (getParameters().getActivateHost()) {
            return VDSStatus.NonResponsive;
        }
        return (VDSStatus.Maintenance.equals(vdsInitialStatus)) ? VDSStatus.Maintenance : VDSStatus.NonResponsive;
    }
}
