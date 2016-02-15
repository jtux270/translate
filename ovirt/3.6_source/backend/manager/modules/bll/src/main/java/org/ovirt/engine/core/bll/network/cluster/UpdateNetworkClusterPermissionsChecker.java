package org.ovirt.engine.core.bll.network.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.ovirt.engine.core.bll.ClusterPermissionsFinder;
import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.compat.Guid;

@Singleton
class UpdateNetworkClusterPermissionsChecker {

    private final ClusterPermissionsFinder clusterPermissionsFinder;

    @Inject
    UpdateNetworkClusterPermissionsChecker(ClusterPermissionsFinder clusterPermissionsFinder) {
        Objects.requireNonNull(clusterPermissionsFinder, "clusterPermissionsFinder cannot be null");

        this.clusterPermissionsFinder = clusterPermissionsFinder;
    }

    public boolean checkPermissions(CommandBase<?> command, Guid networkId, Guid clusterId, VdcActionType actionType) {
        final List<PermissionSubject> permissionCheckSubjects =
                findPermissionCheckSubjects(networkId, clusterId, actionType);

        return checkPermissions(command, permissionCheckSubjects);
    }

    /**
     * Checks the user has permissions either on one of the objects at least.
     */
    public boolean checkPermissions(CommandBase<?> command, List<PermissionSubject> permissionCheckSubjects) {
        final List<String> messages = new ArrayList<>();
        for (PermissionSubject permSubject : permissionCheckSubjects) {
            messages.clear();
            if (command.checkSinglePermission(permSubject, messages)) {
                return true;
            }
        }

        command.getReturnValue().getCanDoActionMessages().addAll(messages);

        return false;
    }

    public List<PermissionSubject> findPermissionCheckSubjects(Guid networkId, Guid clusterId, VdcActionType actionType) {

        List<PermissionSubject> permissions =
                clusterPermissionsFinder.findPermissionCheckSubjects(clusterId, actionType);
        permissions.add(new PermissionSubject(networkId, VdcObjectType.Network, ActionGroup.ASSIGN_CLUSTER_NETWORK));
        return permissions;
    }
}
