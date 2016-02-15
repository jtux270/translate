package org.ovirt.engine.core.bll.network.cluster;

import java.util.ArrayList;
import java.util.List;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.MultiLevelAdministrationHandler;
import org.ovirt.engine.core.bll.PredefinedRoles;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.network.NetworkParametersBuilder;
import org.ovirt.engine.core.bll.network.RemoveNetworkParametersBuilder;
import org.ovirt.engine.core.bll.utils.VersionSupport;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Permissions;
import org.ovirt.engine.core.common.businessentities.network.Network;
import org.ovirt.engine.core.common.businessentities.network.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.network.VnicProfile;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.Version;
import org.ovirt.engine.core.dal.dbbroker.DbFacade;
import org.ovirt.engine.core.utils.NetworkUtils;

/**
 * Class to hold common static methods that are used in several different places.
 */
public class NetworkHelper {

    /**
     * Grants permissions on the network entity to the given user
     *
     * @param userId
     *            the ID of the user to get the permission
     * @param networkId
     *            the Network ID
     */
    public static void addPermissionsOnNetwork(Guid userId, Guid networkId) {
        addPermission(userId, networkId, PredefinedRoles.NETWORK_ADMIN, VdcObjectType.Network);
    }

    /**
     * Grants permissions on the vnic profile entity to its creator and usage permission to 'everyone' if publicUse is
     * set to <code>true</code>
     *
     * @param userId
     *            the ID of the user to get the permission
     * @param vnicProfileId
     *            the VNIC Profile
     * @param publicUse
     *            Indicates of the network is intended for a public user
     */
    public static void addPermissionsOnVnicProfile(Guid userId, Guid vnicProfileId, boolean publicUse) {
        addPermission(userId, vnicProfileId, PredefinedRoles.NETWORK_ADMIN, VdcObjectType.VnicProfile);

        // if the profile is for public use, set EVERYONE as a VNICProfileUser on the profile
        if (publicUse) {
            addPermission(MultiLevelAdministrationHandler.EVERYONE_OBJECT_ID,
                    vnicProfileId,
                    PredefinedRoles.VNIC_PROFILE_USER,
                    VdcObjectType.VnicProfile);
        }
    }

    public static VnicProfile createVnicProfile(Network net) {
        VnicProfile profile = new VnicProfile();
        profile.setId(Guid.newGuid());
        profile.setName(net.getName());
        profile.setNetworkId(net.getId());
        profile.setPortMirroring(false);
        return profile;
    }

    private static void addPermission(Guid userId, Guid entityId, PredefinedRoles role, VdcObjectType objectType) {
        Permissions perms = new Permissions();
        perms.setad_element_id(userId);
        perms.setObjectType(objectType);
        perms.setObjectId(entityId);
        perms.setrole_id(role.getId());
        MultiLevelAdministrationHandler.addPermission(perms);
    }

    public static Network getNetworkByVnicProfileId(Guid vnicProfileId) {
        if (vnicProfileId == null) {
            return null;
        }

        VnicProfile vnicProfile = DbFacade.getInstance().getVnicProfileDao().get(vnicProfileId);
        return getNetworkByVnicProfile(vnicProfile);
    }

    public static Network getNetworkByVnicProfile(VnicProfile vnicProfile) {
        if (vnicProfile == null) {
            return null;
        }

        Network retVal = null;
        if (vnicProfile.getNetworkId() != null) {
            retVal = DbFacade.getInstance().getNetworkDao().get(vnicProfile.getNetworkId());
        }
        return retVal;
    }

    public static boolean isNetworkInCluster(Network network, Guid clusterId) {
        if (clusterId == null) {
            return false;
        }

        List<Network> networks = DbFacade.getInstance().getNetworkDao().getAllForCluster(clusterId);
        for (Network clusterNetwork : networks) {
            if (clusterNetwork.getId().equals(network.getId())) {
                return true;
            }
        }

        return false;
    }

    public static boolean setupNetworkSupported(Version version) {
        return VersionSupport.isActionSupported(VdcActionType.SetupNetworks, version);
    }

    public static void removeNetworkFromHostsInCluster(Network network, Guid clusterId, CommandContext context) {
        List<VdsNetworkInterface> nics = DbFacade.getInstance().getInterfaceDao().getAllInterfacesByLabelForCluster(clusterId, network.getLabel());
        removeNetworkFromHosts(network, context, nics);
    }

    public static void removeNetworkFromHostsInDataCenter(Network network, Guid dataCenterId, CommandContext context) {
        List<VdsNetworkInterface> nics = DbFacade.getInstance().getInterfaceDao().getAllInterfacesByLabelForDataCenter(dataCenterId, network.getLabel());
        removeNetworkFromHosts(network, context, nics);
    }

    private static void removeNetworkFromHosts(Network network, CommandContext context, List<VdsNetworkInterface> nics) {
        RemoveNetworkParametersBuilder builder = new RemoveNetworkParametersBuilder(network, context);
        ArrayList<VdcActionParametersBase> parameters = builder.buildParameters(nics);

        if (!parameters.isEmpty()) {
            NetworkParametersBuilder.updateParametersSequencing(parameters);
            Backend.getInstance().runInternalMultipleActions(VdcActionType.PersistentSetupNetworks, parameters, context);
        }
    }

    public static boolean shouldRemoveNetworkFromHostUponNetworkRemoval(Network persistedNetwork, Version compatibilityVersion) {
        return !persistedNetwork.isExternal() && NetworkUtils.isLabeled(persistedNetwork)
                && setupNetworkSupported(compatibilityVersion);
    }
}
