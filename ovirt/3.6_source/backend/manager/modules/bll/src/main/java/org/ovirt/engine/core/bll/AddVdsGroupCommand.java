package org.ovirt.engine.core.bll;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.ovirt.engine.core.bll.network.cluster.AddClusterNetworkClusterValidator;
import org.ovirt.engine.core.bll.network.cluster.DefaultManagementNetworkFinder;
import org.ovirt.engine.core.bll.network.cluster.NetworkClusterValidatorBase;
import org.ovirt.engine.core.bll.profiles.CpuProfileHelper;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.validator.ClusterValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.FeatureSupported;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.ManagementNetworkOnClusterOperationParameters;
import org.ovirt.engine.core.common.businessentities.SupportedAdditionalClusterFeature;
import org.ovirt.engine.core.common.businessentities.VDSGroup;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.NetworkCluster;
import org.ovirt.engine.core.common.businessentities.network.NetworkStatus;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.validation.group.CreateEntity;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.ClusterFeatureDao;
import org.ovirt.engine.core.dao.VdsGroupDao;
import org.ovirt.engine.core.dao.network.InterfaceDao;
import org.ovirt.engine.core.dao.network.NetworkClusterDao;
import org.ovirt.engine.core.dao.network.NetworkDao;

public class AddVdsGroupCommand<T extends ManagementNetworkOnClusterOperationParameters>
        extends VdsGroupOperationCommandBase<T> {

    public static final String DefaultNetworkDescription = "Default Management Network";

    @Inject
    private DefaultManagementNetworkFinder defaultManagementNetworkFinder;

    @Inject
    private VdsGroupDao vdsGroupDao;

    @Inject
    private NetworkClusterDao networkClusterDao;

    @Inject
    private NetworkDao networkDao;

    @Inject
    private ClusterFeatureDao clusterFeatureDao;

    @Inject
    private InterfaceDao interfaceDao;

    private Network managementNetwork;

    public AddVdsGroupCommand(T parameters) {
        super(parameters);
        setStoragePoolId(getVdsGroup().getStoragePoolId());
        updateMigrateOnError();
    }

    @Override
    protected void executeCommand() {
        VDSGroup cluster = getVdsGroup();
        cluster.setArchitecture(getArchitecture());

        checkMaxMemoryOverCommitValue();
        cluster.setDetectEmulatedMachine(true);
        vdsGroupDao.save(cluster);

        alertIfFencingDisabled();

        // add default network
        if (getParameters().getVdsGroup().getStoragePoolId() != null) {
            attachManagementNetwork();
        }

        // create default CPU profile for supported clusters.
        if (FeatureSupported.cpuQoS(getParameters().getVdsGroup().getCompatibilityVersion())) {
            getCpuProfileDao().save(CpuProfileHelper.createCpuProfile(getParameters().getVdsGroup().getId(),
                    getParameters().getVdsGroup().getName()));
        }

        if (CollectionUtils.isNotEmpty(cluster.getAddtionalFeaturesSupported())) {
            for (SupportedAdditionalClusterFeature feature : cluster.getAddtionalFeaturesSupported()) {
                feature.setClusterId(cluster.getId());
            }
            clusterFeatureDao.addAllSupportedClusterFeature(cluster.getAddtionalFeaturesSupported());
        }

        setActionReturnValue(cluster.getId());
        setSucceeded(true);
    }

    private void attachManagementNetwork() {
        final NetworkCluster networkCluster = createManagementNetworkCluster();
        networkCluster.setClusterId(getVdsGroupId());
        networkClusterDao.save(networkCluster);
    }

    private Guid getManagementNetworkId() {
        return getParameters().getManagementNetworkId();
    }

    private Network getManagementNetworkById() {
        final Guid managementNetworkId = getManagementNetworkId();
        return networkDao.get(managementNetworkId);
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        return getSucceeded() ? AuditLogType.USER_ADD_VDS_GROUP
                : AuditLogType.USER_ADD_VDS_GROUP_FAILED;
    }

    @Override
    protected void setActionMessageParameters() {
        super.setActionMessageParameters();
        addCanDoActionMessage(EngineMessage.VAR__ACTION__CREATE);
    }

    @Override
    protected boolean canDoAction() {
        final ClusterValidator validator = new ClusterValidator(getDbFacade(), getVdsGroup());

        return validate(validator.nameNotUsed())
                && validate(validator.cpuTypeSupportsVirtService())
                && validate(validator.versionSupported())
                && validate(validator.dataCenterVersionMismatch())
                && validate(validator.dataCenterExists())
                && validate(validator.localStoragePoolAttachedToSingleCluster())
                && validate(validator.qosBaloonSupported())
                && validate(validator.glusterServiceSupported())
                && validate(validator.clusterServiceDefined())
                && validate(validator.mixedClusterServicesSupported())
                && validate(validator.attestationServerConfigured())
                && validate(validator.migrationSupported(getArchitecture()))
                && validateClusterPolicy()
                && validate(validator.virtIoRngSupported())
                && validateManagementNetwork();
    }

    private boolean validateManagementNetwork() {
        if (getManagementNetworkId() == null) {
            return findDefaultManagementNetwork();
        } else {
            return validateInputManagementNetwork();
        }
    }

    private boolean findDefaultManagementNetwork() {
        managementNetwork =
                defaultManagementNetworkFinder.findDefaultManagementNetwork(getVdsGroup().getStoragePoolId());
        if (managementNetwork == null) {
            addCanDoActionMessage(EngineMessage.ACTION_TYPE_FAILED_DEFAULT_MANAGEMENT_NETWORK_NOT_FOUND);
            return false;
        }
        return true;
    }

    private boolean validateInputManagementNetwork() {
        managementNetwork = getManagementNetworkById();
        if (managementNetwork == null) {
            addCanDoActionMessage(EngineMessage.NETWORK_NOT_EXISTS);
            return false;
        }
        final NetworkClusterValidatorBase networkClusterValidator = createNetworkClusterValidator();
        return validate(networkClusterValidator.networkBelongsToClusterDataCenter(getVdsGroup(), managementNetwork))
                && validate(networkClusterValidator.managementNetworkRequired(managementNetwork))
                && validate(networkClusterValidator.managementNetworkNotExternal(managementNetwork));
    }

    private AddClusterNetworkClusterValidator createNetworkClusterValidator() {
        final NetworkCluster networkCluster = createManagementNetworkCluster();
        return new AddClusterNetworkClusterValidator(
                interfaceDao,
                networkDao,
                networkCluster,
                getVdsGroup().getCompatibilityVersion());
    }

    private NetworkCluster createManagementNetworkCluster() {
        return new NetworkCluster(
                getVdsGroupId(),
                managementNetwork.getId(),
                NetworkStatus.OPERATIONAL,
                true,
                true,
                true,
                true,
                false);
    }

    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        return Collections.singletonList(new PermissionSubject(getVdsGroup().getStoragePoolId(),
                VdcObjectType.StoragePool,
                getActionType().getActionGroup()));
    }

    @Override
    protected List<Class<?>> getValidationGroups() {
        addValidationGroup(CreateEntity.class);
        return super.getValidationGroups();
    }

}
